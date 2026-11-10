package fr.corentinbringer.smarttasks;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import software.amazon.awssdk.services.s3.S3Client;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'INTÉGRATION : toute l'application (sécurité, TenantFilter, services, JPA)
 * contre un vrai PostgreSQL lancé dans Docker par Testcontainers.
 *
 * C'est le test le plus important du projet : il vérifie la promesse du multi-tenant,
 * « un utilisateur ne voit et ne modifie jamais les données d'un autre ».
 *
 * - disabledWithoutDocker : sans Docker, le test est ignoré (au lieu d'échouer).
 * - @ServiceConnection : Spring Boot lit l'URL, l'utilisateur et le mot de passe du conteneur,
 *   plus besoin de les recopier dans spring.datasource.*.
 * - S3Client est mocké : ce test porte sur l'isolation des données, pas sur le stockage.
 */
@SpringBootTest(properties = "GOOGLE_CLIENT_ID=test-client-id")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class TenantIsolationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @MockitoBean
    private S3Client s3Client;

    @Autowired
    private MockMvc mvc;

    private static RequestPostProcessor user(String sub) {
        return jwt().jwt(token -> token.subject(sub));
    }

    @Test
    void aUserCanNeitherReadNorWriteIntoAnotherUsersProject() throws Exception {
        // Alice crée un projet
        String body = mvc.perform(post("/api/projects").with(user("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Projet d'Alice"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long projectId = ((Number) JsonPath.read(body, "$.id")).longValue();

        // Bob ne le voit pas dans sa liste...
        mvc.perform(get("/api/projects").with(user("bob")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));

        // ... ni en accès direct par son ID : 404, comme s'il n'existait pas
        mvc.perform(get("/api/projects/" + projectId).with(user("bob")))
                .andExpect(status().isNotFound());

        // ... et ne peut pas y ajouter de tâche (vérification du parent, chap. 03 §5.4)
        mvc.perform(post("/api/projects/" + projectId + "/tasks").with(user("bob"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Intrusion"}
                                """))
                .andExpect(status().isNotFound());

        // Alice, elle, voit toujours son projet, sans la tâche de Bob
        mvc.perform(get("/api/projects/" + projectId).with(user("alice")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/projects/" + projectId + "/tasks").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void activeProjectsAreThoseWithAtLeastOneOpenTask() throws Exception {
        long withOpenTask = createProject("carol", "Avec une tâche en cours");
        createProject("carol", "Vide");

        mvc.perform(post("/api/projects/" + withOpenTask + "/tasks").with(user("carol"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "À faire"}
                                """))
                .andExpect(status().isCreated());

        // 2 projets, mais un seul "actif" ; et rien de ceux d'Alice (autre tenant)
        mvc.perform(get("/api/dashboard").with(user("carol")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeProjectsCount").value(1))
                .andExpect(jsonPath("$.totalTasksCount").value(1));
    }

    private long createProject(String sub, String name) throws Exception {
        String body = mvc.perform(post("/api/projects").with(user(sub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
