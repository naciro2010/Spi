plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.europcar.ciam.goldcar"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

val keycloakVersion = "26.2.4"

dependencies {
    // Keycloak SPI (provided at runtime by Keycloak server)
    compileOnly("org.keycloak:keycloak-core:$keycloakVersion")
    compileOnly("org.keycloak:keycloak-server-spi:$keycloakVersion")
    compileOnly("org.keycloak:keycloak-server-spi-private:$keycloakVersion")
    compileOnly("org.keycloak:keycloak-services:$keycloakVersion")

    // bcrypt (bundled in fat JAR)
    implementation("org.mindrot:jbcrypt:0.4")

    // HikariCP for connection pooling (bundled in fat JAR)
    implementation("com.zaxxer:HikariCP:6.2.1")

    // PostgreSQL JDBC (provided by Keycloak runtime)
    compileOnly("org.postgresql:postgresql:42.7.4")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.keycloak:keycloak-core:$keycloakVersion")
    testImplementation("org.keycloak:keycloak-server-spi:$keycloakVersion")
    testImplementation("org.keycloak:keycloak-server-spi-private:$keycloakVersion")
    testImplementation("org.keycloak:keycloak-services:$keycloakVersion")
    testImplementation("org.postgresql:postgresql:42.7.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    mergeServiceFiles()
    dependencies {
        exclude(dependency("org.keycloak:.*"))
        exclude(dependency("org.postgresql:.*"))
        exclude(dependency("jakarta.*:.*"))
    }
}

tasks.named("build") {
    dependsOn("shadowJar")
}
