# 06 – Clean Architecture & Refactoring

Jusqu'à présent, nous avons développé une application "Layered" classique (Controller -> Service -> Repository). C'est fonctionnel, mais cela crée un couplage fort : le code métier dépend de la base de données (JPA).

Pour une application maintenable sur le long terme, nous allons basculer vers une **Architecture Hexagonale** (ou Clean Architecture).

---

# 🎯 Objectifs du module

✅ Comprendre la **Règle de Dépendance** : Le Domaine ne doit dépendre de rien.
✅ Structurer le projet en packages : `domain`, `application`, `infrastructure`.
✅ Isoler les entités métier (Records) des entités JPA (@Entity).
✅ Utiliser des **Ports** (Interfaces) et des **Adapters** pour inverser les dépendances.

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
 ┃ ┃ ┣ 📂 port.out             # Interfaces définies par le métier (Ports)
 ┃ ┃ ┗ 📂 service              # Logique applicative
 ┃ ┗ 📂 infrastructure         # Les détails techniques
 ┃   ┣ 📂 persistence          # Base de données (JPA)
 ┃   ┃ ┣ 📂 adapter            # Implémentation des Ports
 ┃   ┃ ┣ 📂 jpa.entity         # Entités JPA (@Entity)
 ┃   ┃ ┗ 📂 jpa.repository     # Interfaces Spring Data
 ┃   ┗ 📂 web                  # API REST (Controllers)

```

---

# 🛠️ 2. Mise en pratique : Le domaine

Nous allons "purifier" notre modèle `Project`. Il ne doit plus avoir d'annotations `@Entity`, `@Id`, `@Column`.

**Exercice :** Créez le record `Project` dans `project/domain/model`.

```java
package fr.corentinbringer.smarttasks.project.domain.model;

import java.time.LocalDateTime;

// C'est un objet pur (POJO/Record). Aucune dépendance à Spring ou Jakarta.
public record Project(
    Long id,
    String tenantId,
    String name,
    LocalDateTime createdOn
) {}

```

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

### 4.2. Le Mapper

Nous avons besoin de convertir `Project` (Domaine) ↔ `ProjectEntity` (BDD).

**Exercice :** Créez `ProjectMapper.java`.

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
        // 1. Convertir Domaine -> Entity
        ProjectEntity entity = projectMapper.toEntity(project);
        
        // 2. Sauvegarder
        ProjectEntity saved = projectRepository.save(entity);
        
        // 3. Retourner Domaine
        return projectMapper.toDomain(saved);
    }

    @Override
    public Page<Project> findAll(String tenantId, Pageable pageable) {
        return projectRepository.findAllByTenantId(tenantId, pageable)
                .map(projectMapper::toDomain);
    }
}

```

---

# 🔄 5. Mise à jour du service

Le `ProjectService` ne dépend plus de `ProjectRepository`. Il dépend de `ProjectPort`.

```java
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectPort projectPort; // Injection de l'interface !

    public ProjectResponse create(ProjectCreateRequest request) {
        // Création du record Domaine (et non plus de l'entité JPA)
        Project project = new Project(
            null, 
            TenantContext.getTenant(), 
            request.name(), 
            LocalDateTime.now()
        );

        Project saved = projectPort.save(project);
        
        return new ProjectResponse(saved.id(), saved.name(), saved.createdOn());
    }
}

```

---

# 🧪 Exercices finaux

C'est un gros chantier de refactoring. À vous de jouer pour le reste :

1. **Task** : Refactorez `Task` en suivant le même modèle :
* `Task` (Record domaine)
* `TaskPort` (Interface)
* `TaskPersistenceAdapter` (Implémentation avec Mapper)


2. **Attachment** : Idem pour les pièces jointes.
3. **MinIO** : Créez un port `FileStoragePort` (interface) et déplacez l'implémentation MinIO dans un adapter `MinioFileStorageAdapter`. Ainsi, votre métier ne dépendra plus de la librairie MinIO directement.

---

# 🏁 Conclusion

Bravo ! Vous avez transformé une application monolithique couplée en une application modulaire et testable.

* Si demain on remplace PostgreSQL par MongoDB, on réécrit juste l'Adapter. Le métier ne change pas.
* Si on remplace MinIO par AWS S3, on crée un nouvel Adapter `S3FileStorageAdapter`.

C'est l'architecture standard des projets d'entreprise modernes.