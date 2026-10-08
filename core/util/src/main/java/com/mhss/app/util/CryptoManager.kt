package com.mhss.app.util

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val ITERATIONS = 65_536
    private const val KEY_LENGTH = 256

    fun encrypt(data: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_SIZE).also {
            SecureRandom().nextBytes(it)
        }

        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val encrypted = cipher.doFinal(data)

        return salt + cipher.iv + encrypted
    }

    fun decrypt(data: ByteArray, password: String): ByteArray {
        val salt = data.copyOfRange(0, SALT_SIZE)
        val iv = data.copyOfRange(SALT_SIZE, SALT_SIZE + IV_SIZE)
        val encrypted = data.copyOfRange(SALT_SIZE + IV_SIZE, data.size)

        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(128, iv)
        )

        return cipher.doFinal(encrypted)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        )
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}

