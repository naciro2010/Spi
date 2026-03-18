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
        private const val CLAIM_NAME = "goldcar_customer_id"

        private val CONFIG_PROPERTIES: List<ProviderConfigProperty> = listOf(
            ProviderConfigProperty().apply {
                name = OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN
                label = OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_LABEL
                type = ProviderConfigProperty.BOOLEAN_TYPE
                defaultValue = "true"
                helpText = OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_HELP_TEXT
            },
            ProviderConfigProperty().apply {
                name = OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN
                label = OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN_LABEL
                type = ProviderConfigProperty.BOOLEAN_TYPE
                defaultValue = "false"
                helpText = OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN_HELP_TEXT
            },
            ProviderConfigProperty().apply {
                name = OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO
                label = OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO_LABEL
                type = ProviderConfigProperty.BOOLEAN_TYPE
                defaultValue = "false"
                helpText = OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO_HELP_TEXT
            }
        )
    }

    override fun getId(): String = PROVIDER_ID

    override fun getDisplayCategory(): String = "Token mapper"

    override fun getDisplayType(): String = "Goldcar Customer ID"

    override fun getHelpText(): String = "Maps the Goldcar USUARIOS.CodigoUsuario to a JWT claim"

    override fun getConfigProperties(): List<ProviderConfigProperty> = CONFIG_PROPERTIES

    override fun setClaim(
        token: IDToken,
        mappingModel: ProtocolMapperModel,
        userSession: UserSessionModel,
        keycloakSession: KeycloakSession,
        clientSessionCtx: ClientSessionContext
    ) {
        val user = userSession.user
        val customerId = user.getFirstAttribute("goldcar_customer_id")
        if (customerId != null) {
            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, customerId)
        }
    }

    override fun getProtocolMapperCategory(): String = "Token mapper"

    /**
     * Creates a default protocol mapper model for this mapper.
     */
    fun createDefaultModel(): ProtocolMapperModel {
        return ProtocolMapperModel().apply {
            name = "goldcar-customer-id"
            protocol = "openid-connect"
            protocolMapper = PROVIDER_ID
            isConsentRequired = false
            config = mutableMapOf(
                OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN to "true",
                OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN to "false",
                OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO to "false",
                OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME to CLAIM_NAME,
                OIDCAttributeMapperHelper.JSON_TYPE to "String"
            )
        }
    }
}
