# 07 – Monolithe vs Microservices

Ce dernier module a pour objectif de vous donner une **vision architecturale globale**.
Nous allons comparer deux paradigmes : **le Monolithe (modulaire)** et **les Microservices**.

C’est une compétence essentielle pour comprendre pourquoi SmartTasks est construit ainsi, et comment l'architecture "Clean" du module précédent prépare l'avenir.

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

* `auth-service` (Gère JWT)
* `project-service` (Gère Projets & Tâches)
* `file-service` (Gère MinIO)

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
    
    private final RestClient restClient; // Client HTTP

    @Override
    public Optional<Project> findById(Long id) {
        // Au lieu de faire du SQL, on appelle l'autre microservice
        return restClient.get()
            .uri("http://project-service/api/projects/" + id)
            .retrieve()
            .body(Project.class);
    }
}

```

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
    |   API Gateway    |  (Route les requêtes, gère l'auth)
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
                        MinIO             RabbitMQ (Async)

```

---

# 📝 6. Exercice de réflexion

Vous êtes architecte. On vous demande d'extraire la gestion des fichiers (`Attachment`) dans un microservice dédié `storage-service`.

1. **Impact BDD** : Que devient la table `attachments` ? Doit-elle rester liée aux tables `tasks` par une clé étrangère ?
<details>
<summary>Voir la réponse</summary>
Non, intégrité référentielle impossible entre 2 bases. On stocke juste l'ID.
</details>

3. **Communication** : Comment `TaskService` vérifie qu'un fichier existe avant de le lier ?
<details>
<summary>Voir la réponse</summary>
Appel synchrone (REST/Feign) vers `storage-service`.
</details>

4. **Nettoyage** : Si on supprime une tâche, comment supprimer les fichiers associés ?
<details>
<summary>Voir la réponse</summary>
Appel ssynchrone (Event Driven). `TaskService` publie un événement `TaskDeletedEvent` dans un broker (RabbitMQ/Kafka). Le `storage-service` écoute et supprime les fichiers.
</details>

---

# 🏁 Conclusion du cours

Félicitations ! 🎉 Vous avez traversé les concepts clés du développement Backend moderne :

1. **Spring Boot & REST** (Les bases)
2. **JPA & Relations** (La persistance)
3. **Security & OAuth2** (La protection)
4. **Multi-Tenancy** (L'isolation SaaS)
5. **MinIO** (Le stockage Cloud)
6. **Clean Architecture** (La maintenabilité)
7. **Microservices** (La scalabilité)

Vous avez maintenant toutes les armes pour construire des applications robustes, sécurisées et évolutives.

**Bonne continuation dans votre carrière d'ingénieur !** 🚀
