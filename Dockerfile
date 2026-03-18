# =============================================================
# Stage 1: Build the SPI plugin JAR
# =============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and config first (cache dependencies layer)
COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/
COPY build.gradle.kts settings.gradle.kts ./

# Download gradle and dependencies
COPY gradlew ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon 2>/dev/null || true

# Copy source code and build
COPY src/ src/
RUN ./gradlew shadowJar --no-daemon -x test

# =============================================================
# Stage 2: Keycloak with plugin + theme + psql client
# =============================================================
FROM quay.io/keycloak/keycloak:26.2.4

# Install psql client for DB init (run as root, then switch back)
USER root
RUN microdnf install -y postgresql && microdnf clean all
USER keycloak

# Copy SPI plugin JAR
COPY --from=builder /app/build/libs/goldcar-user-storage-spi-1.0.0.jar /opt/keycloak/providers/

# Copy Goldcar theme
COPY docker/themes/goldcar/ /opt/keycloak/themes/goldcar/

# Copy realm import config
COPY docker/realm-goldcar.json /opt/keycloak/data/import/realm-goldcar.json

# Copy DB init script
COPY docker/init-scripts/01-init.sql /opt/keycloak/data/init-db.sql

# Copy entrypoint
COPY railway-entrypoint.sh /opt/keycloak/railway-entrypoint.sh
USER root
RUN chmod +x /opt/keycloak/railway-entrypoint.sh
USER keycloak

# Build optimized Keycloak (pre-compiles providers + themes)
RUN /opt/keycloak/bin/kc.sh build \
    --db=postgres \
    --health-enabled=true

ENTRYPOINT ["/opt/keycloak/railway-entrypoint.sh"]
