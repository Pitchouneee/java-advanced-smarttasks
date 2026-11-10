# 07 – Tests automatisés

Au module 06, nous avons affirmé que l'architecture hexagonale rendait le code **testable**. Il est temps de le prouver.

Nous allons écrire trois types de tests, du plus rapide au plus complet :

| Type | Ce qu'on teste | Outils | Durée d'exécution |
|---|---|---|---|
| **Unitaire** | Un service, seul | JUnit 5, Mockito | quelques millisecondes |
| **Slice web** | Un controller : JSON, validation, erreurs, sécurité | `@WebMvcTest`, MockMvc | ~2 secondes |
| **Intégration** | Toute l'application contre un vrai PostgreSQL | `@SpringBootTest`, Testcontainers | ~10 secondes (+ téléchargement de l'image la 1re fois) |

> 🔺 **La pyramide des tests** : beaucoup de tests unitaires (rapides, précis), quelques tests de slice, et peu de tests d'intégration (lents, mais ce sont les seuls à vérifier que tout fonctionne ensemble).

⏱️ Durée indicative : 2 h. Le corrigé se trouve dans `solutions/after-clean-architecture/src/test`.

---

# 🎯 Objectifs du module

✅ Écrire un **test unitaire** d'un service en remplaçant ses ports par des **mocks**.
✅ Tester un controller avec **MockMvc**, y compris la sécurité JWT, sans appeler Google.
✅ Écrire un **test d'intégration** avec **Testcontainers** pour vérifier l'**isolation entre tenants**.
✅ Savoir quel type de test choisir selon ce qu'on veut vérifier.

---

# 📦 0. Dépendances

