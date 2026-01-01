# 02 – JPA & Relations

Dans ce module, nous passons d'un modèle simple à un modèle relationnel complet. Nous allons modéliser les relations entre `Project`, `Task` et `Attachment`, et optimiser les performances de nos requêtes.

-----

# 🎯 Objectifs du module

À la fin de ce chapitre, vous saurez :

✅ Comprendre le **cycle de vie des entités JPA** (Managed, Detached) \
✅ Modéliser des relations bidirectionnelles `@OneToMany` / `@ManyToOne` \
✅ Maîtriser le **Lazy Loading** pour éviter les problèmes de performance (N+1 queries) \
✅ Utiliser des **DTO** (Records) pour découpler l'API de la BDD \
✅ Écrire des **projections JPQL** pour optimiser la lecture.

-----

# 1. 🧠 Théorie : le contexte de persistance

JPA (via Hibernate) n'est pas une simple connexion SQL. C'est un ORM (Object Relational Mapper) qui gère un contexte.

## 1.1. Le cycle de vie d'une entité

1. **Transient** : Juste un objet Java (`new Task()`), inconnu de la base
2. **Managed** : L'objet est "suivi" par Hibernate. Toute modification (`task.setCompleted(true)`) sera automatiquement détectée (**Dirty Checking**) et sauvegardée en base à la fin de la transaction, même sans appeler `save()`
3. **Detached** : La session est fermée, l'objet n'est plus synchronisé avec la base

## 1.2. Le piège du Lazy Loading

Pour les relations (ex: une liste de tâches dans un projet), Hibernate utilise des Proxies.

1. **FetchType.LAZY** (Paresseux) : La donnée (la liste des tâches) n'est chargée que si on appelle le getter (getTasks()).
2. **FetchType.EAGER** (Immédiat) : La donnée est chargée tout de suite, même si on n'en a pas besoin.

> ⚠️ Règle d'or : Utilisez toujours LAZY pour les relations @OneToMany et @ManyToOne afin d'éviter de charger toute la base de données en mémoire. Cela empêche JPA de charger des gigaoctets de données inutiles à chaque requête simple, évitant ainsi le problème du "N+1 query problem".

---

## 2. 🧱 Rappel et amélioration : L'Entité `Project`

Notre entité de base doit désormais gérer la date de création de manière automatique.

### 👉 Gestion automatique de la date

Pour gérer la date de création (`createdOn`), nous utilisons l'annotation `@PrePersist` sur une méthode de l'entité `Project`.

```java
package fr.corentinbringer.smarttasks.project.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
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
    private LocalDateTime createdOn;
    
    // Relation aux tâches (voir section 2.2)
    private Set<Task> tasks; 

    // Définition automatique de la date lors de la persistance
    @PrePersist
    protected void onCreate() {
        this.createdOn = LocalDateTime.now(); // Date de l'opération
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

    @ManyToOne(fetch = FetchType.LAZY) // Toujours LAZY pour ManyToOne !
    @JoinColumn(name = "project_id", nullable = false)
    private Project project; // La clé étrangère pointant vers Project
    
    @Column(length = 100, nullable = false)
    private String title;

    @Lob
    private String description;

    private LocalDate dueDate;
    
    @Column(nullable = false)
    private boolean completed = false;

    // ... createdOn et @PrePersist pour la gestion de la date
}
```

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

## 4. 📎 Relation Task $\leftrightarrow$ Attachment (Exercice)

Une tâche peut avoir plusieurs pièces jointes stockées sur MinIO.

**Exercice pour l'étudiant :** Créez l'entité `Attachment.java` et mettez en place la relation bidirectionnelle entre `Task` et `Attachment` en vous basant sur les principes précédents et les champs de la solution finale fournie :

1.  Ajoutez les champs nécessaires à l'entité `Attachment` :
      * `objectKey` (clé du fichier dans MinIO)
      * `originalName`, `mimeType`, `size`
      * `uploadedOn` (utilisez `@PrePersist`).
2.  Mettez en place la relation `@ManyToOne` de `Attachment` vers `Task`.
3.  Mettez en place la relation inverse `@OneToMany` de `Task` vers `Attachment`, avec les options `cascade` et `orphanRemoval`.

## 5. 📦 DTO, validation et contrôleur

Dans une application REST performante, nous devons souvent retourner une version simplifiée de l'entité (un DTO) sans charger toutes les relations. Spring Data JPA le permet via les projections directes en JPQL.

### 5.1. DTO de création et validation

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
public TaskResponse create(@PathVariable Long projectId, @Valid @RequestBody TaskCreateRequest request) {
    return taskService.create(projectId, request);
}
```

> Si la validation échoue, l'application retourne automatiquement une erreur `400 Bad Request` gérée par `ApiExceptionHandler.java`.

### 5.2. DTO de réponse

Le DTO de réponse doit refléter précisément les données attendues par le front (ex: `TaskDetail.tsx` ou `ProjectDetail.tsx`).

**Travail à réaliser :** Créez le `TaskResponse.java` et assurez-vous qu'il contient tous les champs requis.

```java
package fr.corentinbringer.smarttasks.project.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        boolean completed,
        Long projectId,
        LocalDateTime createdOn
) {}
```

## 6. 🔍 Repository avancé : Les projections

Pour la liste des projets, le front n'a pas besoin de la liste des tâches (qui serait très coûteuse à charger en EAGER). Pour cela, on demande à JPA de projeter le résultat directement dans un DTO `ProjectListResponse` **sans charger l'entité complète**.

### Projection de DTO pour `Project`

```java
// Dans repository/ProjectRepository.java (version simplifiée pour l'exercice)

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

**Travail à réaliser :**

1.  Implémentez la méthode `findAllListByTenantId` (voir `ProjectRepository.java` final) mais **sans inclure la clause `WHERE` sur le `tenantId`** pour ce module.
2.  Créez la projection équivalente dans `TaskRepository.java` pour la méthode `findAllByProjectId` qui retourne `Page<TaskListResponse>`.

-----

# 🧪 Exercice final du module

Votre objectif est de finaliser le modèle de données et les accès de base :

1.  **Entités et Relations** : Finalisez les entités `Task.java` et `Attachment.java` en implémentant la relation `Task` $\leftrightarrow$ `Attachment` et en y ajoutant les champs de gestion de date (`@PrePersist`).
2.  **Repository Find** : Implémentez la méthode de recherche sécurisée (par ID) dans le Repository :
    ```java
    Optional<Project> findById(Long id); // Version simplifiée SANS tenantId pour ce module
    Optional<Task> findById(Long id); // Version simplifiée SANS tenantId pour ce module
    ```
3.  **Service & Projections** : Mettez à jour le `TaskService.java` pour implémenter :
      * `findAllByProjectId(Long projectId, Pageable pageable)` en utilisant une projection DTO (`TaskListResponse.java`).
      * `create(Long projectId, TaskCreateRequest request)` pour lier la tâche au projet.

-----

# 📘 Prochain module

➡️ **03 – Sécurité & Multi-Tenancy**

Nous allons enfin aborder la sécurisation de l'API avec JWT et l'isolation des données entre utilisateurs en utilisant le champ `tenantId` (que nous avons omis dans les exemples du code pour ce module).