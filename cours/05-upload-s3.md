# 05 – Upload de fichiers & Stockage Objet (S3)

Dans les architectures cloud modernes, on ne stocke jamais les fichiers utilisateurs sur le disque du serveur (car les serveurs sont éphémères) ni en base de données (car les BLOBs tuent les performances).

On utilise du **Stockage Objet** (Object Storage). Le standard de fait est l'**API S3** d'Amazon : presque tous les fournisseurs (AWS, Scaleway, OVH, Cloudflare R2...) et les serveurs open source (RustFS, Garage, SeaweedFS...) la parlent.

Dans ce cours :
* côté **code**, on utilise le **SDK AWS pour Java v2**, le client S3 officiel ;
* côté **infrastructure locale**, on lance **RustFS**, un serveur open source compatible S3, dans Docker.

> 📜 **Pourquoi pas MinIO ?** MinIO était la référence pour un S3 local. Depuis fin 2025, l'édition Community n'est plus maintenue (plus d'images Docker ni de correctifs publiés). Ce cours l'utilisait avec le SDK MinIO : il a fallu changer la dépendance, la configuration **et** le code. Avec le SDK S3 standard, le prochain changement de serveur ne touchera que `application.yaml`.

> 🔒 **À retenir : ne pas dépendre d'un fournisseur.** On code contre un **standard** (l'API S3), pas contre un produit. Le serveur devient interchangeable : RustFS en local, AWS S3 ou Scaleway en production, sans changer une ligne de Java. On retrouvera cette idée au chapitre 06 avec les ports et adapters.

---

# 🎯 Objectifs du module

✅ Comprendre la différence entre **Stockage Bloc** (disque dur) et **Stockage Objet** (S3).
✅ Manipuler des **Flux (Streams)** en Java pour ne pas saturer la mémoire RAM.
✅ Séparer le stockage physique (S3) des métadonnées (PostgreSQL).
✅ Implémenter l'upload et le téléchargement en **Streaming**.

---

# 📦 1. Infrastructure : RustFS

Assurez-vous que le conteneur `smarttasks-rustfs` est lancé (`docker compose up -d`).

| | |
|---|---|
| API S3 (utilisée par Spring) | `http://localhost:9000` |
| Console web | [http://localhost:9001](http://localhost:9001) |
| Identifiants | `rustfsadmin` / `rustfsadmin` |

Vocabulaire S3 :
* un **bucket** est l'équivalent d'un dossier racine (ici : `smarttasks`) ;
* un **objet** est un fichier, identifié dans son bucket par une **clé** (`objectKey`) ;
* il n'y a pas de vrais dossiers : `factures/2026/a.pdf` est juste une clé qui contient des `/`.

Pas besoin de créer le bucket à la main : l'application le créera au démarrage (§2.3). Vous pourrez ensuite le voir apparaître dans la console.

---

# ⚙️ 2. Configuration Spring

### 2.1. Dépendance (`pom.xml`)

Le SDK AWS est découpé en modules (S3, SQS, DynamoDB...). Un **BOM** (*Bill Of Materials*) aligne leurs versions : on le déclare une fois dans `<dependencyManagement>`, puis on ajoute les modules **sans version**.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>2.57.12</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- ... -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
    </dependency>
</dependencies>
```

> 💡 C'est le même mécanisme que le parent `spring-boot-starter-parent`, qui fixe pour vous les versions de Hibernate, Jackson, etc.

### 2.2. Propriétés (`application.yaml`)

```yaml
storage:
  s3:
    endpoint: http://localhost:9000
    region: us-east-1        # requis par le SDK, ignoré par RustFS
    access-key: rustfsadmin
    secret-key: rustfsadmin
    bucket: smarttasks
```

Plutôt qu'une série de `@Value`, on regroupe ces propriétés dans un **record** typé :

```java
@ConfigurationProperties(prefix = "storage.s3")
public record S3Properties(
        String endpoint,
        String region,
        String accessKey,   // ← access-key (Spring fait la correspondance kebab-case → camelCase)
        String secretKey,
        String bucket
) {}
```

> 🔐 En production, les identifiants ne sont **jamais** écrits dans le yaml : on les passe par variables d'environnement (`STORAGE_S3_ACCESS_KEY`...) ou par un gestionnaire de secrets.

### 2.3. Configuration (`S3Config.java`)

Nous allons créer un Bean `S3Client` qui sera injecté partout où nous en aurons besoin.

**Exercice :** Créez la classe `configuration/storage/S3Config.java`.

```java
@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(S3Properties props) {
        return S3Client.builder()
                // TODO 1 : endpointOverride(URI.create(...))  → sinon le SDK appelle AWS
                // TODO 2 : region(Region.of(...))
                // TODO 3 : credentialsProvider(StaticCredentialsProvider.create(
                //              AwsBasicCredentials.create(accessKey, secretKey)))
                // TODO 4 : forcePathStyle(true)
                .build();
    }

    @Bean
    public ApplicationRunner createBucketIfMissing(S3Client s3Client, S3Properties props) {
        return args -> {
            // TODO 5 : s3Client.headBucket(...) ; si NoSuchBucketException → s3Client.createBucket(...)
        };
    }
}
```

Explications :

| Réglage | Pourquoi |
|---|---|
| `endpointOverride` | Par défaut, le SDK appelle `s3.<region>.amazonaws.com`. On le redirige vers RustFS. |
| `region` | Obligatoire : elle entre dans la signature des requêtes. RustFS accepte n'importe quelle valeur. |
| `forcePathStyle(true)` | AWS utilise des URL `http://<bucket>.s3.amazonaws.com/<clé>` (*virtual-hosted*). En local, `smarttasks.localhost` ne se résout pas : on force `http://localhost:9000/<bucket>/<clé>` (*path-style*). |
| `ApplicationRunner` | Code exécuté une fois au démarrage : on crée le bucket s'il manque, au lieu d'un clic manuel dans la console. |

