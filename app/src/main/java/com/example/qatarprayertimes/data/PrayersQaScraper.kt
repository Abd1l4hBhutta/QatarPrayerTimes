package com.example.qatarprayertimes.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Isolated prayers.qa scraper.
 *
 * Selectors (English `?lang=en` and Arabic share the same classes):
 * - Azan: `tr.qatarptime td` — six `H:MM` cells
 * - Iqama ("P. Starts" / الإقامة): offset cells in that labeled row
 * - Dates: `tr.qatarptext` with Hijri, weekday, Gregorian
 * - Area offsets: `td.qatarptsdif` labels with two `+N` cells (Fajr, Maghrib)
 */
object PrayersQaScraper {
    private const val AZAN_ROW = "tr.qatarptime"
    private const val GLOW_LABEL = "td.qatarptextglow"
    private const val DATE_ROW = "tr.qatarptext"
    private const val AREA_LABEL = "td.qatarptsdif"
    private val CLOCK_TOKEN = Regex("""^\d{1,2}:\d{2}$""")
    private val CLOCK_GLOBAL = Regex("""\d{1,2}:\d{2}""")
    private val OFFSET_RE = Regex("""^[+\-]?\d+$""")
    private val WEEKDAY_RE = Regex(
        "thursday|friday|saturday|sunday|monday|tuesday|wednesday|" +
            "الخميس|الجمعة|السبت|الأحد|الاحد|الإثنين|الاثنين|الثلاثاء|الأربعاء|الاربعاء",
        RegexOption.IGNORE_CASE,
    )

    private val AREA_ALIASES = emptyMap<String, AreaId>()

    fun scrape(): PrayerTimesPayload {
        val html = fetchHtml()
        return parse(html)
    }

    fun parse(html: String): PrayerTimesPayload {
        val doc = Jsoup.parse(html)
        val azanRaw = extractAzanTimes(doc)
        require(azanRaw.size == 6) { "Expected 6 azan times, found ${azanRaw.size}" }
        val offsets = extractIqamaOffsets(doc)
        val prayers = buildPrayers(azanRaw, offsets)
        val dates = extractDates(doc)
        
        val payload = PrayerTimesPayload(
            source = DataSource.PRAYERS_QA,
            fetchedAt = TimeUtils.qatarDateKey(),
            gregorianDate = dates.gregorian,
            hijriDate = dates.hijri,
            weekday = dates.weekday,
            prayers = prayers,
            areaOffsets = extractAreaOffsets(doc),
            jummahFirstCall = extractJummahFirstCall(doc),
        )
        
        // Robustness: For Friday, calculate first call as Dhuhr Iqama Time - 60 minutes
        val isFri = dates.weekday?.contains("Friday", ignoreCase = true) == true || 
                   dates.weekday?.contains("الجمعة") == true
        
        return if (isFri) {
            val dhuhr = prayers.find { it.id == PrayerId.DHUHR }
            if (dhuhr != null) {
                val azanMinutes = TimeUtils.clockToMinutes(dhuhr.azan)
                val iqamaOffset = dhuhr.iqamaOffsetMinutes ?: 20
                val firstCallMinutes = azanMinutes + iqamaOffset - 60
                payload.copy(jummahFirstCall = TimeUtils.minutesToClock(firstCallMinutes))
            } else {
                payload
            }
        } else {
            payload
        }
    }

