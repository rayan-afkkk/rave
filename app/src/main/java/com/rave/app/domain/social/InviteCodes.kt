package com.rave.app.domain.social

import java.text.Normalizer
import kotlin.random.Random

/**
 * Short, human-friendly invite codes like `RAYAN-7K2Q`: a name prefix plus four characters
 * from an alphabet without look-alikes (no 0/O, 1/I/L).
 */
object InviteCodes {
    private const val ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
    private const val SUFFIX_LENGTH = 4
    private const val MAX_PREFIX = 6
    private const val FALLBACK_PREFIX = "RAVE"
    private val PATTERN = Regex("^[A-Z]{1,6}-[$ALPHABET]{4}$")

    fun generate(name: String, random: Random = Random.Default): String {
        val suffix = (1..SUFFIX_LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
        return "${prefixFor(name)}-$suffix"
    }

    fun prefixFor(name: String): String {
        val ascii = Normalizer.normalize(name, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        val letters = ascii.uppercase().filter { it in 'A'..'Z' }.take(MAX_PREFIX)
        return letters.ifEmpty { FALLBACK_PREFIX }
    }

    /** Normalises user input: trims, upper-cases, and accepts a missing dash ("rayan7k2q"). */
    fun normalize(input: String): String {
        val cleaned = input.trim().uppercase().filter { it.isLetterOrDigit() || it == '-' }
        if ('-' in cleaned || cleaned.length <= SUFFIX_LENGTH) return cleaned
        return cleaned.dropLast(SUFFIX_LENGTH) + "-" + cleaned.takeLast(SUFFIX_LENGTH)
    }

    fun isValid(code: String): Boolean = PATTERN.matches(code)
}