> ⚠️ **Piège des checksums.** Depuis la version 2.30, le SDK ajoute par défaut des checksums CRC à chaque envoi. Certains serveurs compatibles S3 ne les gèrent pas encore et répondent par une erreur peu explicite. La solution les limite aux cas où l'API les exige :
> ```java
> .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
> .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
> ```
> C'est un bon exemple de ce que veut dire « compatible S3 » : compatible avec **la plupart** de l'API, pas forcément avec toutes ses évolutions.

---

# 🧱 3. Modèle de données : Métadonnées

En base de données, nous ne stockons que la "carte d'identité" du fichier. Le fichier lui-même sera dans le bucket S3, identifié par une clé unique (`objectKey`).

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
    // - objectKey (String) : UUID unique du fichier dans le bucket S3
    // - originalName (String) : Nom d'origine du fichier (ex: rapport.pdf)
    // - mimeType (String) : Type de contenu (ex: application/pdf)
    // - size (long) : Taille en octets
    // - createdOn (Instant) : Date d'upload (même nom que sur Project et Task)

    // TODO: Relation ManyToOne vers Task (Lazy !)
    
    @PrePersist
    protected void onCreate() {
        this.createdOn = Instant.now();
    }
}

```

Puis ajoutez la relation inverse dans `Task`, sur le même modèle que `Project` → `Task` (module 02) :

```java
// Dans model/Task.java
@OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<Attachment> attachments;
```

Grâce à la cascade, supprimer une tâche (ou son projet) supprime aussi les lignes `attachments`. Les **fichiers** du bucket, eux, ne sont pas gérés par JPA : il faudra les supprimer à part (§5.2).

> 💡 Pourquoi un UUID comme clé et pas le nom d'origine ? Deux utilisateurs peuvent envoyer `rapport.pdf` : avec un UUID, pas de collision, et le nom d'un fichier ne révèle rien sur son propriétaire.


### 3.1. Les DTO et le repository

```java
// project/model/AttachmentResponse.java : ce que l'API renvoie (voir api-spec.md)
public record AttachmentResponse(
        Long id,
        String originalName,
        String mimeType,
        long size,
        String data,          // URL relative de téléchargement : /api/attachments/{id}/download
        Instant createdOn
) {}

