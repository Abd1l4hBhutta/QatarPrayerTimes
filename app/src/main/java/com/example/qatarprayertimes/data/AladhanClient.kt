package com.example.qatarprayertimes.data

import org.json.JSONObject
import org.jsoup.Jsoup

object AladhanClient {
    private const val URL =
        "https://api.aladhan.com/v1/timingsByCity?city=Doha&country=Qatar&method=10"

    private val KEYS = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")

    fun fetch(): PrayerTimesPayload {
        val body = Jsoup.connect(URL)
            .ignoreContentType(true)
            .timeout(12_000)
            .execute()
            .body()
        val json = JSONObject(body)
        val data = json.optJSONObject("data") ?: error("Aladhan response missing data")
        val timings = data.optJSONObject("timings") ?: error("Aladhan response missing timings")
        var previous = -1
        val prayers = PRAYER_IDS.mapIndexed { i, id ->
            val raw = timings.optString(KEYS[i]).substringBefore(" ")
            require(raw.isNotBlank()) { "Aladhan missing ${KEYS[i]}" }
            val azan = TimeUtils.normalizeClock(raw, previous)
            previous = TimeUtils.clockToMinutes(azan)
            val iqamaOffset = DEFAULT_IQAMA_OFFSETS[id]
            PrayerTime(
                id = id,
                azan = azan,
                iqamaOffsetMinutes = iqamaOffset,
                iqama = iqamaOffset?.let { TimeUtils.addMinutes(azan, it) },
            )
        }
        val date = data.optJSONObject("date")
        val hijri = date?.optJSONObject("hijri")
        val gregorian = date?.optJSONObject("gregorian")
        val hijriDate = if (hijri != null) {
            listOf(
                hijri.optString("day"),
                hijri.optJSONObject("month")?.optString("en").orEmpty(),
                hijri.optString("year"),
            ).joinToString(" ").trim()
        } else {
            date?.optString("readable").orEmpty()
        }
        return PrayerTimesPayload(
            source = DataSource.ALADHAN,
            fetchedAt = TimeUtils.qatarDateKey(),
            gregorianDate = date?.optString("readable")
                ?: gregorian?.optString("date").orEmpty(),
            hijriDate = hijriDate,
            weekday = gregorian?.optJSONObject("weekday")?.optString("en")
                ?: hijri?.optJSONObject("weekday")?.optString("en"),
            prayers = prayers,
            areaOffsets = DEFAULT_AREA_OFFSETS,
        )
    }
}
