# Goldcar User Storage SPI Plugin

Keycloak plugin to let Goldcar users log in with their existing database accounts (SIGGER).
No data migration needed. Users can also create new accounts and reset passwords.

## What you need

- **Java 21** - to build the plugin
- **Docker and Docker Compose** - to run everything locally
- **Gradle** - to compile (or use the included wrapper `./gradlew`)
- **A Google Cloud project** (optional) - only if you want Google SSO

## What is inside

| File | What it does |
|---|---|
| `GoldcarUserStorageProviderFactory` | Creates the provider, manages DB connection pool |
| `GoldcarUserStorageProvider` | Finds users, checks passwords, creates accounts, resets passwords |
| `GoldcarUserAdapter` | Turns a `USUARIOS` database row into a Keycloak user |
| `GoldcarUserRepository` | All SQL queries to the legacy database |
| `BcryptValidator` | Hashes and checks bcrypt passwords |
| `GoldcarCustomerIdMapper` | Adds `goldcar_customer_id` to the JWT token |

## What the login page looks like

The custom Goldcar login page has:
- **Goldcar logo** at the top (dark header, gold text)
- **"Continue with Google"** button (Google SSO)
- **Email + password** form
- **"Forgot Password?"** link (sends reset email)
- **"Create Account"** button (registration form)

---

## How to set up from zero (step by step)

### Step 1 - Install Docker

If you don't have Docker yet:

**Mac:**
```bash
brew install --cask docker
# Open Docker Desktop from Applications
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install docker.io docker-compose-v2
sudo usermod -aG docker $USER
# Log out and log back in
```

**Windows:**
Download Docker Desktop from https://www.docker.com/products/docker-desktop/

Check it works:
```bash
docker --version
docker compose version
```

### Step 2 - Install Java 21

**Mac:**
```bash
brew install openjdk@21
```

**Ubuntu/Debian:**
```bash
sudo apt install openjdk-21-jdk
```

Check it works:
```bash
java -version
# Should say: openjdk version "21.x.x"
```

### Step 3 - Clone the project

```bash
git clone <your-repo-url>
cd Spi
```

### Step 4 - Build the plugin

This compiles the Kotlin code into a JAR file that Keycloak will load:

```bash
./gradlew shadowJar
```

What happens:
1. Gradle downloads all dependencies (Keycloak SPI, bcrypt, HikariCP)
2. Compiles the Kotlin code to Java bytecode
3. Creates a "fat JAR" with all dependencies bundled inside
4. Output file: `build/libs/goldcar-user-storage-spi-1.0.0.jar`

**You must do this before starting Docker.** The docker-compose mounts this JAR file into Keycloak.

If you see errors, make sure you have Java 21:
```bash
java -version
```

### Step 5 - (Optional) Set up Google SSO

If you want the "Continue with Google" button to work, you need Google OAuth2 credentials.

**Skip this step** if you just want to test with email/password. The button will show but clicking it will give an error until you configure real credentials.

#### 5a - Create a Google Cloud project

1. Go to https://console.cloud.google.com/
2. Click **Select a project** (top bar) > **New Project**
3. Name it `goldcar-keycloak` > click **Create**

#### 5b - Enable the Google+ API

1. Go to **APIs & Services** > **Library**
2. Search for **"Google+ API"** or **"Google Identity"**
3. Click **Enable**

#### 5c - Create OAuth2 credentials

1. Go to **APIs & Services** > **Credentials**
2. Click **Create Credentials** > **OAuth client ID**
3. Application type: **Web application**
4. Name: `Goldcar Keycloak`
5. Add **Authorized redirect URIs**:
   ```
   http://localhost:8080/realms/goldcar/broker/google/endpoint
   ```
6. Click **Create**
7. Copy the **Client ID** and **Client Secret**

#### 5d - Create the .env file

```bash
cd docker
cp .env.example .env
```

Edit `docker/.env` with your values:
```
GOOGLE_CLIENT_ID=123456789-xxxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxxxxxxxx
```

### Step 6 - What Docker will start

Docker starts 3 containers:

| Name | Image | Port | What it is |
|---|---|---|---|
| `goldcar-db` | `postgres:16-alpine` | `5433` | PostgreSQL - the Goldcar legacy database |
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

- **PostgreSQL** stores the `USUARIOS` table (legacy Goldcar users)
- **Keycloak** handles login, registration, reset password, Google SSO
- **MailHog** catches all emails so you can see them in a browser (no real emails are sent)

### Step 7 - Start Docker

```bash
cd docker
docker compose up -d
```

This will:

