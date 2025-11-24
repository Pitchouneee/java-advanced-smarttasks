# Planning du Cours Java Avancé – SmartTasks (25h)

Ce planning détaille le déroulé complet des 4 jours du cours (25h).  
Chaque journée alterne théorie, démonstrations et travaux pratiques guidés.

---

# 📅 Vue d’ensemble

| Jour       | Thèmes principaux                                       | Durée |
|------------|---------------------------------------------------------|-------|
| **Lundi**  | API REST, Spring Boot, JPA                              | 7h    |
| **Mardi**  | Sécurité OAuth2/JWT, Multi-tenancy, Swagger & Front     | 7h    |
| **Mercredi** | Tests, CI/CD GitHub, Upload fichiers (MinIO)            | 7h    |
| **Jeudi**  | Clean Architecture, Refactoring, Monolithe vs Microservices, Démo finale | 4h    |

---

# 🗓️ Détail du planning

---

## 🟦 **Lundi – API REST, Spring Boot & JPA** (7h)

### **Matin (9h – 12h30)**

* Présentation du cours & projet SmartTasks
* Introduction à Spring Boot
* Création du projet
* Structure d'une API REST
* Notions : Controller, Service, Repository
* Premier endpoint GET /hello

### **Après-midi (14h – 17h)**

* Introduction à JPA & Hibernate
* Entité Project
* Repository Spring Data JPA
* CRUD complet
* Validation (`@NotBlank`)
* Exercices :
  + Créer Task
  + CRUD complet Task
  + Ajouter DTO + mapper

---

## 🟩 **Mardi – Sécurité & Multi-tenancy + Swagger + Front** (7h)

### **Matin (9h – 12h30)**

* Comprendre OAuth2 & JWT
* Mise en place du Resource Server
* Extraction des rôles & infos utilisateur
* Sécurisation des endpoints
* Mise en place CORS (React)

### **Après-midi (14h – 17h)**

* Multi-tenant soft :
  + `X-Tenant-ID`
  + `TenantContext`
  + Filtre HTTP
* Ajout du tenant dans Project/Task
* Documentation Swagger/OpenAPI
* Intégration front :
  + Axios client
  + Variables d'environnement
  + Premier appel API

---

## 🟧 **Mercredi – Tests, CI, Upload fichiers (MinIO)** (7h)

### **Matin (9h – 12h30)**

* Tests unitaires avec JUnit 5
* Mockito & MockMvc
* Tests d’intégration avec H2
* Couverture JaCoCo

### **Après-midi (14h – 17h)**

* Pipeline CI GitHub Actions
* Docker + MinIO
* Upload de fichiers :
  + Multipart/form-data
  + Service MinIO
  + Pièces jointes dans les Tasks
* Endpoint de téléchargement
* TP :
  + Formulaire React d’upload
  + Affichage des fichiers

---

## 🟥 **Jeudi – Clean Architecture & Microservices + Démo** (4h)

### **Matin (9h – 12h30)**

* Introduction Clean Architecture
* Ports & Adapters
* Séparation domain / application / infrastructure / presentation
* Refactoring du projet
* Introduction Microservices :
  + Avantages / inconvénients
  + Migration depuis SmartTasks

### **Fin de matinée (12h30 – 13h)**

* Démonstration finale
* Entretien technique simulé autour du projet
* Conclusion du cours

---

# 🏁 Fin du module

Le planning peut évoluer légèrement en fonction du rythme du groupe, mais ce déroulé garantit :
* un apprentissage progressif
* un projet complet et cohérent
* un maximum de pratique

Bonne semaine SmartTasks 🚀