// project/model/DownloadResult.java : ce que le service renvoie au controller pour un téléchargement
public record DownloadResult(
        InputStreamResource resource,   // le flux S3, emballé pour Spring
        String fileName,
        String mimeType,
        long size
) {}
```

```java
// project/repository/AttachmentRepository.java
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    // Liste paginée des pièces jointes d'une tâche (projection, comme au module 02)
    @Query("""
            SELECT new fr.corentinbringer.smarttasks.project.model.AttachmentResponse(
                a.id, a.originalName, a.mimeType, a.size,
                '/api/attachments/' || a.id || '/download',
                a.createdOn
            )
            FROM Attachment a
            WHERE a.task.id = :taskId AND a.tenantId = :tenantId
            ORDER BY a.createdOn DESC
            """)
    Page<AttachmentResponse> findAllByTaskIdAndTenantId(@Param("taskId") Long taskId,
                                                        @Param("tenantId") String tenantId,
                                                        Pageable pageable);

    Optional<Attachment> findByIdAndTenantId(Long id, String tenantId);
}
```

`||` est la concaténation de chaînes en JPQL : l'URL `data` est construite directement par la requête.

---

# 🧩 4. Le Service technique (Infrastructure)

Nous allons isoler l'accès au stockage dans un service dédié. Ce service ne doit pas connaître les entités JPA, il manipule juste des fichiers.

### 4.1. Une exception « maison » pour les erreurs techniques

Le SDK AWS lève ses propres exceptions (`S3Exception`, `NoSuchKeyException`, `SdkClientException`...), et `file.getInputStream()` lève une `IOException`. On ne veut pas que tout le code dépende de ces types : le service les **traduit** en une seule exception.

**Exercice :** Créez `exception/StorageException.java`, troisième membre de notre hiérarchie *sealed* (module 01, §9) :

```java
/** Erreur technique du stockage objet : ce n'est pas la faute du client → 500. */
public final class StorageException extends SmartTasksException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Ajoutez-la au `permits` de `SmartTasksException` :

```java
public sealed abstract class SmartTasksException extends RuntimeException
        permits ResourceNotFoundException, InvalidRequestException, StorageException {
```

Relancez la compilation : **elle échoue** dans `ApiExceptionHandler` (`the switch expression does not cover all possible input values`). C'est exactement ce qu'on voulait au module 01 : le compilateur nous rappelle de traiter le nouveau cas.

```java
return switch (ex) {
    case ResourceNotFoundException e -> error(HttpStatus.NOT_FOUND, "Resource not found");
    case InvalidRequestException e -> error(HttpStatus.BAD_REQUEST, e.getMessage());
    case StorageException e -> {
        log.error("Object storage error", e);   // le détail technique reste dans les logs
        yield error(HttpStatus.INTERNAL_SERVER_ERROR, "File storage error");
    }
};
```

> 💡 `yield` renvoie la valeur d'une branche de `switch` écrite en bloc `{ ... }` (quand on a besoin de plusieurs instructions).

### 4.2. Le service

**Exercice :** Implémentez `storage/service/S3StorageService.java`.

```java
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Properties props;

    /**
     * Envoie un fichier dans le bucket.
     * @return la clé unique (objectKey) générée pour ce fichier.
     */
    public String uploadFile(MultipartFile file) {
        // 1. Générer une clé unique pour éviter les collisions (UUID)
        String objectKey = UUID.randomUUID().toString();

        // 2. Envoyer le flux (InputStream) sans le charger en mémoire
        try (InputStream is = file.getInputStream()) {
            // TODO: s3Client.putObject(
            //           b -> b.bucket(...).key(...).contentType(...),
            //           RequestBody.fromInputStream(is, file.getSize()));
        } catch (IOException | SdkException e) {
            throw new StorageException("Upload failed for object " + objectKey, e);
        }

        return objectKey;
    }

    /** Récupère le flux de données d'un fichier. */
    public InputStream downloadFile(String objectKey) {
        // TODO: return s3Client.getObject(b -> b.bucket(...).key(...));
        //       en traduisant SdkException en StorageException
        return null;
    }

    /** Supprime un fichier (utilisé par la compensation, §5.2). */
    public void deleteFile(String objectKey) {
        // TODO: s3Client.deleteObject(...), même traduction des erreurs
    }
}

```

