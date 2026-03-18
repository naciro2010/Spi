package com.europcar.ciam.goldcar.provider

import com.europcar.ciam.goldcar.model.GoldcarUserAdapter
import com.europcar.ciam.goldcar.repository.GoldcarUserRepository
import com.europcar.ciam.goldcar.util.BcryptValidator
import org.keycloak.component.ComponentModel
import org.keycloak.credential.CredentialInput
import org.keycloak.credential.CredentialInputUpdater
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
    CredentialInputUpdater,
    UserRegistrationProvider,
    UserQueryMethodsProvider {

    private val loadedUsers = mutableMapOf<String, UserModel>()

    override fun close() {}

    // --- UserLookupProvider ---

    override fun getUserByUsername(realm: RealmModel, username: String): UserModel? =
        getUserByEmail(realm, username)

    override fun getUserByEmail(realm: RealmModel, email: String): UserModel? {
        loadedUsers[email]?.let { return it }
        val entity = repository.findByEmail(email) ?: return null
        return GoldcarUserAdapter(session, realm, model, entity).also { loadedUsers[email] = it }
    }

    override fun getUserById(realm: RealmModel, id: String): UserModel? {
        val codigoUsuario = StorageId(id).externalId.toLongOrNull() ?: return null
        val entity = repository.findById(codigoUsuario) ?: return null
        return GoldcarUserAdapter(session, realm, model, entity).also { loadedUsers[entity.email] = it }
    }

    // --- CredentialInputValidator ---

    override fun supportsCredentialType(credentialType: String) =
        credentialType == PasswordCredentialModel.TYPE

    override fun isConfiguredFor(realm: RealmModel, user: UserModel, credentialType: String): Boolean {
        if (credentialType != PasswordCredentialModel.TYPE) return false
        val entity = repository.findByEmail(user.email ?: return false) ?: return false
        return !entity.password.isNullOrBlank()
    }

    override fun isValid(realm: RealmModel, user: UserModel, credentialInput: CredentialInput): Boolean {
        if (credentialInput.type != PasswordCredentialModel.TYPE) return false
        val entity = repository.findByEmail(user.email ?: return false) ?: return false
        val storedHash = entity.password ?: return false
        return BcryptValidator.verify(credentialInput.challengeResponse ?: return false, storedHash)
    }

    // --- CredentialInputUpdater (reset password) ---

    override fun updateCredential(realm: RealmModel, user: UserModel, input: CredentialInput): Boolean {
        if (input.type != PasswordCredentialModel.TYPE) return false
        val codigoUsuario = StorageId(user.id).externalId.toLongOrNull() ?: return false
        val newPassword = input.challengeResponse ?: return false
        val hash = BcryptValidator.hash(newPassword)
        return repository.updatePassword(codigoUsuario, hash)
    }

    override fun disableCredentialType(realm: RealmModel, user: UserModel, credentialType: String) {
        // Not supported: we don't allow disabling passwords in the legacy DB
    }

    override fun getDisableableCredentialTypesStream(realm: RealmModel, user: UserModel): Stream<String> =
        Stream.empty()

    // --- UserRegistrationProvider ---

    override fun addUser(realm: RealmModel, username: String): UserModel {
        val entity = repository.createUser(email = username, password = null, nombre = null, apellidos = null)
        return GoldcarUserAdapter(session, realm, model, entity).also { loadedUsers[username] = it }
    }

    override fun removeUser(realm: RealmModel, user: UserModel): Boolean {
        val codigoUsuario = StorageId(user.id).externalId.toLongOrNull() ?: return false
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
        return repository.searchByEmail(search, firstResult ?: 0, maxResults ?: 100).stream().map { entity ->
            GoldcarUserAdapter(session, realm, model, entity).also { loadedUsers[entity.email] = it } as UserModel
        }
    }

    override fun getUsersCount(realm: RealmModel) = repository.getUsersCount()
}
