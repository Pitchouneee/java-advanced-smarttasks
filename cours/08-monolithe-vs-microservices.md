# 08 – Monolithe vs Microservices

Ce dernier module a pour objectif de vous donner une **vision architecturale globale**.
Nous allons comparer deux paradigmes : **le Monolithe (modulaire)** et **les Microservices**.

C’est une compétence essentielle pour comprendre pourquoi SmartTasks est construit ainsi, et comment l'architecture "Clean" du module 06 prépare l'avenir.

---

# 🎯 Objectifs du module

✅ Distinguer le **Monolithe spaghetti** du **Monolithe modulaire**.
✅ Comprendre les **compromis** (Trade-offs) des Microservices (Complexité vs Scalabilité).
✅ Faire le lien entre **Clean Architecture** et découpage en services.
✅ Visualiser une architecture distribuée (Gateway, Discovery, Broker).

---

# 🏰 1. Le Monolithe : Pas une insulte !

Un **monolithe** est une application où tous les modules (Projet, Tâche, Utilisateur) sont packagés et déployés ensemble (un seul `.jar`).

### 1.1. Le Monolithe "Spaghetti" vs "Modulaire"

* **Spaghetti** : Les controllers appellent directement les repositories, tout est mélangé. Impossible à découper.
* **Modulaire (SmartTasks)** : Le code est séparé en packages distincts (`project`, `dashboard`, `infra`). Les modules communiquent via des interfaces claires.

### ✨ Avantages

* **Simplicité** : Un seul repo, un seul build, une seule BDD.
* **Performance** : Les appels entre modules sont des appels de méthode (in-memory), pas de réseau.
* **Transactions** : `@Transactional` garantit que tout est sauvegardé ou rien. C'est l'atout majeur (ACID).

### ⚠️ Inconvénients

* **Scalabilité** : On doit dupliquer toute l'application pour scaler, même si seul le module "Upload" est chargé.
* **Technologie** : Difficile de changer de langage ou de framework sur une partie seulement.

---

# 🐝 2. Les Microservices : La complexité distribuée

Une architecture **microservices** découpe l'application en services autonomes, communiquant via le réseau (HTTP/REST ou Messaging).

### Exemples pour SmartTasks :

* `project-service` (Gère Projets & Tâches)
* `file-service` (Gère le stockage S3)
* `notification-service` (Envoie les e-mails de rappel)

> 🔐 **Et l'authentification ?** Pas de `auth-service` maison : comme au module 03, ce n'est **pas** notre application qui émet les tokens, c'est un **fournisseur d'identité** (Google, ou Keycloak, Auth0, Microsoft Entra ID...). Chaque service est un *Resource Server* : il vérifie lui-même la signature du JWT avec les clés publiques du fournisseur, sans appeler personne. Écrire son propre service d'authentification (stockage des mots de passe, réinitialisation, double authentification...) est un projet en soi, et une source classique de failles.

### ✨ Avantages

* **Scalabilité fine** : On peut lancer 10 instances du `file-service` et 2 du `project-service`.
* **Indépendance** : Une équipe peut travailler sur un service sans casser les autres.
* **Résilience** : Si le service de notification plante, on peut toujours créer des tâches.

### ⚠️ Le prix à payer (Fallacies of Distributed Computing)

* **Latence** : Un appel réseau est lent et peut échouer.
* **Cohérence** : Comment garantir qu'une tâche est créée ET que le fichier est uploadé si ce sont deux bases différentes ? (Adieu `@Transactional`, bonjour **SAGA**).
* **Ops** : Nécessite Docker, Kubernetes, Monitoring (Grafana/Prometheus), Tracing (Jaeger)... Bien sur, ceci n'est pas une règle absolue.

---

# ⚖️ 3. Le bon choix au bon moment

