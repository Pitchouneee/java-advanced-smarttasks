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
4. Renseigner les deux variables :

```env
VITE_GOOGLE_CLIENT_ID=your_google_oauth_client_id
VITE_GOOGLE_CLIENT_SECRET=your_google_oauth_client_secret
```

### 🔑 Obtenir les identifiants Google OAuth

1. Aller sur : https://console.cloud.google.com/
2. Menu **API & Services**
3. Dans la sidebar : **Identifiants**
4. Bouton **Créer des identifiants** → *ID client OAuth*
5. Type : **Application Web**
6. Configurer les URLs :

**Authorized JavaScript origins**
```
http://localhost:5173
```

**Authorized redirect URIs**
```
http://localhost:5173/signup
```

7. Cliquer sur **Créer**
8. Copier le **Client ID** et **Client Secret**

---

# 📦 4. Lancer les conteneurs nécessaires

Tout est déjà configuré dans le fichier :

```
docker-compose.yml
```

Il contient :

- PostgreSQL  
- MinIO  
- Réseaux  
- Volumes  

Lancer l’environnement :

```bash
docker compose up -d
```

### Vérifications rapides

| Service | URL |
|--------|-----|
| MinIO console | http://localhost:9001 |

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
 ┣ 📄 docker-compose.yml
 ┗ README.md
```

---

# 💡 7. Problèmes fréquents et solutions

| Problème | Solution |
|----------|----------|
| `JAVA_HOME not found` | IntelliJ > Settings > Build Tools > Maven > SDK |
| Docker ne démarre pas | Activer la virtualisation dans le BIOS |
| Port déjà utilisé | Modifier les ports dans `docker-compose.yml` |
| Maven non détecté | Ouvrir avec IntelliJ, qui télécharge Maven automatiquement |
| Login OAuth impossible | Vérifier les URLs Google (origins + redirect URIs) |
| Erreur MinIO auth | Vérifier user/password dans `docker-compose.yml` |

---

# 🎉 Vous êtes prêts !

Passez maintenant au module suivant :  
👉 **01 – API REST & Spring Boot**

Bon courage et bon code 🚀