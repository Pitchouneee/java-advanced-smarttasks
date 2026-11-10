package fr.corentinbringer.smarttasks.project.infrastructure.web;

import fr.corentinbringer.smarttasks.exception.ResourceNotFoundException;
import fr.corentinbringer.smarttasks.configuration.SecurityConfig;
import fr.corentinbringer.smarttasks.project.application.service.ProjectService;
import fr.corentinbringer.smarttasks.project.domain.model.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de la couche WEB uniquement (« slice ») : controller, validation, sérialisation JSON,
 * gestion des erreurs et sécurité. Le service est un mock : pas de base de données.
 *
 * - GOOGLE_CLIENT_ID : la propriété est exigée par application.yaml (audiences du JWT).
 * - @Import(SecurityConfig.class) : @WebMvcTest ne charge pas les classes @Configuration,
 *   sans cet import on testerait la sécurité par défaut de Spring, pas la nôtre.
 */
@WebMvcTest(controllers = ProjectController.class, properties = "GOOGLE_CLIENT_ID=test-client-id")
@Import(SecurityConfig.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProjectService projectService;

    /** Simule un utilisateur connecté : un JWT dont le "sub" vaut "alice", sans appeler Google. */
    private static RequestPostProcessor alice() {
        return jwt().jwt(token -> token.subject("alice"));
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(projectService);
    }

    @Test
    void createReturns201WithTheCreatedProject() throws Exception {
        when(projectService.create("Demo"))
                .thenReturn(new Project(1L, "alice", "Demo", Instant.parse("2026-01-10T08:00:00Z")));

        mvc.perform(post("/api/projects").with(alice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Demo"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Demo"))
                .andExpect(jsonPath("$.createdOn").value("2026-01-10T08:00:00Z"))
                // Le tenantId est une donnée interne : il ne doit pas fuiter dans l'API
                .andExpect(jsonPath("$.tenantId").doesNotExist());
    }

    @Test
    void createRejectsAnInvalidNameWith400() throws Exception {
        mvc.perform(post("/api/projects").with(alice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details.name").exists());

        verifyNoInteractions(projectService);
    }

    @Test
    void createRejectsAMissingNameWith400() throws Exception {
        // {} → name = null : @Size seul l'accepterait (null est "valide" pour @Size), d'où @NotBlank
        mvc.perform(post("/api/projects").with(alice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());

        verifyNoInteractions(projectService);
    }

    @Test
    void listUsesTheStablePageFormatExpectedByTheFront() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        when(projectService.findAll(any()))
                .thenReturn(new PageImpl<>(List.of(new Project(1L, "alice", "Demo", Instant.now())), pageable, 1));

        mvc.perform(get("/api/projects").with(alice()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Demo"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    // --- Erreurs standard de Spring MVC : bon code HTTP, même format JSON que nos erreurs ---

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/projects").with(alice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ name: oublie les guillemets"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(projectService);
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mvc.perform(get("/api/projects/abc").with(alice()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void unsupportedHttpMethodReturns405WithAllowHeader() throws Exception {
        mvc.perform(delete("/api/projects").with(alice()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void wrongContentTypeReturns415() throws Exception {
        mvc.perform(post("/api/projects").with(alice())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Demo"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void unknownProjectReturns404() throws Exception {
        when(projectService.findById(99L)).thenThrow(new ResourceNotFoundException("Project not found"));

        mvc.perform(get("/api/projects/99").with(alice()))
                .andExpect(status().isNotFound());

        verify(projectService).findById(99L);
    }
}
