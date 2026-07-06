# Color Game — Backend (Spring Boot)

Backend REST API pour l'application Color Game : authentification JWT, gestion des rôles (ADMIN/USER), et historique des actions sur la grille de jeu.

Frontend associé : [color-game-frontend](https://github.com/aws110503/color-game-frontend)

## Stack technique

- Java 21
- Spring Boot (Web, Security, Data JPA)
- PostgreSQL 14+
- JWT (bibliothèque jjwt)
- Maven

## Prérequis

- JDK 21 installé (`java -version` doit afficher 21.x)
- PostgreSQL installé et en cours d'exécution
- Maven (fourni via le wrapper `mvnw`/`mvnw.cmd`, pas d'installation séparée nécessaire)

## 1. Configuration de la base de données

1. Créer la base de données PostgreSQL :
```sql
   CREATE DATABASE color_game_db;
```
2. Exécuter le script `database.sql` (à la racine de ce dépôt) pour créer les tables et insérer le compte administrateur :
```bash
   psql -U postgres -d color_game_db -f database.sql
```
   (ou exécuter son contenu directement dans pgAdmin → Query Tool)

## 2. Configuration de l'application

1. Copier le fichier d'exemple :
```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
```
   (sous Windows : copier/coller manuellement le fichier et le renommer)

2. Éditer `src/main/resources/application.properties` et renseigner :
   - `spring.datasource.password` → votre mot de passe PostgreSQL
   - `app.jwt.secret` → une longue chaîne aléatoire (minimum 256 bits) servant à signer les tokens JWT

   ⚠️ Ce fichier contient des informations sensibles et est volontairement exclu du dépôt Git (`.gitignore`). Ne jamais le commiter avec de vraies valeurs.

## 3. Lancer le backend

```bash
./mvnw spring-boot:run
```
(sous Windows : `.\mvnw.cmd spring-boot:run`)

Le serveur démarre sur `http://localhost:8080`. Les tables sont créées automatiquement au premier lancement (`spring.jpa.hibernate.ddl-auto=update`), mais exécuter `database.sql` au préalable garantit que le compte ADMIN existe.

## Compte administrateur par défaut

| Champ | Valeur |
|---|---|
| Username | `admin` |
| Email | `admin@colorgame.com` |
| Mot de passe | celui utilisé pour générer le hash BCrypt inséré dans `database.sql` |

## Endpoints principaux de l'API

| Méthode | Endpoint | Accès | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Créer un compte (rôle USER par défaut) |
| POST | `/api/auth/login` | Public | Connexion (username ou email), retourne un JWT |
| POST | `/api/history` | Authentifié | Enregistrer un changement de couleur |
| GET | `/api/history/me` | Authentifié | Historique de l'utilisateur connecté |
| GET | `/api/admin/users` | ADMIN | Liste de tous les utilisateurs |
| GET | `/api/admin/history/all` | ADMIN | Historique global de tous les utilisateurs |

Toutes les routes protégées attendent un header :