| Critère | Monolithe Modulaire | Microservices |
| --- | --- | --- |
| **Taille de l'équipe** | < 20 développeurs | > 20 développeurs |
| **Complexité domaine** | Faible à Moyenne | Très élevée |
| **Time to Market** | Rapide 🚀 | Lent au début (Setup infra) |
| **Performance** | Très haute (pas de réseau) | Latence réseau à gérer |

> **Règle d’or :** "Don't start with Microservices". Commencez par un Monolithe bien structuré (Clean Arch). Si (et seulement si) vous avez des problèmes de scale ou d'organisation, découpez-le.

---

# 🔗 4. De la Clean Architecture aux microservices

C'est ici que tout prend sens. Grâce au refactoring du Module 06, passer en microservices est "facile".

**Dans le Monolithe (Module 06) :**
Le `TaskService` appelle `ProjectPort`. L'implémentation est `ProjectPersistenceAdapter` (appel BDD local).

**Vers Microservices :**
Si on sort les Projets dans un service à part, on ne touche **PAS** au `TaskService` !
On crée juste une nouvelle implémentation de `ProjectPort` :

```java
@Component
public class ProjectHttpAdapter implements ProjectPort {

    private final RestClient restClient; // Client HTTP synchrone de Spring

    public ProjectHttpAdapter(@Value("${services.project.url}") String projectServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(projectServiceUrl)          // ex : http://project-service
                .build();
    }

    @Override
    public Optional<Project> findByIdAndTenantId(Long id, String tenantId) {
        // Au lieu de faire du SQL, on appelle l'autre microservice
        try {
            ProjectResponse response = restClient.get()
                    .uri("/api/projects/{id}", id)  // variable d'URI : encodée par Spring
                    .retrieve()                      // lève une exception sur 4xx / 5xx
                    .body(ProjectResponse.class);    // le contrat REST du service distant

            // L'adapter traduit le contrat distant en objet du domaine
            return Optional.ofNullable(response)
                    .map(r -> new Project(r.id(), tenantId, r.name(), r.createdOn()));

        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();                 // 404 distant = « pas trouvé » pour le métier
        }
    }

    // ... les autres méthodes du port (save, findAll...) suivent le même principe
}

```

Ce qui change par rapport à l'adapter JPA :
* **La signature reste celle du port** (`findByIdAndTenantId`) : c'est tout l'intérêt, le `TaskService` ne voit aucune différence.
* **Le `tenantId` n'est pas envoyé** : le `project-service` le déduit lui-même du JWT, comme notre monolithe. Encore faut-il lui transmettre le token de l'utilisateur (*token relay*, voir ci-dessous) : sans lui, l'appel répondra `401`.
* **On désérialise le DTO distant** (`ProjectResponse`), pas le record du domaine : le JSON ne contient pas le `tenantId`, et le domaine ne doit pas dépendre du format d'un autre service.
* **Les erreurs réseau deviennent des cas métier** : un `404` distant devient `Optional.empty()`. Un timeout ou un `500`, eux, remontent en exception : c'est la « latence et l'échec » vus plus haut.

> 💡 Avec Spring Boot 4, on peut aussi injecter un `RestClient.Builder` préconfiguré (traces, métriques) en ajoutant `spring-boot-starter-restclient`.

### 🔁 Propager l'identité : le *token relay*

Dans le monolithe, le tenant est lu dans le JWT par le `TenantFilter`. Quand le `task-service` appelle le `project-service`, ce dernier doit **lui aussi** savoir pour quel utilisateur il travaille, sinon il répond `401` (pas de token) ou, pire, renvoie les données sans filtre.

**Appel synchrone (HTTP)** : on retransmet le JWT de l'utilisateur, tel quel. Le service appelé le vérifie et en déduit le tenant (le `sub`), exactement comme s'il était appelé par le front :

```java
this.restClient = RestClient.builder()
        .baseUrl(projectServiceUrl)
        .requestInterceptor((request, body, execution) -> {
            // Token relay : on recopie le JWT de la requête en cours
            if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth) {
                request.getHeaders().setBearerAuth(auth.getToken().getTokenValue());
            }
            return execution.execute(request, body);
        })
        .build();
```