1. **Start PostgreSQL** and run `docker/init-scripts/01-init.sql`:
   - Creates the `USUARIOS` table with columns: `CodigoUsuario`, `Email`, `Password`, `Nombre`, `Apellidos`, `Telefono`
   - Inserts a test user:
     - Email: `goldcarweb@gmail.com`
     - Password: `1234567` (stored as bcrypt hash)
     - Name: `Test User`
     - Phone: `+34600000000`

2. **Start MailHog** (fake email server):
   - SMTP on port `1025` (Keycloak sends emails here)
   - Web UI on port `8025` (you open this in browser to see the emails)

3. **Start Keycloak** in dev mode:
   - Connects to PostgreSQL via Docker network
   - Loads the plugin JAR from `build/libs/`
   - Loads the Goldcar theme from `docker/themes/goldcar/`
   - Imports the `goldcar` realm with all configuration
   - Theme caching is disabled so you can edit CSS/templates and just restart

### Step 8 - Check that everything is running

```bash
docker compose ps
```

You should see all 3 containers running:

```
NAME                 STATUS
goldcar-legacy-db    running (healthy)
goldcar-keycloak     running
goldcar-mailhog      running
```

Keycloak takes about 30-60 seconds to start. Watch the logs:

```bash
docker compose logs -f keycloak
```

Wait until you see this line:

```
Keycloak 26.2.4 on JVM (powered by Quarkus) started
```

Press `Ctrl+C` to stop watching logs.

### Step 9 - All the URLs

| URL | What it is | Login |
|---|---|---|
| http://localhost:8080/realms/goldcar/account | **Login page** - Goldcar themed, with Google SSO | user email + password |
| http://localhost:8080/admin | **Admin console** - manage users, settings | `admin` / `admin` |
| http://localhost:8025 | **MailHog** - see all emails (reset password, verify) | no login needed |
| http://localhost:5433 | **PostgreSQL** - direct DB access (via psql) | `goldcar` / `goldcar` |

### Step 10 - Test the login page

1. Open http://localhost:8080/realms/goldcar/account
2. Click **Sign In**
3. You see the Goldcar login page with:
   - **"Continue with Google"** button at the top
   - **"or"** separator
   - **Email** and **Password** fields
   - **"Forgot Password?"** link on the right
   - **"Sign In"** gold button
   - **"Create Account"** dark button at the bottom
4. Log in with:
   - Email: `goldcarweb@gmail.com`
   - Password: `1234567`
5. You are now logged in to the Goldcar account page

### Step 11 - Test creating a new account

1. Open http://localhost:8080/realms/goldcar/account
2. Click **Sign In**
3. Click **"Create Account"** (dark button at the bottom)
4. Fill in the registration form:
   - First Name: `Juan`
   - Last Name: `Garcia`
   - Email: `juan@example.com`
   - Password: `MyPassword123`
   - Confirm Password: `MyPassword123`
5. Click **Register**
6. The new user is created in the `USUARIOS` table in PostgreSQL

Verify the user was created in the database:
```bash
docker exec -it goldcar-legacy-db psql -U goldcar -d goldcar_legacy -c "SELECT CodigoUsuario, Email, Nombre, Apellidos FROM USUARIOS;"
```

### Step 12 - Test reset password

1. Open http://localhost:8080/realms/goldcar/account
2. Click **Sign In**
3. Click **"Forgot Password?"** link
4. Enter the email: `goldcarweb@gmail.com`
5. Click **Submit**
6. Open http://localhost:8025 in another browser tab (MailHog)
7. You see a Goldcar branded email with:
   - Dark header with gold "GOLDCAR" text
   - "Reset Password" gold button
   - Expiration notice
   - Goldcar footer
8. Click the **"Reset Password"** button in the email
9. Enter a new password (e.g., `NewPassword123`)
10. Confirm the new password
11. Click **Submit**
12. Go back to the login page and log in with the new password

The new password is stored as a bcrypt hash in the `USUARIOS.Password` column.

### Step 13 - Test Google SSO (if configured)

1. Make sure you completed Step 5 (Google Cloud setup) and created the `.env` file
2. Restart Docker to pick up the new env vars:
   ```bash
   docker compose down && docker compose up -d
   ```
3. Open http://localhost:8080/realms/goldcar/account
4. Click **Sign In**
5. Click **"Continue with Google"**
6. Google login page opens
7. Log in with your Google account
8. First time: Keycloak shows a "first broker login" page to link the account
9. After linking, you are logged in

### Step 14 - Test with curl (API)

