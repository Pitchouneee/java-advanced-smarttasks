
# 01 – API REST avec Spring Boot

Ce premier module pose les bases de la construction d’une API REST avec **Spring Boot**.  
C’est ici que vous apprendrez à structurer votre backend SmartTasks.

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

🗂️ Générez le projet et extrayez-le dans le dossier `projet-back/`.

---

## 2. 🧱 Structure d’un projet Spring Boot

Spring Boot vous aide à organiser automatiquement votre projet. Voici l’architecture typique :

```
src/main/java/fr/corentinbringer/smarttasks
 ┣ 📂 controller        # Entrée de l’API (REST Controllers)
 ┣ 📂 service           # Logique métier
 ┣ 📂 repository        # Accès à la base de données (DAO)
 ┣ 📂 domain            # Modèles de données (entités)
 ┗ SmartTasksApplication.java
```

Cette séparation respecte un principe fondamental : **la séparation des responsabilités**.

| Couche        | Rôle principal                                                   |
|---------------|------------------------------------------------------------------|
| Controller    | Gère les requêtes HTTP (GET, POST, etc.)                         |
| Service       | Contient la logique métier de l’application                      |
| Repository    | Dialogue avec la base de données (via Spring Data JPA)           |
| Domain        | Représente les objets du modèle (ex : `Project`, `Task`, etc.)   |

> ⚠️ Les controllers ne doivent contenir **aucune logique métier** : ce sont juste des "passerelles" entre le web et votre application.
> 🧼 Cette séparation permet un code **modulaire**, **testable** et **maintenable**.

---

## 3. 📝 Premier Controller REST

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

Créez le fichier `domain/Project.java` :

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

Créez `repository/ProjectRepository.java` :

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

Créez `service/ProjectService.java` :

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

### Pourquoi une couche Service ?

Elle permet de :

- **Centraliser** la logique métier (ex. : vérifier des règles) et d'éviter la duplication
- **Préparer** les données à évoluer (ex. : filtrer, transformer, ajout de règles, validatoins, traitements)
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

> ⚠️ Ce code est volontairement simple. On ajoutera les **DTO** et la **validation** dans les exercices finaux de ce chapitre.

---

## 8. ⚙️ Configurer PostgreSQL

Dans `src/main/resources/application.yml` :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smarttasks
    username: smart
    password: smart
  jpa:
    hibernate:
      ddl-auto: update # Crée/Met à jour les tables automatiquement (utile en dev)
    show-sql: true     # Affiche les requêtes SQL dans la console
```

### Explications

- `ddl-auto: update` crée automatiquement les tables en fonction des entités
- `show-sql: true` est utile pour **comprendre ce que fait Hibernate**
- En production, ces options sont souvent désactivées pour plus de contrôle

> 🔒 Ne pas exposer ces infos de connexion dans un repo public ! Utilisez un `.env` ou des variables d’environnement.

---

## 9. 🛡️ Gestion des erreurs

Créez `controller/ApiExceptionHandler.java` :

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

> 🤔 **DTO vs record ?**  
> Les `record` Java sont très pratiques pour les données immuables simples (ex : payload JSON). Utilisez-les si vous n’avez pas besoin de logique métier ou de setters.

---

## ➡️ Prochain module

Passez au chapitre suivant :  
👉 **02 – JPA & Relations**

On y verra comment relier vos entités (`OneToMany`, `ManyToOne`, `Cascade`, etc.) et comment structurer proprement une base de données relationnelle dans Spring Boot.
