# 📚 Cours Java avancé -- Projet SmartTasks (25h, Master)

Bienvenue sur le dépôt officiel du **cours Java avancé (25h)** destiné
aux étudiants de Master à l'ESI. Toute la semaine, vous allez concevoir
un **mini SaaS complet**, de l'architecture backend jusqu'à
l'intégration front.

Le fil conducteur du cours est un projet concret : **SmartTasks**, une
application de gestion de tâches multi-tenant.

---

# 🎯 Objectifs du cours

À la fin de ce module, vous serez capables de :

### 🟦 Compétences techniques

*   Créer une **API REST professionnelle** avec Spring Boot
*   Maîtriser **Spring Data JPA** (entités, relations, DTO, mappers)
*   Implémenter une **sécurité OAuth2 / JWT**
*   Comprendre et appliquer le **multi-tenant** (soft)
*   Documenter une API via **Swagger / OpenAPI**
*   Gérer l'**upload de fichiers** via l'API S3 (RustFS en local)
*   Structurer proprement un projet (Clean Architecture)

### 🟩 Compétences projet / architecture

*   Comprendre les architectures **monolithe vs microservices**
*   Découper proprement un projet en modules
*   Travailler avec une stack complète : **Java + React**

---

# 🧑‍💻 Projet fil rouge : SmartTasks

SmartTasks est une application de gestion de projets et de tâches.\
Chaque utilisateur dispose de son propre espace de données
(**multi-tenant**, un tenant = un compte Google), isolé de celui des autres.

> 💡 Dans un vrai SaaS B2B, le tenant serait plutôt une **organisation** regroupant plusieurs utilisateurs.
> On simplifie volontairement ici : le principe d'isolation (colonne `tenant_id`, filtrage systématique) est exactement le même.

### Fonctionnalités à implémenter :

*   🔐 Authentification OAuth2 (Resource Server / JWT)
*   🏢 Gestion multi-tenant (tenant déduit du JWT, claim `sub`)
*   📝 CRUD projets et tâches
*   📎 Upload de fichiers pour les tâches
*   📘 Documentation Swagger
*   🔗 Intégration avec un front React (fourni)

---

# 📅 Planning de la semaine

| Jour         | Thèmes principaux                                                                  | Durée |
| ------------ | ---------------------------------------------------------------------------------- | ----- |
| **Lundi**    | 00 Setup · 01 API REST avec Spring Boot · 02 JPA & relations                       | 7h    |
| **Mardi**    | 03 Sécurité OAuth2/JWT & multi-tenant (5h) · 04 Swagger/OpenAPI & front (2h)       | 7h    |
| **Mercredi** | 05 Upload fichiers S3 / RustFS (3h) · 06 Clean Architecture (4h)                   | 7h    |
| **Jeudi**    | 07 Tests automatisés (2h) · 08 Monolithe vs microservices (1h) · démo finale (1h)  | 4h    |

---

# 🧪 Méthodologie pédagogique

Chaque séance suit une structure simple et efficace :

*   **20% théorie**
*   **60% TP guidé**
*   **20% autonomie / refactorings**

---

# 🧰 Prérequis & installation

Un guide d'installation complet est disponible dans `cours/00-setup.md` .

Requis :\
Java 25\
Docker Desktop\
Node.js\
Un IDE (IntelliJ recommandé)

---

# 🧠 Module bonus : Monolithe vs Microservices

Fin de semaine : analyse complète des architectures, migration possible, 
limites & avantages.

---

# 🛠️ Stack & outils

Backend : Java 25, Spring Boot 4.x, JPA, Security, PostgreSQL, 
S3 (RustFS en local)\
Frontend : React, Vite\
Outils : Docker, GitHub Actions, Swagger

---

# 🚀 Bon code et bonne semaine !

Amusez-vous à construire SmartTasks 💪
