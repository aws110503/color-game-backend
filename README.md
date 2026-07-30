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

## 4. Lancer avec Docker

### Avec Docker Compose (recommandé)

1. Copier le fichier d'environnement :
```bash
cp ../.env.example ../.env
```
2. Éditer `../.env` avec vos vrais mots de passe
3. Lancer :
```bash
docker-compose up -d
```

L'application sera accessible sur :
- Frontend : http://localhost:4200
- Backend API : http://localhost:8080

### Sans Docker Compose

```bash
docker build -t color-game-backend .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/color_game_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  -e APP_JWT_SECRET=your_32_char_secret_here \
  color-game-backend
```

## 5. Security Features

### Application Security (AppSec)
- **JWT Hardening** : Access tokens (15min) + Refresh tokens (7j), token blacklist, rejection algo 'none'
- **Rate Limiting** : 5 tentatives/minute sur /login, 3/minute sur /register, blocage IP après 10 échecs
- **Security Headers** : HSTS, X-Frame-Options: DENY, CSP, X-Content-Type-Options: nosniff, Referrer-Policy
- **Input Validation** : Jakarta Bean Validation sur tous les DTOs
- **Audit Logging** : Table security_audit_log avec tous les événements sensibles
- **Global Exception Handler** : Aucune stack trace exposée aux utilisateurs

### DevSecOps Pipeline
Le pipeline de sécurité complet est disponible dans `.github/workflows/security-pipeline.yml` :
1. **Gitleaks** - Détection de secrets
2. **SpotBugs + FindSecBugs** - Analyse statique SAST
3. **OWASP Dependency Check** - Scan de dépendances
4. **Maven Build & Test** - Compilation et tests
5. **Trivy** - Scan de vulnérabilités Docker
6. **OWASP ZAP** - Test dynamique DAST
7. **Deploy** - Déploiement si toutes les portes passent

### Monitoring
- Dashboard de sécurité admin : `/admin/security`
- Endpoints API :
  - `GET /api/admin/security/audit-log` - Logs paginés
  - `GET /api/admin/security/stats` - Statistiques 24h
  - `GET /api/admin/security/suspicious-ips` - IPs suspectes