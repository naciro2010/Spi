package com.europcar.ciam.goldcar.provider

import com.europcar.ciam.goldcar.repository.GoldcarUserRepository
import org.keycloak.component.ComponentModel
import org.keycloak.component.ComponentValidationException
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.provider.ProviderConfigProperty
import org.keycloak.provider.ProviderConfigurationBuilder
import org.keycloak.storage.UserStorageProviderFactory

class GoldcarUserStorageProviderFactory : UserStorageProviderFactory<GoldcarUserStorageProvider> {

    companion object {
        const val PROVIDER_ID = "goldcar-user-storage"

        const val CONFIG_JDBC_URL = "jdbc-url"
        const val CONFIG_DB_USER = "db-user"
        const val CONFIG_DB_PASSWORD = "db-password"
    }

    @Volatile
    private var repository: GoldcarUserRepository? = null

    override fun getId(): String = PROVIDER_ID

    override fun create(session: KeycloakSession, model: ComponentModel): GoldcarUserStorageProvider {
        val repo = getOrCreateRepository(model)
        return GoldcarUserStorageProvider(session, model, repo)
    }

    private fun getOrCreateRepository(model: ComponentModel): GoldcarUserRepository {
        repository?.let { return it }

        synchronized(this) {
            repository?.let { return it }

            val jdbcUrl = model.get(CONFIG_JDBC_URL)
            val dbUser = model.get(CONFIG_DB_USER)
            val dbPassword = model.get(CONFIG_DB_PASSWORD)

            val repo = GoldcarUserRepository(jdbcUrl, dbUser, dbPassword)
            repository = repo
            return repo
        }
    }

    override fun getConfigProperties(): List<ProviderConfigProperty> {
        return ProviderConfigurationBuilder.create()
            .property()
            .name(CONFIG_JDBC_URL)
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("JDBC URL")
            .helpText("JDBC URL for the Goldcar legacy database (e.g., jdbc:postgresql://host:5432/goldcar_legacy)")
            .add()
            .property()
            .name(CONFIG_DB_USER)
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("Database User")
            .helpText("Username for the Goldcar legacy database")
            .add()
            .property()
            .name(CONFIG_DB_PASSWORD)
            .type(ProviderConfigProperty.PASSWORD)
            .label("Database Password")
            .helpText("Password for the Goldcar legacy database")
            .secret(true)
            .add()
            .build()
    }

    override fun validateConfiguration(session: KeycloakSession, realm: RealmModel, config: ComponentModel) {
        val jdbcUrl = config.get(CONFIG_JDBC_URL)
        val dbUser = config.get(CONFIG_DB_USER)
        val dbPassword = config.get(CONFIG_DB_PASSWORD)

        if (jdbcUrl.isNullOrBlank()) {
            throw ComponentValidationException("JDBC URL is required")
        }
        if (dbUser.isNullOrBlank()) {
            throw ComponentValidationException("Database user is required")
        }
        if (dbPassword.isNullOrBlank()) {
            throw ComponentValidationException("Database password is required")
        }
    }

    override fun close() {
        repository?.close()
        repository = null
    }
}