Get an access token:
```bash
curl -s -X POST http://localhost:8080/realms/goldcar/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=gcapp-ios" \
  -d "username=goldcarweb@gmail.com" \
  -d "password=1234567" | jq .
```

You get a JSON with `access_token`, `refresh_token`, `token_type`, etc.

Decode the JWT to see the `goldcar_customer_id` claim:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/goldcar/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=gcapp-ios" \
  -d "username=goldcarweb@gmail.com" \
  -d "password=1234567" | jq -r '.access_token')

echo "$TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null | jq .
```

### Step 15 - Look at the database

See all users:
```bash
docker exec -it goldcar-legacy-db psql -U goldcar -d goldcar_legacy -c "SELECT * FROM USUARIOS;"
```

Add a test user manually:
```bash
docker exec -it goldcar-legacy-db psql -U goldcar -d goldcar_legacy -c "
INSERT INTO USUARIOS (Email, Password, Nombre, Apellidos, Telefono)
VALUES ('manual@example.com', '\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Manual', 'User', '+34622222222');
"
```

The password hash above is `1234567`.

### Step 16 - Stop everything

```bash
cd docker
docker compose down
```

To also delete all database data (start fresh):
```bash
docker compose down -v
```

---

## Goldcar theme customization

Theme files are in `docker/themes/goldcar/`. Theme caching is disabled in dev mode, so you just restart Keycloak to see changes.

### File structure

```
docker/themes/goldcar/
├── login/                              # Login + register page theme
│   ├── theme.properties                # Inherits from keycloak default
│   ├── login.ftl                       # Custom login page template
│   ├── register.ftl                    # Custom registration page template
│   └── resources/
│       ├── css/goldcar.css             # All CSS styles
│       └── img/goldcar-logo.svg        # Logo (replace with real logo)
│
└── email/                              # Email theme
    ├── theme.properties                # Inherits from keycloak default
    ├── html/
    │   ├── password-reset.ftl          # HTML reset password email
    │   └── email-verification.ftl      # HTML email verification
    ├── text/
    │   ├── password-reset.ftl          # Plain text fallback
    │   └── email-verification.ftl      # Plain text fallback
    └── messages/
        ├── messages_en.properties      # English
        ├── messages_es.properties      # Spanish
        └── messages_fr.properties      # French
```

### How to change the colors

Edit `docker/themes/goldcar/login/resources/css/goldcar.css`. The main colors:

| What | Class | Default value |
|---|---|---|
| Page background | `body.login-pf` | `#1A1A1A` (dark) |
| Primary button (Sign In) | `.goldcar-btn-primary` | `#F7A800` (gold) |
| Secondary button (Create Account) | `.goldcar-btn-secondary` | `#1A1A1A` (dark) |
| Google button | `.goldcar-btn-google` | `#FFFFFF` (white border) |
| Input focus border | `.goldcar-input:focus` | `#F7A800` (gold) |
| Links (Forgot Password) | `.goldcar-link-forgot` | `#F7A800` (gold) |
| Error messages | `.alert-error` | `#E74C3C` (red) |

### How to change the logo

Replace `docker/themes/goldcar/login/resources/img/goldcar-logo.svg` with your real logo.

For a PNG logo, update `goldcar.css`:
```css
#kc-header-wrapper::before {
    background-image: url("../img/your-logo.png");
    width: 200px;   /* adjust size */
    height: 70px;   /* adjust size */
}
```

### How to edit the login page layout

Edit `docker/themes/goldcar/login/login.ftl`. This is a FreeMarker template.

Key sections in the template:
- **Google SSO button**: the `<#if social.providers??>` block
- **Email/password form**: the `<form id="kc-form-login">` block
- **Forgot password link**: the `<#if realm.resetPasswordAllowed>` block
- **Create account button**: the `<#if realm.registrationAllowed>` block

### How to edit the registration page

Edit `docker/themes/goldcar/login/register.ftl`.

Fields: firstName, lastName, email, password, password-confirm.

### How to edit the emails

Edit `.ftl` files in `docker/themes/goldcar/email/html/`.

Available variables:
- `${user.firstName}` - user first name
- `${link}` - the reset/verify URL
- `${linkExpiration}` - link expiry time
- `${msg("key")}` - translated text from `messages/` folder

### After changing theme files

```bash
cd docker
docker compose restart keycloak
```

No need to rebuild the JAR. Just restart Keycloak. Changes are visible in ~30 seconds.

---

## How it all works together

