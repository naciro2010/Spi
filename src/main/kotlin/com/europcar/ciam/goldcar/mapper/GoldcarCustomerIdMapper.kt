package com.europcar.ciam.goldcar.mapper

import org.keycloak.models.ClientSessionContext
import org.keycloak.models.KeycloakSession
import org.keycloak.models.ProtocolMapperModel
import org.keycloak.models.UserSessionModel
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper
import org.keycloak.provider.ProviderConfigProperty
import org.keycloak.representations.IDToken

class GoldcarCustomerIdMapper :
    AbstractOIDCProtocolMapper(),
    OIDCAccessTokenMapper,
    OIDCIDTokenMapper,
    UserInfoTokenMapper {

    companion object {
        const val PROVIDER_ID = "goldcar-customer-id-mapper"
    }

    override fun getId(): String = PROVIDER_ID
    override fun getDisplayCategory(): String = "Token mapper"
    override fun getDisplayType(): String = "Goldcar Customer ID"
    override fun getHelpText(): String = "Maps the Goldcar USUARIOS.CodigoUsuario to a JWT claim"
    override fun getProtocolMapperCategory(): String = "Token mapper"

    override fun getConfigProperties(): List<ProviderConfigProperty> = listOf(
        configProperty(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_LABEL, OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_HELP_TEXT, "true"),
        configProperty(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN_LABEL, OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN_HELP_TEXT, "false"),
        configProperty(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO_LABEL, OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO_HELP_TEXT, "false"),
    )

    override fun setClaim(
        token: IDToken,
        mappingModel: ProtocolMapperModel,
        userSession: UserSessionModel,
        keycloakSession: KeycloakSession,
        clientSessionCtx: ClientSessionContext
    ) {
        val customerId = userSession.user.getFirstAttribute("goldcar_customer_id")
        if (customerId != null) {
            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, customerId)
        }
    }

    private fun configProperty(name: String, label: String, helpText: String, default: String) =
        ProviderConfigProperty().apply {
            this.name = name
            this.label = label
            this.type = ProviderConfigProperty.BOOLEAN_TYPE
            this.defaultValue = default
            this.helpText = helpText
        }
}
