package com.europcar.ciam.goldcar.provider

import com.europcar.ciam.goldcar.model.GoldcarUserAdapter
import com.europcar.ciam.goldcar.repository.GoldcarUserRepository
import com.europcar.ciam.goldcar.util.BcryptValidator
import org.keycloak.component.ComponentModel
import org.keycloak.credential.CredentialInput
import org.keycloak.credential.CredentialInputValidator
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.models.UserModel
import org.keycloak.models.credential.PasswordCredentialModel
import org.keycloak.storage.StorageId
import org.keycloak.storage.UserStorageProvider
import org.keycloak.storage.user.UserLookupProvider
import org.keycloak.storage.user.UserQueryMethodsProvider
import org.keycloak.storage.user.UserRegistrationProvider
import java.util.stream.Stream

class GoldcarUserStorageProvider(
    private val session: KeycloakSession,
    private val model: ComponentModel,
    private val repository: GoldcarUserRepository
) : UserStorageProvider,
    UserLookupProvider,
    CredentialInputValidator,
    UserRegistrationProvider,
    UserQueryMethodsProvider {

    private val loadedUsers = mutableMapOf<String, UserModel>()

    // --- UserStorageProvider lifecycle ---

    override fun close() {
        // Provider is per-transaction; repository is managed by factory
    }

    // --- UserLookupProvider ---

    override fun getUserByUsername(realm: RealmModel, username: String): UserModel? {
        return getUserByEmail(realm, username)
    }

    override fun getUserByEmail(realm: RealmModel, email: String): UserModel? {
        loadedUsers[email]?.let { return it }

        val entity = repository.findByEmail(email) ?: return null
        val adapter = GoldcarUserAdapter(session, realm, model, entity)
        loadedUsers[email] = adapter
        return adapter
    }

    override fun getUserById(realm: RealmModel, id: String): UserModel? {
        val storageId = StorageId(id)
        val externalId = storageId.externalId
        val codigoUsuario = externalId.toLongOrNull() ?: return null

        val entity = repository.findById(codigoUsuario) ?: return null
        val adapter = GoldcarUserAdapter(session, realm, model, entity)
        loadedUsers[entity.email] = adapter
        return adapter
    }

    // --- CredentialInputValidator ---

    override fun supportsCredentialType(credentialType: String): Boolean {
        return credentialType == PasswordCredentialModel.TYPE
    }

    override fun isConfiguredFor(realm: RealmModel, user: UserModel, credentialType: String): Boolean {
        if (credentialType != PasswordCredentialModel.TYPE) return false
        val email = user.email ?: return false
        val entity = repository.findByEmail(email) ?: return false
        return !entity.password.isNullOrBlank()
    }

    override fun isValid(realm: RealmModel, user: UserModel, credentialInput: CredentialInput): Boolean {
        if (credentialInput.type != PasswordCredentialModel.TYPE) return false

        val email = user.email ?: return false
        val entity = repository.findByEmail(email) ?: return false
        val storedHash = entity.password ?: return false

        val inputPassword = credentialInput.challengeResponse ?: return false
        return BcryptValidator.verify(inputPassword, storedHash)
    }

    // --- UserRegistrationProvider ---

    override fun addUser(realm: RealmModel, username: String): UserModel {
        val entity = repository.createUser(email = username, nombre = null, apellidos = null)
        val adapter = GoldcarUserAdapter(session, realm, model, entity)
        loadedUsers[username] = adapter
        return adapter
    }

    override fun removeUser(realm: RealmModel, user: UserModel): Boolean {
        val storageId = StorageId(user.id)
        val codigoUsuario = storageId.externalId.toLongOrNull() ?: return false
        return repository.deleteUser(codigoUsuario)
    }

    // --- UserQueryMethodsProvider ---

    override fun searchForUserStream(
        realm: RealmModel,
        params: MutableMap<String, String>,
        firstResult: Int?,
        maxResults: Int?
    ): Stream<UserModel> {
        val search = params[UserModel.SEARCH] ?: params[UserModel.USERNAME] ?: params[UserModel.EMAIL] ?: ""
        val first = firstResult ?: 0
        val max = maxResults ?: 100

        val entities = repository.searchByEmail(search, first, max)
        return entities.stream().map { entity ->
            val adapter = GoldcarUserAdapter(session, realm, model, entity)
            loadedUsers[entity.email] = adapter
            adapter as UserModel
        }
    }

    override fun getUsersCount(realm: RealmModel): Int {
        return repository.getUsersCount()
    }
}
