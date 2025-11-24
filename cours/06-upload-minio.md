# 06 – Upload de fichiers avec MinIO

Dans ce module, nous allons ajouter une fonctionnalité très fréquente dans les applications métiers :  
➡️ l’**upload de fichiers** (pièces jointes) liés aux tâches, en utilisant **MinIO** comme stockage objet compatible S3.

---

# 🎯 Objectifs du module

À la fin du chapitre, vous serez capables de :

* Comprendre le principe d’un **stockage objet** (S3 / MinIO)
* Lancer un conteneur MinIO avec Docker
* Configurer un client MinIO dans Spring Boot
* Créer un endpoint REST pour uploader un fichier
* Lier un fichier à une **Task** (métadonnées en base, contenu dans MinIO)
* (Bonus) Générer une URL de téléchargement

---

# 📦 1. Rappel : conteneur MinIO

Dans votre `docker-compose.yml` :

```yaml
services:
  minio:
    image: minio/minio
    container_name: minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: admin
      MINIO_ROOT_PASSWORD: password
    ports:
      - "9000:9000"
      - "9001:9001"
```

Lancer MinIO :

```bash
docker compose up -d minio
```

Accès à la console web :  
➡️ http://localhost:9001  
Utilisateur : `admin`

Mot de passe : `password`

Créer un **bucket** nommé par exemple : `smarttasks` .

---

# 🔗 2. Dépendance du client MinIO

Nous allons utiliser le **client officiel Java MinIO**.

Dans `pom.xml` :

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.10</version>
</dependency>
```

> La version peut évoluer, adapter si besoin.

---

# ⚙️ 3. Configuration MinIO dans Spring

Dans `application.yml` :

```yaml
minio:
  url: http://localhost:9000
  access-key: admin
  secret-key: password
  bucket: smarttasks
```

Créer une classe de configuration :  
 `config/MinioConfig.java`

```java
@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

---

# 🧱 4. Entité FileAttachment

Nous ne stockons **pas** le contenu du fichier en base, seulement des métadonnées :

* nom d’origine
* type MIME
* taille
* clé du fichier dans MinIO
* lien avec la Task
* tenant

`domain/FileAttachment.java` :

```java
@Entity
@Table(name = "file_attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFilename;

    private String contentType;

    private Long size;

    private String objectKey; // identifiant dans MinIO

    @Column(name = "tenant_id")
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;
}
```

Repository :  
`repository/FileAttachmentRepository.java` :

```java
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
    List<FileAttachment> findByTaskId(Long taskId);
}
```

---

# 🧩 5. Service de stockage MinIO

Créer `service/FileStorageService.java` :

```java
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public String upload(String objectKey, InputStream data, long size, String contentType) {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(data, size, -1)
                    .contentType(contentType)
                    .build();

            minioClient.putObject(args);
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Erreur upload MinIO", e);
        }
    }

    public InputStream download(String objectKey) {
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build();
            return minioClient.getObject(args);
        } catch (Exception e) {
            throw new RuntimeException("Erreur download MinIO", e);
        }
    }
}
```

---

# 📎 6. Service métier pour les pièces jointes

`service/FileAttachmentService.java` :

```java
@Service
@RequiredArgsConstructor
public class FileAttachmentService {

    private final FileAttachmentRepository repository;
    private final TaskRepository taskRepository;
    private final FileStorageService storageService;

    public FileAttachment uploadForTask(Long taskId, MultipartFile file) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));

        String tenantId = TenantContext.getTenant();
        String objectKey = UUID.randomUUID() + "_" + file.getOriginalFilename();

        try (InputStream is = file.getInputStream()) {
            storageService.upload(objectKey, is, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Erreur lecture fichier", e);
        }

        FileAttachment attachment = new FileAttachment();
        attachment.setOriginalFilename(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());
        attachment.setSize(file.getSize());
        attachment.setObjectKey(objectKey);
        attachment.setTenantId(tenantId);
        attachment.setTask(task);

        return repository.save(attachment);
    }

    public List<FileAttachment> listForTask(Long taskId) {
        return repository.findByTaskId(taskId);
    }
}
```

---

# 🌐 7. Controller REST d’upload

`controller/FileAttachmentController.java` :

```java
@RestController
@RequestMapping("/api/tasks/{taskId}/attachments")
@RequiredArgsConstructor
public class FileAttachmentController {

    private final FileAttachmentService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileAttachment upload(
            @PathVariable Long taskId,
            @RequestPart("file") MultipartFile file
    ) {
        return service.uploadForTask(taskId, file);
    }

    @GetMapping
    public List<FileAttachment> list(@PathVariable Long taskId) {
        return service.listForTask(taskId);
    }
}
```

Test rapide via `curl` :

```bash
curl -X POST "http://localhost:8080/api/tasks/1/attachments"   -H "X-Tenant-ID: demo"   -H "Content-Type: multipart/form-data"   -F "file=@/chemin/vers/fichier.pdf"
```

---

# ⬇️ 8. (Bonus) Endpoint de téléchargement

Pour simplifier, on peut renvoyer le fichier en direct :

```java
@GetMapping("/{attachmentId}/download")
public ResponseEntity<Resource> download(@PathVariable Long attachmentId) {
    FileAttachment attachment = repository.findById(attachmentId)
            .orElseThrow(() -> new NoSuchElementException("Attachment not found"));

    InputStream is = storageService.download(attachment.getObjectKey());
    InputStreamResource resource = new InputStreamResource(is);

    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(attachment.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename="" + attachment.getOriginalFilename() + """)
            .body(resource);
}
```

---

# 🖥️ 9. Intégration côté front (idée)

Dans le front React, vous pouvez :

* Ajouter un formulaire `input type="file"` sur la page d’une tâche
* Envoyer le fichier avec `FormData` :

```ts
const formData = new FormData();
formData.append("file", file);

await apiClient.post(`/tasks/${taskId}/attachments`, formData, {
  headers: { "Content-Type": "multipart/form-data" },
});
```

---

# 📝 Exercices
1. Limiter la taille des fichiers (ex : max 10 Mo)
2. Restreindre les types MIME (PDF, images uniquement)
3. Ajouter une colonne `uploadedAt` dans `FileAttachment`
4. Afficher la liste des pièces jointes dans le front avec :
   - nom
   - taille
   - lien de téléchargement

---

# 📘 Prochain module

➡️ **07 – Clean Architecture & refactoring**

Vous avez maintenant un système de pièces jointes complet, prêt pour un usage réel 🚀
