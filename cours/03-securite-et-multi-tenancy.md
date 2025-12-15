# 03 – Sécurité & Multi-Tenancy

Ce module introduit deux aspects fondamentaux d’une application professionnelle : **la sécurité (OAuth2 + JWT)** et **le multi-tenant**, indispensable pour assurer l'isolation des données dans SmartTasks.

-----

# 🎯 Objectifs du module

À la fin de ce chapitre, vous serez capables de :

✅ Configurer un **Resource Server OAuth2** (JWT) avec Spring Security. \
✅ Protéger l'ensemble des endpoints REST. \
✅ Comprendre le mécanisme de **multi-tenant soft** de SmartTasks. \
✅ Isoler les données en utilisant le **JWT Subject** comme identifiant de Tenant. \
✅ Utiliser un **TenantContext** (`ThreadLocal`) pour propager l'identifiant au travers des couches Service et Repository.

-----

# 🔐 1. Sécurisation : Resource Server & JWT

SmartTasks utilise l'approche moderne du **Resource Server**. Le backend ne gère pas l'authentification elle-même (qui est déléguée à Google OAuth via le front-end), mais valide le token JWT reçu du client.

### 1.1. Dépendances

Assurez-vous d'avoir les dépendances nécessaires dans votre `pom.xml` :

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

### 1.2. Configuration Spring Security

Notre configuration désactive la protection CSRF (car c'est une API sans session) et exige une authentification pour l'intégralité de l'API (`/api/**`).

```java
// Dans smarttasks/configuration/SecurityConfig.java

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .cors(Customizer.withDefaults()) // Active CORS (nécessaire pour le front)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").authenticated() // Protège l'API
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()) // Configure le Resource Server pour accepter les JWT
                );

        return http.build();
    }

    // Configuration CORS également nécessaire (voir fichier complet)
    // ...
}
```

### 1.3. Extraction des Infos utilisateur (JWT)

Une fois le JWT validé, Spring Security le place dans le contexte de sécurité. Nous pouvons extraire l'objet `Jwt` qui contient toutes les *claims*.

Dans le contexte de SmartTasks, **l'identifiant unique de l'utilisateur (le `sub`) est central à notre stratégie Multi-Tenancy** (voir section 2).

-----

# 🏢 2. Multi-Tenant Soft (Isolation des Données)

Dans SmartTasks, les données de chaque utilisateur (ou *tenant*) doivent être strictement isolées. Nous utilisons le **Multi-Tenant Soft** : chaque table possède une colonne `tenant_id` pour le filtrage.

La clé d'isolation est l'ID de l'utilisateur extrait du JWT.

### 2.1. Le TenantContext (`ThreadLocal`)

Afin que l'identifiant du tenant soit accessible dans toutes les couches (du Controller au Repository), nous utilisons un `ThreadLocal` appelé `TenantContext`.

```java
// Dans smarttasks/configuration/tenant/TenantContext.java

public class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public static void setTenant(String tenant) {
        CURRENT.set(tenant);
    }

    public static String getTenant() {
        return CURRENT.get(); // Utilisé par les services et repositories
    }

    public static void clear() {
        CURRENT.remove();
    }
}
```

### 2.2. Le filtre d'extraction du tenant

Le point d'entrée pour le Multi-Tenancy est un filtre HTTP qui s'exécute après l'authentification JWT.

Dans notre projet, l'identifiant du tenant est le `subject` (ID unique) du JWT.

```java
// Dans smarttasks/configuration/tenant/TenantFilter.java

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String tenantId = null;

        // 1. On récupère le JWT validé par Spring Security
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            tenantId = jwt.getSubject(); // L'ID utilisateur (sub) est notre tenantId
        }

        if (tenantId == null) {
            // Devrait être géré par SecurityConfig, mais sécurité en profondeur
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required or JWT invalid.");
            return;
        }

        // 2. On stocke l'ID dans le ThreadLocal
        TenantContext.setTenant(tenantId);
        try {
            chain.doFilter(req, res); // On passe au Controller/Service/Repository
        } finally {
            TenantContext.clear(); // 3. On nettoie toujours à la fin de la requête
        }
    }
}
```

### 2.3. Modèle et Repositories Multi-Tenant

Chaque entité de données doit posséder le champ `tenantId`.

```java
// Dans smarttasks/project/model/Project.java ou Task.java

@Column(updatable = false, nullable = false)
private String tenantId;
```

**Travail à Réaliser :** Mettez à jour vos entités pour inclure ce champ.

Dans la couche Service, vous injectez le `tenantId` lors de la création et l'utilisez pour filtrer lors de la lecture.

```java
// Dans smarttasks/project/service/ProjectService.java (Exemple de création)

public ProjectResponse create(ProjectCreateRequest request) {
    Project project = new Project();
    project.setName(request.name());
    // Injection du Tenant ID lors de la création
    project.setTenantId(TenantContext.getTenant()); 
    // ...
}
```

Dans la couche Repository, **vous devez absolument filtrer sur le `tenantId` pour chaque requête de lecture**.

```java
// Dans smarttasks/project/repository/ProjectRepository.java (Exemple de recherche)

@Query("""
       SELECT new fr.corentinbringer.smarttasks.project.model.ProjectListResponse(
           p.id, p.name, p.createdOn
       )
       FROM Project p
       WHERE p.tenantId = :tenantId
       """)
Page<ProjectListResponse> findAllListByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
```

-----

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

-----

# 📘 Prochain module

➡️ **04 – Swagger / OpenAPI**

Nous allons documenter l'API pour faciliter l'intégration front-end et la maintenance.