On ne transmet **pas** le `tenantId` dans un header maison (`X-Tenant-ID`) : le service appelé devrait alors faire confiance à l'appelant, et n'importe qui ayant accès au réseau interne pourrait se faire passer pour un autre tenant (même raisonnement que dans `api-spec.md`, « Identification du tenant »).

**Appel asynchrone (message RabbitMQ / Kafka)** : il n'y a plus de requête HTTP ni de token (et le token aura peut-être expiré quand le message sera traité). Le `tenantId` est alors écrit **dans le message** :

```java
public record TaskDeletedEvent(String tenantId, Long taskId, List<String> objectKeys) {}
```

Le consommateur exécute son traitement avec ce tenant. Ici, on fait confiance au message parce que seuls nos services peuvent publier dans le broker (droits d'accès sur la file) : c'est la sécurité du broker qui garantit l'origine.

On peut également utiliser Feign dans l'environnement Spring Boot.

👉 **La puissance de la Clean Architecture est là : le métier ne sait pas si la donnée vient de la BDD locale ou d'un service distant.**

---

# 🧭 5. Architecture cible distribuée

Si SmartTasks devenait le nouveau Trello, voici l'architecture :

```
       Client (React)
             │
             ▼
    +------------------+
    |   API Gateway    |  (Route les requêtes, rejette tôt les JWT invalides)
    +--------+---------+
             │
    +--------+------------+------------------+
    │                     │                  │
    ▼                     ▼                  ▼
+--------------+   +--------------+   +--------------+
| Project Svc  |   |   File Svc   |   | Notification |
| (Postgres A) |   | (Postgres B) |   |     Svc      |
+--------------+   +-------+------+   +------+-------+
                           │                 ▲
                           ▼                 │
                      S3 (RustFS)         RabbitMQ (Async)

```

---

Le JWT est émis par le fournisseur d'identité (Google), **hors** de ce schéma. La Gateway peut le vérifier pour rejeter tôt les requêtes invalides, mais chaque service le vérifie **aussi** (défense en profondeur) : un service ne doit pas faire confiance à une requête au seul motif qu'elle vient du réseau interne.

---

# 📝 6. Exercice de réflexion

Vous êtes architecte. On vous demande d'extraire la gestion des fichiers (`Attachment`) dans un microservice dédié `storage-service`.

1. **Impact BDD** : Que devient la table `attachments` ? Doit-elle rester liée aux tables `tasks` par une clé étrangère ?
<details>
<summary>Voir la réponse</summary>

Non, intégrité référentielle impossible entre 2 bases. On stocke juste l'ID.
</details>

2. **Communication** : Comment `TaskService` vérifie qu'un fichier existe avant de le lier ?
<details>
<summary>Voir la réponse</summary>

Appel synchrone (REST/Feign) vers `storage-service`.
</details>

3. **Nettoyage** : Si on supprime une tâche, comment supprimer les fichiers associés ?
<details>
<summary>Voir la réponse</summary>

Appel asynchrone (Event Driven). `TaskService` publie un événement `TaskDeletedEvent` dans un broker (RabbitMQ/Kafka). Le `storage-service` écoute et supprime les fichiers.
</details>

---

# 🏁 Conclusion du cours

Félicitations ! 🎉 Vous avez traversé les concepts clés du développement Backend moderne :

1. **Spring Boot & REST** (Les bases)
2. **JPA & Relations** (La persistance)
3. **Security & OAuth2** (La protection)
4. **Multi-Tenancy** (L'isolation SaaS)
5. **Stockage objet S3** (Le stockage Cloud)
6. **Clean Architecture** (La maintenabilité)
7. **Microservices** (La scalabilité)

Vous avez maintenant toutes les armes pour construire des applications robustes, sécurisées et évolutives.

**Bonne continuation dans votre carrière d'ingénieur !** 🚀
