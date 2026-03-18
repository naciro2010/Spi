# Goldcar User Storage SPI Plugin

Keycloak User Storage SPI plugin that bridges the centralized CIAM infrastructure (Keycloak 26.x) with the legacy Goldcar database (SIGGER), enabling existing Goldcar users to authenticate via OIDC without migrating their data.

## Prerequisites

- Java 21 (LTS)
- Docker & Docker Compose (for local development)
- Gradle (or use the included wrapper)

## Architecture

| Component | Description |
|---|---|
| `GoldcarUserStorageProviderFactory` | Singleton factory; creates per-transaction providers |
| `GoldcarUserStorageProvider` | Implements user lookup, credential validation, registration, and search |
| `GoldcarUserAdapter` | Maps `USUARIOS` rows to Keycloak's `UserModel` |
| `GoldcarUserRepository` | JDBC data access with HikariCP connection pooling |
| `BcryptValidator` | bcrypt password verification using jBCrypt |
| `GoldcarCustomerIdMapper` | OIDC protocol mapper that adds `goldcar_customer_id` claim to JWT |

---

## Guide de deploiement local (step-by-step)

Ce guide explique comment lancer le plugin en local avec Docker (base de donnees PostgreSQL + Keycloak).

### Etape 1 : Cloner le repository

```bash
git clone <url-du-repo>
cd Spi
```

### Etape 2 : Compiler le plugin (fat JAR)

Compilez le plugin avec le Shadow JAR qui inclut toutes les dependances necessaires :

```bash
./gradlew shadowJar
```

Le fichier JAR est genere dans :

```
build/libs/goldcar-user-storage-spi-1.0.0.jar
```

> **Note** : Cette etape est **obligatoire** avant de lancer Docker, car le `docker-compose.yml` monte ce JAR dans le conteneur Keycloak.

### Etape 3 : Comprendre l'infrastructure Docker

Le fichier `docker/docker-compose.yml` demarre deux conteneurs :

| Service | Image | Port expose | Description |
|---|---|---|---|
| `goldcar-db` | `postgres:16-alpine` | `5433` (host) → `5432` (container) | Base de donnees PostgreSQL legacy Goldcar |
| `keycloak` | `quay.io/keycloak/keycloak:26.2.4` | `8080` | Serveur Keycloak avec le plugin SPI deploye |

**Schema reseau :**

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Network                        │
│                                                          │
│  ┌──────────────┐       ┌──────────────────────────┐    │
│  │  goldcar-db   │◄──────│       keycloak            │    │
│  │  PostgreSQL   │       │  Keycloak 26.2.4          │    │
│  │  Port: 5432   │       │  + goldcar-spi.jar        │    │
│  └──────┬───────┘       │  + realm-goldcar.json     │    │
│         │                └──────────┬───────────────┘    │
└─────────┼───────────────────────────┼────────────────────┘
          │                           │
    Host:5433                   Host:8080
```

### Etape 4 : Demarrer les conteneurs

```bash
cd docker
docker compose up -d
```

**Ce qui se passe automatiquement :**

1. **PostgreSQL** demarre et execute le script `docker/init-scripts/01-init.sql` qui :
   - Cree la table `USUARIOS` (schema legacy Goldcar)
   - Insere un utilisateur de test :
     - Email : `goldcarweb@gmail.com`
     - Mot de passe : `1234567` (hash bcrypt)
     - Nom : `Test User`
     - Telephone : `+34600000000`

2. **Keycloak** demarre en mode `start-dev` et :
   - Se connecte a PostgreSQL (via le reseau Docker interne)
   - Charge le plugin SPI depuis `build/libs/goldcar-user-storage-spi-1.0.0.jar`
   - Importe le realm `goldcar` depuis `docker/realm-goldcar.json`

### Etape 5 : Verifier que les conteneurs sont en marche

```bash
docker compose ps
```

Vous devez voir les deux services `running` / `healthy` :

```
NAME                 STATUS
goldcar-legacy-db    running (healthy)
goldcar-keycloak     running
```

Attendez environ 30 secondes que Keycloak finisse de demarrer. Vous pouvez surveiller les logs :

```bash
docker compose logs -f keycloak
```

Attendez de voir le message :

```
Keycloak 26.2.4 on JVM (powered by Quarkus) started
```

### Etape 6 : Acceder a la console d'administration Keycloak

Ouvrez votre navigateur et allez sur :

| URL | Description |
|---|---|
| http://localhost:8080/admin | Console d'administration Keycloak |
| http://localhost:8080/realms/goldcar | Endpoint du realm Goldcar |

**Identifiants admin :**
- Utilisateur : `admin`
- Mot de passe : `admin`

### Etape 7 : Verifier la configuration du plugin

1. Connectez-vous a http://localhost:8080/admin avec `admin / admin`
2. Selectionnez le realm **goldcar** dans le menu deroulant en haut a gauche
3. Allez dans **User Federation** dans le menu de gauche
4. Vous devez voir le provider **goldcar-user-storage** configure avec :
   - JDBC URL : `jdbc:postgresql://goldcar-db:5432/goldcar_legacy`
   - DB User : `goldcar`
   - DB Password : `goldcar`

