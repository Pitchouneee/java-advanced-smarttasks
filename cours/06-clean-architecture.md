# 06 – Clean Architecture & Refactoring

Jusqu'à présent, nous avons développé une application "Layered" classique (Controller -> Service -> Repository). C'est fonctionnel, mais cela crée un couplage fort : le code métier dépend de la base de données (JPA).

Pour une application maintenable sur le long terme, nous allons basculer vers une **Architecture Hexagonale** (ou Clean Architecture).

---

# 🎯 Objectifs du module

✅ Comprendre la **Règle de Dépendance** : Le Domaine ne doit dépendre de rien.
✅ Structurer le projet en packages : `domain`, `application`, `infrastructure`.
✅ Isoler les entités métier (Records) des entités JPA (@Entity).
✅ Utiliser des **Ports** (Interfaces) et des **Adapters** pour inverser les dépendances.
✅ Mettre les **règles métier** dans le domaine (records avec comportement).
✅ Distinguer Clean Architecture et Hexagonale, ports d'entrée et ports de sortie.

---

# 🌀 1. Théorie : Le Principe d'inversion

Dans une architecture classique :

> Service (Métier) ➡️ Repository (Technique)

Si on change de base de données, on risque de casser le métier.

Dans la Clean Architecture :

> Service (Métier) ➡️ **Interface (Port)** ⬅️ Adapter (Technique)

Le métier définit ses besoins ("J'ai besoin de sauvegarder"), et la couche technique implémente ce besoin. Le métier ne connaît pas l'implémentation.

### Structure cible des packages

```
src/main/java/fr/corentinbringer/smarttasks
 ┣ 📂 project
 ┃ ┣ 📂 domain                 # Le Cœur du métier (Aucun framework ici !)
 ┃ ┃ ┗ 📂 model                # Objets purs (Record)
 ┃ ┣ 📂 application            # L'Orchestration
 ┃ ┃ ┣ 📂 model                # Commandes et objets d'échange (CreateTaskCommand, FileContent...)
 ┃ ┃ ┣ 📂 port.out             # Interfaces définies par le métier (Ports)
 ┃ ┃ ┗ 📂 service              # Logique applicative
 ┃ ┗ 📂 infrastructure         # Les détails techniques
 ┃   ┣ 📂 persistence          # Base de données (JPA)
 ┃   ┃ ┣ 📂 adapter            # Implémentation des Ports
 ┃   ┃ ┣ 📂 jpa.entity         # Entités JPA (@Entity)
 ┃   ┃ ┣ 📂 jpa.mapper         # Conversion Entité JPA ↔ Domaine
 ┃   ┃ ┗ 📂 jpa.repository     # Interfaces Spring Data
 ┃   ┣ 📂 storage              # Adapter S3 (S3FileStorageAdapter)
 ┃   ┗ 📂 web                  # API REST (Controllers + DTO JSON, conversion DTO ↔ domaine)

```

### Clean Architecture ou Hexagonale ?

