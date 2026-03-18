# Goldcar User Storage SPI Plugin

Keycloak User Storage SPI plugin that bridges the centralized CIAM infrastructure (Keycloak 26.x) with the legacy Goldcar database (SIGGER), enabling existing Goldcar users to authenticate via OIDC without migrating their data.

## Prerequisites

- Java 21 (LTS)
- Docker & Docker Compose (for local development)

## Build

```bash
./gradlew shadowJar
```

The fat JAR is produced at `build/libs/goldcar-user-storage-spi-1.0.0.jar`.

## Run Tests

```bash
./gradlew test
```

> **Note**: Repository tests require Docker (Testcontainers spins up a PostgreSQL container).

## Local Development

### 1. Build the plugin

```bash
./gradlew shadowJar
```

### 2. Start Keycloak + PostgreSQL

```bash
cd docker
docker compose up -d
```

This starts:
- **PostgreSQL** on port `5433` with `goldcar_legacy` database and seeded `USUARIOS` table
- **Keycloak 26.x** on port `8080` with the plugin auto-deployed and `goldcar` realm imported

### 3. Access Keycloak

- Admin Console: http://localhost:8080/admin (credentials: `admin / admin`)
- Goldcar realm: http://localhost:8080/realms/goldcar

### 4. Test Authentication

```bash
# Direct grant (Resource Owner Password Credentials)
curl -X POST http://localhost:8080/realms/goldcar/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=gcapp-ios" \
  -d "username=goldcarweb@gmail.com" \
  -d "password=1234567"
```

The returned JWT `access_token` should contain the `goldcar_customer_id` claim.

## Deployment to Keycloak

1. Copy the built JAR to Keycloak's providers directory:
   ```bash
   cp build/libs/goldcar-user-storage-spi-1.0.0.jar /opt/keycloak/providers/
   ```

2. Rebuild Keycloak (for production/optimized mode):
   ```bash
   /opt/keycloak/bin/kc.sh build
   ```

3. Restart Keycloak.

## Configuration

In Keycloak Admin Console:

1. Go to **User Federation** > **Add Provider** > **goldcar-user-storage**
2. Configure:
   - **JDBC URL**: `jdbc:postgresql://<host>:5432/goldcar_legacy`
   - **Database User**: the DB username
   - **Database Password**: the DB password
3. Save

## Architecture

| Component | Description |
|---|---|
| `GoldcarUserStorageProviderFactory` | Singleton factory; creates per-transaction providers |
| `GoldcarUserStorageProvider` | Implements user lookup, credential validation, registration, and search |
| `GoldcarUserAdapter` | Maps `USUARIOS` rows to Keycloak's `UserModel` |
| `GoldcarUserRepository` | JDBC data access with HikariCP connection pooling |
| `BcryptValidator` | bcrypt password verification using jBCrypt |
| `GoldcarCustomerIdMapper` | OIDC protocol mapper that adds `goldcar_customer_id` claim to JWT |

## Security

- Parameterized SQL queries (no string concatenation)
- DB credentials stored in Keycloak's encrypted component config
- bcrypt constant-time comparison (jBCrypt)
- `goldcar_customer_id` and `phone` attributes are read-only
- No passwords or hashes are logged
