package com.example.qatarprayertimes.data

import android.content.Context

class PreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun read(): UserPreferences = UserPreferences(
        area = prefs.getString(KEY_AREA, null)?.let { runCatching { AreaId.valueOf(it) }.getOrNull() } ?: AreaId.DOHA,
        timeFormat = if (prefs.getString(KEY_FORMAT, TimeFormat.H12.name) == TimeFormat.H24.name) TimeFormat.H24 else TimeFormat.H12,
        viewMode = if (prefs.getString(KEY_VIEW, ViewMode.BOTH.name) == ViewMode.AZAN.name) ViewMode.AZAN else ViewMode.BOTH,
        locale = if (prefs.getString(KEY_LOCALE, AppLocale.EN.name) == AppLocale.AR.name) AppLocale.AR else AppLocale.EN,
    )

    fun write(preferences: UserPreferences) {
        prefs.edit()
            .putString(KEY_AREA, preferences.area.name)
            .putString(KEY_FORMAT, preferences.timeFormat.name)
            .putString(KEY_VIEW, preferences.viewMode.name)
            .putString(KEY_LOCALE, preferences.locale.name)
            .apply()
    }

    companion object {
        private const val KEY_AREA = "area"
        private const val KEY_FORMAT = "time_format"
        private const val KEY_VIEW = "view_mode"
        private const val KEY_LOCALE = "locale"
    }
}
