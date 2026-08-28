export const PRAYER_IDS = [
  "fajr",
  "sunrise",
  "dhuhr",
  "asr",
  "maghrib",
  "isha",
] as const;

export type PrayerId = (typeof PRAYER_IDS)[number];

export const AREA_IDS = ["doha", "abu-samra", "dukhan", "al-shamal"] as const;

export type AreaId = (typeof AREA_IDS)[number];

export type PrayerTime = {
  id: PrayerId;
  azan: string;
  iqama: string | null;
  iqamaOffsetMinutes: number | null;
};

export type AreaOffsets = Record<AreaId, { fajr: number; maghrib: number }>;

export type DataSource = "prayers.qa" | "aladhan";

export type PrayerTimesPayload = {
  source: DataSource;
  fetchedAt: string;
  timezone: "Asia/Qatar";
  gregorianDate: string;
  hijriDate: string;
  weekday: string | null;
  prayers: PrayerTime[];
  areaOffsets: AreaOffsets;
};

export type Locale = "en" | "ar";

export type TimeFormat = "12h" | "24h";

export type ViewMode = "azan" | "both";

export type UserPreferences = {
  area: AreaId;
  timeFormat: TimeFormat;
  viewMode: ViewMode;
  locale: Locale;
};
