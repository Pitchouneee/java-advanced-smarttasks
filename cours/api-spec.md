# 📋 Cahier des charges – Endpoints API (SmartTasks)

Ce document définit le contrat d'interface strict que le backend doit respecter pour fonctionner avec le frontend React fourni.

> 🎯 **Règle d'or** : Le front-end (React) est le "client". Le backend doit s'adapter à ses attentes (noms des champs, formats, endpoints).

---

## 🛠️ Contraintes techniques globales

### 1. Headers HTTP
Le frontend envoie systématiquement l'en-tête suivant pour identifier l'organisation. Vous devez l'utiliser pour filtrer les données (Multi-tenancy).

| Header | Description | Exemple |
| :--- | :--- | :--- |
| `X-Tenant-ID` | Identifiant de l'utilisateur courant. | `demo` |
| `Authorization` | Token JWT (Bearer). | `Bearer eyJhbG...` |

### 2. Formats de données
* **Dates** : Format ISO 8601 strict (`yyyy-MM-dd'T'HH:mm:ss`).
* **ID** : Format `string` ou `number` (le front gère les deux, mais préférez `Long` côté JSON).

---

## 🏗️ Structures de données (DTO)

### `PageResponse<T>` (Pagination)
Structure standard utilisée pour toutes les listes.
```json
{
  "content": [ { ... } ],
  "empty": false,
  "first": true,
  "last": false,
  "number": 0,          // Page courante (index 0)
  "size": 20,           // Éléments par page
  "totalPages": 5,
  "totalElements": 42,
  "numberOfElements": 10
}

```

### `Project`

⚠️ Notez le nom du champ date : `createdOn`.

```json
{
  "id": 1,
  "name": "Campagne Marketing 2025",
  "createdOn": "2025-01-01T10:00:00"
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
  "createdOn": "2025-01-02T14:30:00"
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
  "createdOn": "2025-01-02T15:00:00"
}

```

### `DashboardResponse`

```json
{
  "activeProjectsCount": 12,
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
* **Réponse** : Objet `Project` créé (avec ID et date).

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


* **Réponse** : Objet `Task` créé.

#### `GET /api/tasks/{id}`

Récupère le détail d'une tâche (utilisé dans la page de détail).

* **Réponse** : Objet `Task`

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
* **Réponse** : Objet `Attachment` créé (contenant le lien `data` généré).

#### `GET /api/attachments/{id}/download`

Télécharge le fichier binaire.

* **Réponse** : Stream binaire du fichier.
* **Headers attendus** : `Content-Type` (ex: application/pdf) et `Content-Disposition` (attachment; filename="...").

---

### 📊 Tableau de bord

#### `GET /api/dashboard`

Données agrégées pour l'accueil.

* **Réponse** : Objet `DashboardResponse`