Quelques points sur l'API du SDK v2 :
* les méthodes acceptent un **lambda de builder** (`b -> b.bucket(...).key(...)`) : c'est un raccourci pour `PutObjectRequest.builder()...build()` ;
* `RequestBody.fromInputStream(is, size)` exige la **taille** : S3 doit connaître le `Content-Length` avant de recevoir les octets ;
* `getObject` renvoie un `ResponseInputStream`, c'est-à-dire un `InputStream` branché sur la connexion HTTP : le fichier n'est **pas** chargé en mémoire. Il doit être fermé après lecture (Spring s'en charge en fin de réponse, §6) ;
* les erreurs du SDK sont des exceptions **non vérifiées** qui héritent toutes de `SdkException` : un seul `catch` suffit.

> 💡 Pourquoi plus de `throws Exception` ? Une méthode qui déclare `throws Exception` oblige chaque appelant à écrire un `try/catch` qui ne sait pas quoi faire de l'erreur, et finit souvent par la transformer en n'importe quoi (voir l'encadré du §5.3).

---

# 📎 5. Le Service métier (`AttachmentService`)

C'est ici qu'on orchestre tout : vérifier les droits, stocker le fichier, puis sauvegarder les infos en base.

### 5.1. Le problème : deux systèmes, pas de transaction commune

L'upload écrit dans **deux** systèmes : le bucket S3, puis PostgreSQL. Un `@Transactional` ne couvre que la base : il ne sait pas « annuler » un fichier déjà envoyé dans S3.

```
1. putObject (S3)        ✅ le fichier est dans le bucket
2. save (PostgreSQL)     ❌ échec (base indisponible, contrainte violée...)
                         → rollback de la base... mais le fichier, lui, reste dans le bucket
```

Résultat : un **fichier orphelin**, que plus aucune ligne ne référence. Personne ne le supprimera jamais, et on le paiera tous les mois au fournisseur S3.

> 🤔 Et dans l'autre ordre (base d'abord, fichier ensuite) ? Si l'upload échoue après le commit, la base référence un fichier qui n'existe pas : c'est **pire**, car l'utilisateur voit une pièce jointe qu'il ne peut pas télécharger. Le fichier orphelin est le moindre mal.

### 5.2. La solution : une action de compensation

Puisqu'on ne peut pas annuler automatiquement, on **compense** à la main : si l'écriture en base échoue, on supprime le fichier qu'on vient d'envoyer.

**Exercice :** Complétez `AttachmentService.java`.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskService taskService;
    private final S3StorageService storageService;

    // ⚠️ Pas de @Transactional ici (voir l'explication sous le code)
    public AttachmentResponse create(Long taskId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidRequestException("File is empty");   // 400
        }

        // 1. Récupérer la tâche (filtrée par tenant : 404 si elle n'est pas à l'utilisateur)
        Task task = taskService.findById(taskId);

        // 2. Stocker le fichier. S'il échoue : StorageException (500), rien à annuler.
        String objectKey = storageService.uploadFile(file);

        // 3. Sauvegarder les métadonnées. S'il échoue : on compense.
        try {
            Attachment attachment = new Attachment();
            attachment.setTenantId(TenantContext.getTenant());
            attachment.setTask(task);
            attachment.setObjectKey(objectKey);
            attachment.setOriginalName(file.getOriginalFilename());
            attachment.setMimeType(file.getContentType());
            attachment.setSize(file.getSize());

            return toResponse(attachmentRepository.save(attachment));

        } catch (RuntimeException e) {
            // TODO: supprimer le fichier (storageService.deleteFile), puis relancer e
        }
    }

    private static AttachmentResponse toResponse(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getOriginalName(), a.getMimeType(), a.getSize(),
                "/api/attachments/" + a.getId() + "/download", a.getCreatedOn());
    }
}

