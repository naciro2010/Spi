package com.europcar.ciam.goldcar.repository

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.mindrot.jbcrypt.BCrypt
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class GoldcarUserRepositoryTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("goldcar_legacy")
            withUsername("goldcar")
            withPassword("goldcar")
        }
    }

    private lateinit var repository: GoldcarUserRepository

    @BeforeAll
    fun setup() {
        postgres.start()

        // Create table and seed data
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("""
                    CREATE TABLE USUARIOS (
                        CodigoUsuario SERIAL PRIMARY KEY,
                        Email VARCHAR(255) UNIQUE NOT NULL,
                        Password VARCHAR(255),
                        Nombre VARCHAR(100),
                        Apellidos VARCHAR(100),
                        Telefono VARCHAR(20)
                    )
                """.trimIndent())

                val hash = BCrypt.hashpw("1234567", BCrypt.gensalt(10))
                stmt.execute("""
                    INSERT INTO USUARIOS (Email, Password, Nombre, Apellidos, Telefono)
                    VALUES ('goldcarweb@gmail.com', '$hash', 'Test', 'User', '+34600000000')
                """.trimIndent())
            }
        }

        repository = GoldcarUserRepository(postgres.jdbcUrl, postgres.username, postgres.password)
    }

    @AfterAll
    fun teardown() {
        repository.close()
    }

    @Test
    @Order(1)
    fun `findByEmail returns user for existing email`() {
        val user = repository.findByEmail("goldcarweb@gmail.com")

        assertNotNull(user)
        assertEquals("goldcarweb@gmail.com", user!!.email)
        assertEquals("Test", user.nombre)
        assertEquals("User", user.apellidos)
        assertEquals("+34600000000", user.telefono)
        assertNotNull(user.password)
    }

    @Test
    @Order(2)
    fun `findByEmail is case insensitive`() {
        val user = repository.findByEmail("GoldcarWeb@Gmail.com")
        assertNotNull(user)
        assertEquals("goldcarweb@gmail.com", user!!.email)
    }

    @Test
    @Order(3)
    fun `findByEmail returns null for non-existing email`() {
        val user = repository.findByEmail("nonexistent@example.com")
        assertNull(user)
    }

    @Test
    @Order(4)
    fun `findById returns user for existing id`() {
        val user = repository.findById(1)
        assertNotNull(user)
        assertEquals("goldcarweb@gmail.com", user!!.email)
    }

    @Test
    @Order(5)
    fun `findById returns null for non-existing id`() {
        val user = repository.findById(9999)
        assertNull(user)
    }

    @Test
    @Order(6)
    fun `createUser inserts new user and returns entity`() {
        val user = repository.createUser("newuser@example.com", "New", "User")

        assertNotNull(user)
        assertEquals("newuser@example.com", user.email)
        assertEquals("New", user.nombre)
        assertEquals("User", user.apellidos)
        assertNull(user.password)
        assertTrue(user.codigoUsuario > 0)

        // Verify it's retrievable
        val found = repository.findByEmail("newuser@example.com")
        assertNotNull(found)
        assertEquals(user.codigoUsuario, found!!.codigoUsuario)
    }

    @Test
    @Order(7)
    fun `searchByEmail returns matching users`() {
        val results = repository.searchByEmail("goldcar", 0, 10)
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.email == "goldcarweb@gmail.com" })
    }

    @Test
    @Order(8)
    fun `getUsersCount returns correct count`() {
        val count = repository.getUsersCount()
        assertTrue(count >= 2) // seed user + created user
    }

    @Test
    @Order(9)
    fun `deleteUser removes user`() {
        val user = repository.createUser("todelete@example.com", "Delete", "Me")
        assertTrue(repository.deleteUser(user.codigoUsuario))
        assertNull(repository.findByEmail("todelete@example.com"))
    }
}
