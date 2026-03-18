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

    private val dataSource: HikariDataSource = HikariDataSource(HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        this.username = dbUser
        this.password = dbPassword
        maximumPoolSize = 5
        minimumIdle = 1
        connectionTimeout = 5000
        idleTimeout = 300000
        maxLifetime = 600000
        poolName = "goldcar-spi-pool"
    })

    private val selectColumns = "CodigoUsuario, Email, Password, Nombre, Apellidos, Telefono"

    fun findByEmail(email: String): GoldcarUserEntity? = query(
        "SELECT $selectColumns FROM USUARIOS WHERE LOWER(Email) = LOWER(?)"
    ) { it.setString(1, email) }

    fun findById(codigoUsuario: Long): GoldcarUserEntity? = query(
        "SELECT $selectColumns FROM USUARIOS WHERE CodigoUsuario = ?"
    ) { it.setLong(1, codigoUsuario) }

    fun createUser(email: String, password: String?, nombre: String?, apellidos: String?): GoldcarUserEntity {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO USUARIOS (Email, Password, Nombre, Apellidos) VALUES (?, ?, ?, ?) RETURNING CodigoUsuario"
            ).use { stmt ->
                stmt.setString(1, email)
                stmt.setString(2, password)
                stmt.setString(3, nombre)
                stmt.setString(4, apellidos)
                stmt.executeQuery().use { rs ->
                    rs.next()
                    return GoldcarUserEntity(
                        codigoUsuario = rs.getLong("CodigoUsuario"),
                        email = email,
                        password = password,
                        nombre = nombre,
                        apellidos = apellidos,
                        telefono = null
                    )
                }
            }
        }
    }

    fun updatePassword(codigoUsuario: Long, bcryptHash: String): Boolean {
        dataSource.connection.use { conn ->
            conn.prepareStatement("UPDATE USUARIOS SET Password = ? WHERE CodigoUsuario = ?").use { stmt ->
                stmt.setString(1, bcryptHash)
                stmt.setLong(2, codigoUsuario)
                return stmt.executeUpdate() > 0
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
                "SELECT $selectColumns FROM USUARIOS WHERE LOWER(Email) LIKE LOWER(?) ORDER BY CodigoUsuario LIMIT ? OFFSET ?"
            ).use { stmt ->
                stmt.setString(1, "%$pattern%")
                stmt.setInt(2, maxResults)
                stmt.setInt(3, firstResult)
                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<GoldcarUserEntity>()
                    while (rs.next()) results.add(mapRow(rs))
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

    private fun query(sql: String, bind: (java.sql.PreparedStatement) -> Unit): GoldcarUserEntity? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                bind(stmt)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) mapRow(rs) else null
                }
            }
        }
    }

    private fun mapRow(rs: ResultSet) = GoldcarUserEntity(
        codigoUsuario = rs.getLong("CodigoUsuario"),
        email = rs.getString("Email"),
        password = rs.getString("Password"),
        nombre = rs.getString("Nombre"),
        apellidos = rs.getString("Apellidos"),
        telefono = rs.getString("Telefono")
    )

    override fun close() {
        if (!dataSource.isClosed) dataSource.close()
    }
}
