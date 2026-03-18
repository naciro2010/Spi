package com.europcar.ciam.goldcar.model

import org.keycloak.component.ComponentModel
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.storage.StorageId
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage

class GoldcarUserAdapter(
    session: KeycloakSession,
    realm: RealmModel,
    storageComponentModel: ComponentModel,
    private val entity: GoldcarUserEntity
) : AbstractUserAdapterFederatedStorage(session, realm, storageComponentModel) {

    companion object {
        private val READ_ONLY_ATTRS = setOf("goldcar_customer_id", "phone")
    }

    override fun getId(): String =
        StorageId(storageProviderModel.id, entity.codigoUsuario.toString()).id

    override fun getUsername(): String = entity.email
    override fun setUsername(username: String?) {}

    override fun getEmail(): String = entity.email
    override fun setEmail(email: String?) {}
    override fun isEmailVerified(): Boolean = true

    override fun getFirstName(): String? = entity.nombre
    override fun setFirstName(firstName: String?) {}

    override fun getLastName(): String? = entity.apellidos
    override fun setLastName(lastName: String?) {}

    override fun isEnabled(): Boolean = true

    override fun getFirstAttribute(name: String?): String? = when (name) {
        "goldcar_customer_id" -> entity.codigoUsuario.toString()
        "phone" -> entity.telefono
        else -> super.getFirstAttribute(name)
    }

    override fun getAttributes(): Map<String, List<String>> {
        val attrs = super.getAttributes().toMutableMap()
        attrs["goldcar_customer_id"] = listOf(entity.codigoUsuario.toString())
        entity.telefono?.let { attrs["phone"] = listOf(it) }
        return attrs
    }

    override fun setSingleAttribute(name: String?, value: String?) {
        if (name !in READ_ONLY_ATTRS) super.setSingleAttribute(name, value)
    }

    override fun setAttribute(name: String?, values: MutableList<String>?) {
        if (name !in READ_ONLY_ATTRS) super.setAttribute(name, values)
    }

    override fun removeAttribute(name: String?) {
        if (name !in READ_ONLY_ATTRS) super.removeAttribute(name)
    }
}
