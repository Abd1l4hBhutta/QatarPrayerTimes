import { QATAR_TIMEZONE } from "./constants";
import type {
  AreaId,
  AreaOffsets,
  Locale,
  PrayerId,
  PrayerTime,
  TimeFormat,
} from "./types";

const CLOCK = /^(\d{1,2}):(\d{2})/;

export function normalizeClock(raw: string, previousMinutes = -1): string {
  const match = raw.trim().match(CLOCK);
  if (!match) {
    throw new Error(`Could not parse clock value: ${raw}`);
  }

  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  let total = hours * 60 + minutes;

  while (previousMinutes >= 0 && total <= previousMinutes) {
    total += 12 * 60;
  }

  total = ((total % 1440) + 1440) % 1440;
  return minutesToClock(total);
}

export function clockToMinutes(clock: string): number {
  const match = clock.trim().match(CLOCK);
  if (!match) {
    throw new Error(`Could not parse clock value: ${clock}`);
  }
  return Number(match[1]) * 60 + Number(match[2]);
}

export function minutesToClock(total: number): string {
  const wrapped = ((total % 1440) + 1440) % 1440;
  const hours = Math.floor(wrapped / 60);
  const minutes = wrapped % 60;
  return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}`;
}

export function addMinutes(clock: string, delta: number): string {
  return minutesToClock(clockToMinutes(clock) + delta);
}

export function formatClock(
  clock: string,
  timeFormat: TimeFormat,
  locale: Locale,
): string {
  const minutes = clockToMinutes(clock);
  const date = new Date(Date.UTC(2020, 0, 1, Math.floor(minutes / 60), minutes % 60));
  return new Intl.DateTimeFormat(locale === "ar" ? "ar-QA" : "en-GB", {
    hour: "numeric",
    minute: "2-digit",
    hour12: timeFormat === "12h",
    timeZone: "UTC",
  }).format(date);
}

export function nowInQatar(): { minutes: number; seconds: number } {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: QATAR_TIMEZONE,
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date());

  const hour = Number(parts.find((part) => part.type === "hour")?.value ?? 0);
  const minute = Number(parts.find((part) => part.type === "minute")?.value ?? 0);
  const second = Number(parts.find((part) => part.type === "second")?.value ?? 0);

  return { minutes: hour * 60 + minute, seconds: second };
}

export function qatarDateKey(date = new Date()): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: QATAR_TIMEZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

export function applyAreaOffsets(
  prayers: PrayerTime[],
  area: AreaId,
  offsets: AreaOffsets,
): PrayerTime[] {
  const areaOffset = offsets[area] ?? offsets.doha;

  return prayers.map((prayer) => {
    const extra =
      prayer.id === "fajr"
        ? areaOffset.fajr
        : prayer.id === "maghrib"
          ? areaOffset.maghrib
          : 0;

    const azan = extra ? addMinutes(prayer.azan, extra) : prayer.azan;
    const iqama =
      prayer.iqamaOffsetMinutes == null
        ? null
        : addMinutes(azan, prayer.iqamaOffsetMinutes);

    return { ...prayer, azan, iqama };
  });
}

export function getNextAndCurrent(
  prayers: PrayerTime[],
  nowMinutes: number,
  nowSeconds = 0,
): { next: PrayerId; current: PrayerId | null; secondsUntilNext: number } {
  const withIndex = prayers.map((prayer) => ({
    id: prayer.id,
    minutes: clockToMinutes(prayer.azan),
  }));

  const upcoming = withIndex.find((prayer) => prayer.minutes > nowMinutes);
  const next = upcoming ?? withIndex[0];
  const currentList = withIndex.filter((prayer) => prayer.minutes <= nowMinutes);
  const current = currentList.at(-1)?.id ?? null;

  const nowTotalSeconds = nowMinutes * 60 + nowSeconds;
  const nextTotalSeconds = upcoming
    ? clockToMinutes(prayers.find((p) => p.id === next.id)!.azan) * 60
    : 24 * 60 * 60 + clockToMinutes(prayers[0].azan) * 60;

  return {
    next: next.id,
    current,
    secondsUntilNext: nextTotalSeconds - nowTotalSeconds,
  };
}

export function formatCountdown(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const hours = Math.floor(safe / 3600);
  const minutes = Math.floor((safe % 3600) / 60);
  const seconds = safe % 60;
  return [hours, minutes, seconds].map((n) => String(n).padStart(2, "0")).join(":");
}
