# 03 – Sécurité & Multi-Tenancy

Ce module introduit deux aspects fondamentaux d’une application professionnelle : **la sécurité (OAuth2 + JWT)** et **le multi-tenant**, indispensable pour assurer l'isolation des données dans SmartTasks.

-----

# 🎯 Objectifs du module

À la fin de ce chapitre, vous serez capables de :

✅ Comprendre le flux **OAuth2 / OpenID Connect** avec un frontend séparé \
✅ Configurer Spring Security en mode **Resource Server** \
✅ Comprendre les stratégies d'isolation de données (**Database vs Schema vs Discriminator**) \
✅ Manipuler le **SecurityContext** et les **ThreadLocal** pour propager l'identité \
✅ Implémenter un filtre de sécurité personnalisé

---

# 1. 🔐 Théorie : architecture de sécurité

### 1.1. Le Flux d'authentification (Resource Server)

Dans notre architecture, le backend ne gère pas le login.

1. **Frontend** : Redirige l'utilisateur vers Google.
2. **Google** : Authentifie l'utilisateur et renvoie un **Token JWT** (JSON Web Token) au front.
3. **Frontend** : Envoie ce token dans le header `Authorization: Bearer <token>` à chaque requête vers l'API.
4. **Backend (API)** : Vérifie la signature du JWT (sans rappeler Google) et extrait les droits.

### 1.2. La Chaîne de filtres Spring Security

Spring Security fonctionne comme une série de filtres (Chain of Responsibility) qui interceptent la requête HTTP avant qu'elle n'arrive à vos contrôleurs.

Nous allons insérer notre logique **après** que Spring ait validé le token.

---

# 🛠️ 2. Mise en pratique : Configuration sécurité

### 2.1. Dépendances

Ajoutez les starters nécessaires dans `pom.xml` :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

```

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
                // TODO: Autoriser l'accès public à Swagger (/swagger-ui/**, /v3/api-docs/**)
                // TODO: Verrouiller toutes les routes /api/** (authenticated())
                .anyRequest().authenticated()
            )
            
            // Configuration OAuth2 Resource Server pour décoder les JWT
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // Bean de configuration CORS nécessaire pour le navigateur (Code fourni)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // ... (Voir code solution pour la config CORS standard)
        return source;
    }
}

```

---

# 🏢 3. Théorie : Le multi-tenancy

SmartTasks héberge plusieurs entreprises. Comment isoler leurs données ?

Il existe 3 stratégies majeures :

1. **Database per Tenant** : 1 BDD par client. (Très isolé, mais cher et dur à maintenir).
2. **Schema per Tenant** : 1 BDD, mais 1 schéma SQL par client. (Bon compromis).
3. **Discriminator Column (Soft Isolation)** : 1 seule table, une colonne `tenant_id` partout.

👉 Nous choisissons l'option **3 (Discriminator)** pour sa simplicité et sa performance.
La clé d'isolation sera l'ID unique de l'utilisateur (le champ `sub` du JWT).

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
    
    return mapToResponse(projectRepository.save(project));
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

    // ✅ Liste sécurisée
    @Query("SELECT p FROM Project p WHERE p.tenantId = :tenantId")
    Page<Project> findAllByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
}

```

---

# 🧪 Exercice pratique

1.  **Modèle** : Ajoutez la colonne `tenantId` de type `String` à vos entités `Project`, `Task` et `Attachment`.
2.  **Sécurité** : Intégrez le `SecurityConfig.java` et le `TenantFilter.java` à votre projet (en vous assurant que le filtre utilise le `jwt.getSubject()` pour le tenant ID).
3.  **Service** : Modifiez toutes les méthodes de création (`create`) dans vos services pour appeler `project.setTenantId(TenantContext.getTenant())`.
4.  **Repository** : Modifiez toutes les méthodes de lecture (`findById`, `findAll`, etc.) dans vos Repositories pour inclure la clause `WHERE entite.tenantId = :tenantId`.
5.  **Bonus** : Ajoutez un endpoint simple de debugg :
    ```
    GET /api/me
    ```
    qui retourne l'ID de l'utilisateur (le `subject` du JWT) pour vérifier que l'extraction fonctionne.

---

# ➡️ Prochain module

Vos données sont sécurisées et isolées. Il est temps de gérer les fichiers.
Passez au chapitre suivant : **04 – Upload de fichiers (MinIO)**.