#!/bin/bash
set -e

echo "=== Goldcar Keycloak - Starting ==="

# ---------------------------------------------------------------
# 1. Initialize the USUARIOS table in the legacy database
# ---------------------------------------------------------------
if [ -f /opt/keycloak/data/init-db.sql ] && [ -n "$KC_DB_URL" ]; then
    # Parse JDBC URL: jdbc:postgresql://host:port/dbname?params
    DB_CONN=$(echo "$KC_DB_URL" | sed 's|jdbc:postgresql://||')
    DB_HOST=$(echo "$DB_CONN" | cut -d: -f1)
    DB_PORT=$(echo "$DB_CONN" | cut -d: -f2 | cut -d/ -f1)
    DB_NAME=$(echo "$DB_CONN" | cut -d/ -f2 | cut -d? -f1)

    echo ">>> Waiting for PostgreSQL at $DB_HOST:$DB_PORT..."
    RETRY=0
    until PGPASSWORD="$KC_DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$KC_DB_USERNAME" -d "$DB_NAME" -c "SELECT 1" > /dev/null 2>&1; do
        RETRY=$((RETRY + 1))
        if [ $RETRY -ge 12 ]; then
            echo ">>> WARNING: PostgreSQL not reachable. Skipping DB init."
            break
        fi
        sleep 5
    done

    if [ $RETRY -lt 12 ]; then
        echo ">>> Running USUARIOS table init..."
        PGPASSWORD="$KC_DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$KC_DB_USERNAME" -d "$DB_NAME" \
            -f /opt/keycloak/data/init-db.sql
        echo ">>> USUARIOS table ready."
    fi
fi

# ---------------------------------------------------------------
# 2. Start Keycloak
# ---------------------------------------------------------------
echo ">>> Starting Keycloak on port ${PORT:-8080}..."

exec /opt/keycloak/bin/kc.sh start \
    --optimized \
    --import-realm \
    --hostname-strict=false \
    --proxy-headers=xforwarded \
    --http-enabled=true \
    --http-port="${PORT:-8080}"
