# 03 – Sécurité & Multi-Tenancy

Ce module introduit deux aspects fondamentaux d’une application professionnelle : **la sécurité (OAuth2 + JWT)** et **le multi-tenant**, indispensable pour assurer l'isolation des données dans SmartTasks.

-----

# 🎯 Objectifs du module

À la fin de ce chapitre, vous serez capables de :

✅ Comprendre le flux **OAuth2 / OpenID Connect** avec un frontend séparé \
✅ Configurer Spring Security en mode **Resource Server** \
✅ Comprendre les stratégies d'isolation de données (**Database vs Schema vs Discriminator**) \
✅ Manipuler le **SecurityContext** et les **ThreadLocal** pour propager l'identité \
✅ Implémenter un filtre de sécurité personnalisé \
✅ Autoriser le front-end à appeler l'API (**CORS**)

---

# 1. 🔐 Théorie : architecture de sécurité

### 1.1. Le Flux d'authentification (Resource Server)

Dans notre architecture, le backend ne gère pas le login.

1. **Frontend** : Ouvre la popup « Sign in with Google ».
2. **Google** : Authentifie l'utilisateur et renvoie au front un **ID token** : un JWT (JSON Web Token) signé par Google, qui décrit *qui* est l'utilisateur (`sub`, `email`, `name`…).
3. **Frontend** : Envoie ce token dans le header `Authorization: Bearer <token>` à chaque requête vers l'API.
4. **Backend (API)** : Vérifie le JWT **localement** (signature, émetteur, audience, expiration) puis en extrait l'identité.

```
┌────────┐  1. popup login   ┌────────┐
│ React  │ ────────────────► │ Google │
│ (front)│ ◄──────────────── │        │
└───┬────┘  2. ID token (JWT)└───┬────┘
    │                            │ clés publiques (JWKS),
    │ 3. Authorization: Bearer   │ téléchargées une fois puis
    ▼                            │ mises en cache
┌────────────────┐               │
│ API Spring     │ ◄─────────────┘
│ 4. vérifie JWT │
└────────────────┘
```

> 🔍 **Comment le backend vérifie-t-il la signature « sans rappeler Google » ?**
> Google signe ses tokens avec une **clé privée** et publie les **clés publiques** correspondantes (le *JWKS*). Au premier token reçu, Spring les télécharge depuis l'`issuer-uri`, puis les garde en cache. Ensuite, chaque vérification est un simple calcul cryptographique local, sans aucun appel réseau par requête.

> ⚠️ **ID token vs access token : le raccourci de ce cours**
> En OAuth2 « dans les règles », le front obtiendrait un **access token** émis *pour l'API* par un serveur d'autorisation (Keycloak, Auth0, Entra ID…), avec des *scopes* (`projects:read`…).
> Ici, on réutilise l'**ID token** de Google, qui sert normalement à prouver l'identité de l'utilisateur *au front*. C'est un raccourci acceptable pour un projet pédagogique, à condition de **vérifier l'audience** (§2.3), mais il a des limites :
> * **Durée de vie d'1 h** : il n'y a pas de *refresh token* côté front. Passé ce délai, l'API répond `401` et l'utilisateur doit se reconnecter.
> * **Pas de scopes** : le token dit *qui* est l'utilisateur, pas *ce qu'il a le droit de faire*. Ici, tout utilisateur authentifié a les mêmes droits sur ses propres données.
> * **Pas de révocation** : un token volé reste valide jusqu'à son expiration.
>
> En entreprise, on placerait un serveur d'autorisation entre les deux (par exemple Keycloak avec Google comme fournisseur d'identité).

### 1.2. La Chaîne de filtres Spring Security

Spring Security fonctionne comme une série de filtres (Chain of Responsibility) qui interceptent la requête HTTP avant qu'elle n'arrive à vos contrôleurs.

Nous allons insérer notre logique **après** que Spring ait validé le token.

---

# 🛠️ 2. Mise en pratique : Configuration sécurité

### 2.1. Dépendances

Ajoutez le starter dans `pom.xml` :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>