On emploie souvent les deux termes l'un pour l'autre. Ils partagent la même idée (le métier au centre, la technique autour, les dépendances vers l'intérieur), mais ne viennent pas du même auteur :

| | Architecture **Hexagonale** (*Ports & Adapters*) | **Clean Architecture** |
|---|---|---|
| Auteur | Alistair Cockburn (2005) | Robert C. Martin (2012) |
| Image | un hexagone : l'application au centre, des **ports** sur les bords, des **adapters** branchés dessus | des cercles concentriques : *Entities*, *Use Cases*, *Interface Adapters*, *Frameworks & Drivers* |
| Vocabulaire | port **in** (ce que l'application offre), port **out** (ce dont elle a besoin), adapter | entité, cas d'utilisation (*use case*), *presenter*, *gateway* |

Ce que nous faisons dans ce module est de l'**hexagonal**, avec les noms de packages de la Clean Architecture (`domain`, `application`, `infrastructure`).

Il existe **deux sortes de ports** :

* **Port de sortie** (`port.out`) : ce dont l'application **a besoin**, implémenté par l'infrastructure. Exemples : `ProjectPort` (implémenté par l'adapter JPA), `FileStoragePort` (implémenté par l'adapter S3).
* **Port d'entrée** (`port.in`) : ce que l'application **offre**, appelé par l'infrastructure (controllers, consommateurs de messages, batchs). C'est une interface par cas d'utilisation :

```java
// application/port/in/CreateProjectUseCase.java
public interface CreateProjectUseCase {
    Project create(String name);
}

// Le service implémente le port ; le controller dépend de l'interface, plus de la classe
@Service
public class ProjectService implements CreateProjectUseCase { ... }
```

Dans ce cours, les controllers appellent **directement** les services : nous n'avons que des ports de sortie. C'est un compromis courant (voir §7) : les ports d'entrée deviennent utiles quand plusieurs implémentations d'un même cas d'utilisation coexistent, ou pour rendre visible la liste des cas d'utilisation de l'application.

---

# 🛠️ 2. Mise en pratique : Le domaine

Nous allons "purifier" notre modèle `Project`. Il ne doit plus avoir d'annotations `@Entity`, `@Id`, `@Column`.

**Exercice :** Créez le record `Project` dans `project/domain/model`.

```java
package fr.corentinbringer.smarttasks.project.domain.model;

import java.time.Instant;

// C'est un objet pur (POJO/Record). Aucune dépendance à Spring ou Jakarta.
public record Project(
    Long id,
    String tenantId,
    String name,
    Instant createdOn
) {}

```


### 2.1. Un domaine qui a du comportement

Un record qui ne contient que des données est un **modèle anémique** : toutes les règles se retrouvent dans les services, dupliquées et difficiles à tester. Le domaine est le bon endroit pour les règles métier.

**Exercice :** Enrichissez le record `Task` :

```java
public record Task(Long id, String tenantId, Long projectId, String title, String description,
                   LocalDate dueDate, boolean completed, Instant createdOn) {

    // Constructeur compact : vérifie les invariants, quel que soit le chemin de création
    public Task {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("A task must have a title");
        }
    }

    public static Task create(String tenantId, Long projectId, String title,
                              String description, LocalDate dueDate, Instant now) {
        return new Task(null, tenantId, projectId, title, description, dueDate, false, now);
    }

    /** En retard = pas terminée ET échéance strictement passée. */
    public boolean isOverdue(LocalDate today) {
        return !completed && dueDate != null && dueDate.isBefore(today);
    }

    /** Immuable : on renvoie une nouvelle tâche, l'originale ne change pas. */
    public Task complete() {
        return new Task(id, tenantId, projectId, title, description, dueDate, true, createdOn);
    }
}
```

* Le **constructeur compact** (`public Task { ... }`, sans parenthèses) s'exécute avant l'affectation des champs : il garantit qu'**aucune** tâche invalide ne peut exister, même créée par un mapper ou un test. La validation `@NotBlank` du DTO protège l'API ; celle-ci protège le **domaine**. Si elle échoue, c'est un bug (→ `500`), pas une erreur du client.
* Les méthodes `complete()`, `withDetails(...)` (dans la solution) ou `Project.rename(...)` renvoient une **nouvelle instance** : le record reste immuable.
* `isOverdue` se teste avec un simple `assertThat(task.isOverdue(today)).isTrue()` : pas de mock, pas de Spring (voir `TaskTest` dans le corrigé, et le module 07).

> 🤔 La règle « en retard » existe aussi en JPQL (`countOverdueTasksByTenantId`), pour compter les tâches **en base** sans les charger en mémoire. C'est une duplication assumée pour la performance : si la règle change, il faut modifier les deux endroits. Un test qui compare les deux résultats sur les mêmes données protège contre l'oubli.

---

# 🔌 3. Les Ports (Interfaces)

Le service métier a besoin de sauvegarder et lire des projets. Il définit un contrat.

**Exercice :** Créez l'interface `ProjectPort` dans `project/application/port/out`.

```java
public interface ProjectPort {
    Project save(Project project);
    
    // Le métier utilise ses propres objets (Project), pas les entités JPA !
    Page<Project> findAll(String tenantId, Pageable pageable);
    
    Optional<Project> findByIdAndTenantId(Long id, String tenantId);
}

```

---

# 🏗️ 4. L'Infrastructure (Persistence)

C'est ici (et seulement ici) que nous utilisons JPA.

### 4.1. L'Entité JPA

Déplacez votre ancienne classe `@Entity` vers `infrastructure/persistence/jpa/entity/ProjectEntity.java`.

⚠️ **Supprimez la méthode `@PrePersist`** qui remplissait `createdOn` (module 02). Désormais, c'est le **service** qui crée le record `Project` avec `Instant.now()` (§5). Si on garde les deux, la date est fixée deux fois, à deux endroits différents :

* la valeur du domaine est **écrasée** par Hibernate au moment de l'`INSERT` : celle que le service a calculée (et peut-être déjà utilisée) n'est pas celle qui est enregistrée ;
* un test unitaire du service ne voit pas la « vraie » date, puisqu'elle n'est fixée que par JPA ;
* et surtout, on ne sait plus **qui** est responsable de cette donnée.

La date de création est une information **métier** : elle appartient au domaine. L'entité JPA se contente de la stocker (le mapper la recopie).

> 💡 Pour aller plus loin : injecter un `java.time.Clock` dans le service et appeler `Instant.now(clock)`. En test, `Clock.fixed(...)` donne une date connue d'avance, qu'on peut vérifier avec un `assertEquals`.

Le repository Spring Data travaille maintenant sur `ProjectEntity` : `ProjectRepository extends JpaRepository<ProjectEntity, Long>`. Et comme le port renvoie des objets du **domaine**, les projections DTO du module 02 (`findAllListByTenantId`, qui renvoyait des `ProjectListResponse`) ne conviennent plus : l'adapter a besoin d'entités, qu'il convertit avec le mapper.

```java
@Query("SELECT p FROM ProjectEntity p WHERE p.tenantId = :tenantId")
Page<ProjectEntity> findAllByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
```

> 🤔 On perd l'avantage de la projection (ne lire que les colonnes utiles). C'est le prix du découplage ; si une liste devient un problème de performance, on peut ajouter au port une méthode de lecture dédiée qui renvoie un record de lecture.

### 4.2. Le Mapper

Nous avons besoin de convertir `Project` (Domaine) ↔ `ProjectEntity` (BDD).

**Exercice :** Créez `infrastructure/persistence/jpa/mapper/ProjectMapper.java`.

```java
@Component
public class ProjectMapper {

    public Project toDomain(ProjectEntity entity) {
        if (entity == null) return null;
        return new Project(
            entity.getId(),
            entity.getTenantId(),
            entity.getName(),
            entity.getCreatedOn()
        );
    }

    public ProjectEntity toEntity(Project domain) {
        // TODO: Créer une ProjectEntity et mapper les champs
        // Attention : Ne pas oublier d'initialiser les listes si besoin
        return null;
    }
}

```

### 4.3. L'adaptateur

C'est la classe qui implémente le Port. C'est elle qui fait le pont entre le monde "idéal" (Domaine) et le monde "réel" (Base de données).

**Exercice :** Implémentez `ProjectPersistenceAdapter`.

```java
@Component
@RequiredArgsConstructor
public class ProjectPersistenceAdapter implements ProjectPort {

    private final ProjectRepository projectRepository; // Le Repo JPA classique
    private final ProjectMapper projectMapper;

    @Override
    public Project save(Project project) {
        if (project.id() == null) {
            // Création : Domaine -> Entity, INSERT, puis Entity -> Domaine (avec l'id généré)
            ProjectEntity saved = projectRepository.save(projectMapper.toEntity(project));
            return projectMapper.toDomain(saved);
        }

        // Mise à jour : on modifie l'entité chargée (dirty checking, module 02).
        // ⚠️ Ne PAS faire save(mapper.toEntity(project)) ici : l'entité reconstruite a une
        // collection tasks vide (null), et avec orphanRemoval = true, Hibernate supprimerait
        // les tâches du projet (ou lèverait "collection ... was no longer referenced").
        ProjectEntity existing = projectRepository.findById(project.id())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        existing.setName(project.name());
        return projectMapper.toDomain(existing);
    }

    @Override
    public Page<Project> findAll(String tenantId, Pageable pageable) {
        return projectRepository.findAllByTenantId(tenantId, pageable)
                .map(projectMapper::toDomain);
    }

    @Override
    public Optional<Project> findByIdAndTenantId(Long id, String tenantId) {
        // Optional.map ne convertit que si le projet existe (sinon Optional.empty())
        return projectRepository.findByIdAndTenantId(id, tenantId)
                .map(projectMapper::toDomain);
    }
}

```

---

# 🔄 5. Mise à jour du service

Le `ProjectService` ne dépend plus de `ProjectRepository`. Il dépend de `ProjectPort`.

Et il ne dépend plus non plus de la couche **web** : il ne reçoit plus `ProjectCreateRequest` et ne renvoie plus `ProjectResponse`. Ces DTO décrivent le JSON de l'API : ils appartiennent à l'infrastructure. Le service reçoit des valeurs simples (ou une **commande**) et renvoie des objets du **domaine**.

```java
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectPort projectPort; // Injection de l'interface !

    @Transactional
    public Project create(String name) {
        // Création du record Domaine (et non plus de l'entité JPA)
        Project project = new Project(null, TenantContext.getTenant(), name, Instant.now());

        return projectPort.save(project); // on renvoie le domaine, pas un DTO
    }
}

```

C'est le controller qui traduit dans les deux sens :

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public ProjectResponse create(@Valid @RequestBody ProjectCreateRequest request) {
    Project project = projectService.create(request.name()); // JSON → valeurs simples
    return ProjectResponse.from(project);                    // domaine → JSON
}
```

```java
// Dans infrastructure/web/model/ProjectResponse.java
public record ProjectResponse(Long id, String name, Instant createdOn) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.id(), project.name(), project.createdOn());
    }
}
```

> 💡 **Pourquoi c'est important ?** Demain, on ajoute un import CSV ou un consommateur de messages qui crée des projets : il appellera `projectService.create(name)` sans rien connaître du JSON de l'API. Et si on renomme un champ du JSON, seul le controller change.

Quand il y a plus de deux ou trois paramètres, on les regroupe dans une **commande**, un record de la couche application :

```java
// Dans application/model/CreateTaskCommand.java
public record CreateTaskCommand(String title, String description, LocalDate dueDate) {}

