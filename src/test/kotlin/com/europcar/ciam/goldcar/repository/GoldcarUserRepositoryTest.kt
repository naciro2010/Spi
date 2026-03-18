package com.europcar.ciam.goldcar.repository

import com.europcar.ciam.goldcar.util.BcryptValidator
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
        assertNull(repository.findByEmail("nonexistent@example.com"))
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
        assertNull(repository.findById(9999))
    }

    @Test
    @Order(6)
    fun `createUser without password inserts user`() {
        val user = repository.createUser("nopass@example.com", null, "No", "Pass")

        assertNotNull(user)
        assertEquals("nopass@example.com", user.email)
        assertNull(user.password)
        assertTrue(user.codigoUsuario > 0)

        val found = repository.findByEmail("nopass@example.com")
        assertNotNull(found)
        assertEquals(user.codigoUsuario, found!!.codigoUsuario)
    }

    @Test
    @Order(7)
    fun `createUser with password stores bcrypt hash`() {
        val hash = BcryptValidator.hash("mypassword")
        val user = repository.createUser("withpass@example.com", hash, "With", "Pass")

        assertNotNull(user)
        assertEquals(hash, user.password)

        val found = repository.findByEmail("withpass@example.com")
        assertNotNull(found)
        assertTrue(BcryptValidator.verify("mypassword", found!!.password!!))
    }

    @Test
    @Order(8)
    fun `updatePassword changes the stored hash`() {
        val user = repository.findByEmail("goldcarweb@gmail.com")!!
        val newHash = BcryptValidator.hash("newpassword")

        assertTrue(repository.updatePassword(user.codigoUsuario, newHash))

        val updated = repository.findByEmail("goldcarweb@gmail.com")!!
        assertTrue(BcryptValidator.verify("newpassword", updated.password!!))
    }

    @Test
    @Order(9)
    fun `searchByEmail returns matching users`() {
        val results = repository.searchByEmail("goldcar", 0, 10)
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.email == "goldcarweb@gmail.com" })
    }

    @Test
    @Order(10)
    fun `getUsersCount returns correct count`() {
        assertTrue(repository.getUsersCount() >= 2)
    }

    @Test
    @Order(11)
    fun `deleteUser removes user`() {
        val user = repository.createUser("todelete@example.com", null, "Delete", "Me")
        assertTrue(repository.deleteUser(user.codigoUsuario))
        assertNull(repository.findByEmail("todelete@example.com"))
    }
}
