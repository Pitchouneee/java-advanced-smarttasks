# 📋 Cahier des charges – Endpoints API

Ce document définit **les endpoints REST que le backend doit exposer** afin de permettre l’intégration complète avec le front-end React fourni.

> 🎯 Le front pilote la structure de l’API.  
> Tous les noms de routes, de champs et de formats doivent être strictement respectés.

---

## 📁 Ressource : Projet (`Project`)

### ✅ POST `/api/projects`

**Objectif** : Créer un nouveau projet.

#### 🔸 Corps de la requête (`application/json`)
```json
{
  "name": "Projet démo"
}
```

#### ✅ Réponse (`201 Created`)
```json
{
  "id": 1,
  "name": "Projet démo",
  "createdOn": "2025-12-04T14:00:00Z"
}
```

> 💡 Le champ `createdOn` est automatiquement défini côté back (ex: via `@CreationTimestamp`).

---

### ✅ GET `/api/projects`

**Objectif** : Lister tous les projets existants (ordre non imposé).

#### 🔸 Réponse (`200 OK`)
```json
[
  {
    "id": 1,
    "name": "Projet démo",
    "createdOn": "2025-12-04T14:00:00Z"
  },
  {
    "id": 2,
    "name": "Roadmap 2026",
    "createdOn": "2025-12-05T08:30:00Z"
  }
]
```

---

## 🛠️ Contraintes techniques

- Respecter les noms exacts
- Les dates doivent être **en format ISO 8601** (`Z` ou `+00:00` accepté)
- Retourner un **code HTTP approprié** (`201`, `200`, etc.)
- Aucune logique de pagination pour l’instant
- Les erreurs doivent être retournées au format :

```json
{
  "error": "Message explicite"
}
```