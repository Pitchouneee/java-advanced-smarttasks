# 05 – Swagger / OpenAPI & Intégration Front

Dans ce module, nous allons :

1. Documenter l’API SmartTasks avec **OpenAPI / Swagger**
2. Exposer une UI de test pour les endpoints
3. Préparer l’intégration côté **front React** (appel de l’API depuis le navigateur)

---

# 🎯 Objectifs du module

À la fin de ce chapitre, vous serez capables de :

* Ajouter la documentation OpenAPI à un projet Spring Boot
* Exposer Swagger UI pour tester vos endpoints
* Structurer une couche d’accès API côté React
* Gérer les URLs d’API via variables d’environnement
* Comprendre les bases de CORS côté backend

---

# 📦 1. Ajouter Swagger / OpenAPI à Spring Boot

Nous utilisons **springdoc-openapi**, librairie standard pour Spring Boot.

Dans `pom.xml` :

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

> ⚠️ Vérifiez que la version est compatible avec votre version de Spring Boot.

---

# 🌐 2. Endpoints OpenAPI / Swagger UI

Une fois la dépendance ajoutée et l’application redémarrée :

* Documentation brute JSON :  
  ➡️ `http://localhost:8080/v3/api-docs`

* Interface Swagger UI :  
  ➡️ `http://localhost:8080/swagger-ui/index.html`

Swagger UI permet de :

* Lister tous les endpoints
* Voir les verbes HTTP, paramètres, body, réponses
* Tester directement l’API depuis le navigateur

---

# 🧩 3. Exemple d’annotations OpenAPI

Pour enrichir la documentation, vous pouvez utiliser des annotations comme `@Operation` et `@Parameter` .

Exemple sur `ProjectController` :

```java
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService service;

    @Operation(summary = "Liste tous les projets de l'entreprise courante")
    @GetMapping
    public List<ProjectDto> findAll() {
        return service.findAll().stream()
            .map(ProjectMapper::toDto)
            .toList();
    }

    @Operation(summary = "Crée un nouveau projet")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Projet créé"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto create(@Valid @RequestBody CreateProjectRequest request) {
        return ProjectMapper.toDto(service.create(request));
    }
}
```

> Les DTO utilisés (ici `ProjectDto` , `CreateProjectRequest` ) seront automatiquement décrits dans le schéma OpenAPI.

---

# 🌍 4. CORS (Cross-Origin Resource Sharing)

Le front React tourne souvent sur `http://localhost:5173` (Vite), 
le backend sur `http://localhost:8080` .

👉 Sans configuration CORS, le navigateur peut bloquer les requêtes.

Configuration simple dans `SecurityConfig` ou une classe de config dédiée :

```java
@Bean
public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true);
        }
    };
}
```

---

# 🧱 5. Intégration côté front React

Côté front, on centralise les appels API dans un **client HTTP**.

## a) Variable d’environnement Vite

Dans `projet-front` , créez un fichier `.env` :

```env
VITE_API_BASE_URL=http://localhost:8080
```

Dans le code React, vous y accédez via :

```ts
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
```

---

## b) Client Axios

Installer Axios (si pas déjà) :

```bash
npm install axios
```

Créer un fichier `src/api/client.ts` :

```ts
import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const apiClient = axios.create({
  baseURL: `${API_BASE_URL}/api`,
});

// Optionnel : intercepteur pour ajouter le JWT
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("access_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  const tenant = localStorage.getItem("tenant_id") || "demo";
  config.headers["X-Tenant-ID"] = tenant;

  return config;
});
```

---

## c) Service pour les projets

Créer `src/api/projects.ts` :

```ts
import { apiClient } from "./client";

export interface Project {
  id: number;
  name: string;
}

export async function fetchProjects(): Promise<Project[]> {
  const response = await apiClient.get<Project[]>("/projects");
  return response.data;
}

export async function createProject(name: string): Promise<Project> {
  const response = await apiClient.post<Project>("/projects", { name });
  return response.data;
}
```

---

# 🖥️ 6. Utilisation dans un composant React

Exemple rapide dans `src/features/projects/ProjectsPage.tsx` :

```tsx
import { useEffect, useState } from "react";
import { fetchProjects, createProject, Project } from "../api/projects";

export function ProjectsPage() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [name, setName] = useState("");

  useEffect(() => {
    fetchProjects().then(setProjects);
  }, []);

  const handleCreate = async () => {
    if (!name.trim()) return;
    const created = await createProject(name.trim());
    setProjects((prev) => [...prev, created]);
    setName("");
  };

  return (
    <div>
      <h1>Projets</h1>

      <div>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Nom du projet"
        />
        <button onClick={handleCreate}>Créer</button>
      </div>

      <ul>
        {projects.map((p) => (
          <li key={p.id}>{p.name}</li>
        ))}
      </ul>
    </div>
  );
}
```

---

# 🧪 7. Tester via Swagger + Front
1. Démarrer le backend :  
   

```bash
   mvn spring-boot:run
   ```

2. Démarrer le front :  
   

```bash
   npm run dev
   ```

3. Vérifier les endpoints dans Swagger UI  
   ➡️ `http://localhost:8080/swagger-ui/index.html`

4. Vérifier l’affichage des projets dans la page React.

---

# 🎓 8. Exercices
1. Documenter tous les endpoints de `TaskController` via `@Operation`.
2. Ajouter des réponses `@ApiResponse` pour les erreurs (404, 400).
3. Ajouter dans le front :
   - une liste de tâches pour un projet donné
   - un formulaire de création de tâche

---

# 📘 Prochain module

➡️ **05 – Tests & CI**

Vous avez maintenant une API documentée et un front capable de l’appeler proprement 🚀
