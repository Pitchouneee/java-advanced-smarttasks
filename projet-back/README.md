# projet-back : votre backend SmartTasks

Ce dossier est **vide au départ, et c'est normal** : c'est ici que vous allez construire votre API Spring Boot, module après module.

## 1. Générer le projet (module 01)

Sur [start.spring.io](https://start.spring.io), avec la configuration du module 01 :

| Option | Valeur |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 4.0.x |
| Group | `fr.corentinbringer` |
| Artifact | `smarttasks` |
| Packaging | Jar |
| Java | 25 |

Dépendances : Spring Web, Spring Data JPA, Validation, PostgreSQL Driver, Lombok, Spring Boot DevTools.

Cliquez sur **Generate**, puis **dézippez le contenu de l'archive directement dans ce dossier**. Vous devez obtenir :

```
projet-back/
 ┣ 📂 .mvn/
 ┣ 📂 src/
 ┃ ┣ 📂 main/java/fr/corentinbringer/smarttasks/SmarttasksApplication.java
 ┃ ┗ 📂 test/
 ┣ 📄 mvnw, mvnw.cmd       ← Maven Wrapper : pas besoin d'installer Maven
 ┣ 📄 pom.xml
 ┗ 📄 README.md            ← ce fichier
```

> ⚠️ Pas de sous-dossier `projet-back/smarttasks/` : le `pom.xml` doit être **à la racine** de `projet-back`. La CI du module 07 (bonus) lance `./mvnw` depuis ce dossier.

## 2. Ouvrir et lancer

1. IntelliJ : **File → Open** → sélectionnez le dossier `projet-back` (IntelliJ détecte le `pom.xml`).
2. Démarrez l'infrastructure depuis la racine du dépôt : `docker compose up -d` (PostgreSQL et RustFS, voir `cours/00-setup.md`).
3. Définissez la variable d'environnement `GOOGLE_CLIENT_ID` dans la configuration de lancement (à partir du module 03).
4. Lancez `SmarttasksApplication`, ou en ligne de commande : `./mvnw spring-boot:run`.
5. Tests : `./mvnw test` (module 07).

## 3. Ce que vous ajouterez, module par module

| Module | Ajouts dans `projet-back` |
|---|---|
| 01 – API REST | `project/` (controller, service, repository, model), `exception/` |
| 02 – JPA | entités `Project` / `Task` et leurs relations, DTO, projections |
| 03 – Sécurité | `configuration/SecurityConfig`, `configuration/tenant/` (TenantContext, TenantFilter) |
| 04 – Swagger | dépendance springdoc, `configuration/OpenApiConfig` |
| 05 – Upload S3 | `configuration/storage/`, `storage/`, entité `Attachment` |
| 06 – Clean Architecture | réorganisation en `domain` / `application` / `infrastructure` |
| 07 – Tests | `src/test/java/...` |

## 4. En cas de blocage

Les corrigés sont dans `solutions/` :

* `before-clean-architecture` : application complète **avant** le refactoring du module 06 (architecture en couches ; la documentation Swagger du module 04 n'y figure pas) ;
* `after-clean-architecture` : état attendu à la fin des modules 06 et 07.

Essayez d'abord par vous-même : comparer votre code au corrigé **après** avoir cherché est beaucoup plus formateur que de le recopier.

## 5. À ne pas commiter

Le `.gitignore` généré par Initializr exclut déjà `target/` et les fichiers d'IDE. Ne commitez jamais de secret (identifiants Google, clés S3 de production) : utilisez des variables d'environnement.
