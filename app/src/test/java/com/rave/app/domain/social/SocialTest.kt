package com.rave.app.domain.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SocialTest {

    @Test
    fun `invite codes use name prefix and safe alphabet`() {
        val code = InviteCodes.generate("Rayan Shah", Random(42))
        assertTrue(code.startsWith("RAYANS-"))
        assertTrue(InviteCodes.isValid(code))
        assertEquals("ZOE", InviteCodes.prefixFor("Zoë"))
        assertEquals("RAVE", InviteCodes.prefixFor("123"))
    }

    @Test
    fun `normalizes typed codes`() {
        assertEquals("RAYAN-7K2Q", InviteCodes.normalize(" rayan7k2q "))
        assertEquals("RAYAN-7K2Q", InviteCodes.normalize("rayan-7k2q"))
        assertFalse(InviteCodes.isValid("RAYAN-0O1I"))
    }

    @Test
    fun `phone hashes ignore formatting`() {
        assertEquals(PhoneHasher.hash("+44 7700 900123"), PhoneHasher.hash("07700900123"))
        assertNull(PhoneHasher.hash("12"))
        assertEquals(64, PhoneHasher.hash("0300 1234567")!!.length)
    }
}