```
User clicks "Sign In"
        │
        ▼
┌──────────────────────────┐
│   Goldcar Login Page      │
│   (login.ftl + CSS)       │
│                           │
│  [Continue with Google]   │ ──► Google OAuth2 ──► Keycloak creates/links user
│  ─────── or ──────────    │
│  Email: [___________]     │
│  Password: [________]     │
│  [Forgot Password?]       │ ──► Reset email via MailHog ──► Update USUARIOS.Password
│  [    SIGN IN       ]     │ ──► SPI checks bcrypt hash in USUARIOS table
│  [  CREATE ACCOUNT  ]     │ ──► SPI inserts new row in USUARIOS table
└──────────────────────────┘
        │
        ▼
   JWT token with goldcar_customer_id claim
```

---

## Development workflow

When you change the **plugin code** (Kotlin):

```bash
# 1. Build again
./gradlew shadowJar

# 2. Restart Keycloak to load the new JAR
cd docker
docker compose restart keycloak

# 3. Check the logs
docker compose logs -f keycloak
```

When you change **theme files** only (CSS, FTL templates):

```bash
# Just restart Keycloak (no rebuild needed)
cd docker
docker compose restart keycloak
```

## Run tests

```bash
./gradlew test
```

Tests need Docker running (Testcontainers starts a PostgreSQL container automatically).

## Deploy to Railway (step by step)

Railway runs everything in the cloud. You get a URL like `https://goldcar-keycloak.up.railway.app`.

### What Railway will run

```
┌──────────────────────────────────────────────────┐
│                  Railway Project                  │
│                                                   │
│  ┌──────────────┐      ┌──────────────────────┐  │
│  │  PostgreSQL   │◄─────│  Keycloak Service     │  │
│  │  (Railway     │      │  (Dockerfile)         │  │
│  │   plugin)     │      │  + SPI plugin JAR     │  │
│  │               │      │  + Goldcar theme      │  │
│  └──────────────┘      │  + realm config        │  │
│                         └──────────────────────┘  │
└──────────────────────────────────────────────────┘
```

The `Dockerfile` does a multi-stage build:
1. **Stage 1** (builder): uses Java 21 to compile the Kotlin plugin into a fat JAR
2. **Stage 2** (keycloak): copies the JAR + theme + realm into the Keycloak image, runs `kc.sh build` for optimized mode

The `railway-entrypoint.sh` script:
1. Waits for PostgreSQL to be ready
2. Creates the `USUARIOS` table and seed user (idempotent)
3. Starts Keycloak in production mode with realm import

### Step 1 - Create a Railway account

1. Go to https://railway.app
2. Sign up (GitHub login recommended)
3. You get $5 free credit (enough for testing)

### Step 2 - Install the Railway CLI

```bash
# Mac
brew install railway

# Linux
curl -fsSL https://railway.app/install.sh | sh

# Or use npm
npm install -g @railway/cli
```

Login:
```bash
railway login
```

### Step 3 - Create a new project

```bash
cd Spi
railway init
```

Choose **"Empty Project"** when asked.

### Step 4 - Add a PostgreSQL database

```bash
railway add --plugin postgresql
```

This creates a PostgreSQL instance. Railway automatically sets these env vars:
- `DATABASE_URL`
- `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`

### Step 5 - Set environment variables

```bash
# Keycloak admin credentials
railway variables set KC_BOOTSTRAP_ADMIN_USERNAME=admin
railway variables set KC_BOOTSTRAP_ADMIN_PASSWORD=<choose-a-strong-password>

# Database connection (use Railway's PostgreSQL)
railway variables set KC_DB=postgres
railway variables set KC_DB_URL=\${{Postgres.JDBC_DATABASE_URL}}
railway variables set KC_DB_USERNAME=\${{Postgres.PGUSER}}
railway variables set KC_DB_PASSWORD=\${{Postgres.PGPASSWORD}}

# Google SSO (optional - skip if you don't need it)
railway variables set GOOGLE_CLIENT_ID=your-google-client-id
railway variables set GOOGLE_CLIENT_SECRET=your-google-client-secret
```

**For SMTP (to send real emails)**, use a service like SendGrid, Mailgun, or Gmail:

```bash
# Example with SendGrid
railway variables set SMTP_HOST=smtp.sendgrid.net
railway variables set SMTP_PORT=587
railway variables set SMTP_FROM=noreply@goldcar.com
railway variables set SMTP_SSL=false
railway variables set SMTP_STARTTLS=true
railway variables set SMTP_AUTH=true
railway variables set SMTP_USER=apikey
railway variables set SMTP_PASSWORD=SG.your-sendgrid-api-key

# Example with Gmail
railway variables set SMTP_HOST=smtp.gmail.com
railway variables set SMTP_PORT=587
railway variables set SMTP_FROM=your-email@gmail.com
railway variables set SMTP_SSL=false
railway variables set SMTP_STARTTLS=true
railway variables set SMTP_AUTH=true
railway variables set SMTP_USER=your-email@gmail.com
railway variables set SMTP_PASSWORD=your-gmail-app-password
```

