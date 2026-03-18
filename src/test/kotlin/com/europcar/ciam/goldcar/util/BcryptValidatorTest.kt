package com.europcar.ciam.goldcar.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mindrot.jbcrypt.BCrypt

class BcryptValidatorTest {

    @Test
    fun `hash produces a valid bcrypt hash`() {
        val hash = BcryptValidator.hash("testPassword")
        assertTrue(hash.startsWith("\$2a\$10\$"))
        assertTrue(BcryptValidator.verify("testPassword", hash))
        assertFalse(BcryptValidator.verify("wrong", hash))
    }

    @Test
    fun `verify returns true for matching password`() {
        val hash = BCrypt.hashpw("1234567", BCrypt.gensalt(10))
        assertTrue(BcryptValidator.verify("1234567", hash))
    }

    @Test
    fun `verify returns false for wrong password`() {
        val hash = BCrypt.hashpw("correctPassword", BCrypt.gensalt(10))
        assertFalse(BcryptValidator.verify("wrongPassword", hash))
    }

    @Test
    fun `verify handles different cost factors`() {
        val password = "testPassword123"
        assertTrue(BcryptValidator.verify(password, BCrypt.hashpw(password, BCrypt.gensalt(4))))
        assertTrue(BcryptValidator.verify(password, BCrypt.hashpw(password, BCrypt.gensalt(12))))
    }
}