```

Il inclut `spring-boot-starter-security` : pas besoin de l'ajouter en plus.

> ⚠️ **Spring Boot 4 a renommé ce starter.** Dans Boot 3 (et dans beaucoup de tutoriels en ligne), il s'appelait `spring-boot-starter-oauth2-resource-server`. Avec Boot 4, l'ancien nom existe encore mais il est **déprécié** (sa description Maven l'indique) et sera supprimé dans une version future : utilisez le nouveau. Sur [start.spring.io](https://start.spring.io), cherchez « OAuth2 Resource Server ».

### 2.2. Configuration du Resource Server

Nous devons dire à Spring : "Toutes les routes `/api/**` sont privées, et tu dois valider les tokens JWT".

**Exercice :** Créez la classe `configuration/SecurityConfig.java`.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // On désactive CSRF car nous utilisons des tokens (stateless) et non des sessions cookies
            .csrf(AbstractHttpConfigurer::disable)
            
            // Activation de CORS (pour que le front React puisse nous appeler)
            .cors(Customizer.withDefaults())
            
            .authorizeHttpRequests(auth -> auth
                // /error : page d'erreur interne de Spring. Si elle est protégée, une erreur 404 ou 500
                // se transforme en 401 (le forward vers /error n'a pas de token) : le vrai problème est masqué.
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            
            // Configuration OAuth2 Resource Server pour décoder les JWT
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // Bean de configuration CORS : voir l'explication juste en dessous
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));                  // le front (Vite)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));                                    // dont Authorization (le JWT)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

```

#### 🌍 Pourquoi CORS ?

Le front tourne sur `http://localhost:5173`, l'API sur `http://localhost:8080` : ce sont deux **origines** différentes (le port compte). Par sécurité, le navigateur bloque les requêtes JavaScript vers une autre origine, sauf si le serveur l'autorise explicitement : c'est **CORS** (*Cross-Origin Resource Sharing*).

Pour une requête avec un header `Authorization`, le navigateur envoie d'abord une requête **`OPTIONS`** (le *preflight*) pour demander la permission. Le `CorsFilter` de Spring Security y répond à partir du bean ci-dessus, **avant** l'authentification : sinon, le preflight (qui ne porte pas de token) serait rejeté en `401`, et le front ne pourrait jamais appeler l'API.

> 💡 Erreur typique dans la console du navigateur : `has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header`. Ce n'est **pas** une erreur du front : c'est le serveur qui n'a pas autorisé l'origine. Vérifiez l'URL exacte (`http` / `https`, port) dans `setAllowedOrigins`.
>
> ⚠️ Ne mettez jamais `setAllowedOrigins(List.of("*"))` en production : n'importe quel site pourrait appeler votre API depuis le navigateur de vos utilisateurs.


### 2.3. Configuration du JWT : émetteur **et** audience

Spring doit savoir **qui** émet les tokens (pour récupérer les clés publiques et vérifier la signature) et **pour qui** ils sont émis.

**Exercice :** Complétez `application.yaml`.

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://accounts.google.com   # Qui a signé le token ?
          audiences: ${GOOGLE_CLIENT_ID}           # Ce token m'est-il destiné ?
```

Puis, dans IntelliJ : **Run > Edit Configurations > Environment variables**, ajoutez `GOOGLE_CLIENT_ID=<votre client id>` (le même que dans le `.env` du front).

> 🚨 **Pourquoi l'audience est indispensable**
>
> `issuer-uri` seul vérifie que le token est **signé par Google**, mais Google signe les tokens de **toutes** les applications du monde.
> Sans contrôle de l'audience (claim `aud`), n'importe quel site utilisant « Sign in with Google » pourrait récupérer le token d'un utilisateur et l'utiliser contre **votre** API.
>
> Avec `audiences`, Spring rejette (401) tout token dont le `aud` ne correspond pas à votre Client ID.

> 💡 La propriété `audiences` est un raccourci. La version équivalente en Java, avec un `JwtDecoder` personnalisé, ressemble à ceci :
>
> ```java
> @Bean
> JwtDecoder jwtDecoder(@Value("${GOOGLE_CLIENT_ID}") String clientId) {
>     NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation("https://accounts.google.com");
>     OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
>             JwtClaimNames.AUD, aud -> aud != null && aud.contains(clientId));
>     decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
>             JwtValidators.createDefaultWithIssuer("https://accounts.google.com"), audience));
>     return decoder;
> }
> ```

---

# 🏢 3. Théorie : Le multi-tenancy

SmartTasks héberge les données de nombreux utilisateurs. Comment garantir que chacun ne voit que les siennes ?

Il existe 3 stratégies majeures :

1. **Database per Tenant** : 1 BDD par client. (Très isolé, mais cher et dur à maintenir).
2. **Schema per Tenant** : 1 BDD, mais 1 schéma SQL par client. (Bon compromis).
3. **Discriminator Column (Soft Isolation)** : 1 seule table, une colonne `tenant_id` partout.

👉 Nous choisissons l'option **3 (Discriminator)** pour sa simplicité et sa performance.
La clé d'isolation sera l'ID unique de l'utilisateur (le champ `sub` du JWT) : **un tenant = un utilisateur**.

> 💡 **Et pour une entreprise avec plusieurs collaborateurs ?** Il faudrait une notion d'*organisation* : une table `organizations` et une table d'appartenance user → organisation, puis `tenantId = organisationId`. Le filtre et les repositories resteraient identiques : seule la façon de *calculer* le tenant change. C'est une bonne piste d'extension pour les plus rapides.

---

# ⚙️ 4. Mise en pratique : Isolation des données

### 4.1. Le TenantContext (ThreadLocal)

Pour éviter de passer le paramètre `tenantId` dans toutes les méthodes (`service.create(data, tenantId)`), nous allons utiliser un contexte global au Thread courant.

**Exercice :** Créez `configuration/tenant/TenantContext.java`.

```java
public class TenantContext {
    // ThreadLocal permet de stocker une variable unique par thread (requête HTTP)
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenant(String tenantId) {
        // TODO: Enregistrer le tenant dans le ThreadLocal
    }

    public static String getTenant() {
        // TODO: Récupérer le tenant
        return null; 
    }

    public static void clear() {
        // TODO: Nettoyer le ThreadLocal (Indispensable pour éviter les fuites de mémoire !)
    }
}

```

### 4.2. Le Filtre d'Interception (`TenantFilter`)

C'est le cœur du système. Ce filtre doit s'exécuter à chaque requête pour :

1. Lire le token JWT validé par Spring.
2. Extraire l'ID utilisateur (le `sub`).
3. Le placer dans le `TenantContext`.

**Exercice :** Implémentez `configuration/tenant/TenantFilter.java`.

```java
@Component
public class TenantFilter extends OncePerRequestFilter {

    // Routes publiques : pas de JWT, donc pas de tenant → on ne les filtre pas
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().equals("/error");   // Swagger s'ajoutera ici au module 04
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        // 1. Récupérer l'authentification Spring Security actuelle
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. Vérifier si c'est un token JWT
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            // TODO: Extraire le 'subject' du token (jwtAuth.getToken().getSubject())
            // TODO: Initialiser le TenantContext avec cet ID
        }

        try {
            // Continuer la chaîne de filtres
            chain.doFilter(request, response);
        } finally {
            // TODO: IMPORTANT - Nettoyer le TenantContext ici.
            // Pourquoi ? Car les threads Tomcat sont réutilisés (Thread Pool).
            // Si on ne nettoie pas, la prochaine requête pourrait utiliser les données du précédent utilisateur.
        }
    }
}

```

> 💡 **Et si l'authentification n'est pas un JWT ?** Sur les routes publiques (`/error`, puis Swagger au module 04), il n'y a pas de token, donc pas de tenant. Le filtre :
> * les exclut avec `shouldNotFilter(...)` (sinon il répondrait `401` malgré le `permitAll()` de `SecurityConfig` : les deux listes doivent rester cohérentes) ;
> * renvoie un `401` partout ailleurs si aucun tenant n'a pu être extrait, plutôt que de laisser passer une requête avec un tenant `null`.

### 4.3. 🧭 Où s'exécute ce filtre ? (ordre des filtres)

Question piège : notre `TenantFilter` lit le `SecurityContext`… mais **qui garantit qu'il s'exécute après la validation du JWT ?**

Il existe en réalité **deux niveaux de filtres** :

```
Requête HTTP
   │
   ▼
┌─────────────────────────── Filtres du conteneur (Tomcat) ───────────────────────────┐
│                                                                                     │
│  ... filtres Spring Boot (encodage, etc.)                                           │
│   │                                                                                 │
│   ▼                                                                                 │
│  ┌──────────── springSecurityFilterChain (ordre -100) ────────────┐                 │
│  │  CorsFilter → ... → BearerTokenAuthenticationFilter → ...      │                 │
│  │        (valide le JWT et remplit le SecurityContext)           │                 │
│  │        → AuthorizationFilter (401/403 si refusé)               │                 │
│  └────────────────────────────────────────────────────────────────┘                 │
│   │                                                                                 │
│   ▼                                                                                 │
│  TenantFilter (@Component, ordre par défaut = LOWEST_PRECEDENCE, donc en dernier)   │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
   │
   ▼
DispatcherServlet → Controller
```

* **Spring Boot enregistre automatiquement tout bean `Filter`** (donc notre `@Component`) comme filtre du conteneur. Sans `@Order`, il reçoit la priorité la plus basse et s'exécute en **dernier**.
* La chaîne Spring Security est elle-même **un seul** filtre du conteneur, d'ordre `-100` (propriété `spring.security.filter.order`). Elle passe donc **avant** notre filtre.
* Résultat : quand `TenantFilter` s'exécute, le JWT est déjà validé et le `SecurityContext` est rempli. Bonus : une requête refusée (`401`) par Spring Security n'atteint jamais notre filtre.

⚠️ **Ça marche… mais par convention implicite.** Deux pièges classiques :

1. **Ajouter `@Order(Ordered.HIGHEST_PRECEDENCE)`** « pour que le filtre passe en premier » : il s'exécute alors *avant* Spring Security, le `SecurityContext` est vide, et plus aucune requête n'a de tenant.
2. **L'ajouter aussi dans la chaîne de sécurité** (`http.addFilterAfter(tenantFilter, ...)`) tout en le laissant en `@Component` : il est enregistré **deux fois**.

**Alternative explicite (recommandée en production) :** placer le filtre *dans* la chaîne Spring Security, à une position précise, et désactiver l'enregistrement automatique :

```java
// SecurityConfig.java
http.addFilterAfter(tenantFilter, BearerTokenAuthenticationFilter.class);

// Empêche Spring Boot d'enregistrer AUSSI le filtre dans le conteneur
@Bean
FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter filter) {
    FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
}
```

> 🎯 Dans ce cours, on garde la version `@Component` (plus simple), mais vous devez être capables d'expliquer **pourquoi** elle fonctionne.

### 4.4. ☕ Java 25 : threads virtuels et `ScopedValue`

Notre `TenantContext` repose sur un `ThreadLocal`, parce qu'avec Tomcat **une requête = un thread**. Java 21 et 25 changent la donne sur ces deux points.

**Les threads virtuels (Java 21)**

Par défaut, Tomcat traite les requêtes avec un pool d'environ 200 threads « plateforme » (de vrais threads du système). Pendant qu'une requête attend PostgreSQL, Google (clés JWKS) ou S3, son thread est **bloqué** et ne sert à rien. Au-delà de 200 requêtes simultanées, les suivantes font la queue.

Un **thread virtuel** est géré par la JVM, pas par le système : il coûte quelques centaines d'octets, on peut en créer des millions. Quand il attend une entrée/sortie, il libère le thread système qui le portait. Une ligne suffit pour que Spring Boot traite chaque requête dans un thread virtuel :

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Notre code ne change pas : il reste écrit de manière simple et bloquante, mais l'application supporte beaucoup plus de requêtes simultanées qui attendent des I/O. (La solution l'active.)

> ⚠️ Les threads virtuels n'accélèrent pas le **calcul** : ils aident quand les threads passent leur temps à **attendre**. Et ils ne suppriment pas les autres limites : avec 10 000 requêtes simultanées, le pool de connexions PostgreSQL (10 par défaut) reste le goulot d'étranglement.

**`ThreadLocal` : ce qui ne va pas**

* Il est **modifiable** n'importe où : n'importe quel code peut appeler `setTenant(...)` au milieu d'une requête.
* Il faut penser au **`clear()`** dans un `finally`. Si on l'oublie avec un pool de threads, le thread est réutilisé par la requête suivante… avec le tenant de l'utilisateur précédent : une **fuite de données entre clients**.
* Avec des millions de threads virtuels, chaque copie de `ThreadLocal` coûte de la mémoire.

**`ScopedValue` (définitif en Java 25)**

Une `ScopedValue` est une valeur **immuable**, liée à un **bloc de code** : elle existe pendant l'exécution du bloc, puis disparaît toute seule.

```java
public final class TenantContext {

    private static final ScopedValue<String> TENANT = ScopedValue.newInstance();

    public static String getTenant() {
        return TENANT.isBound() ? TENANT.get() : null;
    }

    /** Exécute op avec le tenant donné ; en sortie du bloc, le tenant n'existe plus. */
    public static <R, X extends Throwable> R callWithTenant(String tenant,
                                                            ScopedValue.CallableOp<? extends R, X> op) throws X {
        return ScopedValue.where(TENANT, tenant).call(op);
    }
}
```

Dans le `TenantFilter`, le `try/finally` disparaît :

```java
try {
    TenantContext.callWithTenant(tenantId, () -> {
        chain.doFilter(req, res);     // toute la requête s'exécute « dans » le tenant
        return null;
    });
} catch (IOException | ServletException | RuntimeException e) {
    throw e;
} catch (Exception e) {               // le lambda déclare deux exceptions vérifiées : Java les réunit en Exception
    throw new IllegalStateException(e);
}
```

Les services n'ont **pas** besoin de changer : ils appellent toujours `TenantContext.getTenant()`. C'est l'intérêt d'avoir caché le `ThreadLocal` derrière une classe.

| | `ThreadLocal` | `ScopedValue` |
|---|---|---|
| Modifiable en cours de route | oui (`set`) | non : on ne peut que lier un nouveau bloc |
| Nettoyage | manuel (`remove()` dans un `finally`) | automatique en sortie du bloc |
| Risque de fuite entre requêtes | oui, si on oublie le `clear()` | non |
| Transmis aux sous-tâches | non (sauf `InheritableThreadLocal`, qui copie) | oui, avec la concurrence structurée (`StructuredTaskScope`, encore en preview) |

> 🧪 **Bonus :** migrez `TenantContext` vers `ScopedValue`. Attention, les tests du module 07 appellent `TenantContext.setTenant("alice")` dans un `@BeforeEach` : avec une `ScopedValue`, il faudra exécuter l'appel testé **dans** le bloc, par exemple `TenantContext.callWithTenant("alice", () -> taskService.create(...))`.
>
> La solution garde le `ThreadLocal` : c'est encore ce qu'utilisent Spring Security (`SecurityContextHolder`) et la plupart des bibliothèques, et il reste correct tant que le `finally` est là.

---

# 🛡️ 5. Application au domaine

Maintenant que le contexte est prêt, il faut l'appliquer aux entités.

### 5.1. Modification des Entités

**Exercice :** Ajoutez le champ `tenantId` sur **toutes** vos entités (`Project`, `Task`, `Attachment`).

```java
@Column(nullable = false, updatable = false)
private String tenantId;

