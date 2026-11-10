# 00 – Setup & Installation

Ce document décrit l’ensemble des prérequis techniques nécessaires pour suivre efficacement le cours Java avancé et développer le projet **SmartTasks**.

🎯 Objectif : garantir que tout le monde démarre avec un environnement fonctionnel et homogène.

---

# 🧰 1. Outils nécessaires

> 💡 Vous n’avez pas besoin d’installer manuellement Java ou Maven : **IntelliJ s’en charge automatiquement.**

### ✔️ IntelliJ IDEA (Community recommandé)

👉 https://www.jetbrains.com/idea/download/  
➡️ Utilisé pour : écrire du code, lancer les projets, télécharger JDK 25 et Maven automatiquement.

### ✔️ Node.js (≥ 18)

👉 https://nodejs.org/

```bash
node -v
npm -v
```

### ✔️ Docker Desktop

👉 https://www.docker.com/products/docker-desktop/

Pour vérifier :

```bash
docker run hello-world
```

### ✔️ Un client HTTP

Recommandation : **Bruno**  
👉 https://www.usebruno.com/

Alternatives : Postman / Insomnia / Thunder Client

### ✔️ Git

👉 https://git-scm.com/

```bash
git --version
```

---

# 🗄️ 2. Cloner le dépôt du cours

```bash
git clone https://github.com/Pitchouneee/java-advanced-smarttasks.git
cd java-advanced-smarttasks
```

---

# ⚙️ 3. Préparer le front-end

Le front React est prêt et configuré pour l’authentification **Google OAuth**.

### Étapes :

1. Aller dans : `projet-front`
2. Copier `.env.example`
3. Le renommer en `.env`
4. Renseigner la variable :

```env
VITE_GOOGLE_CLIENT_ID=your_google_oauth_client_id
VITE_API_BASE_URL=http://localhost:8080
```

> 🔒 **Pas de Client Secret côté front.** Toute variable `VITE_*` est intégrée au JavaScript envoyé au navigateur : n'importe qui peut la lire avec F12. Le bouton « Sign in with Google » n'a besoin que du Client ID, qui est public par nature. Un secret n'a sa place que côté serveur.

### 🔑 Obtenir les identifiants Google OAuth

1. Aller sur : https://console.cloud.google.com/
2. Menu **API & Services**
3. Dans la sidebar : **Identifiants**
4. Si Google le demande, configurer d'abord l'**écran de consentement OAuth** : type *Externe*, nom de l'application, votre e-mail. En mode *Test*, ajoutez votre adresse dans les **utilisateurs test**.
5. Bouton **Créer des identifiants** → *ID client OAuth*
6. Type : **Application Web**
7. Configurer les URLs :

**Authorized JavaScript origins** (les deux lignes, comme le recommande Google pour le développement local)
```
http://localhost
http://localhost:5173
```

**Authorized redirect URIs** : laisser **vide**.

> 💡 Le front utilise le bouton « Sign in with Google » (bibliothèque Google Identity Services) : la connexion se fait dans une **popup** et le token est renvoyé directement au JavaScript de la page. Il n'y a pas de redirection, donc pas de redirect URI à déclarer. Google vérifie seulement que la page qui demande le token vient d'une origine autorisée.

8. Cliquer sur **Créer**
9. Copier le **Client ID** (Google affiche aussi un *Client Secret* : ne pas l'utiliser, il est inutile ici)

> 💡 Gardez ce Client ID sous la main : le backend en aura aussi besoin au module **03** pour vérifier que les tokens lui sont bien destinés.

---

# 📦 4. Lancer les conteneurs nécessaires

Tout est déjà configuré dans le fichier :

```
docker-compose.yaml
```

Il contient :

- PostgreSQL  
- RustFS (stockage objet compatible S3, remplace MinIO)  
- Réseaux  
- Volumes  

Lancer l’environnement :

```bash
docker compose up -d
```

### Vérifications rapides

| Service | Adresse | Identifiants |
|--------|-----|-----|
| PostgreSQL | `localhost:5432`, base `smarttasks` | `smart` / `smart` |
| RustFS : API S3 | http://localhost:9000 | `rustfsadmin` / `rustfsadmin` |
| RustFS : console web | http://localhost:9001 | `rustfsadmin` / `rustfsadmin` |

`docker compose ps` doit afficher les deux conteneurs `smarttasks-postgres` et `smarttasks-rustfs` à l'état *running*.

---

# 🧪 5. Vérification du front React

Dans le dossier `projet-front` :

```bash
npm install
npm run dev
```

➡️ Lancer l'app sur :  
http://localhost:5173

---

# 🔗 6. Structure du projet attendue

```
📦 java-advanced-smarttasks
 ┣ 📂 cours
 ┣ 📂 projet-back
 ┣ 📂 projet-front
 ┣ 📂 solutions
 ┣ 📄 docker-compose.yaml
 ┗ README.md
```

---

# 💡 7. Problèmes fréquents et solutions

| Problème | Solution |
|----------|----------|
| `JAVA_HOME not found` | IntelliJ > Settings > Build Tools > Maven > SDK |
| Docker ne démarre pas | Activer la virtualisation dans le BIOS |
| Port déjà utilisé | Modifier les ports dans `docker-compose.yaml` |
| Maven non détecté | Ouvrir avec IntelliJ, qui télécharge Maven automatiquement |
| Login OAuth impossible | Vérifier les *Authorized JavaScript origins* (`http://localhost` **et** `http://localhost:5173`) et que votre compte est dans les utilisateurs test |
| Erreur S3 `403` / `SignatureDoesNotMatch` | Les identifiants `storage.s3.*` de `application.yaml` doivent correspondre à `RUSTFS_ACCESS_KEY` / `RUSTFS_SECRET_KEY` dans `docker-compose.yaml` |

---

# 🎉 Vous êtes prêts !

Passez maintenant au module suivant :  
👉 **01 – API REST & Spring Boot**

Bon courage et bon code 🚀