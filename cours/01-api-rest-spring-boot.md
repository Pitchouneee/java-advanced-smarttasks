# 01 – API REST avec Spring Boot

Ce premier module pose les bases du backend SmartTasks.  
L’objectif : comprendre comment fonctionne **Spring Boot**, créer une première API REST et structurer proprement le projet.

---

# 🎯 Objectifs du module

À la fin de ce chapitre, vous saurez :

* Créer un projet Spring Boot moderne (Java 21/25)
* Comprendre la structure d’une API REST (Controller → Service → Repository)
* Créer vos premiers endpoints REST
* Retourner des objets JSON
* Utiliser DTO & mapping propre
* Gérer les erreurs (Spring Boot Error Handling)

---

# 🚀 1. Création du projet Spring Boot

Allez sur **Spring Initializr**  
👉 https://start.spring.io/

Configuration recommandée :

| Option | Valeur |
|--------|--------|
| Project | Maven |
| Language | Java |
| Spring Boot | 3.3+ |
| Packaging | Jar |
| Java | 21 ou 25 |

### Dépendances à ajouter

* Spring Web  
* Spring Data JPA  
* Validation  
* PostgreSQL Driver (ou MariaDB)  
* Lombok  
* DevTools (optionnel)  

Générez et extrayez le projet dans `projet-back/` .

---

# 🧱 2. Structure d’un projet Spring Boot moderne

Spring organise automatiquement votre application :

```
src/main/java/com/smarttasks
 ┣ controller        # Entrée API (REST)
 ┣ service           # Logique métier
 ┣ repository        # Accès base de données (Spring Data JPA)
 ┣ domain            # Entités JPA
 ┗ SmartTasksApplication.java
```

⚠️ **Ne mettez jamais la logique dans les contrôleurs.**  
Le contrôleur = juste un point d’entrée HTTP.

---

# 📝 3. Premier Controller REST

Créez un fichier :  
 `controller/HelloController.java`

```java
@RestController
@RequestMapping("/api/hello")
public class HelloController {

    @GetMapping
    public Map<String, String> hello() {
        return Map.of("message", "Bienvenue dans SmartTasks !");
    }
}
```

Lancer l'application :

```bash
mvn spring-boot:run
```

Testez :  
➡️ http://localhost:8080/api/hello

---

# 🗂️ 4. Créer une première entité JPA : Project

 `domain/Project.java`

```java
@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
}
```

---

# 🛢️ 5. Repository JPA

 `repository/ProjectRepository.java`

```java
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
```

Spring génère automatiquement :
* `findAll()`
* `findById()`
* `save()`
* `deleteById()`
* etc.

---

# 🧠 6. Service métier

 `service/ProjectService.java`

```java
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository repository;

    public List<Project> findAll() {
        return repository.findAll();
    }

    public Project create(String name) {
        return repository.save(new Project(null, name));
    }
}
```

---

# 🌐 7. REST Controller pour Project

 `controller/ProjectController.java`

```java
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService service;

    @GetMapping
    public List<Project> findAll() {
        return service.findAll();
    }

    @PostMapping
    public Project create(@RequestBody Map<String, String> body) {
        return service.create(body.get("name"));
    }
}
```

Test :

```bash
curl -X POST http://localhost:8080/api/projects -H "Content-Type: application/json" -d '{"name":"Projet Demo"}'
```

---

# 🔧 8. Configuration de la base de données

 `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smarttasks
    username: smart
    password: smart
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

---

# 🧪 9. Gestion des erreurs

Créer un handler global :  
 `controller/ApiExceptionHandler.java`

```java
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound() {
        return Map.of("error", "Ressource introuvable");
    }
}
```

---

# 🎯 10. Exercices fin de module
1. Ajouter une entité **Task** (title, description, dueDate)
2. Ajouter les endpoints CRUD complets
3. Séparer avec des **DTO** (éviter d'exposer les entités)
4. Ajouter validation (`@NotBlank`, `@Length`, etc.)

---

# ➡️ Prochain chapitre

Passez au module suivant :  
👉 **02 – JPA & Relations**

Vous avez maintenant les bases de Spring Boot pour commencer SmartTasks 🚀
