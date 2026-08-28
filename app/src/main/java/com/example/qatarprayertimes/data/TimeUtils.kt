package com.example.qatarprayertimes.data

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object TimeUtils {
    val QATAR: TimeZone = TimeZone.getTimeZone("Asia/Qatar")
    private val CLOCK = Regex("""^(\d{1,2}):(\d{2})""")

    fun normalizeClock(raw: String, previousMinutes: Int = -1): String {
        val match = CLOCK.find(raw.trim()) ?: error("Could not parse clock value: $raw")
        val hours = match.groupValues[1].toInt()
        val minutes = match.groupValues[2].toInt()
        var total = hours * 60 + minutes
        while (previousMinutes >= 0 && total <= previousMinutes) {
            total += 12 * 60
        }
        return minutesToClock(((total % 1440) + 1440) % 1440)
    }

    fun clockToMinutes(clock: String): Int {
        val match = CLOCK.find(clock.trim()) ?: error("Could not parse clock value: $clock")
        return match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
    }

    fun minutesToClock(total: Int): String {
        val wrapped = ((total % 1440) + 1440) % 1440
        val hours = wrapped / 60
        val minutes = wrapped % 60
        return "%02d:%02d".format(hours, minutes)
    }

    fun addMinutes(clock: String, delta: Int): String = minutesToClock(clockToMinutes(clock) + delta)

    fun formatClock(clock: String, timeFormat: TimeFormat, locale: AppLocale): String {
        val minutes = clockToMinutes(clock)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(Calendar.HOUR_OF_DAY, minutes / 60)
        cal.set(Calendar.MINUTE, minutes % 60)
        cal.set(Calendar.SECOND, 0)
        val pattern = if (timeFormat == TimeFormat.H12) "h:mm a" else "HH:mm"
        val sdf = java.text.SimpleDateFormat(pattern, if (locale == AppLocale.AR) Locale("ar", "QA") else Locale.UK)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(cal.time)
    }

    fun nowInQatar(): Pair<Int, Int> {
        val cal = Calendar.getInstance(QATAR)
        val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return minutes to cal.get(Calendar.SECOND)
    }

    fun qatarDateKey(): String {
        val cal = Calendar.getInstance(QATAR)
        return "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun applyAreaOffsets(
        prayers: List<PrayerTime>,
        area: AreaId,
        offsets: Map<AreaId, AreaOffset>,
    ): List<PrayerTime> {
        val areaOffset = offsets[area] ?: offsets.getValue(AreaId.DOHA)
        return prayers.map { prayer ->
            val extra = when (prayer.id) {
                PrayerId.FAJR -> areaOffset.fajr
                PrayerId.MAGHRIB -> areaOffset.maghrib
                else -> 0
            }
            val azan = if (extra != 0) addMinutes(prayer.azan, extra) else prayer.azan
            val iqama = prayer.iqamaOffsetMinutes?.let { addMinutes(azan, it) }
            prayer.copy(azan = azan, iqama = iqama)
        }
    }

    data class Highlight(
        val next: PrayerId,
        val current: PrayerId?,
        val secondsUntilNext: Int,
    )

    fun getNextAndCurrent(
        prayers: List<PrayerTime>,
        nowMinutes: Int,
        nowSeconds: Int = 0,
    ): Highlight {
        val withIndex = prayers.map { it.id to clockToMinutes(it.azan) }
        val upcoming = withIndex.firstOrNull { it.second > nowMinutes }
        val next = upcoming ?: withIndex.first()
        val current = withIndex.lastOrNull { it.second <= nowMinutes }?.first
        val nowTotalSeconds = nowMinutes * 60 + nowSeconds
        val nextTotalSeconds = if (upcoming != null) {
            upcoming.second * 60
        } else {
            24 * 60 * 60 + clockToMinutes(prayers.first().azan) * 60
        }
        return Highlight(next.first, current, nextTotalSeconds - nowTotalSeconds)
    }

    fun formatCountdown(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return listOf(hours, minutes, seconds).joinToString(":") { "%02d".format(it) }
    }
}
