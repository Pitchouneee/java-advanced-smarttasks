# 02 – JPA & Relations

Dans ce module, nous allons approfondir l’usage de **Spring Data JPA** avec des entités reliées entre elles.  
Objectif : modéliser les projets, utilisateurs et tâches du système **SmartTasks**.

---

# 🎯 Objectifs du module

À la fin de ce chapitre, vous saurez :

* Créer des entités JPA complètes
* Gérer les relations :
  + `@OneToMany`
  + `@ManyToOne`
  + `@ManyToMany` (optionnel)
* Comprendre le chargement **LAZY**/**EAGER**
* Gérer les DTO pour éviter d’exposer votre modèle interne
* Gérer la validation (`@NotBlank`,  `@Email`, etc.)

---

# 🧱 1. Rappel : entité simple

Une entité JPA = un objet Java mappé à une table SQL :

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

    @NotBlank
    private String name;
}
```

---

# 📌 2. Relation Project → Task (OneToMany)

Un projet possède plusieurs tâches.

## 👉 Entité Task

 `domain/Task.java`

```java
@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    private String description;

    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;
}
```

## 👉 Ajouter la relation inverse

Dans `Project.java` :

```java
@OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
@JsonIgnore
private List<Task> tasks = new ArrayList<>();
```

⚠️ `@JsonIgnore` : empêche les boucles infinies en JSON.

---

# 📌 3. Repository pour Task

`repository/TaskRepository.java` :

```java
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
}
```

---

# 🧠 4. Service : gestion des tâches

 `service/TaskService.java`

```java
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public List<Task> findByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    public Task create(Long projectId, Task task) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new NoSuchElementException("Projet introuvable"));
        task.setProject(project);
        return taskRepository.save(task);
    }
}
```

---

# 🌐 5. Controller REST des tâches

 `controller/TaskController.java`

```java
@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public List<Task> findAll(@PathVariable Long projectId) {
        return taskService.findByProject(projectId);
    }

    @PostMapping
    public Task create(@PathVariable Long projectId, @RequestBody Task task) {
        return taskService.create(projectId, task);
    }
}
```

---

# 📦 6. DTO & Validation (bonne pratique)

Ne JAMAIS exposer les entités JPA brutes en production.

👉 Exemple DTO pour Task :

```java
public record TaskDto(
    Long id,
    @NotBlank String title,
    String description,
    LocalDate dueDate
) {}
```

## Mapper simple

```java
public class TaskMapper {
    public static TaskDto toDto(Task t) {
        return new TaskDto(t.getId(), t.getTitle(), t.getDescription(), t.getDueDate());
    }

    public static Task fromDto(TaskDto dto) {
        Task t = new Task();
        t.setTitle(dto.title());
        t.setDescription(dto.description());
        t.setDueDate(dto.dueDate());
        return t;
    }
}
```

---

# 🔍 7. Chargement LAZY vs EAGER

| Mode | Description |
|------|-------------|
| **LAZY** | Les relations sont chargées uniquement si besoin |
| **EAGER** | Les relations sont chargées automatiquement |

Règle d’or :  
👉 **Toujours mettre OneToMany en LAZY**  
👉 **Toujours mettre ManyToOne en LAZY** (pour éviter des cascades SQL)

---

# 🧪 8. Exercice pratique
1. Ajouter une entité **User** avec :
   - nom  
   - email  
   - role  

2. Relation ManyToOne :  
   un utilisateur appartient à une seule entreprise / tenant (pour plus tard)

3. Ajouter les endpoints CRUD pour User

4. Bonus :
   - empêcher deux utilisateurs d’avoir le même email ( `@Column(unique = true)` )
   - ajouter pagination ( `Pageable` )

---

# 📘 Prochain module

➡️ **03 – Sécurité & Multi-Tenancy**

Bravo ! Vous avez maintenant un modèle relationnel complet 🚀
