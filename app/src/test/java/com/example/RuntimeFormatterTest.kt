package com.jbuilds.bingemode

import com.jbuilds.bingemode.utils.RuntimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeFormatterTest {
    @Test
    fun formatsHoursAndMinutes() {
        assertEquals("2h 1m", RuntimeFormatter.format(121))
        assertEquals("1h 30m", RuntimeFormatter.format(90))
    }

    @Test
    fun formatsWholeHoursAndMinuteOnlyDurations() {
        assertEquals("2h", RuntimeFormatter.format(120))
        assertEquals("1h", RuntimeFormatter.format(60))
        assertEquals("45m", RuntimeFormatter.format(45))
    }

    @Test
    fun returnsNullForMissingOrInvalidRuntime() {
        assertNull(RuntimeFormatter.format(null))
        assertNull(RuntimeFormatter.format(0))
        assertNull(RuntimeFormatter.format(-5))
    }
}
