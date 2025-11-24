# 08 – Monolithe vs Microservices

Ce dernier module de la semaine a pour objectif de vous donner une **vision architecturale globale**.  
Nous allons comparer deux façons de structurer une application : **le monolithe** et **les microservices**.

C’est une compétence essentielle pour comprendre pourquoi SmartTasks reste un monolithe… et comment il pourrait évoluer plus tard.

---

# 🎯 Objectifs du module

À la fin de ce chapitre, vous serez capables de :

* Expliquer les différences entre monolithe et microservices
* Comprendre les avantages / inconvénients de chaque modèle
* Identifier quand utiliser l’un ou l’autre
* Découper un monolithe vers une architecture microservices
* Visualiser une architecture moderne avec API Gateway, discovery, etc.

---

# 🧩 1. Définition : Monolithe

Un **monolithe** est une application unique qui contient :

* le backend
* les fonctionnalités métier
* l’accès aux données
* les services externes
* les jobs
* parfois le front

### ✨ Avantages

* Simple à développer
* Simple à tester
* Simple à déployer
* Une seule base de code
* Moins de complexité technique
* Moins de coûts d’infrastructure

### ⚠️ Inconvénients

* Devient difficile à maintenir avec la taille
* Une seule équipe doit coordonner tout
* Un bug peut arrêter toute l’application
* Déploiement unique → pas de granularité
* Pas optimal pour les très grands systèmes

---

# 🔥 2. Définition : Microservices

Une architecture **microservices** découpe une application en plusieurs services indépendants :

Exemples :
* `auth-service`
* `task-service`
* `project-service`
* `file-service`
* `notification-service`
* etc.

Chacun possède :

* son code
* sa base de données (ou schéma séparé)
* son cycle de vie
* ses déploiements

### ✨ Avantages

* Scalabilité indépendante
* Déploiements indépendants
* Équipes autonomes
* Haute résilience
* Technologie différente par service possible (polyglotte)

### ⚠️ Inconvénients

* Complexité très élevée
* Problèmes réseau, latence, timeouts
* Monitoring obligatoire
* Gestion des logs distribués
* Transactions distribuées
* Besoin d’un orchestrateur : Kubernetes
* Besoin d’une API Gateway + Service Mesh
* Débogage difficile
* Coût financier important

---

# 🏢 3. Quand choisir quoi ?

| Contexte | Monolithe | Microservices |
|----------|-----------|---------------|
| Petite équipe | ⭐⭐⭐⭐⭐ | ⭐ |
| Projet étudiant | ⭐⭐⭐⭐⭐ | ⭐ |
| Rapidité de développement | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| Application simple | ⭐⭐⭐⭐⭐ | ⭐ |
| Grande entreprise | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Besoin de scalabilité extrême | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| Déploiement cloud complexe | ⭐⭐ | ⭐⭐⭐⭐⭐ |

Règle d’or :  
➡️ **Commencer monolithe. Migrer en microservices uniquement quand nécessaire.**

---

# 🏗️ 4. Pourquoi SmartTasks est un monolithe ?

Le projet SmartTasks reste monolithique car :

* cours de 25h → monolithe = plus efficace
* équipe étudiante → microservices trop lourds
* fonctionnalités limitées
* besoin de rester simple et pédagogique
* déploiement facilité

Pour un MVP / SaaS en début de vie :  
👉 **Le monolithe est le meilleur choix.**

---

# 🔄 5. Comment migrer SmartTasks vers des microservices ?

SmartTasks pourrait être découpé ainsi :

### Microservice 1 – `auth-service`

* Gestion des utilisateurs
* JWT
* OAuth2 / Keycloak

### Microservice 2 – `project-service`

* Projets
* Tâches
* Relations

### Microservice 3 – `file-service`

* Upload MinIO
* Gestion des fichiers

### Microservice 4 – `tenant-service`

* Gestion des entreprises
* Isolation multi-tenant

---

# 🧭 6. Architecture type microservices

```
              +----------------------+
              |      API Gateway     |
              +----------+-----------+
                         |
      +------------------+------------------+
      |                  |                  |
+-----v-----+      +-----v-----+      +-----v-----+
|  Project  |      |   Files   |      |   Auth    |
|  Service  |      |  Service  |      |  Service  |
+-----------+      +-----------+      +-----------+
      |                  |                  |
+-----v-----+      +-----v-----+      +-----v-----+
| Postgres  |      |  MinIO    |      |  Keycloak |
+-----------+      +-----------+      +-----------+
```

Les microservices communiquent entre eux via :

* HTTP REST
* Messaging (Kafka, RabbitMQ)
* gRPC (optionnel)

---

# 🛠️ 7. Migration progressive (stratégie)
1. **Identifier les frontières naturelles** du domaine (DDD)  
   - tâches
   - utilisateurs
   - fichiers

2. **Extraire un service à la fois**  
   Ex : file-service → indépendant

3. **Mettre en place une API Gateway**  
   Pour exposer une seule URL publique.

4. **Externaliser l’auth**  
   → Keycloak ou Auth0

5. **Isoler les bases de données**  
   → Un schéma ou base par service.

6. **Mettre en place un orchestrateur**  
   → Kubernetes

7. **Monitoring distribué**  
   → Prometheus, Grafana, Loki, Jaeger

---

# 🧪 8. Exercices du module
1. Proposer un découpage DDD de SmartTasks en 3 microservices.  
2. Décrire les endpoints REST de chaque service.  
3. Identifier les tables qui devraient être séparées.  
4. Dessiner une architecture complète avec API Gateway.  
5. Bonus : implémenter un mini `file-service` standalone.

---

# 🏁 Conclusion du module (et du cours)

Vous savez maintenant :

* Créer une API Rest complète
* Gérer JPA & relations
* Implémenter la sécurité OAuth2 + JWT
* Gérer le multi-tenant
* Documenter une API
* Tester & industrialiser un backend
* Stocker des fichiers dans MinIO
* Structurer un projet avec Clean Architecture
* Comprendre les architectures modernes

Vous êtes prêts pour développer des projets professionnels en Java 🚀

Bravo pour votre travail sur SmartTasks ! 🎉
