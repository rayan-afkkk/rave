package com.rave.app.domain.social

import java.security.MessageDigest

/**
 * Contact matching never uploads raw phone numbers: numbers are normalised to their last
 * 10 digits and SHA-256 hashed with an app-wide salt. Only hashes are compared.
 *
 * Note: phone numbers have a small key space, so hashes alone are not strong anonymisation;
 * matching is opt-in and the server only ever sees hashes.
 */
object PhoneHasher {
    private const val SALT = "rave-contacts-v1:"
    private const val SIGNIFICANT_DIGITS = 10

    fun normalize(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        if (digits.length < 7) return null
        return digits.takeLast(SIGNIFICANT_DIGITS)
    }

    fun hash(raw: String): String? {
        val normalized = normalize(raw) ?: return null
        val bytes = MessageDigest.getInstance("SHA-256").digest((SALT + normalized).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
