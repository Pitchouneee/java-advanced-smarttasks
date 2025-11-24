# 07 – Clean Architecture & Refactoring

Ce module vise à améliorer la structure de votre projet SmartTasks en appliquant les principes de la **Clean Architecture** et des bonnes pratiques de développement logiciel.

Objectif :  
➡️ rendre votre code **plus maintenable**, **testable**, et **facilement extensible**.

---

# 🎯 Objectifs du module

À la fin de ce chapitre vous serez capables de :

* Comprendre les principes de la Clean Architecture
* Structurer votre projet Spring Boot en couches claires
* Séparer le domaine métier des aspects techniques
* Implémenter des DTO, mappers, ports/adapters
* Factoriser le code et éliminer les duplications
* Mettre en place une architecture professionnelle

---

# 🧱 1. Pourquoi la Clean Architecture ?

Problèmes fréquents dans un projet non structuré :

* Logique dans les contrôleurs
* Couplage fort avec les frameworks
* Difficulté à tester
* Entités JPA exposées directement
* Multiplication des dépendances circulaires
* Code imprévisible à maintenir

La Clean Architecture vise à **séparer le métier du reste**.

---

# 🌀 2. Les couches Clean Architecture

Voici le modèle classique :

```
               +-------------------------+
               |     Presentation        |
               |  (controller, DTOs)     |
               +------------+------------+
                            |
               +------------+------------+
               |      Application        |
               | (services, use-cases)   |
               +------------+------------+
                            |
               +------------+------------+
               |        Domain           |
               | (business models, rules)|
               +------------+------------+
                            |
               +------------+------------+
               |   Infrastructure         |
               | (JPA, MinIO, Security)   |
               +---------------------------+
```

Règle d'or :  
👉 Les couches supérieures connaissent les couches inférieures, **jamais l'inverse**.

---

# 🗂️ 3. Organisation recommandée pour SmartTasks

```
src/main/java/com/smarttasks
 ┣ domain/                 # Entités métiers + règles
 ┣ application/            # Services métiers (use cases)
 ┣ infrastructure/         # JPA, MinIO, Security
 ┣ presentation/           # Controllers REST + DTO
 ┗ SmartTasksApplication.java
```

---

# 🧩 4. Domain (métier pur)

Exemple : `domain/Project.java`

* sans annotation JPA
* sans référence à des frameworks
* juste les règles métier

```java
public class Project {
    private Long id;
    private String name;

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Nom invalide");
        }
        this.name = newName;
    }
}
```

---

# 🏗️ 5. Infrastructure (JPA, MinIO…)

Ici vous placez les implémentations techniques.

Exemple : JPA pour Project  
 `infrastructure/jpa/ProjectEntity.java`

```java
@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "tenant_id")
    private String tenantId;
}
```

Repository JPA :

```java
public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, Long> {
    List<ProjectEntity> findByTenantId(String tenantId);
}
```

---

# 🔌 6. Ports & Adapters

Les **ports** définissent des interfaces métier.  
Les **adapters** connectent ces ports au monde technique.

Port côté domaine :

```java
public interface ProjectRepository {
    Project save(Project project);
    List<Project> findAllByTenant(String tenant);
}
```

Adapter JPA :

```java
@Component
@RequiredArgsConstructor
public class ProjectJpaAdapter implements ProjectRepository {

    private final ProjectJpaRepository jpa;

    @Override
    public Project save(Project project) {
        ProjectEntity e = new ProjectEntity(
            project.getId(),
            project.getName(),
            TenantContext.getTenant()
        );
        ProjectEntity saved = jpa.save(e);
        return new Project(saved.getId(), saved.getName());
    }

    @Override
    public List<Project> findAllByTenant(String tenant) {
        return jpa.findByTenantId(tenant).stream()
            .map(e -> new Project(e.getId(), e.getName()))
            .toList();
    }
}
```

---

# 🧠 7. Application layer (services métier)

Les services orchestrent les use cases :

```java
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository repository;

    public List<Project> listAll() {
        return repository.findAllByTenant(TenantContext.getTenant());
    }

    public Project create(String name) {
        Project project = new Project(null, name);
        return repository.save(project);
    }
}
```

---

# 🖥️ 8. Presentation layer (REST API)

Les contrôleurs REST appellent les services + gèrent les DTO.

DTO :

```java
public record ProjectDto(Long id, String name) {}
```

Mapper :

```java
public class ProjectMapper {
    public static ProjectDto toDto(Project p) {
        return new ProjectDto(p.getId(), p.getName());
    }
}
```

Controller :

```java
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService service;

    @GetMapping
    public List<ProjectDto> all() {
        return service.listAll().stream()
            .map(ProjectMapper::toDto)
            .toList();
    }

    @PostMapping
    public ProjectDto create(@RequestBody CreateProjectRequest request) {
        return ProjectMapper.toDto(service.create(request.name()));
    }
}
```

---

# 🔧 9. Critères de qualité

Votre projet est propre si :

* aucune entité JPA n'est exposée en JSON
* aucune logique métier n'est dans les contrôleurs
* aucun contrôleur n'appelle un repository directement
* aucun DTO n'est utilisé dans le domaine
* aucun service métier ne dépend d’un framework
* les tests unitaires fonctionnent **sans lancer Spring**

---

# 🧹 10. Atelier refactoring

Refactorer ensemble :

### 🔹 Étape 1  

Créer 4 packages : `domain` , `application` , `infrastructure` , `presentation` .

### 🔹 Étape 2  

Isoler les entités JPA dans `infrastructure/jpa` .

### 🔹 Étape 3  

Créer des ports (interfaces) dans `domain` .

### 🔹 Étape 4  

Implémenter les ports dans les adapters.

### 🔹 Étape 5  

Mettre les services métiers dans `application` .

### 🔹 Étape 6  

Mettre les controllers & DTO dans `presentation` .

---

# 📝 Exercices du module
1. Appliquer la Clean Architecture à **Task** :
   - entité métier
   - port
   - adapter
   - service
   - contrôleur
   - DTO + mapper

2. Faire de même pour **FileAttachment**

3. Bonus :
   - isoler `TenantContext` dans infrastructure
   - ajouter une interface `TenantProvider` côté domaine

---

# 📘 Prochain module

➡️ **08 – Monolithe vs Microservices**

Vous avez maintenant une structure de projet professionnelle, propre et scalable 🚀
