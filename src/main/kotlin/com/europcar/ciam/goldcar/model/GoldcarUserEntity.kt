package com.europcar.ciam.goldcar.model

data class GoldcarUserEntity(
    val codigoUsuario: Long,
    val email: String,
    val password: String?,
    val nombre: String?,
    val apellidos: String?,
    val telefono: String?
)
