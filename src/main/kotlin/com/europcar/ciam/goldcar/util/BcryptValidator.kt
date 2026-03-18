package com.europcar.ciam.goldcar.util

import org.mindrot.jbcrypt.BCrypt

object BcryptValidator {

    fun verify(rawPassword: String, bcryptHash: String): Boolean {
        return BCrypt.checkpw(rawPassword, bcryptHash)
    }
}
