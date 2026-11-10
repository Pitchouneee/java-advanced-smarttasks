# 02 – JPA & Relations

Dans ce module, nous passons d'un modèle simple à un modèle relationnel. Nous allons modéliser la relation entre `Project` et `Task`, comprendre comment Hibernate charge les relations, et optimiser nos requêtes de lecture.

> 📎 La troisième entité, `Attachment` (pièces jointes), sera ajoutée au module 05 avec le stockage des fichiers. Elle suivra exactement les mêmes principes que `Task`.

-----

# 🎯 Objectifs du module

À la fin de ce chapitre, vous saurez :

✅ Comprendre le **cycle de vie des entités JPA** (Managed, Detached) \
✅ Modéliser des relations bidirectionnelles `@OneToMany` / `@ManyToOne` \
✅ Choisir entre **LAZY** et **EAGER**, et **détecter puis corriger** le problème des requêtes **N+1** \
✅ Utiliser des **DTO** (Records) pour découpler l'API de la BDD \
✅ Écrire des **projections JPQL** pour optimiser la lecture.

-----

# 1. 🧠 Théorie : le contexte de persistance

JPA (via Hibernate) n'est pas une simple connexion SQL. C'est un ORM (Object Relational Mapper) qui gère un contexte.

## 1.1. Le cycle de vie d'une entité

1. **Transient** : Juste un objet Java (`new Task()`), inconnu de la base
2. **Managed** : L'objet est "suivi" par Hibernate. Toute modification (`task.setCompleted(true)`) sera automatiquement détectée (**Dirty Checking**) et sauvegardée en base à la fin de la transaction, même sans appeler `save()`
3. **Detached** : La session est fermée, l'objet n'est plus synchronisé avec la base

## 1.2. Charger les relations : LAZY ou EAGER

Quand une entité a une relation (le projet d'une tâche, les tâches d'un projet), Hibernate doit décider **quand** charger l'autre côté :

* **`FetchType.LAZY`** (paresseux) : la relation n'est chargée qu'au premier accès (`project.getTasks()`). En attendant, Hibernate place un **proxy** à sa place.
* **`FetchType.EAGER`** (immédiat) : la relation est chargée en même temps que l'entité, qu'on en ait besoin ou non.

⚠️ **Les valeurs par défaut ne sont pas les mêmes selon l'annotation :**

| Annotation | Défaut | Ce qu'on fait |
|---|---|---|
| `@ManyToOne`, `@OneToOne` | **EAGER** | on écrit **toujours** `fetch = FetchType.LAZY` |
| `@OneToMany`, `@ManyToMany` | LAZY | rien à faire |

Pourquoi tout passer en LAZY ? Avec EAGER, **chaque** lecture d'une tâche charge aussi son projet, même pour afficher un simple titre. Et une fois EAGER déclaré sur l'entité, on ne peut plus le désactiver requête par requête. Avec LAZY, on charge peu par défaut, et on demande **explicitement** les relations quand un cas d'usage en a besoin (§1.3).

## 1.3. Le problème N+1

LAZY évite de charger des données inutiles, mais il ne règle **pas** tout. Regardez ce code :

```java
List<Project> projects = projectRepository.findAll();   // 1 requête
for (Project p : projects) {
    System.out.println(p.getName() + " : " + p.getTasks().size() + " tâches");
    //                                       ↑ LAZY : 1 requête PAR projet
}
```

Dans les logs SQL :

```
select p.id, p.name, p.created_on from projects p
select t.* from tasks t where t.project_id=?     ← projet 1
select t.* from tasks t where t.project_id=?     ← projet 2
...                                               ← × N
```

**1 requête pour la liste + N requêtes pour les tâches** : c'est le problème **N+1**. Avec 100 projets, on fait 101 allers-retours vers la base, là où une seule requête suffisait. Invisible avec 3 projets en développement, catastrophique en production.

> ❌ **Idée reçue** : « LAZY évite le N+1 ». C'est faux, et c'est même l'inverse : c'est le chargement LAZY **dans une boucle** qui le déclenche.
> EAGER ne le corrige pas non plus : pour une requête JPQL comme `findAll()`, Hibernate charge d'abord la liste, puis lance **une requête par ligne** pour remplir chaque relation EAGER. On garde le N+1, et on le subit même quand on n'utilise pas la relation.

