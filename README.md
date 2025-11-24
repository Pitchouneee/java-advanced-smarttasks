# 📚 Cours Java avancé -- Projet SmartTasks (25h, Master)

Bienvenue sur le dépôt officiel du **cours Java avancé (25h)** destiné
aux étudiants de Master à l'ESI. Toute la semaine, vous allez concevoir
un **mini SaaS complet**, de l'architecture backend jusqu'à
l'intégration front.

Le fil conducteur du cours est un projet concret : **SmartTasks**, une
application de gestion de tâches multi-entreprises.

---

# 🎯 Objectifs du cours

À la fin de ce module, vous serez capables de :

### 🟦 Compétences techniques

*   Créer une **API REST professionnelle** avec Spring Boot
*   Maîtriser **Spring Data JPA** (entités, relations, DTO, mappers)
*   Implémenter une **sécurité OAuth2 / JWT**
*   Comprendre et appliquer le **multi-tenant** (soft)
*   Documenter une API via **Swagger / OpenAPI**
*   Réaliser des **tests unitaires** (JUnit, MockMVC)
*   Gérer l'**upload de fichiers** via MinIO / S3
*   Structurer proprement un projet (Clean Architecture)

### 🟩 Compétences projet / architecture

*   Comprendre les architectures **monolithe vs microservices**
*   Découper proprement un projet en modules
*   Travailler avec une stack complète : **Java + React**
*   Utiliser Git et un workflow simple de CI

---

# 🧑‍💻 Projet fil rouge : SmartTasks

SmartTasks est une application destinée aux entreprises souhaitant
organiser leurs projets, utilisateurs et tâches.\
Chaque entreprise dispose de son propre espace de données
(**multi-tenant**), isolé des autres.

### Fonctionnalités à implémenter :

*   🔐 Authentification OAuth2 (Resource Server / JWT)
*   🏢 Gestion multi-tenant (header `X-Tenant-ID`)
*   📝 CRUD projets, utilisateurs, tâches
*   📎 Upload de fichiers pour les tâches
*   📘 Documentation Swagger
*   🧪 Tests unitaires et d'intégration
*   🔗 Intégration avec un front React (fourni)

---

# 🗂️ Structure du dépôt

    📦 smarttasks
     ┣ 📂 cours
     ┃ ┣ 00-setup.md
     ┃ ┣ 01-api-rest-spring-boot.md
     ┃ ┣ 02-jpa-et-relations.md
     ┃ ┣ 03-securite-et-multi-tenancy.md
     ┃ ┣ 04-swagger-front.md
     ┃ ┣ 05-tests-ci.md
     ┃ ┣ 06-upload-minio.md
     ┃ ┣ 07-clean-architecture.md
     ┃ ┣ 08-monolithe-vs-microservices.md
     ┃ ┗ planning.md
     ┣ 📂 projet-back
     ┣ 📂 projet-front
     ┣ 📂 solutions
     ┗ README.md

---

# 📅 Planning de la semaine

  Jour           Thèmes principaux                                  Durée
  -------------- -------------------------------------------------- -------
  **Lundi**      API REST, structure Spring, JPA                    7h
  **Mardi**      Sécurité OAuth2, JWT, multi-tenant, Swagger        7h
  **Mercredi**   Tests unitaires, CI GitHub, Upload MinIO           7h
  **Jeudi**      Clean Architecture, refacto, microservices, démo   4h

---

# 🧪 Méthodologie pédagogique

Chaque séance suit une structure simple et efficace :

*   **20% théorie**
*   **60% TP guidé**
*   **20% autonomie / refactorings**

---

# 🧰 Prérequis & installation

Un guide d'installation complet est disponible dans `cours/00-setup.md` .

Requis : - Java 21/25\
* Maven\
* Docker Desktop\
* Node.js\
* Un IDE (IntelliJ recommandé)

---

# 🧠 Module bonus : Monolithe vs Microservices

Fin de semaine : analyse complète des architectures, migration possible, 
limites & avantages.

---

# 🛠️ Stack & outils

Backend : Java 25, Spring Boot 4.x, JPA, Security, PostgreSQL, 
MinIO\
Frontend : React, Vite\
Outils : Docker, GitHub Actions, Swagger

---

# 🚀 Bon code et bonne semaine !

Amusez-vous à construire SmartTasks 💪
