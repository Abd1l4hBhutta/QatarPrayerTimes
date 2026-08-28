package com.example.qatarprayertimes.data

enum class PrayerId { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

enum class AreaId { DOHA }

enum class AppLocale { EN, AR }

enum class TimeFormat { H12, H24 }

enum class ViewMode { AZAN, BOTH }

enum class DataSource { PRAYERS_QA, ALADHAN }

enum class SoundType { STOCK, CUSTOM }

data class AzanSoundState(
    val fileName: String?,
    val muted: Boolean,
    val soundId: String? = null,
    val type: SoundType = SoundType.STOCK,
)

data class PrayerTime(
    val id: PrayerId,
    val azan: String,
    val iqama: String?,
    val iqamaOffsetMinutes: Int?,
)

data class AreaOffset(
    val fajr: Int,
    val maghrib: Int,
)

data class PrayerTimesPayload(
    val source: DataSource,
    val fetchedAt: String,
    val gregorianDate: String,
    val hijriDate: String,
    val weekday: String?,
    val prayers: List<PrayerTime>,
    val areaOffsets: Map<AreaId, AreaOffset>,
    val jummahFirstCall: String? = null,
)

data class UserPreferences(
    val area: AreaId = AreaId.DOHA,
    val timeFormat: TimeFormat = TimeFormat.H12,
    val viewMode: ViewMode = ViewMode.BOTH,
    val locale: AppLocale = AppLocale.EN,
)

val PRAYER_IDS = PrayerId.entries
val AREA_IDS = AreaId.entries

val DEFAULT_IQAMA_OFFSETS: Map<PrayerId, Int?> = mapOf(
    PrayerId.FAJR to 25,
    PrayerId.SUNRISE to null,
    PrayerId.DHUHR to 20,
    PrayerId.ASR to 25,
    PrayerId.MAGHRIB to 10,
    PrayerId.ISHA to 20,
)

val DEFAULT_AREA_OFFSETS: Map<AreaId, AreaOffset> = mapOf(
    AreaId.DOHA to AreaOffset(0, 0),
)