**La règle :** LAZY partout, puis pour chaque cas d'usage qui a besoin d'une relation, on la charge **dans la même requête**. Trois outils :

**1. `JOIN FETCH` dans une requête JPQL**

```java
@Query("SELECT p FROM Project p LEFT JOIN FETCH p.tasks")
List<Project> findAllWithTasks();
```

Une seule requête SQL avec une jointure : les projets **et** leurs tâches. (Depuis Hibernate 6, les doublons de projets dus à la jointure sont supprimés automatiquement : plus besoin de `DISTINCT`.)

**2. `@EntityGraph` sur une méthode du repository**

```java
@Override
@EntityGraph(attributePaths = "tasks")
List<Project> findAll();
```

Même résultat, sans écrire de JPQL : pratique pour les méthodes dérivées (`findByName...`).

**3. Une projection DTO** (voir §6)

Pour une liste en lecture seule, le plus efficace reste de ne charger **aucune entité** : on sélectionne directement les colonnes utiles dans un record. Une seule requête, aucun proxy, aucun risque de N+1.

> ⚠️ **Piège : `JOIN FETCH` sur une collection + pagination.** SQL ne sait pas paginer « 10 projets avec toutes leurs tâches » (la jointure multiplie les lignes). Hibernate charge alors **tout** en mémoire et pagine en Java, avec l'avertissement `HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory`. Pour une liste paginée, préférez une projection DTO, ou la propriété `spring.jpa.properties.hibernate.default_batch_fetch_size: 50`, qui charge les collections par lots de 50 (N+1 devient 1 + N/50).

## 1.4. Voir les requêtes SQL

On ne peut corriger que ce qu'on voit. Dans `application.yaml` :

```yaml
spring:
  jpa:
    show-sql: true                     # affiche chaque requête
    properties:
      hibernate:
        format_sql: true               # ... indentée et lisible
        generate_statistics: true      # en fin de session : "N JDBC statements executed"
```

> 💡 **Pourquoi pas de `LazyInitializationException` ?** Accéder à une relation LAZY demande une session Hibernate ouverte. Spring Boot laisse la session ouverte jusqu'à la fin de la requête HTTP (`spring.jpa.open-in-view=true`, d'où l'avertissement au démarrage). C'est confortable, mais cela **masque** les N+1 : le chargement LAZY peut se produire n'importe où, jusque dans la sérialisation JSON. Beaucoup d'équipes le désactivent (`open-in-view: false`) pour être obligées de charger explicitement ce dont elles ont besoin.

---

## 2. 🧱 Rappel et amélioration : L'Entité `Project`

Notre entité de base doit désormais gérer la date de création de manière automatique.

### 👉 Gestion automatique de la date

Pour gérer la date de création (`createdOn`), nous utilisons l'annotation `@PrePersist` sur une méthode de l'entité `Project`.

```java
package fr.corentinbringer.smarttasks.project.model;

import jakarta.persistence.*;
import java.time.Instant;
// ... imports Lombok et Set

@Entity
@Table(name = "projects")
// ...
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String name;

    @Column(updatable = false, nullable = false)
    private Instant createdOn;
    
    // Relation aux tâches (voir section 3.2)
    private Set<Task> tasks; 

    // Définition automatique de la date lors de la persistance
    @PrePersist
    protected void onCreate() {
        this.createdOn = Instant.now(); // Instant UTC de l'opération
    }
}
```

## 3. 📌 Relation Project $\leftrightarrow$ Task (One-to-Many)

Un projet possède plusieurs tâches, ce qui se traduit par une relation `@ManyToOne` sur la tâche.

### 3.1. Entité `Task` (côté *Many*)

L'entité `Task` porte la clé étrangère vers le projet.

```java
package fr.corentinbringer.smarttasks.project.model;

import jakarta.persistence.*;
import java.time.LocalDate;
// ... imports

@Entity
@Table(name = "tasks")
// ...
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // LAZY explicite : par défaut, @ManyToOne est EAGER
    @JoinColumn(name = "project_id", nullable = false)
    private Project project; // La clé étrangère pointant vers Project
    
    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "text") // texte long, sans limite de taille
    private String description;

    private LocalDate dueDate;
    
    @Column(nullable = false)
    private boolean completed = false;

    // ... createdOn et @PrePersist pour la gestion de la date
}
```

