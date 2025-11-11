# 📚 Cours Java Master – Projet SmartTasks (25h)

Bienvenue dans ce dépôt pédagogique pour un cours Java avancé de 25h à destination aux élèves d'ESI. Le fil rouge de cette semaine est un projet concret : **SmartTasks**, un système de gestion de tâches multi-entreprises.

L'objectif est double :

* ⚙️ Acquérir des compétences avancées en Java (Spring Boot, JPA, sécurité, tests, fichiers)
* 🚀 Construire une application REST modulaire

## 🗂️ Contenu du dépôt

```
📦 smarttasks
 ┣ 📂 cours
 ┃ ┣ 📜 01-api-rest-spring-boot.md
 ┃ ┣ 📜 02-jpa-et-relations.md
 ┃ ┣ 📜 03-securite-et-multi-tenancy.md
 ┃ ┣ 📜 04-swagger-front.md
 ┃ ┣ 📜 05-tests-ci.md
 ┃ ┣ 📜 06-upload-minio.md
 ┃ ┣ 📜 07-clean-architecture.md
 ┃ ┣ 📜 08-monolithe-vs-microservices.md
 ┃ ┗ 📜 planning.md
 ┣ 📂 projet-back
 ┃ ┗ 📜 (code Spring Boot à compléter)
 ┣ 📂 projet-front
 ┃ ┗ 📜 (code React fourni)
 ┣ 📜 README.md
```

## 🧑‍💻 Projet fil rouge : SmartTasks

SmartTasks est une application permettant à plusieurs entreprises de gérer leurs projets, leurs utilisateurs et leurs tâches, de manière isolée (multi-tenant).

Chaque étudiant contribue à construire cette application en suivant les modules de cours.

Fonctionnalités visées :

* Authentification OAuth2
* Gestion multi-entreprise (tenant)
* API REST pour projets, utilisateurs, tâches
* Upload de fichiers liés aux tâches
* Tests unitaires et CI
* Documentation Swagger
* Front React connecté (fourni)

## 📅 Planning résumé

| Jour     | Thèmes                                     | Durée |
| -------- | ------------------------------------------ | ----- |
| Lundi    | API REST, JPA                              | 7h    |
| Mardi    | Sécurité, Swagger, Front                   | 7h    |
| Mercredi | Tests, Upload fichiers                     | 7h    |
| Jeudi    | Refacto, démo, monolithes vs microservices | 4h    |

Détail complet dans [`cours/planning.md`](cours/planning.md)

## 🧠 Bonus : Architecture monolithe vs microservices

Un module est dédié à la comparaison entre architecture **monolithique** et **microservices**. Il vous permettra de comprendre :

* Les avantages/inconvénients de chaque approche
* Pourquoi on reste en monolithe dans ce cours
* Comment migrer vers des microservices plus tard

Voir [`cours/08-monolithe-vs-microservices.md`](cours/08-monolithe-vs-microservices.md)

## 🎓 Objectifs pédagogiques

À la fin du cours, vous serez capables de :

* Créer une API REST Java proprement architecturée
* Comprendre les concepts de sécurité et multi-tenant
* Documenter et tester leur application
* Travailler avec des fichiers (upload + stockage)

---

🛠️ Technologies utilisées :

* Java 25 + Spring Boot 4
* Spring Data JPA + PostgreSQL/MariaDB
* Spring Security (Resource Server)
* Swagger / OpenAPI
* React (fourni)
* MinIO (optionnel)

📘 Ce dépôt est pensé pour être un **support de cours interactif**. Chaque dossier correspond à une séance et contient des fichiers `.md` avec théorie + TP guidés.

> Bon code et bon courage 💪
