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

Docker starts 3 containers:

| Name | Image | Port | What it is |
|---|---|---|---|
| `goldcar-db` | `postgres:16-alpine` | `5433` | The Goldcar database |
| `keycloak` | `quay.io/keycloak/keycloak:26.2.4` | `8080` | Keycloak server with plugin + Goldcar theme |
| `mailhog` | `mailhog/mailhog` | `8025` | Fake SMTP server to see emails in browser |

How they connect:

```
┌──────────────────────────────────────────────────────────────┐
│                     Docker Network                            │
│                                                               │
│  ┌────────────┐  ┌────────────────────┐  ┌──────────────┐   │
│  │ goldcar-db  │◄─│    keycloak         │──►│   mailhog     │   │
│  │ PostgreSQL  │  │ + plugin JAR        │  │ SMTP :1025   │   │
│  │ port 5432   │  │ + goldcar theme     │  │ Web  :8025   │   │
│  └──────┬─────┘  └─────────┬──────────┘  └──────┬───────┘   │
└─────────┼──────────────────┼─────────────────────┼────────────┘
          │                  │                     │
     localhost:5433     localhost:8080         localhost:8025
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

2. Start **MailHog** (fake email server):
   - SMTP on port `1025` (Keycloak sends emails here)
   - Web UI on port `8025` (you see the emails here)

3. Start **Keycloak**:
   - Connects to PostgreSQL
   - Loads the plugin JAR
   - Loads the Goldcar theme (login + email)
   - Imports the `goldcar` realm with SMTP config pointing to MailHog

### Step 5 - Check that everything is running

```bash
docker compose ps
```

You should see:

```
NAME                 STATUS
goldcar-legacy-db    running (healthy)
goldcar-keycloak     running
goldcar-mailhog      running
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
| http://localhost:8080/realms/goldcar/account | User login page (Goldcar theme) |
| http://localhost:8025 | MailHog - see all emails sent by Keycloak |

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

### Step 10 - Test reset password

1. Open http://localhost:8080/realms/goldcar/account in your browser
2. Click **Sign In**
3. You see the Goldcar themed login page (dark background, gold button)
4. Click **Forgot Password?**
5. Enter `goldcarweb@gmail.com` and submit
6. Open http://localhost:8025 (MailHog) in another tab
7. You see the reset password email with Goldcar branding
8. Click the link in the email
9. Set a new password
10. Log in with the new password

### Step 11 - Stop everything

```bash
cd docker
docker compose down
```

To also delete the database data:

```bash
docker compose down -v
```

---

## Goldcar theme customization

The theme files are in `docker/themes/goldcar/`. Changes are live-reloaded (no rebuild needed, just restart Keycloak).

### File structure

```
docker/themes/goldcar/
├── login/                          # Login page theme
│   ├── theme.properties            # Inherits from default keycloak theme
│   └── resources/
│       ├── css/goldcar.css         # Custom CSS (colors, buttons, inputs)
│       └── img/goldcar-logo.svg    # Logo (replace with real Goldcar logo)
│
└── email/                          # Email theme
    ├── theme.properties            # Inherits from default keycloak theme
    ├── html/
    │   ├── password-reset.ftl      # HTML reset password email
    │   └── email-verification.ftl  # HTML email verification
    ├── text/
    │   ├── password-reset.ftl      # Plain text reset password email
    │   └── email-verification.ftl  # Plain text email verification
    └── messages/
        ├── messages_en.properties  # English text
        ├── messages_es.properties  # Spanish text
        └── messages_fr.properties  # French text
```

### How to change the colors

Edit `docker/themes/goldcar/login/resources/css/goldcar.css`:

| What | CSS variable | Default |
|---|---|---|
| Background | `body` background-color | `#1A1A1A` (dark) |
| Buttons | `.btn-primary` background-color | `#F7A800` (gold) |
| Button text | `.btn-primary` color | `#1A1A1A` (dark) |
| Links | `a` color | `#F7A800` (gold) |
| Input focus | `input:focus` border-color | `#F7A800` (gold) |

### How to change the logo

Replace `docker/themes/goldcar/login/resources/img/goldcar-logo.svg` with the real Goldcar logo file (SVG or PNG).

If you use a PNG, update `goldcar.css`:

```css
#kc-header-wrapper::before {
    background-image: url("../img/your-logo.png");
}
```

### How to change the emails

Edit the `.ftl` files in `docker/themes/goldcar/email/html/`. These are FreeMarker templates with inline CSS (for email client compatibility).

Available variables in email templates:
- `${user.firstName}` - user first name
- `${link}` - the reset/verify link
- `${linkExpiration}` - how long the link is valid
- `${msg("key")}` - translated text from `messages/` folder

### After changing theme files

```bash
cd docker
docker compose restart keycloak
```

No need to rebuild the JAR. Just restart Keycloak.

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