    private fun extractJummahFirstCall(doc: Document): String? {
        val text = doc.body().text()
        val patterns = listOf(
            Regex("""(?:First Call|النداء الأول|Call 1).*?(\d{1,2}:\d{2})""", RegexOption.IGNORE_CASE),
            Regex("""(\d{1,2}:\d{2}).*?(?:First Call|النداء الأول)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1]
        }
        
        doc.select("tr, td").forEach { el ->
            val content = el.text()
            if (content.contains("النداء الأول") || content.contains("First Call")) {
                val timeMatch = CLOCK_GLOBAL.find(content)
                if (timeMatch != null) return timeMatch.value
            }
        }
        
        return null
    }

    private fun fetchHtml(): String {
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (compatible; QatarPrayerTimes/1.0; +https://prayers.qa)",
            "Accept-Language" to "en-US,en;q=0.9,ar;q=0.8",
        )
        val english = connect("https://prayers.qa/?lang=en", headers)
        if (english.contains("qatarptime") || CLOCK_GLOBAL.containsMatchIn(english)) {
            return english
        }
        return connect("https://prayers.qa/?lang=ar", headers)
    }

    private fun connect(url: String, headers: Map<String, String>): String {
        var req = Jsoup.connect(url)
            .userAgent(headers.getValue("User-Agent"))
            .header("Accept-Language", headers.getValue("Accept-Language"))
            .timeout(12_000)
            .ignoreHttpErrors(true)
        val response = req.execute()
        require(response.statusCode() in 200..299) { "prayers.qa HTTP ${response.statusCode()}" }
        return response.body()
    }

    private fun extractAzanTimes(doc: Document): List<String> {
        val fromRow = doc.select("$AZAN_ROW").first()
            ?.select("td")
            ?.map { clean(it.text()) }
            ?.filter { CLOCK_TOKEN.matches(it) }
            .orEmpty()
        if (fromRow.size == 6) return fromRow
        val body = doc.body().text()
        val afterAthan = body.split(Regex("Athan|الأذان", RegexOption.IGNORE_CASE)).getOrElse(1) { body }
        return CLOCK_GLOBAL.findAll(afterAthan).take(6).map { it.value }.toList()
    }

    private fun extractIqamaOffsets(doc: Document): List<Int?> {
        val label = doc.select(GLOW_LABEL).firstOrNull { el ->
            Regex("P\\.?\\s*Starts|الإقامة|اقامة", RegexOption.IGNORE_CASE).containsMatchIn(clean(el.text()))
        } ?: return PRAYER_IDS.map { DEFAULT_IQAMA_OFFSETS[it] }

        val cells = label.closest("tr")
            ?.select("td")
            ?.map { clean(it.text()) }
            ?.filter { it == "-" || OFFSET_RE.matches(it) }
            .orEmpty()

        return if (cells.size >= 6) {
            cells.take(6).map { parseOffset(it) }
        } else {
            PRAYER_IDS.map { DEFAULT_IQAMA_OFFSETS[it] }
        }
    }

    private fun parseOffset(raw: String): Int? {
        if (raw == "-" || raw.isEmpty()) return null
        return raw.replace("+", "").toInt()
    }

    private fun buildPrayers(azanRaw: List<String>, offsets: List<Int?>): List<PrayerTime> {
        var previous = -1
        return PRAYER_IDS.mapIndexed { i, id ->
            val azan = TimeUtils.normalizeClock(azanRaw[i], previous)
            previous = TimeUtils.clockToMinutes(azan)
            val iqamaOffset = offsets.getOrNull(i) ?: DEFAULT_IQAMA_OFFSETS[id]
            PrayerTime(
                id = id,
                azan = azan,
                iqamaOffsetMinutes = iqamaOffset,
                iqama = iqamaOffset?.let { TimeUtils.addMinutes(azan, it) },
            )
        }
    }

    private data class Dates(val hijri: String, val gregorian: String, val weekday: String?)

    private fun extractDates(doc: Document): Dates {
        doc.select(DATE_ROW).forEach { row ->
            val cells = row.select("td").map { clean(it.text()) }.filter { it.isNotBlank() }
            if (cells.size < 2) return@forEach
            val hijri = cells.firstOrNull { (it.contains(Regex("14\\d{2}")) || it.contains("هـ")) && !it.contains("حقوق", ignoreCase = true) && !it.contains("Copyright", ignoreCase = true) }
            val gregorian = cells.firstOrNull {
                it != hijri &&
                    (it.contains(Regex("20\\d{2}")) || it.endsWith("م")) &&
                    !it.contains("حقوق", ignoreCase = true)
            }
            val day = cells.firstOrNull { WEEKDAY_RE.containsMatchIn(it.replace("\u0640", "")) }
            if (hijri != null && gregorian != null) {
                return Dates(hijri, gregorian, day?.replace("\u0640", ""))
            }
        }
        error("Could not parse Hijri/Gregorian dates from prayers.qa")
    }

    private fun extractAreaOffsets(doc: Document): Map<AreaId, AreaOffset> {
        return DEFAULT_AREA_OFFSETS
    }

    private fun resolveArea(raw: String): AreaId? {
        return if (raw.lowercase().contains("doha") || raw.contains("الدوحة")) AreaId.DOHA else null
    }

    private fun clean(value: String): String = value.replace("\u00a0", " ").replace(Regex("\\s+"), " ").trim()
}
