package com.example.qatarprayertimes.data

import android.content.Context
import android.util.Log

class PrayerTimesRepository(context: Context) {
    private val cache = PrayerTimeCache(context)

    fun load(forceRefresh: Boolean = false): PrayerTimesPayload {
        if (!forceRefresh) {
            cache.readIfToday()?.let { return it }
        }
        val payload = try {
            PrayersQaScraper.scrape()
        } catch (scrapeError: Exception) {
            Log.w(TAG, "prayers.qa scrape failed, using Aladhan fallback", scrapeError)
            try {
                AladhanClient.fetch()
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Aladhan fallback failed", fallbackError)
                throw fallbackError
            }
        }
        cache.write(payload)
        return payload
    }

    companion object {
        private const val TAG = "PrayerTimesRepository"
    }
}
