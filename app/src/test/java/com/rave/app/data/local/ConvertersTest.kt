package com.rave.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `reminder times round-trip`() {
        val times = listOf("07:00", "21:30")
        assertEquals(times, converters.toStringList(converters.fromStringList(times)))
    }

    @Test
    fun `empty list round-trips to empty list`() {
        assertEquals(emptyList<String>(), converters.toStringList(converters.fromStringList(emptyList())))
    }
}
