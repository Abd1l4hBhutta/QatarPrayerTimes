import { DEFAULT_AREA_OFFSETS, DEFAULT_IQAMA_OFFSETS } from "./constants";
import { addMinutes, normalizeClock } from "./time";
import type { PrayerTime, PrayerTimesPayload } from "./types";
import { PRAYER_IDS } from "./types";

const ALADHAN_URL =
  "https://api.aladhan.com/v1/timingsByCity?city=Doha&country=Qatar&method=10";

type AladhanResponse = {
  data?: {
    timings?: Record<string, string>;
    date?: {
      readable?: string;
      hijri?: {
        date?: string;
        day?: string;
        year?: string;
        weekday?: { en?: string; ar?: string };
        month?: { en?: string; ar?: string };
      };
      gregorian?: {
        date?: string;
        weekday?: { en?: string };
      };
    };
  };
};

export async function fetchAladhanTimes(): Promise<PrayerTimesPayload> {
  const response = await fetch(ALADHAN_URL, {
    cache: "no-store",
    signal: AbortSignal.timeout(12_000),
  });
  if (!response.ok) {
    throw new Error(`Aladhan HTTP ${response.status}`);
  }

  const json = (await response.json()) as AladhanResponse;
  const timings = json.data?.timings;
  if (!timings) {
    throw new Error("Aladhan response missing timings");
  }

  const keys = ["Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"] as const;
  const prayers: PrayerTime[] = [];
  let previous = -1;

  for (let i = 0; i < PRAYER_IDS.length; i += 1) {
    const id = PRAYER_IDS[i];
    const raw = timings[keys[i]]?.split(" ")[0];
    if (!raw) {
      throw new Error(`Aladhan missing ${keys[i]}`);
    }
    const azan = normalizeClock(raw, previous);
    previous = clockMinutes(azan);
    const iqamaOffsetMinutes = DEFAULT_IQAMA_OFFSETS[id];
    prayers.push({
      id,
      azan,
      iqamaOffsetMinutes,
      iqama:
        iqamaOffsetMinutes == null ? null : addMinutes(azan, iqamaOffsetMinutes),
    });
  }

  const hijri = json.data?.date?.hijri;
  const hijriDate = hijri
    ? `${hijri.day ?? ""} ${hijri.month?.en ?? ""} ${hijri.year ?? ""}`.trim()
    : json.data?.date?.readable ?? "";
  const gregorianDate = json.data?.date?.readable ?? json.data?.date?.gregorian?.date ?? "";

  return {
    source: "aladhan",
    fetchedAt: new Date().toISOString(),
    timezone: "Asia/Qatar",
    gregorianDate,
    hijriDate,
    weekday: json.data?.date?.gregorian?.weekday?.en ?? hijri?.weekday?.en ?? null,
    prayers,
    areaOffsets: DEFAULT_AREA_OFFSETS,
  };
}

function clockMinutes(clock: string): number {
  const [hours, minutes] = clock.split(":").map(Number);
  return hours * 60 + minutes;
}