### Step 6 - Deploy

```bash
railway up
```

This will:
1. Push your code to Railway
2. Build the Docker image (multi-stage: compile JAR + Keycloak)
3. Start the container
4. Run the entrypoint (init DB + start Keycloak)

The build takes about 3-5 minutes the first time.

### Step 7 - Get your public URL

```bash
railway domain
```

Railway gives you a URL like: `https://goldcar-keycloak-production.up.railway.app`

### Step 8 - Update Google OAuth redirect URI

If you use Google SSO, go to Google Cloud Console and add the redirect URI:

```
https://your-railway-url.up.railway.app/realms/goldcar/broker/google/endpoint
```

### Step 9 - Test it

Open your Railway URL:

| URL | What it is |
|---|---|
| `https://your-url.up.railway.app/admin` | Admin console |
| `https://your-url.up.railway.app/realms/goldcar/account` | Login page (Goldcar theme) |

Login with the admin credentials you set in Step 5.

Test with curl:
```bash
KEYCLOAK_URL=https://your-url.up.railway.app

curl -s -X POST $KEYCLOAK_URL/realms/goldcar/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=gcapp-ios" \
  -d "username=goldcarweb@gmail.com" \
  -d "password=1234567" | jq .
```

### Step 10 - Check logs if something goes wrong

```bash
railway logs
```

### All Railway environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `KC_DB` | yes | - | Set to `postgres` |
| `KC_DB_URL` | yes | - | JDBC URL (from Railway Postgres plugin) |
| `KC_DB_USERNAME` | yes | - | DB user (from Railway Postgres plugin) |
| `KC_DB_PASSWORD` | yes | - | DB password (from Railway Postgres plugin) |
| `KC_BOOTSTRAP_ADMIN_USERNAME` | yes | - | Keycloak admin username |
| `KC_BOOTSTRAP_ADMIN_PASSWORD` | yes | - | Keycloak admin password |
| `GOOGLE_CLIENT_ID` | no | `REPLACE_WITH_GOOGLE_CLIENT_ID` | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | no | `REPLACE_WITH_GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret |
| `SMTP_HOST` | no | `mailhog` | SMTP server host |
| `SMTP_PORT` | no | `1025` | SMTP server port |
| `SMTP_FROM` | no | `noreply@goldcar.com` | From email address |
| `SMTP_SSL` | no | `false` | Use SSL |
| `SMTP_STARTTLS` | no | `false` | Use STARTTLS |
| `SMTP_AUTH` | no | `false` | SMTP authentication |
| `SMTP_USER` | no | - | SMTP username |
| `SMTP_PASSWORD` | no | - | SMTP password |
| `PORT` | auto | `8080` | Set automatically by Railway |

---

## Deploy to a server (manual)

1. Copy the JAR:
   ```bash
   cp build/libs/goldcar-user-storage-spi-1.0.0.jar /opt/keycloak/providers/
   ```

2. Copy the theme:
   ```bash
   cp -r docker/themes/goldcar /opt/keycloak/themes/
   ```

3. Build Keycloak (optimized mode):
   ```bash
   /opt/keycloak/bin/kc.sh build
   ```

4. Restart Keycloak.

## Manual plugin setup

If the realm is not imported automatically:

1. Go to **Realm Settings** > set Login Theme to `goldcar`, Email Theme to `goldcar`
2. Go to **Realm Settings** > **Login** tab > enable **User registration**
3. Go to **User Federation** > **Add Provider** > **goldcar-user-storage**
4. Fill in:
   - **JDBC URL**: `jdbc:postgresql://<host>:5432/goldcar_legacy`
   - **Database User**: your db username
   - **Database Password**: your db password
5. Go to **Identity Providers** > **Add provider** > **Google**
6. Fill in your Google Client ID and Client Secret
7. Set redirect URI in Google Console to: `https://your-keycloak/realms/goldcar/broker/google/endpoint`

## Security

- SQL queries use parameters (no injection)
- DB credentials are stored encrypted in Keycloak
- bcrypt with constant-time comparison (cost factor 10)
- `goldcar_customer_id` and `phone` are read-only
- No passwords are logged
- Google SSO credentials loaded from environment variables
