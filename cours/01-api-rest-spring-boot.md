# 01 – API REST avec Spring Boot

Ce premier module pose les fondations de votre backend **SmartTasks**. 
Nous n'allons pas simplement "écrire du code", mais comprendre comment Spring Boot orchestre les composants d'une application professionnelle.

---

## 🎯 Objectifs du module

À la fin de ce module, vous serez capables de :

✅ Créer un projet Spring Boot (Java 25)  
✅ Comprendre la structure en **couches** d’une application (Controller → Service → Repository → Model)  
✅ Créer vos premiers endpoints REST (GET, POST)  
✅ Retourner des objets en **JSON**  
✅ Gérer les erreurs de manière propre et centralisée

> Ce module introduit également **Spring Data JPA**, mais les relations entre entités et les requêtes avancées seront détaillées au chapitre **02 – JPA & Relations**.

---

## 1. 🚀 Création du projet Spring Boot

Allez sur [https://start.spring.io](https://start.spring.io)

Voici la configuration recommandée :

| Option         | Valeur           |
|----------------|------------------|
| Project        | Maven            |
| Language       | Java             |
| Spring Boot    | 4.0              |
| Packaging      | Jar              |
| Java           | 25               |

### 📦 Dépendances à ajouter

- Spring Web  
- Spring Data JPA  
- Validation  
- PostgreSQL Driver  
- Lombok  
- Spring Boot DevTools *(optionnel mais pratique)*

> 🔎 **Dans le `pom.xml` généré**, les noms ne sont pas les mêmes que sur start.spring.io. Spring Boot 4 a découpé et renommé ses starters :
>
> | start.spring.io | `artifactId` dans le pom |
> |---|---|
> | Spring Web | `spring-boot-starter-webmvc` (s'appelait `spring-boot-starter-web` en Boot 3) |
> | Spring Data JPA | `spring-boot-starter-data-jpa` |
> | Validation | `spring-boot-starter-validation` |
>
> Initializr ajoute aussi un starter de **test par module** (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`...) au lieu de l'unique `spring-boot-starter-test` de Boot 3. Méfiez-vous des tutoriels écrits pour Boot 3 : les anciens noms sont souvent dépréciés.

🗂️ Générez le projet et extrayez-le dans le dossier `projet-back/`.

---

## 2. 🧠 Théorie : le cœur de Spring

### 2.1. L'Inversion de Contrôle (IoC)

Dans une application Java classique, c'est vous qui instanciez les objets (`new Service()`). Avec Spring, c'est le **Framework** qui crée et gère les objets pour vous. Ces objets gérés s'appellent des **Beans**.

Le conteneur Spring (ApplicationContext) :

1. Scanne votre code au démarrage
2. Détecte les classes annotées (`@Component`, `@Service`, `@RestController`, `@Repository`)
3. Les instancie (singleton par défaut) et les injecte là où c'est nécessaire

### 2.2. Architecture en couches

Nous respectons une séparation stricte des responsabilités (SoC) :

1. **Presentation** (`controller`) : Reçoit la requête HTTP, valide les entrées, appelle le Service. Aucune logique métier ici
2. **Application** (`service`) : Contient les règles métier, les validations complexes, l'orchestration
3. **Infrastructure** (`repository`) : Dialogue avec la base de données via JPA

```
src/main/java/fr/corentinbringer/smarttasks
 ┣ 📂 project               # Tout ce qui concerne les projets (puis les tâches)
 ┃ ┣ 📂 controller          # Entrée de l’API (REST Controllers)
 ┃ ┣ 📂 service             # Logique métier
 ┃ ┣ 📂 repository          # Accès à la base de données (DAO)
 ┃ ┗ 📂 model               # Modèles de données (entités, puis DTO)
 ┣ 📂 exception             # Gestion des erreurs, commune à toute l'application (§9)
 ┗ SmarttasksApplication.java
```

> 🗂️ **Par fonctionnalité, puis par couche.** On regroupe d'abord par **fonctionnalité** (`project`, plus tard `dashboard`), puis par couche à l'intérieur. Quand l'application grandit, tout ce qui concerne les projets reste au même endroit, au lieu d'être éparpillé dans quatre dossiers géants (`controller/`, `service/`...) partagés par toutes les fonctionnalités. Au module 06, on remplacera ces couches par `domain` / `application` / `infrastructure`, toujours à l'intérieur de `project`.

| Couche        | Rôle principal                                                   |
|---------------|------------------------------------------------------------------|
| Controller    | Gère les requêtes HTTP (GET, POST, etc.)                         |
| Service       | Contient la logique métier de l’application                      |
| Repository    | Dialogue avec la base de données (via Spring Data JPA)           |
| Model         | Représente les objets du modèle (ex : `Project`, `Task`, etc.)   |

> ⚠️ Les controllers ne doivent contenir **aucune logique métier** : ce sont juste des "passerelles" entre le web et votre application.
> 🧼 Cette séparation permet un code **modulaire**, **testable** et **maintenable**.

---

## 3. 📝 Premier Controller REST

Créez un fichier `HelloController.java` directement dans le package racine `fr.corentinbringer.smarttasks` (c'est un simple test, vous pourrez le supprimer ensuite) :

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

Explication :

- `@RestController` indique que cette classe gère des requêtes HTTP et renvoie du JSON
- `@RequestMapping("/api/hello")` : toutes les routes commencent par ce préfixe
- `@GetMapping` : correspond à une requête GET
- La méthode retourne un `Map<String, String>`, automatiquement convertie en JSON

Testez dans votre navigateur :  
➡️ http://localhost:8080/api/hello

---

## 4. 🗃️ Création d’une entité simple

On va maintenant créer une **entité** nommée `Project`.

Créez le fichier `project/model/Project.java` :

```java
@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
}
```

> 💡 Ici, on utilise **Lombok** (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`) pour générer automatiquement getters, setters et constructeurs.

**NB :** L’explication des relations JPA (`@OneToMany`, `@ManyToOne`, etc.) sera faite au **prochain module**.

---

## 5. 📥 Repository : accès base de données

Créez `project/repository/ProjectRepository.java` :

```java
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
```

### Pourquoi utiliser Spring Data JPA ?

Grâce à **Spring Data JPA**, aucun code SQL à écrire ici :  
le framework fournit automatiquement les méthodes comme :

- `findAll()`
- `findById(id)`
- `save(entity)`
- `deleteById(id)`

> ⚠️ Le Repository ne contient **aucune logique métier**, il est uniquement responsable de l’accès aux données.

En résumé : 

- ✅ Pas besoin d’écrire les requêtes SQL les plus courantes (`findAll`, `save`, `deleteById`, etc.)
- 📦 Requêtes dérivées automatiques à partir du nom des méthodes
- 🔁 Intégration naturelle avec JPA et Hibernate

Mais attention :

> ⚠️ Il est **tout à fait possible et parfois recommandé** d’écrire :
> - des requêtes JPQL (`@Query`) pour des accès précis
> - des requêtes SQL natives si performance ou logique trop spécifique

🎯 **L’arbitrage** se fait en fonction de :
- la complexité de la requête
- les performances attendues
- la lisibilité/maintenabilité du code

---

## 6. 💼 Service métier

Créez `project/service/ProjectService.java` :

```java
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    public Project create(String name) {
        return projectRepository.save(new Project(null, name));
    }
}
```

### Pourquoi une couche Service ?

Elle permet de :

- **Centraliser** la logique métier (ex. : vérifier des règles) et d'éviter la duplication
- **Préparer** les données à évoluer (ex. : filtrer, transformer, ajout de règles, validations, traitements)
- **Cacher** les détails d’implémentation aux contrôleurs
- **Séparer** les responsabilités entre les couches (Single Responsibility Principle)

On pourrait par exemple ici :
- refuser un nom vide
- logguer la création
- déclencher une notification

> 👉 C’est une **bonne pratique** de **ne jamais appeler un Repository directement dans un Controller**.
> 🔁 La couche service peut être testée indépendamment du contrôleur, ce qui améliore la maintenabilité.

---

## 7. 🌐 Créer les endpoints REST

`project/controller/ProjectController.java`

```java
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<Project> findAll() {
        return projectService.findAll();
    }

    @PostMapping
    public Project create(@RequestBody Map<String, String> body) {
        return projectService.create(body.get("name"));
    }
}
```

> ⚠️ Ce code est volontairement simple. On ajoutera les **DTO** et la **validation** dans les exercices finaux de ce chapitre.

---

## 8. ⚙️ Configurer PostgreSQL

Dans `src/main/resources/application.yaml` :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smarttasks
    username: smart
    password: smart
  jpa:
    hibernate:
      ddl-auto: update # Crée / complète les tables au démarrage (dev uniquement, voir ci-dessous)
    show-sql: true     # Affiche les requêtes SQL dans la console
```

### Explications

- `ddl-auto` indique à Hibernate quoi faire du schéma de la base au démarrage, à partir des entités :

| Valeur | Effet au démarrage | Usage |
|---|---|---|
| `none` | rien | production (avec un outil de migration) |
| `validate` | vérifie que les tables correspondent aux entités, sinon refuse de démarrer | production |
| `update` | crée les tables et colonnes **manquantes**, garde les données | **développement (notre choix)** |
| `create` | supprime puis recrée toutes les tables : **les données sont perdues** à chaque redémarrage | démos, tests |
| `create-drop` | comme `create`, et supprime tout à l'arrêt | tests |

> ⚠️ **Les limites de `update`** : il **ajoute** des tables et des colonnes, mais ne supprime, ne renomme et ne retype **jamais** rien. Si vous renommez un champ, l'ancienne colonne reste (et si elle est `NOT NULL`, vos insertions échouent). Si vous changez un type, la colonne garde l'ancien. Dans ce cas, repartez d'une base vide : `docker compose down -v` puis `docker compose up -d` (le `-v` supprime les volumes, donc les données).
>
> En production, le schéma n'est jamais modifié par Hibernate : on écrit des scripts de migration versionnés avec **Flyway** ou **Liquibase**, et on met `ddl-auto: validate`.
- `show-sql: true` est utile pour **comprendre ce que fait Hibernate**
- En production, ces options sont souvent désactivées pour plus de contrôle

> 🔒 Ne pas exposer ces infos de connexion dans un repo public ! Utilisez un `.env` ou des variables d’environnement.

---

## 9. 🛡️ Gestion des erreurs

Que doit répondre `GET /api/projects/42` si le projet n'existe pas ? Pas une erreur 500 avec une stacktrace, mais un `404` propre. On va :

1. définir **nos propres exceptions**, qui disent ce qui s'est passé ;
2. les traduire **en un seul endroit** en codes HTTP.

### 9.1. Une hiérarchie d'exceptions *sealed* (Java 17+)

Créez dans le package `exception` :

```java
// SmartTasksException.java
public sealed abstract class SmartTasksException extends RuntimeException
        permits ResourceNotFoundException, InvalidRequestException {

    protected SmartTasksException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
// ResourceNotFoundException.java
public final class ResourceNotFoundException extends SmartTasksException {
    public ResourceNotFoundException(String message) {
        super(message, null);
    }
}
```

```java
// InvalidRequestException.java
public final class InvalidRequestException extends SmartTasksException {
    public InvalidRequestException(String message) {
        super(message, null);
    }
}
```

* **`sealed ... permits`** : seules les classes listées peuvent hériter de `SmartTasksException`. Le compilateur connaît donc **toute** la liste.
* **`final`** sur les sous-classes : la hiérarchie s'arrête là. (Une sous-classe d'une classe *sealed* doit être `final`, `sealed` ou `non-sealed`.)
* Les classes sont dans le **même package** : c'est obligatoire pour une hiérarchie *sealed* hors module Java.

Le service les utilise :

```java
public Project findById(Long id) {
    return projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " not found"));
}
```

> 🤔 **Pourquoi pas `NoSuchElementException` ?** C'est tentant (c'est ce que lève `Optional.orElseThrow()`), mais cette exception peut venir de n'importe où : un `Optional.get()` oublié, un `iterator.next()` de trop... Si on la traduit en `404`, un **bug** devient « ressource introuvable » : le client ne comprend pas, et personne ne voit d'erreur 500 dans les logs. Avec notre propre exception, un `404` signifie toujours « on a cherché, ce n'est pas là ».

### 9.2. Un seul handler, un `switch` exhaustif

Créez `exception/ApiExceptionHandler.java` :

```java
@Slf4j // Lombok : crée le champ `log` utilisé dans handleServerError
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {   // voir §9.3

    /**
     * Erreurs prévues : pattern matching sur la hiérarchie sealed.
     */
    @ExceptionHandler(SmartTasksException.class)
    public ResponseEntity<Map<String, String>> handleApplicationError(SmartTasksException ex) {
        return switch (ex) {
            case ResourceNotFoundException e -> error(HttpStatus.NOT_FOUND, "Resource not found");
            case InvalidRequestException e -> error(HttpStatus.BAD_REQUEST, e.getMessage());
        };   // pas de "default" : le compilateur sait que tous les cas sont couverts
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    /**
     * 400 - Validation errors on @Valid DTOs (méthode du parent, redéfinie)
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation failed",
                "details", errors
        ));
    }

    /**
     * Toutes les autres erreurs standard de Spring MVC (400, 404, 405, 413, 415...) :
     * on garde le code HTTP et les headers calculés par Spring, avec notre format { "error": ... }.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response == null) {
            return null; // réponse déjà envoyée au client
        }

        String message = response.getBody() instanceof ProblemDetail problem && problem.getDetail() != null
                ? problem.getDetail()
                : "Request failed";

        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(Map.of("error", message));
    }

    /**
     * 500 - Generic server error (y compris une NoSuchElementException "perdue" : c'est un bug)
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleServerError(Exception ex) {
        log.error("Unexpected server error", ex);

        return Map.of(
                "error", "Internal server error",
                "message", "An unexpected error occurred. Please try again later."
        );
    }
}
```

`switch (ex) { case ResourceNotFoundException e -> ... }` est un **switch avec pattern matching** (Java 21) : il teste le type de `ex` et le range dans une variable `e` déjà typée, sans `instanceof` ni cast.

> 🔒 **L'intérêt du duo *sealed* + `switch`** : au module 05, nous ajouterons une troisième erreur, `StorageException`. Dès qu'elle sera ajoutée au `permits`, ce `switch` **ne compilera plus** tant que le nouveau cas n'est pas traité. Impossible d'oublier une erreur : c'est le compilateur qui vérifie, pas une revue de code. Avec un `default`, on perdrait cette garantie.

### 9.3. Ne pas casser les erreurs de Spring : `ResponseEntityExceptionHandler`

Un `@ExceptionHandler(Exception.class)` attrape **tout**, y compris les erreurs que Spring MVC sait déjà traiter. Sans précaution, toutes ces requêtes du client répondraient `500` :

| Requête | Exception levée par Spring | Code attendu |
|---|---|---|
| JSON mal formé | `HttpMessageNotReadableException` | `400` |
| `GET /api/projects/abc` (ID non numérique) | `MethodArgumentTypeMismatchException` | `400` |
| URL inconnue | `NoResourceFoundException` | `404` |
| `DELETE /api/projects` (méthode non prévue) | `HttpRequestMethodNotSupportedException` | `405` |
| Fichier trop gros (module 05) | `MaxUploadSizeExceededException` | `413` |
| `Content-Type: text/plain` au lieu de JSON | `HttpMediaTypeNotSupportedException` | `415` |

Un `500` dit « c'est la faute du serveur » : le client ne comprend pas qu'il doit corriger sa requête, et les alertes de l'équipe d'exploitation se déclenchent pour rien.

`ResponseEntityExceptionHandler` est une classe de Spring qui traite déjà toutes ces exceptions avec le bon code. En **héritant** d'elle :

* on récupère les bons codes HTTP (et les bons headers, comme `Allow: GET, POST` pour un `405`) ;
* `handleExceptionInternal` est le point de passage commun : on le redéfinit pour garder **notre** format `{ "error": "..." }` au lieu du format par défaut de Spring (`ProblemDetail`, RFC 9457) ;
* la validation (`MethodArgumentNotValidException`) est déjà gérée par le parent : on **redéfinit** `handleMethodArgumentNotValid` (`@Override`). Un `@ExceptionHandler(MethodArgumentNotValidException.class)` en plus ferait doublon, et Spring refuserait de démarrer (`Ambiguous @ExceptionHandler method`).

> 💡 **`ProblemDetail`** (RFC 9457) est un format standard d'erreur HTTP : `{ "type", "title", "status", "detail", "instance" }`. Pour une nouvelle API publique, c'est un bon choix. Ici, on garde `{ "error": ... }` pour rester cohérent avec le reste de l'API et avec le front.

### Pourquoi faire ça ?

🎯 Cela permet de :

- **Centraliser** la gestion des erreurs (plutôt que `try/catch` partout)
- Offrir des **messages clairs** aux consommateurs de l’API
- **Maîtriser le format de réponse** (`{ "error": "..." }`)
- **Masquer les détails internes** :

> ❌ Ne renvoyez jamais des stacktraces complètes ou des messages techniques à l’utilisateur !  
> ⚠️ Cela peut révéler des **informations sensibles** sur l’architecture ou la base de données (faille de sécurité).
> 👌 C’est une bonne pratique pour **toutes** vos APIs REST.

---

## 10. 🧪 Exercices fin de module

1. Créez une entité **Task** avec :  
   - `title`  
   - `description`  
   - `dueDate`  

2. Implémentez le CRUD complet (`GET`, `POST`, `PUT`, `DELETE`)

3. Créez des **DTO** (ou `record`) pour ne pas exposer directement les entités

4. Ajoutez la **validation** :
   - `@NotBlank`, `@Size`, etc.
   - retour d’erreur propre si invalide
5. `GET /api/projects/{id}` et `GET /api/tasks/{id}` doivent répondre `404` (via `ResourceNotFoundException`) si l'ID n'existe pas

> 🤔 **DTO vs record ?**  
> Les `record` Java sont très pratiques pour les données immuables simples (ex : payload JSON). Utilisez-les si vous n’avez pas besoin de logique métier ou de setters.

---

## ➡️ Prochain module

Passez au chapitre suivant :  
👉 **02 – JPA & Relations**

On y verra comment relier vos entités (`OneToMany`, `ManyToOne`, `Cascade`, etc.) et comment structurer proprement une base de données relationnelle dans Spring Boot.
