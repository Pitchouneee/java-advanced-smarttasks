# 04 – Upload de fichiers & Stockage Objet (MinIO)

Dans les architectures cloud modernes, on ne stocke jamais les fichiers utilisateurs sur le disque du serveur (car les serveurs sont éphémères) ni en base de données (car les BLOBs tuent les performances).

On utilise du **Stockage Objet** (Object Storage) compatible S3. Dans ce cours, nous utiliserons **MinIO**, une solution open source compatible S3.

> Attention, depuis fin novembre MinIO Community n'est plus maintenu au profit de la version commerciale, mais pour ici un projet scolaire cela fera l'affaire.

---

# 🎯 Objectifs du module

✅ Comprendre la différence entre **Stockage Bloc** (disque dur) et **Stockage Objet** (S3).
✅ Manipuler des **Flux (Streams)** en Java pour ne pas saturer la mémoire RAM.
✅ Séparer le stockage physique (MinIO) des métadonnées (PostgreSQL).
✅ Implémenter l'upload et le téléchargement en **Streaming**.

---

# 📦 1. Infrastructure : MinIO

Assurez-vous que votre conteneur MinIO est lancé via Docker Compose.

1. Accédez à la console : [http://localhost:9001](http://localhost:9001)
2. Login : `admin` / `password`
3. **Action requise :** Créez un **Bucket** nommé `smarttasks`.
* *Un bucket est l'équivalent d'un lecteur ou d'un dossier racine dans le monde S3.*

---

# ⚙️ 2. Configuration Spring

Nous avons besoin du SDK MinIO pour communiquer avec le service.

### 2.1. Dépendance (`pom.xml`)

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>

```

### 2.2. Configuration (`MinioConfig.java`)

Nous allons créer un Bean `MinioClient` qui sera injecté partout où nous en aurons besoin.

**Exercice :** Créez la classe `configuration/minio/MinioConfig.java`.

```java
@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String url;
    
    // ... autres @Value pour accessKey, secretKey ...

    @Bean
    public MinioClient minioClient() {
        // TODO: Construire et retourner le client MinIO
        // Utilisez MinioClient.builder()...
        return null;
    }
}

```

---

# 🧱 3. Modèle de données : Métadonnées

En base de données, nous ne stockons que la "carte d'identité" du fichier. Le fichier lui-même sera dans MinIO, identifié par une clé unique (`objectKey`).

**Exercice :** Créez l'entité `Attachment` dans `project/model/Attachment.java`.

```java
@Entity
@Table(name = "attachments")
// Lombok...
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false, nullable = false)
    private String tenantId;

    // TODO: Ajoutez les champs suivants :
    // - objectKey (String) : UUID unique du fichier dans MinIO
    // - originalName (String) : Nom d'origine du fichier (ex: rapport.pdf)
    // - mimeType (String) : Type de contenu (ex: application/pdf)
    // - size (long) : Taille en octets
    // - uploadedOn (LocalDateTime) : Date d'upload

    // TODO: Relation ManyToOne vers Task (Lazy !)
    
    @PrePersist
    protected void onCreate() {
        this.uploadedOn = LocalDateTime.now();
    }
}

```

---

# 🧩 4. Le Service technique (Infrastructure)

Nous allons isoler la complexité de MinIO dans un service dédié. Ce service ne doit pas connaître les entités JPA, il manipule juste des fichiers.

**Exercice :** Implémentez `minio/service/MinioService.java`.

```java
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    
    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Upload un fichier vers MinIO.
     * @return L'ID unique (Object Name) généré pour ce fichier.
     */
    public String uploadFile(MultipartFile file) throws Exception {
        // 1. Générer un nom unique pour éviter les collisions (UUID)
        String objectName = UUID.randomUUID().toString();

        // 2. Envoyer le flux (InputStream) à MinIO
        // Astuce : file.getInputStream(), file.getSize(), file.getContentType()
        try (InputStream is = file.getInputStream()) {
            // TODO: Appeler minioClient.putObject(...)
        }
        
        return objectName;
    }

    /**
     * Récupère le flux de données d'un fichier.
     */
    public InputStream downloadFile(String objectKey) throws Exception {
        // TODO: Appeler minioClient.getObject(...)
        return null;
    }
}

```

---

# 📎 5. Le Service métier (`AttachmentService`)

C'est ici qu'on orchestre tout : vérifier les droits, uploader physiquement, puis sauvegarder les infos en base.

**Exercice :** Complétez `AttachmentService.java`.

```java
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskService taskService; // Pour récupérer la tâche
    private final MinioService minioService;

    @Transactional
    public AttachmentResponse create(Long taskId, MultipartFile file) {
        // 1. Récupérer la tâche (vérifie implicitement le tenant via le service)
        Task task = taskService.findById(taskId);

        try {
            // 2. Upload physique
            String objectKey = minioService.uploadFile(file);

            // 3. Création de l'entité Attachment
            Attachment attachment = new Attachment();
            attachment.setTenantId(TenantContext.getTenant());
            attachment.setTask(task);
            attachment.setObjectKey(objectKey);
            attachment.setOriginalName(file.getOriginalFilename());
            attachment.setMimeType(file.getContentType());
            attachment.setSize(file.getSize());

            // 4. Sauvegarde BDD et retour DTO
            Attachment saved = attachmentRepository.save(attachment);
            return mapToResponse(saved);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'upload", e);
        }
    }
}

```

---

# 🌐 6. Controller et Streaming

Pour le téléchargement, il est crucial de **streamer** la réponse. Si on charge un fichier de 1 Go en mémoire vive avant de l'envoyer, le serveur va crasher (`OutOfMemoryError`).

Spring permet de renvoyer un `InputStreamResource` qui connectera directement le flux MinIO au flux HTTP de sortie.

**Exercice :** Dans `AttachmentController.java`.

```java
@GetMapping("/{id}/download")
public ResponseEntity<InputStreamResource> downloadAttachment(@PathVariable Long id) {
    // 1. Appel au service pour récupérer un DTO contenant le Stream et les métadonnées
    DownloadResult result = attachmentService.download(id);

    // 2. Encodage du nom de fichier (pour gérer les espaces et accents)
    String encodedName = URLEncoder.encode(result.fileName(), StandardCharsets.UTF_8);

    // 3. Construction de la réponse HTTP avec les bons headers
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"")
            .header(HttpHeaders.CONTENT_TYPE, result.mimeType())
            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(result.size()))
            .body(result.resource());
}

```

---

# 🚀 Validation

1. Assurez-vous que MinIO tourne.
2. Prenez une Tâche existante (ID 1 par exemple).
3. Uploadez un fichier PDF via Postman/Bruno :
* **POST** `http://localhost:8080/api/tasks/1/attachments`
* **Body** : `form-data`, clé `file` (type File).


4. Vérifiez dans la console MinIO que le fichier est apparu (avec un nom UUID).
5. Vérifiez dans PostgreSQL que la ligne est créée dans `attachments`.
6. Téléchargez le fichier via l'API.

---

# ➡️ Prochain module

Votre backend est fonctionnel et gère des fichiers !
Il est temps de le rendre utilisable par les autres développeurs.
Passez au chapitre suivant : **05 – Swagger / OpenAPI**.