// Dans le controller
CreateTaskCommand command = new CreateTaskCommand(request.title(), request.description(), request.dueDate());
return TaskResponse.from(taskService.create(projectId, command));
```

`TaskCreateRequest` (web, avec `@NotBlank`, `@Size`) et `CreateTaskCommand` (application) se ressemblent : c'est normal. Ils changent pour des raisons différentes (le format de l'API d'un côté, le besoin métier de l'autre).

---

# 📁 6. Le cas des fichiers

Avant le refactoring, `AttachmentService` recevait un `MultipartFile` et renvoyait un `InputStreamResource` : deux classes de **Spring Web**. Le métier était donc lié à HTTP, impossible de l'appeler depuis un batch.

On introduit un objet neutre dans `application/model` :

```java
/** Un fichier vu par l'application : métadonnées + flux vers le contenu. */
public record FileContent(String name, String contentType, long size, InputStream content) {}
```

* **Upload** : le controller ouvre le flux du `MultipartFile`, construit un `FileContent` et le passe au service. Le `FileStoragePort` reçoit lui aussi un `FileContent` :
  ```java
  public interface FileStoragePort {
      String uploadFile(FileContent file);      // et non plus MultipartFile
      InputStream downloadFile(String objectKey);
      void deleteFile(String objectKey);
  }
  ```
* **Download** : le service renvoie un `FileContent`, et c'est le controller qui l'emballe dans un `InputStreamResource` pour Spring.

```java
// TaskController : MultipartFile s'arrête ici
try (InputStream content = file.getInputStream()) {
    FileContent fileContent = new FileContent(file.getOriginalFilename(), file.getContentType(), file.getSize(), content);
    return AttachmentResponse.from(attachmentService.create(id, fileContent));
} catch (IOException e) {
    throw new StorageException("Cannot read the uploaded file", e);
}
```

---

# ⚖️ 7. Compromis assumés

Une architecture hexagonale « pure » ne contiendrait **aucune** dépendance à un framework dans `domain` et `application`. Notre solution en garde quelques-unes, **volontairement**. L'important n'est pas d'être pur, c'est de savoir où sont les écarts et ce qu'ils coûtent.

| Écart | Où | Pourquoi on l'accepte | Comment le corriger |
|---|---|---|---|
| `Page` / `Pageable` (Spring Data) | ports et services | La pagination est partout ; réécrire ces types ajoute beaucoup de code de conversion pour peu de bénéfice tant qu'on reste sur Spring | Types maison `PageQuery(int page, int size)` et `PageResult<T>(List<T> content, long total)` dans `application/model`, conversion dans les adapters et les controllers |
| `TenantContext` (ThreadLocal technique) | services | Évite de passer le tenant en paramètre à chaque méthode | Un port `CurrentTenantProvider` (implémenté par l'infrastructure à partir du JWT), ou le tenant passé explicitement en paramètre |
| `@Service`, `@Transactional` | services | Annotations sans logique, qui ne gênent ni les tests unitaires ni la lecture | Déclarer les beans dans une classe `@Configuration` de l'infrastructure, et gérer les transactions dans un adapter ou un décorateur |
| Pas de ports d'entrée | controllers → services | Une seule implémentation par cas d'utilisation : une interface de plus n'apporterait rien aujourd'hui | Une interface par cas d'utilisation dans `application/port/in` (`CreateProjectUseCase`...), implémentée par le service |

> 🎯 **À retenir :** un compromis **documenté** est une décision d'architecture ; un compromis **involontaire** est une dette technique. Les deux écarts que nous avons corrigés (DTO web dans les services, `MultipartFile` dans le port) contredisaient directement la règle de dépendance et rendaient le métier impossible à réutiliser hors HTTP : ceux-là n'étaient pas acceptables.

---

# 🧪 Exercices finaux

C'est un gros chantier de refactoring. À vous de jouer pour le reste :

1. **Task** : Refactorez `Task` en suivant le même modèle :
* `Task` (Record domaine)
* `TaskPort` (Interface)
* `TaskPersistenceAdapter` (Implémentation avec Mapper)


2. **Attachment** : Idem pour les pièces jointes.
3. **Stockage S3** : Créez un port `FileStoragePort` (interface) et déplacez `S3StorageService` dans un adapter `S3FileStorageAdapter`. Ainsi, votre métier ne dépendra plus du SDK AWS directement.
4. **Couche web** : Faites en sorte qu'aucune classe de `application` n'importe `infrastructure.web` ni `org.springframework.web` (DTO convertis dans les controllers avec des méthodes `from(...)`, commandes `CreateTaskCommand` / `UpdateTaskCommand`, `FileContent`).
   Vérification rapide : `grep -rn "infrastructure.web\|springframework.web" src/main/java/**/application` ne doit rien renvoyer.
5. **Bonus** : Supprimez l'un des compromis du §7 (au choix : la pagination maison `PageQuery` / `PageResult`, ou le port `CurrentTenantProvider`).

---

# 🏁 Conclusion

Bravo ! Vous avez transformé une application monolithique couplée en une application modulaire et testable.

* Si demain on remplace PostgreSQL par MongoDB, on réécrit juste l'Adapter. Le métier ne change pas.
* Cas vécu sur ce cours : MinIO a cessé d'être maintenu. Il a fallu toucher le code partout où le SDK MinIO était utilisé. Avec le port `FileStoragePort`, seul l'adapter aurait changé ; et grâce à l'API S3 standard, passer de RustFS à AWS S3 ne demande même plus de toucher l'adapter (`application.yaml` suffit).
* Si on passe à un stockage **non** S3 (Azure Blob Storage par exemple), on crée un nouvel Adapter `AzureBlobFileStorageAdapter`. Le métier ne change pas.

C'est l'architecture standard des projets d'entreprise modernes.

---

# ➡️ Prochain module

Nous avons affirmé que cette architecture rendait le code **testable**. Prouvons-le.

Passez au chapitre suivant : **07 – Tests automatisés**.