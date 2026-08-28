import type { AreaId, Locale, PrayerId } from "./types";

export const copy = {
  en: {
    appTitle: "Qatar Prayer Times",
    appSubtitle: "Azan and iqama for Doha and outlying areas",
    nextPrayer: "Next prayer",
    now: "Now",
    azan: "Azan",
    iqama: "Iqama",
    area: "Area",
    settings: "Settings",
    timeFormat: "Time format",
    hour12: "12-hour",
    hour24: "24-hour",
    display: "Display",
    azanOnly: "Azan only",
    azanAndIqama: "Azan + Iqama",
    language: "Language",
    english: "English",
    arabic: "العربية",
    loading: "Loading today's times…",
    errorTitle: "Times are unavailable",
    errorBody:
      "We could not reach prayers.qa or the Aladhan fallback. Check your connection and try again.",
    retry: "Try again",
    sourcePrayersQa: "Source: prayers.qa",
    sourceAladhan: "Fallback source: Aladhan (Qatar method)",
    timezone: "Times in Arabia Standard Time (Qatar)",
    sunriseNote: "Sunrise has no iqama",
  },
  ar: {
    appTitle: "مواقيت الصلاة في قطر",
    appSubtitle: "الأذان والإقامة للدوحة والمناطق المجاورة",
    nextPrayer: "الصلاة القادمة",
    now: "الآن",
    azan: "الأذان",
    iqama: "الإقامة",
    area: "المنطقة",
    settings: "الإعدادات",
    timeFormat: "صيغة الوقت",
    hour12: "١٢ ساعة",
    hour24: "٢٤ ساعة",
    display: "العرض",
    azanOnly: "الأذان فقط",
    azanAndIqama: "الأذان والإقامة",
    language: "اللغة",
    english: "English",
    arabic: "العربية",
    loading: "جاري تحميل مواقيت اليوم…",
    errorTitle: "تعذر عرض المواقيت",
    errorBody:
      "لم نتمكن من الوصول إلى prayers.qa أو المصدر الاحتياطي. تحقق من الاتصال ثم أعد المحاولة.",
    retry: "إعادة المحاولة",
    sourcePrayersQa: "المصدر: prayers.qa",
    sourceAladhan: "المصدر الاحتياطي: Aladhan (طريقة قطر)",
    timezone: "التوقيت: توقيت قطر",
    sunriseNote: "لا إقامة للشروق",
  },
} as const satisfies Record<Locale, Record<string, string>>;

export const prayerNames: Record<Locale, Record<PrayerId, string>> = {
  en: {
    fajr: "Fajr",
    sunrise: "Sunrise",
    dhuhr: "Dhuhr",
    asr: "Asr",
    maghrib: "Maghrib",
    isha: "Isha",
  },
  ar: {
    fajr: "الفجر",
    sunrise: "الشروق",
    dhuhr: "الظهر",
    asr: "العصر",
    maghrib: "المغرب",
    isha: "العشاء",
  },
};

export const areaNames: Record<Locale, Record<AreaId, string>> = {
  en: {
    doha: "Doha",
    "abu-samra": "Abu Samra",
    dukhan: "Dukhan",
    "al-shamal": "Al Shamal",
  },
  ar: {
    doha: "الدوحة",
    "abu-samra": "أبو سمرة",
    dukhan: "دخان",
    "al-shamal": "الشمال",
  },
};