```

Le service expose aussi le téléchargement :

```java
public DownloadResult download(Long attachmentId) {
    Attachment attachment = attachmentRepository.findByIdAndTenantId(attachmentId, TenantContext.getTenant())
            .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

    // StorageException (500) si le fichier est introuvable dans le bucket
    InputStream is = storageService.downloadFile(attachment.getObjectKey());

    return new DownloadResult(new InputStreamResource(is),
            attachment.getOriginalName(), attachment.getMimeType(), attachment.getSize());
}
```

Et l'upload est un nouvel endpoint de `TaskController` (la pièce jointe appartient à une tâche) :

```java
@PostMapping("/{id}/attachments")
@ResponseStatus(HttpStatus.CREATED)
public AttachmentResponse createAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
    return attachmentService.create(id, file);
}
```

Trois détails importants :

* **Pourquoi pas de `@Transactional` ?** Avec une transaction autour de la méthode, le commit a lieu **après** le `return`, donc après notre `catch` : une erreur au commit ne serait pas compensée. Sans `@Transactional`, `save()` ouvre et commite sa propre transaction. S'il rend la main, la ligne est en base ; s'il lève une exception, rien n'est écrit. Le `catch` couvre donc tous les cas.
* **On relance l'erreur d'origine** (`throw e`) : c'est elle qui explique l'échec, pas la compensation.
* **La compensation peut elle-même échouer** (S3 tombé entre-temps). Elle ne doit pas masquer l'erreur d'origine : on l'entoure d'un `try/catch` qui se contente de journaliser (`log.warn("... orphaned ...")`). Il restera un orphelin, qu'un batch de nettoyage pourra rattraper.

> 🔗 C'est exactement le principe du pattern **SAGA**, que l'on reverra au chapitre 08 : quand une opération touche plusieurs systèmes sans transaction commune, chaque étape réussie doit avoir une **action de compensation** qui l'annule si une étape suivante échoue.
>
> La suppression suit la même logique, dans l'autre sens : on supprime d'abord la ligne en base, puis le fichier **après le commit** (`@TransactionalEventListener(phase = AFTER_COMMIT)` dans la solution).

### 5.3. Le bon code HTTP pour chaque erreur

| Situation | Exception | Code HTTP | Responsable |
|---|---|---|---|
| Fichier vide | `InvalidRequestException` | `400 Bad Request` | le client |
| Tâche inexistante ou d'un autre tenant | `ResourceNotFoundException` | `404 Not Found` | le client |
| Fichier trop gros (> 1 Mo par défaut) | `MaxUploadSizeExceededException` | `413 Content Too Large` | le client |
| S3 injoignable, objet absent du bucket | `StorageException` | `500 Internal Server Error` | le serveur |

> ⚠️ **Erreur classique** : tout attraper avec `catch (Exception e)` et relancer une `ResourceNotFoundException`. L'API répond alors `404` quand S3 est en panne. Le front affiche « introuvable », l'utilisateur pense que son fichier a disparu, et l'équipe d'exploitation ne voit aucune erreur `500` dans ses tableaux de bord.
>
> Règle : **4xx = le client peut corriger sa requête, 5xx = c'est au serveur de se réparer.**

> 💡 La taille maximale d'un fichier se règle dans `application.yaml` : `spring.servlet.multipart.max-file-size: 10MB` (et `max-request-size`).
>
> ✅ Rien à coder pour le `413` : `MaxUploadSizeExceededException` fait partie des erreurs déjà traitées par `ResponseEntityExceptionHandler`, dont hérite notre `ApiExceptionHandler` (module 01, §9.3).

---

# 🌐 6. Controller et Streaming

Pour le téléchargement, il est crucial de **streamer** la réponse. Si on charge un fichier de 1 Go en mémoire vive avant de l'envoyer, le serveur va crasher (`OutOfMemoryError`).

Spring permet de renvoyer un `InputStreamResource` qui connectera directement le flux S3 (`ResponseInputStream`) au flux HTTP de sortie, puis le fermera.

### 6.1. Le nom du fichier : `Content-Disposition`

Le header `Content-Disposition: attachment; filename="..."` indique au navigateur de télécharger le fichier sous ce nom. Le piège : les espaces et les accents.

❌ **À ne pas faire :**

```java
String encodedName = URLEncoder.encode(result.fileName(), StandardCharsets.UTF_8);
.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"")
```

`URLEncoder` encode pour les **formulaires HTML** (`application/x-www-form-urlencoded`), où l'espace devient `+`. L'utilisateur télécharge `rapport+final.pdf`. Et dans un `filename="..."` entre guillemets, les navigateurs ne décodent pas les `%XX` : les accents arrivent illisibles.

✅ **La bonne façon :** la RFC 6266 prévoit un paramètre `filename*` encodé en UTF-8. Spring le génère pour nous :

```java
ContentDisposition cd = ContentDisposition.attachment()
        .filename(result.fileName(), StandardCharsets.UTF_8)
        .build();
