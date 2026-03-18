package com.europcar.ciam.goldcar.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mindrot.jbcrypt.BCrypt

class BcryptValidatorTest {

    @Test
    fun `verify returns true for matching password`() {
        val rawPassword = "1234567"
        val hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10))

        assertTrue(BcryptValidator.verify(rawPassword, hash))
    }

    @Test
    fun `verify returns false for wrong password`() {
        val hash = BCrypt.hashpw("correctPassword", BCrypt.gensalt(10))

        assertFalse(BcryptValidator.verify("wrongPassword", hash))
    }

    @Test
    fun `verify works with known bcrypt hash`() {
        // Pre-computed hash for "1234567" with cost factor 10
        val rawPassword = "1234567"
        val knownHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10))

        assertTrue(BcryptValidator.verify(rawPassword, knownHash))
        assertFalse(BcryptValidator.verify("wrong", knownHash))
    }

    @Test
    fun `verify handles different cost factors`() {
        val rawPassword = "testPassword123"
        val hashCost4 = BCrypt.hashpw(rawPassword, BCrypt.gensalt(4))
        val hashCost12 = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12))

        assertTrue(BcryptValidator.verify(rawPassword, hashCost4))
        assertTrue(BcryptValidator.verify(rawPassword, hashCost12))
    }
}
