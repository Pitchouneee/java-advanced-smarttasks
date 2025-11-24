# 00 – Setup & Installation

Ce document décrit l’ensemble des prérequis techniques nécessaires pour suivre efficacement le cours Java avancé et développer le projet **SmartTasks**.

L’objectif :
👉 garantir que tout le monde démarre avec un environnement fonctionnel et homogène.

---

# 🧰 1. Outils nécessaires

Vous devez installer :

### ✔️ Java Development Kit (JDK 21 ou 25)

Recommandé : **Temurin** (Adoptium)

👉 https://adoptium.net/

Pour vérifier :

```bash
java -version
```

### ✔️ Maven (≥ 3.9)

👉 https://maven.apache.org/download.cgi  

```bash
mvn -version
```

### ✔️ Node.js (≥ 18)

👉 https://nodejs.org/  

```bash
node -v
npm -v
```

### ✔️ Docker Desktop

👉 https://www.docker.com/products/docker-desktop/  

```bash
docker run hello-world
```

### ✔️ Un IDE Java

Recommandé : IntelliJ IDEA Community  
👉 https://www.jetbrains.com/idea/download/

### ✔️ Un client HTTP

Postman / Insomnia / Thunder Client

### ✔️ Git

👉 https://git-scm.com/  

```bash
git --version
```

---

# 🗄️ 2. Cloner le dépôt du cours

```bash
git clone https://github.com/<ton-repo>/smarttasks.git
cd smarttasks
```

---

# 🗃️ 3. Conteneurs nécessaires

### PostgreSQL (recommandé)

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_USER: smart
      POSTGRES_PASSWORD: smart
      POSTGRES_DB: smarttasks
    ports:
      - "5432:5432"
```

### MinIO

```yaml
  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: admin
      MINIO_ROOT_PASSWORD: password
    ports:
      - "9000:9000"
      - "9001:9001"
```

Accès console MinIO : http://localhost:9001

---

# 🧪 4. Vérification backend Spring Boot

```bash
mvn spring-boot:run
```

---

# 🧪 5. Vérification front React

```bash
npm install
npm run dev
```

➡️ http://localhost:5173

---

# 🔗 6. Structure du projet attendue

```
📦 smarttasks
 ┣ 📂 cours
 ┣ 📂 projet-back
 ┣ 📂 projet-front
 ┣ 📂 solutions
 ┗ README.md
```

---

# 💡 7. Problèmes courants
* `JAVA_HOME not found` → ajouter variable d’environnement  
* Docker ne démarre pas → vérifier virtualisation  
* Ports déjà utilisés → modifier dans docker-compose  
* Maven non détecté → IntelliJ > Invalidate caches

---

# 🎉 Vous êtes prêts !

Passez maintenant au module :  
**01 – API REST & Spring Boot**

Bonne installation 🚀