// → attachment; filename="=?UTF-8?Q?rapport_final.pdf?="; filename*=UTF-8''rapport%20final.pdf
```

> 💡 Règle générale : dès qu'un header a une syntaxe un peu riche, cherchez la classe Spring qui sait le construire (`ContentDisposition`, `MediaType`, `CacheControl`...) plutôt que de concaténer des chaînes.

### 6.2. Le controller

**Exercice :** Dans `AttachmentController.java`.

```java
@GetMapping("/{id}/download")
public ResponseEntity<InputStreamResource> downloadAttachment(@PathVariable Long id) {
    // 1. Appel au service pour récupérer un DTO contenant le Stream et les métadonnées
    DownloadResult result = attachmentService.download(id);

    // 2. Nom du fichier (espaces et accents gérés par Spring)
    ContentDisposition contentDisposition = ContentDisposition.attachment()
            .filename(result.fileName(), StandardCharsets.UTF_8)
            .build();

    // 3. Type MIME : fourni par le client à l'upload, il peut être absent
    MediaType contentType = result.mimeType() != null
            ? MediaType.parseMediaType(result.mimeType())
            : MediaType.APPLICATION_OCTET_STREAM;

    // 4. Construction de la réponse HTTP avec les bons headers
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .contentType(contentType)
            .contentLength(result.size())
            .body(result.resource());
}

```

---

# 🚀 Validation

1. Assurez-vous que RustFS tourne et que le log de démarrage ne signale pas d'erreur S3.
2. Prenez une Tâche existante (ID 1 par exemple).
3. Uploadez un fichier PDF via Postman/Bruno :
* **POST** `http://localhost:8080/api/tasks/1/attachments`
* **Body** : `form-data`, clé `file` (type File).


4. Vérifiez dans la console RustFS ([http://localhost:9001](http://localhost:9001), bucket `smarttasks`) que le fichier est apparu (avec un nom UUID).
5. Vérifiez dans PostgreSQL que la ligne est créée dans `attachments`.
6. Téléchargez le fichier via l'API.
7. **Cas d'erreur** :
* uploadez un fichier vide : vous devez obtenir `400` ;
* uploadez `rapport d'été final.pdf` puis téléchargez-le : le navigateur doit proposer exactement ce nom ;
* arrêtez RustFS (`docker stop smarttasks-rustfs`) et retentez un upload : vous devez obtenir `500`, et non `404`. Relancez-le ensuite (`docker start smarttasks-rustfs`).
8. **Depuis le front** (module 04) : ouvrez une tâche, ajoutez une pièce jointe puis téléchargez-la. Aucune erreur rouge ne doit apparaître dans la console du navigateur (F12).

---

# ➡️ Prochain module

Votre backend est fonctionnel et gère des fichiers !
Mais le code mélange un peu tout (JPA, métier, web, S3...). Pour le rendre maintenable sur 10 ans, nous allons le restructurer.

Passez au chapitre suivant : **06 – Clean Architecture & Refactoring**.