```

### 5.2. Injection à l'écriture (Service)

Dans `ProjectService` (et `TaskService`), lors de la création, on injecte automatiquement l'ID.

```java
public ProjectResponse create(ProjectCreateRequest request) {
    Project project = new Project();
    project.setName(request.name());
    
    // TODO: Récupérer l'ID depuis TenantContext et l'assigner au projet

    Project saved = projectRepository.save(project);
    return new ProjectResponse(saved.getId(), saved.getName(), saved.getCreatedOn());
}

```

### 5.3. Filtrage à la lecture (Repository)

⚠️ C'est le point critique de sécurité. **Toutes** les méthodes de lecture doivent filtrer par Tenant.

**Exercice :** Mettez à jour `ProjectRepository` et `TaskRepository`.

```java
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // ⛔️ NE JAMAIS UTILISER findById seul ! 
    // Cela permettrait à un user A de lire le projet du user B s'il devine l'ID.

    // ✅ Version sécurisée
    Optional<Project> findByIdAndTenantId(Long id, String tenantId);

    // ✅ Liste sécurisée : la projection du module 02 (findAllList), avec le filtre tenant en plus
    @Query("""
           SELECT new fr.corentinbringer.smarttasks.project.model.ProjectListResponse(p.id, p.name, p.createdOn)
           FROM Project p
           WHERE p.tenantId = :tenantId
           """)
    Page<ProjectListResponse> findAllListByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
}

