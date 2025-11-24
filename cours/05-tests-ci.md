# 05 – Tests & Intégration Continue (CI)

Dans ce module, vous allez apprendre à tester votre API SmartTasks et mettre en place une pipeline d’intégration continue (CI) avec **GitHub Actions**.

---

# 🎯 Objectifs du module

À la fin du chapitre vous serez capables de :

* Écrire des tests unitaires avec **JUnit 5**
* Mock un service ou repository avec **Mockito**
* Tester l’API REST avec **MockMvc**
* Comprendre la différence entre tests unitaires et tests d’intégration
* Mettre en place une **CI GitHub Actions** :
  + build du projet
  + exécution des tests
  + génération d’un artefact

---

# 🧪 1. Dépendances de test

Ces dépendances sont souvent incluses automatiquement :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Elle inclut :
* **JUnit 5**
* **Mockito**
* **MockMvc**
* **AssertJ**

---

# 🧱 2. Structure des tests

Spring Boot crée automatiquement un dossier :

```
src/test/java/com/smarttasks
```

L’organisation recommandée est en miroir du code source :

```
src/main/java/com/smarttasks/service/ProjectService.java
src/test/java/com/smarttasks/service/ProjectServiceTest.java
```

---

# 🔍 3. Test unitaire d’un service (Mockito)

Exemple : tester `ProjectService` .

 `ProjectServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository repository;

    @InjectMocks
    private ProjectService service;

    @Test
    void shouldCreateProject() {
        Project p = new Project(null, "Demo");
        Project saved = new Project(1L, "Demo");

        Mockito.when(repository.save(any())).thenReturn(saved);

        Project result = service.create("Demo");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Demo");
    }
}
```

---

# 🌐 4. Test API REST avec MockMvc

Création d’un test d'intégration du contrôleur.

 `ProjectControllerTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void shouldReturnProjectList() throws Exception {
        mvc.perform(get("/api/projects"))
                .andExpect(status().isOk());
    }
}
```

MockMvc permet de tester l’API **sans lancer un serveur web complet**.

---

# 🧩 5. Test d’intégration complet

Un test qui charge tout le contexte Spring, la base H2 et exécute de vrais appels.

`application-test.yml` :

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driverClassName: org.h2.Driver
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: create-drop
```

Test :

```java
@SpringBootTest
class ProjectIntegrationTest {

    @Autowired
    private ProjectRepository repository;

    @Test
    void shouldSaveProjectInMemoryDb() {
        Project p = repository.save(new Project(null, "Test"));
        assertThat(repository.findById(p.getId())).isPresent();
    }
}
```

---

# 🔄 6. GitHub Actions – pipeline CI

Créer un fichier :

 `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - name: Build & Test
        run: mvn -B verify

      - name: Archive JAR
        uses: actions/upload-artifact@v4
        with:
          name: smarttasks-jar
          path: target/*.jar
```

Cette CI :

* compile votre projet
* exécute vos tests
* met à disposition un artifact téléchargeable

---

# 🌟 7. Bonus : couverture de test

Ajouter **JaCoCo** pour mesurer la couverture.

`pom.xml` :

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.11</version>
  <executions>
    <execution>
      <goals>
        <goal>prepare-agent</goal>
      </goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals>
        <goal>report</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Rapport après un build :  
➡️ `target/site/jacoco/index.html`

---

# 📝 Exercices
1. Ajouter des tests unitaires pour `TaskService`
2. Ajouter un test MockMvc pour la création d’un projet
3. Configurer JaCoCo pour imposer :
   - 80% de couverture sur les services
4. Ajouter un badge GitHub Actions dans README

---

# 📘 Prochain module

➡️ **06 – Upload de fichiers avec MinIO**

Vous savez maintenant garantir la qualité du code avec des tests et une CI solide 🚀