Spring Boot 4 fournit un starter de test par module. Le projet en contient déjà plusieurs (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`...), qui apportent JUnit 5, Mockito, AssertJ et MockMvc. Ajoutez :

```xml
<!-- jwt() pour simuler un utilisateur connecté dans MockMvc -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers : un vrai PostgreSQL dans Docker, le temps des tests -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

Pas de version : le parent Spring Boot les fixe (Testcontainers 2.x).

> 🗑️ Supprimez le test `SmarttasksApplicationTests` généré par Spring Initializr. Il démarre toute l'application, donc il échoue dès que PostgreSQL ou RustFS ne tournent pas, et il ne vérifie rien d'utile. Le test d'intégration du §3 le remplace.

---

# 🧪 1. Test unitaire d'un service

Un test unitaire vérifie **une classe, seule**. Pas de Spring, pas de base de données. C'est possible parce que depuis le module 06, `TaskService` dépend d'**interfaces** (`TaskPort`, `AttachmentPort`) : on peut les remplacer par de fausses implémentations, des **mocks**.

`src/test/java/.../project/application/service/TaskServiceTest.java` :

```java
@ExtendWith(MockitoExtension.class)            // active Mockito, sans Spring
class TaskServiceTest {

    @Mock private TaskPort taskPort;            // faux port : ne fait rien, sauf ce qu'on lui dit
    @Mock private ProjectService projectService;
    @Mock private AttachmentPort attachmentPort;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TaskService taskService;            // le vrai service, construit avec les mocks

    @BeforeEach
    void setUp() {
        TenantContext.setTenant("alice");       // en production, c'est le TenantFilter qui le fait
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();                  // ThreadLocal : toujours nettoyer !
    }

    @Test
    void create_attachesTheTaskToTheProjectWithTheCurrentTenant() {
        // GIVEN : le projet 42 appartient à Alice
        when(projectService.findById(42L)).thenReturn(new Project(42L, "alice", "Demo", Instant.now()));
        when(taskPort.save(any())).then(returnsFirstArg());   // save renvoie ce qu'on lui passe

        // WHEN
        taskService.create(42L, new CreateTaskCommand("Écrire les tests", null, null));

        // THEN : on récupère la tâche réellement envoyée au port
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskPort).save(captor.capture());

        assertThat(captor.getValue().tenantId()).isEqualTo("alice");
        assertThat(captor.getValue().projectId()).isEqualTo(42L);
        assertThat(captor.getValue().completed()).isFalse();
    }

    @Test
    void create_failsWithoutSavingWhenTheProjectIsNotOwnedByTheCurrentTenant() {
        when(projectService.findById(99L)).thenThrow(new ResourceNotFoundException("Project not found"));

        assertThatThrownBy(() -> taskService.create(99L, new CreateTaskCommand("Intrusion", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(taskPort);          // rien n'a été écrit
    }
}
```

Le vocabulaire Mockito à connaître :

| Instruction | Rôle |
|---|---|
| `when(mock.methode(args)).thenReturn(valeur)` | programme la réponse du mock |
| `.thenThrow(exception)` | simule une erreur |
| `.then(returnsFirstArg())` | renvoie le 1er argument reçu (pratique pour `save`) |
| `verify(mock).methode(args)` | vérifie que la méthode a été appelée |
| `verifyNoInteractions(mock)` | vérifie qu'on n'a **pas** touché au mock |
| `ArgumentCaptor` | récupère l'objet passé en argument pour l'inspecter |
| `any()` | n'importe quel argument |

> ⚠️ **Mockito est strict par défaut** : un `when(...)` qui ne sert pas dans le test fait échouer le test (`UnnecessaryStubbingException`). Ce n'est pas un bug : un stub inutile signale souvent un test qui ne teste pas ce qu'il croit.

> 💡 **Pourquoi ce test est rapide ?** Pas de contexte Spring, pas de base : il s'exécute en quelques millisecondes. On peut en écrire des centaines et les lancer à chaque modification. C'est le bénéfice concret des ports du module 06 : avec un `TaskService` qui dépendait directement de `TaskRepository` (JPA), il aurait fallu une base de données.

**Exercice 1 :** Écrivez `AttachmentServiceTest` pour vérifier la **compensation** de l'upload (module 05, §5.2). Trois cas :

1. Si `attachmentPort.save(...)` lève une exception, alors `fileStoragePort.deleteFile(objectKey)` est appelé, et c'est **la même** exception qui remonte (`assertThatThrownBy(...).isSameAs(erreur)`).
2. Si en plus la suppression échoue (`doThrow(...).when(fileStoragePort).deleteFile(...)`), c'est toujours l'erreur d'origine qui remonte.
3. Un fichier vide lève une `InvalidRequestException` **sans** toucher au stockage.

Astuce pour créer un fichier de test : `new FileContent("a.pdf", "application/pdf", 3, new ByteArrayInputStream(new byte[3]))`.

> 🤔 Essayez de tester ce scénario « à la main » avec Postman : il faudrait couper PostgreSQL au bon moment, entre l'upload et l'insertion. Avec des mocks, c'est une ligne.

---

# 🌐 2. Test de la couche web (`@WebMvcTest`)

Le test unitaire ne vérifie pas le JSON, les codes HTTP, la validation ni la sécurité. Pour ça, on démarre **uniquement la couche web** de Spring (une « slice ») : controllers, `ApiExceptionHandler`, filtres et sécurité. Le service reste un mock.

```java
@WebMvcTest(controllers = ProjectController.class, properties = "GOOGLE_CLIENT_ID=test-client-id")
@Import(SecurityConfig.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mvc;                         // envoie des requêtes HTTP simulées

    @MockitoBean
    private ProjectService projectService;       // mock enregistré comme bean Spring

    private static RequestPostProcessor alice() {
        return jwt().jwt(token -> token.subject("alice"));   // utilisateur connecté, sans Google
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());
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
                .andExpect(jsonPath("$.name").value("Demo"))
                .andExpect(jsonPath("$.createdOn").value("2026-01-10T08:00:00Z"))
                .andExpect(jsonPath("$.tenantId").doesNotExist());   // donnée interne : ne doit pas fuiter
    }

    @Test
    void createRejectsAnInvalidNameWith400() throws Exception {
        mvc.perform(post("/api/projects").with(alice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());

        verifyNoInteractions(projectService);    // la validation a bloqué avant le service
    }
}
```

Trois détails qui font perdre du temps :

* **`properties = "GOOGLE_CLIENT_ID=..."`** : `application.yaml` exige cette variable (`audiences`, module 03). Sans elle, le contexte ne démarre pas.
* **`@Import(SecurityConfig.class)`** : `@WebMvcTest` ne charge **pas** les classes `@Configuration`. Sans cet import, on testerait la sécurité par défaut de Spring, pas la nôtre.
* **`jwt()`** (de `spring-security-test`) crée directement un utilisateur authentifié avec le `sub` voulu : pas d'appel à Google, pas de vrai token. Le `TenantFilter` lit ce `sub` comme en production.

> 💡 Ce test a aussi une valeur de **documentation** : il fige le contrat de l'API (`201`, format des erreurs, format des pages `$.page.totalElements`). Si quelqu'un casse le format attendu par le front, le test échoue.

> 🧪 Le corrigé vérifie aussi les erreurs standard de Spring MVC (module 01, §9.3) : JSON mal formé → `400`, `/api/projects/abc` → `400`, `DELETE /api/projects` → `405` avec le header `Allow`, `Content-Type: text/plain` → `415`. Retirez le `extends ResponseEntityExceptionHandler` et relancez : ces tests passent au rouge (`500`).

**Exercice 2 :** Écrivez `TaskInProjectControllerTest` :

1. `POST /api/projects/1/tasks` sans token → `401`.
2. Avec un titre vide → `400`, et le service n'est jamais appelé.
3. Avec un titre valide → `201`, et le JSON contient `projectId` et `completed: false`.
4. Si le service lève `ResourceNotFoundException` (projet d'un autre utilisateur) → `404`.

---

# 🐳 3. Test d'intégration : l'isolation entre tenants

Les tests précédents utilisent des mocks : ils ne prouvent pas que les **requêtes SQL** filtrent bien par tenant. Une erreur dans un `@Query` passerait inaperçue. Pour la promesse centrale du projet (« un utilisateur ne voit jamais les données d'un autre »), on veut un test de bout en bout, contre un **vrai** PostgreSQL.

**Testcontainers** lance un conteneur Docker au début des tests et le détruit à la fin :

```java
@SpringBootTest(properties = "GOOGLE_CLIENT_ID=test-client-id")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)    // sans Docker : test ignoré, pas en échec
class TenantIsolationIntegrationTest {

    @Container
    @ServiceConnection                            // Spring Boot récupère URL / user / mot de passe du conteneur
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @MockitoBean
    private S3Client s3Client;                    // ce test porte sur les données, pas sur le stockage

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

        // Bob ne le voit pas dans sa liste, ni en accès direct (404 : on ne révèle pas son existence)
        mvc.perform(get("/api/projects").with(user("bob")))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/api/projects/" + projectId).with(user("bob")))
                .andExpect(status().isNotFound());

        // ... et ne peut pas y ajouter de tâche (vérification du parent, module 03 §5.4)
        mvc.perform(post("/api/projects/" + projectId + "/tasks").with(user("bob"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Intrusion"}
                                """))
                .andExpect(status().isNotFound());
    }
}
```

* **`@ServiceConnection`** remplace `spring.datasource.*` : pas d'URL à recopier, le port du conteneur est choisi au hasard pour ne pas entrer en conflit avec votre PostgreSQL local.
* **`static`** sur le conteneur : il est démarré **une fois** pour toute la classe, pas pour chaque test.
* **`@MockitoBean S3Client`** : RustFS n'est pas nécessaire ici. Au démarrage, l'application vérifie le bucket (`headBucket`) : le mock répond sans rien faire.
* La première exécution télécharge l'image `postgres:18` : comptez une minute.

> ⚖️ **Pourquoi pas H2 en mémoire ?** H2 est plus rapide, mais ce n'est pas PostgreSQL : types (`text`, `oid`), fonctions et contraintes diffèrent. Un test qui passe sur H2 peut échouer en production. Avec Testcontainers, on teste contre **la même base** qu'en production.

**Exercice 3 :** Complétez le test avec les autres accès de Bob, qui doivent tous répondre `404` :

1. `GET /api/tasks/{id}` sur une tâche d'Alice ;
2. `PUT /api/projects/{id}` et `DELETE /api/projects/{id}` sur le projet d'Alice ;
3. Vérifiez ensuite, avec Alice, que son projet n'a **pas** été renommé ni supprimé.

> 🔥 **Exercice « mutation »** : dans `ProjectRepository` (JPA), retirez temporairement la clause `WHERE p.tenantId = :tenantId` de la requête `findAllByTenantId`. Relancez : le test doit **échouer** (Bob voit le projet d'Alice dans sa liste, `totalElements` vaut 1). Remettez la clause. Un test qui ne casse jamais ne protège de rien : c'est ainsi qu'on vérifie qu'il sert à quelque chose.

---

# ▶️ 4. Lancer les tests

```bash
./mvnw test
```

Ou dans IntelliJ : clic droit sur `src/test/java` → **Run 'All Tests'**.

Résultat attendu sur le corrigé : 25 tests, tous verts (ou 23 verts + 2 ignorés si Docker ne tourne pas). Le corrigé contient aussi `TaskTest`, un test du **domaine** pur (module 06 §2.1) : ni mock, ni Spring.

> ℹ️ Avec Java 21+, Mockito affiche `Mockito is currently self-attaching to enable the inline-mock-maker...`. C'est un avertissement, pas une erreur : les futures versions du JDK interdiront ce mécanisme, et il faudra déclarer Mockito comme agent Java dans le `maven-surefire-plugin`.

---

# 🧭 5. Quel test écrire ?

| Je veux vérifier... | Test |
|---|---|
| une règle métier, un calcul, un cas d'erreur, une compensation | **unitaire** (service + mocks) |
| un code HTTP, le format JSON, la validation, « sans token → 401 » | **`@WebMvcTest`** |
| une requête SQL, une cascade JPA, l'isolation entre tenants | **intégration** (Testcontainers) |

---

# 🤖 Bonus : intégration continue (GitHub Actions)

Des tests qu'on oublie de lancer ne servent à rien. Une **CI** les lance automatiquement à chaque `push` et sur chaque Pull Request.

Créez `.github/workflows/ci.yml` à la racine du dépôt :

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  test:
    runs-on: ubuntu-latest           # Docker est disponible : Testcontainers fonctionne
    defaults:
      run:
        working-directory: projet-back
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
          cache: maven
      - run: ./mvnw -B verify
```

Sur GitHub, l'onglet **Actions** affiche le résultat. Une Pull Request dont les tests échouent affiche une croix rouge : on ne la fusionne pas.

---

# ➡️ Prochain module

Votre application est structurée **et** testée.
Prenons maintenant de la hauteur : que se passerait-il si SmartTasks devait grandir au point d'être découpé en plusieurs services ?

Passez au chapitre suivant : **08 – Monolithe vs Microservices**.
