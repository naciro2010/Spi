# Goldcar User Storage SPI Plugin

Keycloak plugin to let Goldcar users log in with their existing database accounts (SIGGER). No data migration needed.

## What you need

- Java 21
- Docker and Docker Compose
- Gradle

## What is inside

| File | What it does |
|---|---|
| `GoldcarUserStorageProviderFactory` | Creates the provider |
| `GoldcarUserStorageProvider` | Finds users, checks passwords, creates accounts |
| `GoldcarUserAdapter` | Turns a database row into a Keycloak user |
| `GoldcarUserRepository` | Talks to the database |
| `BcryptValidator` | Checks bcrypt passwords |
| `GoldcarCustomerIdMapper` | Adds `goldcar_customer_id` to the JWT token |

---

## How to run locally (step by step)

### Step 1 - Clone the project

```bash
git clone <your-repo-url>
cd Spi
```

### Step 2 - Build the plugin

```bash
./gradlew shadowJar
```

This creates the file:

```
build/libs/goldcar-user-storage-spi-1.0.0.jar
```

**You must do this before starting Docker.** Docker needs this file.

### Step 3 - What Docker will start

Docker starts 2 containers:

| Name | Image | Port | What it is |
|---|---|---|---|
| `goldcar-db` | `postgres:16-alpine` | `5433` | The Goldcar database |
| `keycloak` | `quay.io/keycloak/keycloak:26.2.4` | `8080` | Keycloak server with the plugin |

How they connect:

```
┌───────────────────────────────────────────────┐
│              Docker Network                    │
│                                                │
│  ┌────────────┐      ┌────────────────────┐   │
│  │ goldcar-db  │◄─────│    keycloak         │   │
│  │ PostgreSQL  │      │ + plugin JAR        │   │
│  │ port 5432   │      │ + realm config      │   │
│  └──────┬─────┘      └─────────┬──────────┘   │
└─────────┼──────────────────────┼───────────────┘
          │                      │
     localhost:5433         localhost:8080
```

### Step 4 - Start Docker

```bash
cd docker
docker compose up -d
```

This will:

1. Start **PostgreSQL** and run `docker/init-scripts/01-init.sql`:
   - Creates the `USUARIOS` table
   - Adds a test user:
     - Email: `goldcarweb@gmail.com`
     - Password: `1234567`
     - Name: `Test User`
     - Phone: `+34600000000`

2. Start **Keycloak**:
   - Connects to PostgreSQL
   - Loads the plugin JAR
   - Imports the `goldcar` realm

### Step 5 - Check that everything is running

```bash
docker compose ps
```

You should see:

```
NAME                 STATUS
goldcar-legacy-db    running (healthy)
goldcar-keycloak     running
```

Wait about 30 seconds for Keycloak to start. Watch the logs:

```bash
docker compose logs -f keycloak
```

Wait until you see:

```
Keycloak 26.2.4 on JVM (powered by Quarkus) started
```

### Step 6 - Open Keycloak admin

Open your browser:

| URL | What it is |
|---|---|
| http://localhost:8080/admin | Admin console |
| http://localhost:8080/realms/goldcar | Goldcar realm endpoint |

Login:
- Username: `admin`
- Password: `admin`

### Step 7 - Check the plugin is loaded

1. Go to http://localhost:8080/admin
2. Log in with `admin / admin`
3. Select the **goldcar** realm (top left dropdown)
4. Click **User Federation** in the left menu
5. You should see **goldcar-user-storage** with:
   - JDBC URL: `jdbc:postgresql://goldcar-db:5432/goldcar_legacy`
   - DB User: `goldcar`
   - DB Password: `goldcar`

### Step 8 - Test login

Run this command to get a token:

```bash
curl -s -X POST http://localhost:8080/realms/goldcar/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=gcapp-ios" \
  -d "username=goldcarweb@gmail.com" \
  -d "password=1234567" | jq .
```

You should get a JSON with `access_token`, `refresh_token`, etc.

To see what is inside the token (check `goldcar_customer_id`):

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/goldcar/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=gcapp-ios" \
  -d "username=goldcarweb@gmail.com" \
  -d "password=1234567" | jq -r '.access_token')

echo "$TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null | jq .
```

### Step 9 - Look at the database (optional)

See all users:

```bash
docker exec -it goldcar-legacy-db psql -U goldcar -d goldcar_legacy -c "SELECT * FROM USUARIOS;"
```

Add a new test user:

```bash
docker exec -it goldcar-legacy-db psql -U goldcar -d goldcar_legacy -c "
INSERT INTO USUARIOS (Email, Password, Nombre, Apellidos, Telefono)
VALUES ('test@example.com', '\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'New', 'User', '+34611111111');
"
```

The password hash above is `1234567`.

### Step 10 - Stop everything

```bash
cd docker
docker compose down
```

To also delete the database data:

```bash
docker compose down -v
```

---

## Development workflow

When you change the plugin code:

```bash
# 1. Build again
./gradlew shadowJar

# 2. Restart Keycloak to load the new JAR
cd docker
docker compose restart keycloak

# 3. Check the logs
docker compose logs -f keycloak
```

## Run tests

```bash
./gradlew test
```

Tests need Docker running (Testcontainers starts a PostgreSQL container automatically).

## Deploy to production

1. Copy the JAR:
   ```bash
   cp build/libs/goldcar-user-storage-spi-1.0.0.jar /opt/keycloak/providers/
   ```

2. Build Keycloak:
   ```bash
   /opt/keycloak/bin/kc.sh build
   ```

3. Restart Keycloak.

## Manual plugin setup

If the realm is not imported automatically:

1. Go to **User Federation** > **Add Provider** > **goldcar-user-storage**
2. Fill in:
   - **JDBC URL**: `jdbc:postgresql://<host>:5432/goldcar_legacy`
   - **Database User**: your db username
   - **Database Password**: your db password
3. Save

## Security

- SQL queries use parameters (no injection)
- DB credentials are stored encrypted in Keycloak
- bcrypt with constant-time comparison
- `goldcar_customer_id` and `phone` are read-only
- No passwords are logged
