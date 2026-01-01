# 05 – Swagger / OpenAPI & Intégration Front

Une API REST sans documentation est inutilisable. Dans ce module, nous allons :

1. Générer automatiquement une documentation interactive via **OpenAPI (Swagger)**.
2. Connecter le front-end React fourni à votre API Backend.

---

# 🎯 Objectifs du module

✅ Intégrer **SpringDoc OpenAPI** pour générer la documentation.
✅ Utiliser les annotations `@Operation`, `@ApiResponse` pour enrichir la doc.
✅ Comprendre le mécanisme **CORS** et comment l'autoriser dans Spring Security.
✅ Configurer le client HTTP du front-end via les variables d'environnement.

---

# 📖 1. Documentation automatique (OpenAPI)

Nous utilisons la librairie standard **SpringDoc**. Elle analyse vos contrôleurs au démarrage et génère une page web de test.

### 1.1. Dépendance

Ajoutez ceci dans `pom.xml` :

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>

```

### 1.2. Tester l'interface

Une fois l'application relancée (`SmartTasksApplication`), ouvrez :
➡️ **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

Vous devriez voir vos endpoints (`/api/projects`, `/api/tasks`...). Essayez de lancer une requête via le bouton "Try it out".

### 1.3. Enrichir la documentation

Par défaut, la doc est technique. Ajoutons des descriptions métier.

**Exercice :** Mettez à jour `ProjectController`.

```java
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projets", description = "Gestion des projets de l'entreprise") // Groupe l'API
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Lister les projets", description = "Retourne la liste paginée des projets du tenant courant.")
    @GetMapping
    public Page<ProjectListResponse> findAll(Pageable pageable) {
        return projectService.findAll(pageable);
    }

    @Operation(summary = "Créer un projet")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Projet créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides (ex: nom vide)")
    })
    @PostMapping
    public ProjectResponse create(@Valid @RequestBody ProjectCreateRequest request) {
        return projectService.create(request);
    }
}

```

> 💡 **Devoir :** Faites la même chose pour `TaskController` et `AttachmentController`.

---

# 🌍 2. Autoriser le front-end (CORS)

Le front-end tourne sur `http://localhost:5173`.
Le backend tourne sur `http://localhost:8080`.

Par sécurité, le navigateur bloque les requêtes AJAX entre deux domaines/ports différents. C'est la sécurité **CORS** (Cross-Origin Resource Sharing).

**Exercice :** Dans `SecurityConfig.java`, assurez-vous que le bean CORS est correct.

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    
    // Autoriser le port du front-end (Vite)
    config.setAllowedOrigins(List.of("http://localhost:5173"));
    
    // Autoriser les verbes HTTP utilisés
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    
    // Autoriser tous les headers (notamment Authorization pour le JWT)
    config.setAllowedHeaders(List.of("*"));
    
    // Autoriser l'envoi de cookies/credentials (si besoin)
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}

```

---

# 🖥️ 3. Configuration du front-end

Le code React vous est fourni dans le dossier `projet-front`. Il utilise **Vite** comme outil de build.

### 3.1. Variable d'environnement

Le front-end ne doit pas avoir l'URL du backend "en dur" dans le code. Elle doit être configurable.

**Action :**

1. Allez dans le dossier `projet-front`.
2. Dupliquez le fichier `.env.example` et renommez-le en `.env`.
3. Vérifiez son contenu :

```properties
# URL de votre API Spring Boot
VITE_API_BASE_URL=http://localhost:8080

# Vos identifiants Google (déjà configurés normalement)
VITE_GOOGLE_CLIENT_ID=...

```

### 3.2. Lancer le front-end

```bash
cd projet-front
npm install
npm run dev

```

Ouvrez **[http://localhost:5173](http://localhost:5173)**.

---

# 🚀 4. Test d'intégration complet

C'est le moment de vérité !

1. Assurez-vous que **Docker** (Postgres + MinIO) tourne.
2. Assurez-vous que le **Backend** tourne (`SmartTasksApplication`).
3. Assurez-vous que le **Front-end** tourne (`npm run dev`).

**Scénario de test :**

1. Cliquez sur "Login with Google" sur le Front.
2. Une fois connecté, vous arrivez sur le Dashboard.
3. Allez dans "Projects" -> "Create Project".
4. Créez "Projet Demo".
5. Cliquez dessus, puis créez une Tâche "Test Integration".
6. Ajoutez une pièce jointe à la tâche.

Si tout fonctionne sans erreur rouge dans la console du navigateur (F12), félicitations ! 🎉
Vous avez construit une application **Fullstack**, **Sécurisée** et **Cloud-Ready**.

---

# ➡️ Prochain module

Votre application fonctionne, mais le code backend mélange un peu tout (JPA, Métier, Web...).
Pour la rendre maintenable sur 10 ans, nous allons la restructurer.

Passez au chapitre suivant : **06 – Clean Architecture & Refactoring**.