### Etape 8 : Tester l'authentification

#### Via curl (Direct Grant / ROPC)

```bash
curl -s -X POST http://localhost:8080/realms/goldcar/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=gcapp-ios" \
  -d "username=goldcarweb@gmail.com" \
  -d "password=1234567" | jq .
```

**Reponse attendue :** un JSON contenant `access_token`, `refresh_token`, `token_type`, etc.

#### Decoder le JWT pour verifier le claim `goldcar_customer_id`

```bash
# Recuperer le token
TOKEN=$(curl -s -X POST http://localhost:8080/realms/goldcar/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=gcapp-ios" \
  -d "username=goldcarweb@gmail.com" \
  -d "password=1234567" | jq -r '.access_token')

# Decoder le payload du JWT (partie 2, base64)
echo "$TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null | jq .
```

Vous devriez voir le claim `goldcar_customer_id` dans le payload du token.

### Etape 9 : Acceder directement a la base de donnees (optionnel)

Pour inspecter les donnees de la table `USUARIOS` :

```bash
docker exec -it goldcar-legacy-db psql -U goldcar -d goldcar_legacy -c "SELECT * FROM USUARIOS;"
```

Pour ajouter un utilisateur de test supplementaire :

```bash
docker exec -it goldcar-legacy-db psql -U goldcar -d goldcar_legacy -c "
INSERT INTO USUARIOS (Email, Password, Nombre, Apellidos, Telefono)
VALUES ('test@example.com', '\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Nuevo', 'Usuario', '+34611111111');
"
```

> Le mot de passe hash ci-dessus correspond a `1234567`.

### Etape 10 : Arreter l'environnement

```bash
cd docker
docker compose down
```

Pour supprimer egalement les donnees persistantes (volumes) :

```bash
docker compose down -v
```

---

## Cycle de developpement

Lorsque vous modifiez le code du plugin et souhaitez tester vos changements :

```bash
# 1. Recompiler le plugin
./gradlew shadowJar

# 2. Redemarrer Keycloak pour charger le nouveau JAR
cd docker
docker compose restart keycloak

# 3. Verifier les logs
docker compose logs -f keycloak
```

## Lancer les tests unitaires et d'integration

```bash
./gradlew test
```

> **Note** : Les tests du repository necessitent Docker car Testcontainers demarre un conteneur PostgreSQL automatiquement.

## Deploiement en production

1. Copier le JAR dans le dossier `providers` de Keycloak :
   ```bash
   cp build/libs/goldcar-user-storage-spi-1.0.0.jar /opt/keycloak/providers/
   ```

2. Reconstruire Keycloak (mode optimise) :
   ```bash
   /opt/keycloak/bin/kc.sh build
   ```

3. Redemarrer Keycloak.

## Configuration manuelle du provider

Si le realm n'est pas importe automatiquement, configurez le plugin manuellement dans la console Keycloak :

1. Allez dans **User Federation** > **Add Provider** > **goldcar-user-storage**
2. Configurez :
   - **JDBC URL** : `jdbc:postgresql://<host>:5432/goldcar_legacy`
   - **Database User** : le nom d'utilisateur de la base
   - **Database Password** : le mot de passe de la base
3. Sauvegardez

## Securite

- Parameterized SQL queries (no string concatenation)
- DB credentials stored in Keycloak's encrypted component config
- bcrypt constant-time comparison (jBCrypt)
- `goldcar_customer_id` and `phone` attributes are read-only
- No passwords or hashes are logged
