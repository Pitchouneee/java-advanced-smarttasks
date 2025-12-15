Voici le fichier `api-spec.md` mis à jour et complet, prêt à être téléchargé.

[api-spec.md](https://www.google.com/search?q=api-spec.md)

````file_content
# 📋 Cahier des charges – Endpoints API

Ce document définit **les endpoints REST que le backend doit exposer** afin de permettre l’intégration complète avec le front-end React fourni.

> 🎯 Le front pilote la structure de l’API.
> Tous les noms de routes, de champs et de formats doivent être strictement respectés.

---

## 🏗️ Structures de Données Communes

### `PageResponse<T>` (Réponse de Pagination)
Utilisée pour les collections paginées.
```json
{
  "content": [
    { /* Liste des objets T (Project, Task, or Attachment) */ }
  ],
  "empty": false,
  "first": true,
  "last": false,
  "number": 0,
  "numberOfElements": 10,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5
}
````

### `Project` (Projet)

```json
{
  "id": "string",
  "name": "string",
  "createdOn": "ISO 8601 date"
}
```

### `Task` (Tâche)

```json
{
  "id": "string",
  "projectId": "string",
  "title": "string",
  "description": "string",
  "dueDate": "ISO 8601 date | null",
  "createdAt": "ISO 8601 date"
}
```

### `Attachment` (Pièce Jointe)

```json
{
  "id": 1,
  "originalName": "string",
  "size": 1024,
  "mimeType": "string",
  "data": "string (Relative or absolute endpoint to download the file, e.g., /api/attachments/1/download)",
  "createdAt": "ISO 8601 date"
}
```

### `DashboardResponse` (Tableau de Bord)

```json
{
  "activeProjectsCount": 5,
  "totalTasksCount": 50,
  "overdueTasksCount": 2,
  "latestProjects": [
    { /* Project object */ }
  ]
}
```

-----

## 📁 Ressource : Projet (`Project`)

### ✅ POST `/api/projects`

**Objectif** : Créer un nouveau projet.

#### 🔸 Corps de la requête (`application/json`)

| Champ | Type | Description |
| :--- | :--- | :--- |
| `name` | `string` | Le nom du nouveau projet. |

```json
{
  "name": "Projet démo"
}
```

#### ✅ Réponse (`201 Created`)

Retourne l'objet `Project` créé.

```json
{
  "id": "1",
  "name": "Projet démo",
  "createdOn": "2025-12-04T14:00:00Z"
}
```

-----

### ✅ GET `/api/projects`

**Objectif** : Lister les projets avec pagination.

#### 🔸 Paramètres de requête (Query)

| Nom | Type | Description |
| :--- | :--- | :--- |
| `page` | `number` | Numéro de la page (commence à 0). Par défaut: 0. |
| `size` | `number` | Nombre d'éléments par page. Par défaut: 20. |

#### ✅ Réponse (`200 OK`)

Retourne une `PageResponse<Project>`. (Voir structure `Project` et `PageResponse` ci-dessus).

```json
{
  "content": [
    { "id": "1", "name": "Projet démo", "createdOn": "2025-12-04T14:00:00Z" },
    { "id": "2", "name": "Roadmap 2026", "createdOn": "2025-12-05T08:30:00Z" }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "number": 0,
  "size": 20,
  "...": "..."
}
```

-----

### ✅ GET `/api/projects/{id}`

**Objectif** : Récupérer les détails d'un projet.

#### 🔸 Réponse

  * `200 OK`: Retourne un objet `Project`.
  * `404 Not Found`: Si l'ID du projet n'existe pas.

<!-- end list -->

```json
{
  "id": "1",
  "name": "Projet démo",
  "createdOn": "2025-12-04T14:00:00Z"
}
```

-----

## 📝 Ressource : Tâche (`Task`)

### ✅ GET `/api/projects/{projectId}/tasks`

**Objectif** : Lister les tâches d'un projet spécifique, avec pagination.

#### 🔸 Paramètres de requête (Query)

| Nom | Type | Description |
| :--- | :--- | :--- |
| `page` | `number` | Numéro de la page (commence à 0). Par défaut: 0. |
| `size` | `number` | Nombre d'éléments par page. Par défaut: 20. |

#### ✅ Réponse (`200 OK`)

Retourne une `PageResponse<Task>`. (Voir structure `Task` et `PageResponse` ci-dessus).

-----

### ✅ POST `/api/projects/{projectId}/tasks`

**Objectif** : Créer une nouvelle tâche dans un projet.

#### 🔸 Corps de la requête (`application/json`)

| Champ | Type | Description |
| :--- | :--- | :--- |
| `title` | `string` | Le titre de la tâche (Requis). |
| `description` | `string` | La description de la tâche. |
| `dueDate` | `string` | Date limite de la tâche (Format ISO 8601). |

```json
{
  "title": "Ajouter un endpoint",
  "description": "Décrire le nouvel endpoint dans la spécification.",
  "dueDate": "2026-01-15"
}
```

#### ✅ Réponse (`201 Created`)

Retourne l'objet `Task` créé.

```json
{
  "id": "101",
  "projectId": "1",
  "title": "Ajouter un endpoint",
  "description": "Décrire le nouvel endpoint dans la spécification.",
  "dueDate": "2026-01-15T00:00:00Z",
  "createdAt": "2025-12-05T10:00:00Z"
}
```

-----

### ✅ GET `/api/tasks/{id}`

**Objectif** : Récupérer les détails d'une tâche.

#### 🔸 Réponse

  * `200 OK`: Retourne un objet `Task`.
  * `404 Not Found`: Si l'ID de la tâche n'existe pas.

<!-- end list -->

```json
{
  "id": "101",
  "projectId": "1",
  "title": "Ajouter un endpoint",
  "description": "Décrire le nouvel endpoint dans la spécification.",
  "dueDate": "2026-01-15T00:00:00Z",
  "createdAt": "2025-12-05T10:00:00Z"
}
```

-----

## 📎 Ressource : Pièce Jointe (`Attachment`)

### ✅ GET `/api/tasks/{taskId}/attachments`

**Objectif** : Lister les pièces jointes d'une tâche, avec pagination.

#### 🔸 Paramètres de requête (Query)

| Nom | Type | Description |
| :--- | :--- | :--- |
| `page` | `number` | Numéro de la page (commence à 0). Par défaut: 0. |
| `size` | `number` | Nombre d'éléments par page. Par défaut: 20. |

#### ✅ Réponse (`200 OK`)

Retourne une `PageResponse<Attachment>`. (Voir structure `Attachment` et `PageResponse` ci-dessus).

-----

### ✅ POST `/api/tasks/{taskId}/attachments`

**Objectif** : Uploader une nouvelle pièce jointe pour une tâche.

#### 🔸 Corps de la requête (`multipart/form-data`)

| Champ | Type | Description |
| :--- | :--- | :--- |
| `file` | `File` | Le fichier à uploader. |

#### ✅ Réponse (`201 Created`)

Retourne l'objet `Attachment` créé.

```json
{
  "id": 42,
  "originalName": "rapport.pdf",
  "size": 512000,
  "mimeType": "application/pdf",
  "data": "/api/attachments/42/download",
  "createdAt": "2025-12-05T15:00:00Z"
}
```

-----

### ✅ GET `/api/attachments/{id}/download`

**Objectif** : Télécharger une pièce jointe.

#### 🔸 Réponse (`200 OK`)

Retourne le contenu du fichier (stream binaire).

-----

## 📊 Ressource : Tableau de Bord (`Dashboard`)

### ✅ GET `/api/dashboard`

**Objectif** : Récupérer les données agrégées pour le tableau de bord.

#### ✅ Réponse (`200 OK`)

Retourne un objet `DashboardResponse`.

```json
{
  "activeProjectsCount": 5,
  "totalTasksCount": 50,
  "overdueTasksCount": 2,
  "latestProjects": [
    { "id": "1", "name": "Projet démo", "createdOn": "2025-12-04T14:00:00Z" },
    { "id": "2", "name": "Roadmap 2026", "createdOn": "2025-12-05T08:30:00Z" }
  ]
}
```

-----

## 🛠️ Contraintes techniques

  - Respecter les noms exacts
  - Les dates doivent être **en format ISO 8601** (`Z` ou `+00:00` accepté)
  - Retourner un **code HTTP approprié** (`201`, `200`, `404`, etc.)
  - L'authentification par jeton (`Authorization: Bearer <token>`) est implicite pour toutes les routes nécessitant un utilisateur.
  - Les erreurs doivent être retournées au format :

<!-- end list -->

```json
{
  "error": "Message explicite"
}
```