# 04 – Swagger / OpenAPI & Intégration Front

Une API REST sans documentation est inutilisable. Dans ce module, nous allons :

1. Générer automatiquement une documentation interactive via **OpenAPI (Swagger)**.
2. Connecter le front-end React fourni à votre API Backend.

---

# 🎯 Objectifs du module

✅ Intégrer **SpringDoc OpenAPI** pour générer la documentation.
✅ Utiliser les annotations `@Operation`, `@ApiResponse` pour enrichir la doc.
✅ Tester une API protégée par JWT depuis Swagger UI.
✅ Vérifier la configuration **CORS** (faite au module 03) et diagnostiquer une erreur CORS.
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
    <version>3.0.3</version>
</dependency>

```

> ⚠️ **Choisir la bonne version.** springdoc suit les versions de Spring Boot : la ligne **2.x** vise Boot 3, la ligne **3.0.x** vise Boot **4.0.x**. Une version incompatible compile souvent sans erreur, mais plante au démarrage (`NoSuchMethodError`, `ClassNotFoundException`). Vérifiez toujours le tableau de compatibilité (FAQ de springdoc.org) avant de mettre à jour l'un ou l'autre.

Les URL de Swagger doivent rester accessibles sans token, à **deux** endroits (module 03) :

1. Dans `SecurityConfig`, à côté de `/error`, avant `.requestMatchers("/api/**")` :
   ```java
   .requestMatchers("/error", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
   ```
2. Dans `TenantFilter.shouldNotFilter`, sinon le filtre répond `401` faute de JWT :
   ```java
   @Override
   protected boolean shouldNotFilter(HttpServletRequest request) {
       String path = request.getServletPath();
       return path.equals("/error")
               || path.startsWith("/swagger-ui/")
               || path.equals("/swagger-ui.html")
               || path.equals("/v3/api-docs")
               || path.startsWith("/v3/api-docs/");
   }
   ```

### 1.2. Tester l'interface

Une fois l'application relancée (`SmarttasksApplication`), ouvrez :
➡️ **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

Vous devriez voir vos endpoints (`/api/projects`, `/api/tasks`...). Essayez de lancer une requête via le bouton "Try it out".

❌ Réponse : **`401 Unauthorized`**. Normal : Swagger UI n'envoie aucun token, et toutes nos routes `/api/**` sont protégées (module 03). Il faut lui dire comment s'authentifier.

### 1.3. Déclarer l'authentification Bearer

**Exercice :** Créez `configuration/OpenApiConfig.java`.

```java
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "SmartTasks API", version = "1.0"),
        security = @SecurityRequirement(name = "bearerAuth")   // appliqué à tous les endpoints
)
@SecurityScheme(
        name = "bearerAuth",                                  // même nom que ci-dessus
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
```

* `@SecurityScheme` décrit **comment** s'authentifier : un header `Authorization: Bearer <token>`. Swagger UI affiche alors un bouton **Authorize** 🔓.
* `@SecurityRequirement` dit **où** l'appliquer. Placé dans `@OpenAPIDefinition`, il s'applique à toute l'API. On peut aussi le mettre sur un seul controller ou une seule méthode.

**Récupérer un token pour tester :**

1. Connectez-vous sur le front ([http://localhost:5173](http://localhost:5173)).
2. Ouvrez les outils développeur (F12) → onglet **Réseau**, puis cliquez sur une requête vers `localhost:8080`.
3. Copiez la valeur du header `Authorization` **sans** le mot `Bearer` (une longue chaîne `eyJ...`).
4. Dans Swagger UI, cliquez sur **Authorize**, collez le token, validez.

« Try it out » répond maintenant `200`. Le token Google expire au bout d'une heure : en cas de `401`, recommencez.

> 🔐 Un ID token donne accès à vos données : ne le collez jamais dans un outil en ligne ou un ticket.

### 1.4. Enrichir la documentation

Par défaut, la doc est technique. Ajoutons des descriptions métier.

**Exercice :** Mettez à jour `ProjectController`.

```java
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Gestion des projets de l'utilisateur connecté") // Groupe l'API
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
    @ResponseStatus(HttpStatus.CREATED) // sans ça, Spring répond 200
    public ProjectResponse create(@Valid @RequestBody ProjectCreateRequest request) {
        return projectService.create(request);
    }
}

```

> ⚠️ **La documentation ne change pas le comportement.** `@ApiResponse(responseCode = "201")` ne fait qu'**écrire** « 201 » dans la doc. C'est `@ResponseStatus(HttpStatus.CREATED)` qui fait réellement répondre `201`. Si on oublie ce dernier, la doc **ment** : le client lit 201 et reçoit 200. Vérifiez toujours le code réel dans « Try it out ».

> 💡 **Devoir :** Faites la même chose pour `TaskController` et `TaskInProjectController` (le `POST` de création de tâche répond aussi `201`). Vous documenterez `AttachmentController` au module 05, en le créant.

---

# 🌍 2. Autoriser le front-end (CORS)

La configuration CORS a été faite au module 03 (§2.2), car le front appelle l'API dès la connexion Google. Avant de brancher le front, vérifiez simplement dans `SecurityConfig` :

1. `.cors(Customizer.withDefaults())` est présent dans la chaîne de filtres ;
2. le bean `corsConfigurationSource()` autorise l'origine `http://localhost:5173` et les méthodes `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`.

En cas d'erreur `blocked by CORS policy` dans la console du navigateur (F12), regardez l'onglet **Réseau** : la requête `OPTIONS` (preflight) doit répondre `200` avec un header `Access-Control-Allow-Origin`.

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

1. Assurez-vous que **Docker** (Postgres) tourne.
2. Assurez-vous que le **Backend** tourne (`SmarttasksApplication`).
3. Assurez-vous que le **Front-end** tourne (`npm run dev`).

**Scénario de test :**

1. Cliquez sur "Login with Google" sur le Front.
2. Une fois connecté, vous arrivez sur le Dashboard.
3. Allez dans "Projects" -> "Create Project".
4. Créez "Projet Demo".
5. Cliquez dessus, puis créez une Tâche "Test Integration".

> ℹ️ La partie « pièces jointes » de la page d'une tâche renverra une erreur pour l'instant : ces endpoints arrivent au module 05.

Si tout fonctionne sans erreur rouge dans la console du navigateur (F12), félicitations ! 🎉
Vous avez construit une application **Fullstack**, **Sécurisée** et **Cloud-Ready**.

---

# ➡️ Prochain module

Votre API est documentée, testable depuis Swagger et branchée au front.
Il manque une fonctionnalité : joindre des fichiers aux tâches.

Passez au chapitre suivant : **05 – Upload de fichiers & Stockage Objet (S3)**.