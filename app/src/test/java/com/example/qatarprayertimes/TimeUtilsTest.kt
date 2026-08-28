package com.example.qatarprayertimes

import com.example.qatarprayertimes.data.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {
    @Test
    fun normalizeClockRollsAfternoonTimes() {
        val fajr = TimeUtils.normalizeClock("3:52")
        val sunrise = TimeUtils.normalizeClock("5:12", TimeUtils.clockToMinutes(fajr))
        val dhuhr = TimeUtils.normalizeClock("11:36", TimeUtils.clockToMinutes(sunrise))
        val asr = TimeUtils.normalizeClock("3:06", TimeUtils.clockToMinutes(dhuhr))
        val maghrib = TimeUtils.normalizeClock("6:00", TimeUtils.clockToMinutes(asr))
        val isha = TimeUtils.normalizeClock("7:30", TimeUtils.clockToMinutes(maghrib))
        assertEquals("03:52", fajr)
        assertEquals("05:12", sunrise)
        assertEquals("11:36", dhuhr)
        assertEquals("15:06", asr)
        assertEquals("18:00", maghrib)
        assertEquals("19:30", isha)
    }
}
