package com.europcar.ciam.goldcar.util

import org.mindrot.jbcrypt.BCrypt

object BcryptValidator {

    fun hash(rawPassword: String): String = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10))

    fun verify(rawPassword: String, bcryptHash: String): Boolean = BCrypt.checkpw(rawPassword, bcryptHash)
}
