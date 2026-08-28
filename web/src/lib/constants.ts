import type { AreaOffsets, PrayerId } from "./types";

export const QATAR_TIMEZONE = "Asia/Qatar" as const;

/** Typical Doha iqama offsets used when Aladhan has no iqama data. */
export const DEFAULT_IQAMA_OFFSETS: Record<PrayerId, number | null> = {
  fajr: 25,
  sunrise: null,
  dhuhr: 20,
  asr: 25,
  maghrib: 10,
  isha: 20,
};

/** Fallback area offsets from prayers.qa if the time-diff table cannot be parsed. */
export const DEFAULT_AREA_OFFSETS: AreaOffsets = {
  doha: { fajr: 0, maghrib: 0 },
  "abu-samra": { fajr: 5, maghrib: 2 },
  dukhan: { fajr: 4, maghrib: 3 },
  "al-shamal": { fajr: 1, maghrib: 2 },
};

export const PREFS_STORAGE_KEY = "qatar-prayer-times-prefs";
