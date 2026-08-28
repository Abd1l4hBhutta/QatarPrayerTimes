package com.example.qatarprayertimes.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PrayerTimeCache(context: Context) {
    private val prefs = context.getSharedPreferences("prayer_times_cache", Context.MODE_PRIVATE)

    fun readIfToday(): PrayerTimesPayload? {
        val dateKey = prefs.getString(KEY_DATE, null) ?: return null
        if (dateKey != TimeUtils.qatarDateKey()) return null
        return readLatest()
    }

    fun readLatest(): PrayerTimesPayload? {
        val json = prefs.getString(KEY_PAYLOAD, null) ?: return null
        return runCatching { decode(JSONObject(json)) }.getOrNull()
    }

    fun write(payload: PrayerTimesPayload) {
        prefs.edit()
            .putString(KEY_DATE, TimeUtils.qatarDateKey())
            .putString(KEY_PAYLOAD, encode(payload).toString())
            .apply()
    }

    private fun encode(payload: PrayerTimesPayload): JSONObject {
        val prayers = JSONArray()
        payload.prayers.forEach { prayer ->
            prayers.put(
                JSONObject()
                    .put("id", prayer.id.name)
                    .put("azan", prayer.azan)
                    .put("iqama", prayer.iqama ?: JSONObject.NULL)
                    .put("iqamaOffsetMinutes", prayer.iqamaOffsetMinutes ?: JSONObject.NULL),
            )
        }
        val offsets = JSONObject()
        payload.areaOffsets.forEach { (area, offset) ->
            offsets.put(
                area.name,
                JSONObject().put("fajr", offset.fajr).put("maghrib", offset.maghrib),
            )
        }
        return JSONObject()
            .put("source", payload.source.name)
            .put("fetchedAt", payload.fetchedAt)
            .put("gregorianDate", payload.gregorianDate)
            .put("hijriDate", payload.hijriDate)
            .put("weekday", payload.weekday ?: JSONObject.NULL)
            .put("prayers", prayers)
            .put("areaOffsets", offsets)
            .put("jummahFirstCall", payload.jummahFirstCall ?: JSONObject.NULL)
    }

    private fun decode(json: JSONObject): PrayerTimesPayload {
        val prayersArray = json.getJSONArray("prayers")
        val prayers = buildList {
            for (i in 0 until prayersArray.length()) {
                val item = prayersArray.getJSONObject(i)
                add(
                    PrayerTime(
                        id = PrayerId.valueOf(item.getString("id")),
                        azan = item.getString("azan"),
                        iqama = item.optStringOrNull("iqama"),
                        iqamaOffsetMinutes = if (item.isNull("iqamaOffsetMinutes")) null else item.getInt("iqamaOffsetMinutes"),
                    ),
                )
            }
        }
        val offsetJson = json.getJSONObject("areaOffsets")
        val offsets = DEFAULT_AREA_OFFSETS.toMutableMap()
        offsetJson.keys().forEach { key ->
            val value = offsetJson.getJSONObject(key)
            offsets[AreaId.valueOf(key)] = AreaOffset(value.getInt("fajr"), value.getInt("maghrib"))
        }
        return PrayerTimesPayload(
            source = DataSource.valueOf(json.getString("source")),
            fetchedAt = json.getString("fetchedAt"),
            gregorianDate = json.getString("gregorianDate"),
            hijriDate = json.getString("hijriDate"),
            weekday = json.optStringOrNull("weekday"),
            prayers = prayers,
            areaOffsets = offsets,
            jummahFirstCall = json.optStringOrNull("jummahFirstCall"),
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key)
        return value.ifBlank { null }
    }

    companion object {
        private const val KEY_DATE = "date_key"
        private const val KEY_PAYLOAD = "payload"
    }
}
