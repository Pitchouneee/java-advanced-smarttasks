# 03 – Sécurité & Multi-Tenancy

Ce module introduit deux aspects fondamentaux d’une application professionnelle :  
**la sécurité (OAuth2 + JWT)** et **le multi-tenant**, indispensable pour SmartTasks.

---

# 🎯 Objectifs du module

À la fin de ce chapitre vous serez capables de :

* Configurer un **Resource Server OAuth2** (JWT)
* Protéger les endpoints REST
* Gérer les rôles et autorisations
* Mettre en place un **multi-tenant soft** via un header HTTP
* Isoler les données selon `X-Tenant-ID`
* Comprendre le fonctionnement d’un **TenantContext**

---

# 🔐 1. Introduction à OAuth2 & JWT

SmartTasks utilise le modèle suivant :

* Le front récupère un **JWT** auprès d’un serveur d’auth (Keycloak ou mock)
* Le backend SmartTasks vérifie et valide ce token via **Spring Security Resource Server**

### Avantages du JWT :

* Portable  
* Statuteless  
* Vérifiable sans requête en base  
* Rapide et standard  

---

# ⚙️ 2. Dépendances Maven

Dans `pom.xml` :

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

---

# 🛡️ 3. Configuration Resource Server

`application.yml` :

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081/realms/smarttasks
```

> Si vous n’avez pas de Keycloak, un JWT mock pourra être utilisé.

---

# 🔧 4. Configuration Spring Security moderne

`config/SecurityConfig.java` :

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth.jwt());

        return http.build();
    }
}
```

---

# 👤 5. Extraction des infos utilisateur

Spring extrait automatiquement :

* `sub` → ID utilisateur  
* `preferred_username`  
* `email`  
* `roles` → dans `realm_access.roles`

 Exemple pour récupérer le principal :

```java
@GetMapping("/me")
public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
    return Map.of(
        "username", jwt.getClaim("preferred_username"),
        "roles", jwt.getClaim("realm_access")
    );
}
```

---

# 🏢 6. Multi-tenant soft (isolation logique)

SmartTasks doit permettre à plusieurs entreprises d’utiliser l'app en toute isolation.

Nous utilisons un **multi-tenant soft** :

→ chaque enregistrement en base possède une colonne `tenant_id`

→ le client envoie un header :  

```
X-Tenant-ID: acme
```

→ le backend filtre automatiquement les données selon le tenant.

---

# 🧩 7. TenantContext (ThreadLocal)

Créer un contexte tenant par requête :

 `tenant/TenantContext.java`

```java
public class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public static void setTenant(String tenant) {
        CURRENT.set(tenant);
    }

    public static String getTenant() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
```

---

# 🏷️ 8. Filtre HTTP pour extraire X-Tenant-ID

`tenant/TenantFilter.java` :

```java
@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
                                    throws ServletException, IOException {

        String tenant = req.getHeader("X-Tenant-ID");
        if (tenant == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing tenant header");
            return;
        }

        TenantContext.setTenant(tenant);
        try {
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }
}
```

➡️ Ce filtre s’exécute **avant les services et repositories**, ce qui permet de filtrer ensuite les données.

---

# 🗃️ 9. Modèle multi-tenant

Chaque entité doit avoir une colonne `tenant_id` :

```java
@Column(name = "tenant_id")
private String tenantId;
```

Lors de l’enregistrement :

```java
task.setTenantId(TenantContext.getTenant());
```

Lors d’une lecture :

```java
List<Task> findByTenantId(String tenantId);
```

Pour tout repository :

```java
List<Project> findByTenantId(String tenantId);
```

---

# 🔒 10. Sécurisation par tenant + utilisateur

On combine :

* tenant du header  
* utilisateur authentifié (JWT)  
* stratégie RBAC : ADMIN / USER  

Exemple méthode sécurisée :

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteProject(Long id) { ... }
```

---

# 🧪 11. Exercice pratique
1. Ajouter `tenant_id` à **Project**, **Task**, **User**
2. Modifier leurs services pour injecter automatiquement `TenantContext.getTenant()`
3. Empêcher un utilisateur d’accéder à un tenant différent
4. Ajouter un endpoint :

```
GET /api/me
```

return info du JWT + tenant actuel

---

# 📘 Prochain module

➡️ **04 – Swagger, Documentation & Intégration Front**

Vous avez désormais une API sécurisée et multi-tenant 🚀
