package com.example.qatarprayertimes.ui

import com.example.qatarprayertimes.data.AppLocale
import com.example.qatarprayertimes.data.AreaId
import com.example.qatarprayertimes.data.PrayerId

data class Copy(
    val appTitle: String,
    val appSubtitle: String,
    val nextPrayer: String,
    val now: String,
    val azan: String,
    val iqama: String,
    val area: String,
    val settings: String,
    val timeFormat: String,
    val hour12: String,
    val hour24: String,
    val display: String,
    val azanOnly: String,
    val azanAndIqama: String,
    val language: String,
    val english: String,
    val arabic: String,
    val loading: String,
    val errorTitle: String,
    val errorBody: String,
    val retry: String,
    val sourcePrayersQa: String,
    val sourceAladhan: String,
    val timezone: String,
    val sunriseNote: String,
    val setSound: String,
    val mute: String,
    val unmute: String,
    val dndOverride: String,
    val dndNote: String,
    val addAzanSound: String,
    val jummahFirstCall: String,
    val firstCallLabel: String,
    val home: String,
    val qibla: String,
    val googleQiblaFinder: String,
    val compassQiblaFinder: String,
)

object Strings {
    val en = Copy(
        appTitle = "Qatar Prayer Times",
        appSubtitle = "Azaan And Iqama Timings For Doha",
        nextPrayer = "Next prayer",
        now = "Now",
        azan = "Azan",
        iqama = "Iqama",
        area = "Area",
        settings = "Settings",
        timeFormat = "Time format",
        hour12 = "12-hour",
        hour24 = "24-hour",
        display = "Display",
        azanOnly = "Azan only",
        azanAndIqama = "Azan + Iqama",
        language = "Language",
        english = "English",
        arabic = "العربية",
        loading = "Loading today's times…",
        errorTitle = "Times are unavailable",
        errorBody = "We could not reach prayers.qa or the Aladhan fallback. Check your connection and try again.",
        retry = "Try again",
        sourcePrayersQa = "Source: prayers.qa",
        sourceAladhan = "Fallback source: Aladhan (Qatar method)",
        timezone = "Times in Arabia Standard Time (Qatar)",
        sunriseNote = "Sunrise has no iqama",
        setSound = "Set sound",
        mute = "Mute",
        unmute = "Unmute",
        dndOverride = "Bypass DND / Silent",
        dndNote = "Allow the app to play azan even when phone is on silent or Do Not Disturb.",
        addAzanSound = "Add Azan Sound",
        jummahFirstCall = "The first call to prayer for Friday prayers is at",
        firstCallLabel = "First Call For Jum'mah Prayer",
        home = "Home",
        qibla = "Qibla",
        googleQiblaFinder = "Google Qibla",
        compassQiblaFinder = "Compass Qibla",
    )

    val ar = Copy(
        appTitle = "مواقيت الصلاة في قطر",
        appSubtitle = "مواقيت الأذان والإقامة للدوحة",
        nextPrayer = "الصلاة القادمة",
        now = "الآن",
        azan = "الأذان",
        iqama = "الإقامة",
        area = "المنطقة",
        settings = "الإعدادات",
        timeFormat = "صيغة الوقت",
        hour12 = "١٢ ساعة",
        hour24 = "٢٤ ساعة",
        display = "العرض",
        azanOnly = "الأذان فقط",
        azanAndIqama = "الأذان والإقامة",
        language = "اللغة",
        english = "English",
        arabic = "العربية",
        loading = "جاري تحميل مواقيت اليوم…",
        errorTitle = "تعذر عرض المواقيت",
        errorBody = "لم نتمكن من الوصول إلى prayers.qa أو المصدر الاحتياطي. تحقق من الاتصال ثم أعد المحاولة.",
        retry = "إعادة المحاولة",
        sourcePrayersQa = "المصدر: prayers.qa",
        sourceAladhan = "المصدر الاحتياطي: Aladhan (طريقة قطر)",
        timezone = "التوقيت: توقيت قطر",
        sunriseNote = "لا إقامة للشروق",
        setSound = "ضبط الصوت",
        mute = "كتم",
        unmute = "إلغاء الكتم",
        dndOverride = "تجاوز وضع الصامت / عدم الإزعاج",
        dndNote = "السماح للتطبيق بتشغيل الأذان حتى عندما يكون الهاتف في وضع الصامت أو عدم الإزعاج.",
        addAzanSound = "إضافة صوت الأذان",
        jummahFirstCall = "النداء الأول لصلاة الجمعة عند الساعة",
        firstCallLabel = "النداء الأول لصلاة الجمعة",
        home = "الرئيسية",
        qibla = "القبلة",
        googleQiblaFinder = "جوجل القبلة",
        compassQiblaFinder = "بوصلة القبلة",
    )

    fun copy(locale: AppLocale) = if (locale == AppLocale.AR) ar else en

    fun prayerName(locale: AppLocale, id: PrayerId): String = when (locale) {
        AppLocale.EN -> when (id) {
            PrayerId.FAJR -> "Fajr"
            PrayerId.SUNRISE -> "Sunrise"
            PrayerId.DHUHR -> "Dhuhr"
            PrayerId.ASR -> "Asr"
            PrayerId.MAGHRIB -> "Maghrib"
            PrayerId.ISHA -> "Isha"
        }
        AppLocale.AR -> when (id) {
            PrayerId.FAJR -> "الفجر"
            PrayerId.SUNRISE -> "الشروق"
            PrayerId.DHUHR -> "الظهر"
            PrayerId.ASR -> "العصر"
            PrayerId.MAGHRIB -> "المغرب"
            PrayerId.ISHA -> "العشاء"
        }
    }

    fun areaName(locale: AppLocale, id: AreaId): String = when (locale) {
        AppLocale.EN -> when (id) {
            AreaId.DOHA -> "Doha"
        }
        AppLocale.AR -> when (id) {
            AreaId.DOHA -> "الدوحة"
        }
    }
}
