package com.europcar.ciam.goldcar.repository

import com.europcar.ciam.goldcar.model.GoldcarUserEntity
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet

class GoldcarUserRepository(
    jdbcUrl: String,
    dbUser: String,
    dbPassword: String
) : AutoCloseable {

    private val dataSource: HikariDataSource

    init {
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = dbUser
            this.password = dbPassword
            maximumPoolSize = 5
            minimumIdle = 1
            connectionTimeout = 5000
            idleTimeout = 300000
            maxLifetime = 600000
            poolName = "goldcar-spi-pool"
        }
        dataSource = HikariDataSource(config)
    }

    fun findByEmail(email: String): GoldcarUserEntity? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT CodigoUsuario, Email, Password, Nombre, Apellidos, Telefono FROM USUARIOS WHERE LOWER(Email) = LOWER(?)"
            ).use { stmt ->
                stmt.setString(1, email)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) mapRow(rs) else null
                }
            }
        }
    }

    fun findById(codigoUsuario: Long): GoldcarUserEntity? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT CodigoUsuario, Email, Password, Nombre, Apellidos, Telefono FROM USUARIOS WHERE CodigoUsuario = ?"
            ).use { stmt ->
                stmt.setLong(1, codigoUsuario)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) mapRow(rs) else null
                }
            }
        }
    }

    fun createUser(email: String, nombre: String?, apellidos: String?): GoldcarUserEntity {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO USUARIOS (Email, Nombre, Apellidos) VALUES (?, ?, ?) RETURNING CodigoUsuario"
            ).use { stmt ->
                stmt.setString(1, email)
                stmt.setString(2, nombre)
                stmt.setString(3, apellidos)
                stmt.executeQuery().use { rs ->
                    rs.next()
                    val id = rs.getLong("CodigoUsuario")
                    return GoldcarUserEntity(
                        codigoUsuario = id,
                        email = email,
                        password = null,
                        nombre = nombre,
                        apellidos = apellidos,
                        telefono = null
                    )
                }
            }
        }
    }

    fun deleteUser(codigoUsuario: Long): Boolean {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM USUARIOS WHERE CodigoUsuario = ?").use { stmt ->
                stmt.setLong(1, codigoUsuario)
                return stmt.executeUpdate() > 0
            }
        }
    }

    fun searchByEmail(pattern: String, firstResult: Int, maxResults: Int): List<GoldcarUserEntity> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT CodigoUsuario, Email, Password, Nombre, Apellidos, Telefono FROM USUARIOS WHERE LOWER(Email) LIKE LOWER(?) ORDER BY CodigoUsuario LIMIT ? OFFSET ?"
            ).use { stmt ->
                stmt.setString(1, "%$pattern%")
                stmt.setInt(2, maxResults)
                stmt.setInt(3, firstResult)
                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<GoldcarUserEntity>()
                    while (rs.next()) {
                        results.add(mapRow(rs))
                    }
                    return results
                }
            }
        }
    }

    fun getUsersCount(): Int {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT COUNT(*) FROM USUARIOS").use { stmt ->
                stmt.executeQuery().use { rs ->
                    rs.next()
                    return rs.getInt(1)
                }
            }
        }
    }

    private fun mapRow(rs: ResultSet): GoldcarUserEntity {
        return GoldcarUserEntity(
            codigoUsuario = rs.getLong("CodigoUsuario"),
            email = rs.getString("Email"),
            password = rs.getString("Password"),
            nombre = rs.getString("Nombre"),
            apellidos = rs.getString("Apellidos"),
            telefono = rs.getString("Telefono")
        )
    }

    override fun close() {
        if (!dataSource.isClosed) {
            dataSource.close()
        }
    }
}