> ⚠️ **Pourquoi pas `@Lob` ?** C'est l'annotation « standard » pour les textes longs, mais sur PostgreSQL, Hibernate 6 la traduit en colonne `oid` : un *Large Object* stocké **hors de la table**. Conséquences : la colonne affiche un nombre dans `psql`, la lecture échoue hors transaction (`Large Objects may not be used in auto-commit mode`), et les objets ne sont jamais nettoyés quand la ligne est supprimée. Le type PostgreSQL `text` n'a pas de limite de taille : c'est ce qu'il nous faut.

### 3.2. Relation Inverse dans `Project` (Côté *One*)

Pour pouvoir accéder aux tâches depuis le projet, on ajoute la relation inverse.

```java
// Dans model/Project.java

// ...

@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<Task> tasks; 

// ...
```

  * **`mappedBy = "project"`** : Indique que la relation est gérée par le champ `project` dans l'entité `Task`.
  * **`cascade = CascadeType.ALL`** : Si vous supprimez le `Project`, toutes les `Task`s associées seront supprimées (comportement d'intégrité référentielle).
  * **`orphanRemoval = true`** : Si une tâche est retirée de cette collection, elle sera supprimée de la base.
  * Pas de `fetch` : `@OneToMany` est déjà LAZY par défaut (§1.2).

## 4. 📦 DTO, validation et contrôleur

Dans une application REST performante, nous devons souvent retourner une version simplifiée de l'entité (un DTO) sans charger toutes les relations. Spring Data JPA le permet via les projections directes en JPQL.

> 🗂️ **Où ranger les DTO ?** Pour ce module, entités et DTO cohabitent dans le package `model/` : c'est simple, mais cela mélange deux rôles (la forme des tables et la forme de l'API). Au module 06, nous les séparerons : entités côté persistance, DTO côté web.

### 4.1. DTO de création et validation

Le DTO de création de tâche (`TaskCreateRequest.java`) utilise la validation Spring.

```java
package fr.corentinbringer.smarttasks.project.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskCreateRequest(
        @NotBlank @Size(max = 100) String title, // Validation appliquée ici
        String description,
        LocalDate dueDate
) {}
```

Le contrôleur utilise `@Valid` pour déclencher cette validation :

```java
// Dans controller/TaskInProjectController.java

@PostMapping
@ResponseStatus(HttpStatus.CREATED) // 201 : une ressource a été créée (voir api-spec.md)
public TaskResponse create(@PathVariable Long projectId, @Valid @RequestBody TaskCreateRequest request) {
    return taskService.create(projectId, request);
}
```

> Si la validation échoue, l'application retourne automatiquement une erreur `400 Bad Request` gérée par `ApiExceptionHandler.java`.

### 4.2. DTO de réponse

Le DTO de réponse doit refléter précisément les données attendues par le front (ex: `TaskDetail.tsx` ou `ProjectDetail.tsx`).

**Travail à réaliser :** Créez le `TaskResponse.java` et assurez-vous qu'il contient tous les champs requis.

```java
package fr.corentinbringer.smarttasks.project.model;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        boolean completed,
        Long projectId,
        Instant createdOn
) {}
```

> 🕐 **Pourquoi `Instant` et pas `LocalDateTime` ?** Un `LocalDateTime` n'a **pas de fuseau horaire** : `2025-01-01T10:00` à Paris ou à Montréal, ce n'est pas le même moment. Pour horodater un événement (création, upload…), on utilise `Instant`, un point précis sur la ligne du temps en UTC. Il est sérialisé en `"2025-01-01T10:00:00Z"` et le front le convertit dans le fuseau de l'utilisateur.
> En revanche, `dueDate` reste un `LocalDate` : une échéance « le 15 février » est une date de calendrier, pas un instant.

## 5. 🔍 Repository avancé : Les projections

Pour la liste des projets, le front n'a besoin ni des tâches, ni de l'entité complète : juste l'id, le nom et la date. On demande donc à JPA de **projeter** le résultat directement dans un DTO, sans charger d'entité (et donc sans risque de N+1, §1.3).

### 5.1. Le DTO de liste

```java
package fr.corentinbringer.smarttasks.project.model;

import java.time.Instant;

public record ProjectListResponse(
        Long id,
        String name,
        Instant createdOn
) {}
```

