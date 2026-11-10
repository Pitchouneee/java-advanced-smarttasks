# 📋 Cahier des charges – Endpoints API (SmartTasks)

Ce document définit le contrat d'interface strict que le backend doit respecter pour fonctionner avec le frontend React fourni.

> 🎯 **Règle d'or** : Le front-end (React) est le "client". Le backend doit s'adapter à ses attentes (noms des champs, formats, endpoints).

> 🧪 Les endpoints marqués 🧪 ne sont **pas utilisés par le front fourni** : ils font partie du CRUD demandé au module 01 et se testent avec Bruno ou Swagger.

---

## 🛠️ Contraintes techniques globales

### 1. Headers HTTP
Le frontend envoie systématiquement l'en-tête suivant sur chaque appel à l'API :

| Header | Description | Exemple |
| :--- | :--- | :--- |
| `Authorization` | ID token Google (JWT), au format Bearer. | `Bearer eyJhbG...` |

### 2. Identification du tenant (multi-tenancy)
Il n'y a **pas** de header dédié au tenant : le backend le déduit du JWT déjà validé.

* **Tenant** = claim `sub` du token (identifiant unique et stable de l'utilisateur Google).
* Toutes les données (projets, tâches, pièces jointes) sont filtrées sur ce `tenantId`.

> 🔒 **Pourquoi pas un header `X-Tenant-ID` ?** Un header est contrôlé par le client : n'importe qui pourrait y mettre l'identifiant d'un autre tenant. Le `sub`, lui, est signé par Google et ne peut pas être falsifié.

### 3. Formats de données
* **Horodatages** (`createdOn`) : ISO 8601 en **UTC**, suffixe `Z` (type Java `Instant`).
  Exemple : `"2025-01-01T10:00:00Z"`. Les fractions de seconde sont possibles (`"2025-01-01T10:00:00.123456Z"`) : le client doit les accepter.
* **Dates seules** (`dueDate`) : `yyyy-MM-dd` (type Java `LocalDate`), sans heure ni fuseau.
* **ID** : nombre entier JSON (`Long` côté Java, `number` côté TypeScript).
* **Nommage** : la date de création s'appelle **toujours** `createdOn`, quelle que soit la ressource.

---

## 🏗️ Structures de données (DTO)

### `PageResponse<T>` (Pagination)
Structure utilisée pour toutes les listes. C'est le format **`VIA_DTO`** de Spring Data, activé côté back par :

```java
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
```

```json
{
  "content": [ { ... } ],
  "page": {
    "size": 20,           // Éléments par page
    "number": 0,          // Page courante (index 0)
    "totalElements": 42,
    "totalPages": 3
  }
}
```

> 💡 Sans cette annotation, Spring sérialise directement l'objet `PageImpl` : la structure JSON dépend alors de détails internes qui peuvent changer d'une version à l'autre, et Spring affiche un avertissement au démarrage.

### `Project`

⚠️ Notez le nom du champ date : `createdOn`.

```json
{
  "id": 1,
  "name": "Campagne Marketing 2025",
  "createdOn": "2025-01-01T10:00:00Z"
}

```

### `Task`

```json
{
  "id": 101,
  "projectId": 1,
  "title": "Rédiger le brief",
  "description": "Détails de la tâche...",
  "dueDate": "2025-02-15",  // Optionnel (peut être null)
  "completed": false,       // Une tâche terminée n'est jamais "en retard" (dashboard)
  "createdOn": "2025-01-02T14:30:00Z"
}

```

### `Attachment`

```json
{
  "id": 55,
  "originalName": "cahier_des_charges.pdf",
  "size": 102400,
  "mimeType": "application/pdf",
  "data": "/api/attachments/55/download", // URL relative de téléchargement
  "createdOn": "2025-01-02T15:00:00Z"
}

```

### `DashboardResponse`

```json
{
  "activeProjectsCount": 12,   // projets ayant au moins une tâche non terminée
  "totalTasksCount": 45,
  "overdueTasksCount": 3,
  "latestProjects": [ { ... } ] // Liste d'objets Project
}

```

---

## 🚀 Endpoints API

### 📁 Gestion des projets

#### `GET /api/projects`

Récupère la liste paginée des projets du tenant.

* **Query Params** : `page` (int, defaut 0), `size` (int, defaut 20)
* **Réponse** : `PageResponse<Project>`

#### `GET /api/projects/{id}`

Récupère un projet par son ID.

* **Réponse** : Objet `Project`
* **Erreur** : `404 Not Found` si inexistant.

#### `POST /api/projects`

Crée un nouveau projet.

* **Body** : `{ "name": "Nouveau Projet" }`
* **Réponse** : `201 Created`, objet `Project` créé (avec ID et date).

#### `PUT /api/projects/{id}` 🧪

Renomme un projet.

* **Body** : `{ "name": "Nouveau nom" }`
* **Réponse** : Objet `Project` mis à jour.
* **Erreurs** : `400` si le nom est invalide, `404` si le projet n'existe pas (ou appartient à un autre tenant).

#### `DELETE /api/projects/{id}` 🧪

Supprime un projet **et** ses tâches (cascade).

* **Réponse** : `204 No Content`.
* **Erreur** : `404 Not Found`.

---

### 📝 Gestion des tâches

#### `GET /api/projects/{projectId}/tasks`

Liste les tâches d'un projet spécifique.

* **Query Params** : `page`, `size`
* **Réponse** : `PageResponse<Task>`

#### `POST /api/projects/{projectId}/tasks`

Crée une tâche dans un projet.

* **Body** :
```json
{
  "title": "Titre tâche",
  "description": "Description...",
  "dueDate": "2025-12-31" // Optionnel
}

```


* **Réponse** : `201 Created`, objet `Task` créé.

#### `GET /api/tasks/{id}`

Récupère le détail d'une tâche (utilisé dans la page de détail).

* **Réponse** : Objet `Task`

#### `PUT /api/tasks/{id}` 🧪

Modifie une tâche (remplacement complet des champs modifiables).

* **Body** :
```json
{
  "title": "Titre modifié",
  "description": "Description...",
  "dueDate": "2025-12-31", // Optionnel
  "completed": true
}
```
* **Réponse** : Objet `Task` mis à jour.
* **Erreurs** : `400` si invalide (mêmes règles que la création), `404` si inexistante.

#### `DELETE /api/tasks/{id}` 🧪

Supprime une tâche **et** ses pièces jointes : lignes en base **et** fichiers dans le stockage objet.

* **Réponse** : `204 No Content`.
* **Erreur** : `404 Not Found`.

---

### 📎 Gestion des fichiers (Attachments)

#### `GET /api/tasks/{taskId}/attachments`

Liste les fichiers liés à une tâche.

* **Query Params** : `page`, `size`
* **Réponse** : `PageResponse<Attachment>`

#### `POST /api/tasks/{taskId}/attachments`

Upload un fichier pour une tâche.

* **Content-Type** : `multipart/form-data`
* **Body** : Champ `file` (binaire)
* **Réponse** : `201 Created`, objet `Attachment` créé (contenant le lien `data` généré).

#### `GET /api/attachments/{id}/download`

Télécharge le fichier binaire.

* **Réponse** : Stream binaire du fichier.
* **Headers attendus** : `Content-Type` (ex: application/pdf) et `Content-Disposition` (attachment; filename="...").

#### `DELETE /api/attachments/{id}` 🧪

Supprime une pièce jointe (ligne en base **et** objet dans le stockage).

* **Réponse** : `204 No Content`.
* **Erreur** : `404 Not Found`.

---

### 📊 Tableau de bord

#### `GET /api/dashboard`

Données agrégées pour l'accueil.

* **Réponse** : Objet `DashboardResponse`