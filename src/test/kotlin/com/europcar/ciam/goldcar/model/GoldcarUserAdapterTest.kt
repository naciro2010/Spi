package com.europcar.ciam.goldcar.model

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.keycloak.component.ComponentModel
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel

class GoldcarUserAdapterTest {

    private lateinit var session: KeycloakSession
    private lateinit var realm: RealmModel
    private lateinit var componentModel: ComponentModel
    private lateinit var entity: GoldcarUserEntity
    private lateinit var adapter: GoldcarUserAdapter

    @BeforeEach
    fun setup() {
        session = mockk(relaxed = true)
        realm = mockk(relaxed = true)
        componentModel = mockk(relaxed = true)

        every { componentModel.id } returns "test-component-id"

        entity = GoldcarUserEntity(
            codigoUsuario = 42,
            email = "test@goldcar.com",
            password = "\$2a\$10\$hashedvalue",
            nombre = "Juan",
            apellidos = "Garcia",
            telefono = "+34600111222"
        )

        adapter = GoldcarUserAdapter(session, realm, componentModel, entity)
    }

    @Test
    fun `getId returns composite storage id`() {
        val id = adapter.id
        assertTrue(id.contains("test-component-id"))
        assertTrue(id.contains("42"))
    }

    @Test
    fun `getUsername returns email`() {
        assertEquals("test@goldcar.com", adapter.username)
    }

    @Test
    fun `getEmail returns entity email`() {
        assertEquals("test@goldcar.com", adapter.email)
    }

    @Test
    fun `getFirstName returns nombre`() {
        assertEquals("Juan", adapter.firstName)
    }

    @Test
    fun `getLastName returns apellidos`() {
        assertEquals("Garcia", adapter.lastName)
    }

    @Test
    fun `isEnabled returns true`() {
        assertTrue(adapter.isEnabled)
    }

    @Test
    fun `isEmailVerified returns true`() {
        assertTrue(adapter.isEmailVerified)
    }

    @Test
    fun `getFirstAttribute returns goldcar_customer_id`() {
        assertEquals("42", adapter.getFirstAttribute("goldcar_customer_id"))
    }

    @Test
    fun `getFirstAttribute returns phone`() {
        assertEquals("+34600111222", adapter.getFirstAttribute("phone"))
    }

    @Test
    fun `getAttributes includes custom attributes`() {
        val attrs = adapter.attributes
        assertEquals(listOf("42"), attrs["goldcar_customer_id"])
        assertEquals(listOf("+34600111222"), attrs["phone"])
    }
}