### 5.2. La requête de projection

```java
// Dans repository/ProjectRepository.java

@Query("""
       SELECT new fr.corentinbringer.smarttasks.project.model.ProjectListResponse(
           p.id,
           p.name,
           p.createdOn
       )
       FROM Project p
       ORDER BY p.createdOn DESC
       """)
List<ProjectListResponse> findLatestProjects(Pageable pageable);
```

* `SELECT new ...` appelle le constructeur du record : le **nom complet** de la classe est obligatoire, et l'ordre des colonnes doit correspondre à celui des champs.
* Le SQL généré ne lit que `id`, `name` et `created_on`.
* Le `Pageable` ajoute `LIMIT` / `OFFSET` : c'est lui qui limite le nombre de résultats (ex. les 5 derniers projets).

**Travail à réaliser :**

1.  Dans `ProjectRepository`, écrivez une méthode de liste **paginée** pour l'endpoint `GET /api/projects`. Elle utilise la même projection, sans clause `WHERE` pour l'instant (le filtre par utilisateur arrive au module 03) :
    ```java
    Page<ProjectListResponse> findAllList(Pageable pageable);
    ```
    Avec un `Page`, Spring Data exécute aussi une requête `count` pour calculer le nombre total de pages. Il la déduit seul de votre requête JPQL.
2.  Dans `TaskRepository`, créez la projection équivalente `Page<TaskListResponse> findAllByProjectId(Long projectId, Pageable pageable)`. Le record `TaskListResponse` contient les champs de l'objet `Task` de `api-spec.md` : `id`, `title`, `description`, `dueDate`, `completed`, `projectId`, `createdOn`. Astuce : `t.project.id` dans la requête JPQL (Hibernate lit directement la colonne `project_id`, sans jointure).

### 📄 Format JSON des pages

Quand un controller renvoie un `Page<T>`, Spring doit le convertir en JSON. Par défaut, il sérialise l'objet interne `PageImpl` tel quel : le format peut changer d'une version à l'autre, et Spring affiche un avertissement au démarrage.

**Exercice :** Activez le format stable dans `SmarttasksApplication.java` :

```java
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class SmarttasksApplication { ... }
```

Le JSON devient `{ "content": [...], "page": { "size", "number", "totalElements", "totalPages" } }` : c'est le format attendu par le front (voir `api-spec.md`).

-----

# 🧪 Exercice final du module

Votre objectif est de finaliser le modèle de données et les accès de base :

1.  **Entités et relations** : Finalisez `Project` et `Task` (relation bidirectionnelle, `fetch = LAZY` sur le `@ManyToOne`, `createdOn` avec `@PrePersist`, `description` en `text`).
2.  **Service & projections** : Mettez à jour le `TaskService.java` pour implémenter :
      * `findAllByProjectId(Long projectId, Pageable pageable)` en utilisant la projection DTO (`TaskListResponse`) ;
      * `create(Long projectId, TaskCreateRequest request)` : chargez le projet (`404` s'il n'existe pas) puis liez-lui la tâche.
3.  **Chasse au N+1** :
    1.  Activez les logs SQL et les statistiques (§1.4).
    2.  Créez 5 projets contenant chacun quelques tâches.
    3.  Ajoutez un endpoint temporaire `GET /api/projects/summary` qui appelle `projectRepository.findAll()` et renvoie, pour chaque projet, son nom et son nombre de tâches (`p.getTasks().size()`).
    4.  Appelez-le et **comptez les requêtes** dans les logs. Combien en attendiez-vous ?
    5.  Corrigez avec `JOIN FETCH` (ou `@EntityGraph`) et vérifiez qu'il ne reste qu'**une** requête.
    6.  Bonus : réécrivez-le avec une projection `SELECT new ...(p.name, COUNT(t)) FROM Project p LEFT JOIN p.tasks t GROUP BY p.id, p.name`. Pourquoi est-ce encore mieux ? (Indice : que contient la réponse SQL ?)

-----

# 📘 Prochain module

➡️ **03 – Sécurité & Multi-Tenancy**

Nous allons enfin aborder la sécurisation de l'API avec JWT et l'isolation des données entre utilisateurs en utilisant le champ `tenantId` (que nous avons omis dans les exemples du code pour ce module).