```

### 5.4. Vérifier le parent à l'écriture

Filtrer les lectures ne suffit pas. Quand on **crée** une ressource rattachée à une autre (une tâche dans un projet, une pièce jointe sur une tâche), l'ID du parent vient de l'URL : c'est donc une donnée **contrôlée par le client**.

```
POST /api/projects/42/tasks      ← et si le projet 42 appartient à quelqu'un d'autre ?
```

❌ **Version vulnérable :**

```java
public TaskResponse create(Long projectId, TaskCreateRequest request) {
    Project project = projectRepository.findById(projectId)   // ⛔️ aucun filtre tenant
            .orElseThrow();

    Task task = new Task();
    task.setProject(project);
    task.setTenantId(TenantContext.getTenant());
    // ...
}
```

Conséquences :
* **Intégrité cassée** : une tâche du tenant A pointe vers un projet du tenant B. Si B supprime son projet, la cascade supprime aussi la tâche de A.
* **Énumération** : l'attaquant obtient `201` si le projet existe et `404` sinon. Il peut ainsi deviner les IDs des autres clients.
* **Pire encore**, si le développeur a écrit `task.setTenantId(project.getTenantId())` (« la tâche hérite du tenant de son projet ») : l'attaquant **écrit directement dans l'espace de B**.

✅ **Version sécurisée** : on charge le parent **avec le filtre tenant**. S'il n'appartient pas à l'utilisateur, on obtient la même erreur `404` que s'il n'existait pas (on ne révèle pas son existence).

```java
public TaskResponse create(Long projectId, TaskCreateRequest request) {
    // findById du service = findByIdAndTenantId : 404 si le projet n'est pas au tenant courant
    Project project = projectService.findById(projectId);

    Task task = new Task();
    task.setProject(project);
    task.setTenantId(TenantContext.getTenant());   // toujours le tenant du JWT, jamais celui d'un objet
    // ...
}
```

> 🎯 **Règle à retenir :** tout ID reçu du client (URL, body, query param) doit être résolu **via une méthode filtrée par tenant** avant d'être utilisé, en lecture comme en écriture. Même chose pour les pièces jointes (module 05) : la tâche est chargée via `taskService.findById(taskId)`, lui aussi filtré.

---

# 🧪 Exercice pratique

1.  **Modèle** : Ajoutez la colonne `tenantId` de type `String` à vos entités `Project`, `Task` et `Attachment`.
2.  **Sécurité** : Intégrez le `SecurityConfig.java` et le `TenantFilter.java` à votre projet (en vous assurant que le filtre utilise le `jwt.getSubject()` pour le tenant ID). Configurez `issuer-uri` **et** `audiences` (§2.3).
3.  **Service** : Modifiez toutes les méthodes de création (`create`) dans vos services pour appeler `project.setTenantId(TenantContext.getTenant())`.
4.  **Repository** : Modifiez toutes les méthodes de lecture (`findById`, `findAll`, etc.) dans vos Repositories pour inclure la clause `WHERE entite.tenantId = :tenantId`.
5.  **Parent** : Vérifiez que `TaskService.create` charge le projet via une méthode filtrée par tenant (§5.4).
6.  **Test d'isolation** : connectez-vous avec deux comptes Google différents (ou deux navigateurs). Avec le compte B, essayez de lire (`GET /api/projects/{id}`) puis d'ajouter une tâche (`POST /api/projects/{id}/tasks`) sur un projet du compte A. Les deux appels doivent répondre `404`.
7.  **Bonus** : Ajoutez un endpoint simple de debug :
    ```
    GET /api/me
    ```
    qui retourne l'ID de l'utilisateur (le `subject` du JWT) pour vérifier que l'extraction fonctionne.

---

# ➡️ Prochain module

Vos données sont sécurisées et isolées. Il est temps de gérer les fichiers.
Passez au chapitre suivant : **04 – Swagger / OpenAPI & Intégration Front**.