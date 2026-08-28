import * as cheerio from "cheerio";
import { DEFAULT_AREA_OFFSETS, DEFAULT_IQAMA_OFFSETS } from "./constants";
import { addMinutes, normalizeClock } from "./time";
import type { AreaId, AreaOffsets, PrayerTime, PrayerTimesPayload } from "./types";
import { PRAYER_IDS } from "./types";

/**
 * Isolated prayers.qa scraper.
 *
 * Live markup (v3.1.0) uses the same classes in English and Arabic:
 * - Azan: `tr.qatarptime td` — six `H:MM` cells (Fajr → Isha)
 * - Iqama ("P. Starts" / الإقامة): offsets in the row whose glow label
 *   matches those names (`+25`, `-` for Sunrise, …)
 * - Dates: the centered `tr.qatarptext` with Hijri, weekday, Gregorian
 *   (not `.qatarptextsmall`, which is photo captions)
 * - Area offsets: a `td.qatarptsdif` area label with two `+N` cells in
 *   the same row (Fajr, Maghrib) for AboSamra / Dukhan / Alshamal
 */
export const SELECTORS = {
  azanRow: "tr.qatarptime",
  glowLabel: "td.qatarptextglow",
  dateRow: "tr.qatarptext",
  areaLabel: "td.qatarptsdif",
} as const;

const AREA_ALIASES: Record<string, AreaId> = {
  abosamra: "abu-samra",
  "abo samra": "abu-samra",
  "abu samra": "abu-samra",
  أبوسمره: "abu-samra",
  "أبو سمرة": "abu-samra",
  dukhan: "dukhan",
  دخان: "dukhan",
  alshamal: "al-shamal",
  "al shamal": "al-shamal",
  الشمال: "al-shamal",
};

const CLOCK_TOKEN = /^\d{1,2}:\d{2}$/;
const CLOCK_GLOBAL = /\d{1,2}:\d{2}/g;
const OFFSET_RE = /^[+\-]?\d+$/;

export async function scrapePrayersQa(): Promise<PrayerTimesPayload> {
  const html = await fetchPrayersQaHtml();
  return parsePrayersQaHtml(html);
}

export function parsePrayersQaHtml(html: string): PrayerTimesPayload {
  const $ = cheerio.load(html);

  const azanRaw = extractAzanTimes($);
  if (azanRaw.length !== 6) {
    throw new Error(`Expected 6 azan times, found ${azanRaw.length}`);
  }

  const iqamaOffsets = extractIqamaOffsets($);
  const prayers = buildPrayers(azanRaw, iqamaOffsets);
  const { hijriDate, gregorianDate, weekday } = extractDates($);
  const areaOffsets = extractAreaOffsets($);

  return {
    source: "prayers.qa",
    fetchedAt: new Date().toISOString(),
    timezone: "Asia/Qatar",
    gregorianDate,
    hijriDate,
    weekday,
    prayers,
    areaOffsets,
  };
}

async function fetchPrayersQaHtml(): Promise<string> {
  const headers = {
    "User-Agent":
      "Mozilla/5.0 (compatible; QatarPrayerTimes/1.0; +https://prayers.qa)",
    Accept: "text/html,application/xhtml+xml",
    "Accept-Language": "en-US,en;q=0.9,ar;q=0.8",
  };

  const english = await fetch("https://prayers.qa/?lang=en", {
    headers,
    cache: "no-store",
    signal: AbortSignal.timeout(12_000),
  });

  if (english.ok) {
    const html = await english.text();
    if (html.includes("qatarptime") || /\d{1,2}:\d{2}/.test(html)) {
      return html;
    }
  }

  const arabic = await fetch("https://prayers.qa/?lang=ar", {
    headers,
    cache: "no-store",
    signal: AbortSignal.timeout(12_000),
  });

  if (!arabic.ok) {
    throw new Error(`prayers.qa HTTP ${arabic.status}`);
  }

  return arabic.text();
}

function extractAzanTimes($: cheerio.CheerioAPI): string[] {
  const fromRow = $(SELECTORS.azanRow)
    .first()
    .find("td")
    .toArray()
    .map((el) => cleanText($(el).text()))
    .filter((text) => CLOCK_TOKEN.test(text));

  if (fromRow.length === 6) {
    return fromRow;
  }

  const body = $("body").text();
  const afterAthan = body.split(/Athan|الأذان/i)[1] ?? body;
  return [...afterAthan.matchAll(CLOCK_GLOBAL)].slice(0, 6).map((match) => match[0]);
}

function extractIqamaOffsets($: cheerio.CheerioAPI): (number | null)[] {
  const label = $(SELECTORS.glowLabel)
    .filter((_, el) => /P\.?\s*Starts|الإقامة|اقامة/i.test(cleanText($(el).text())))
    .first();

  const cells = label
    .closest("tr")
    .find("td")
    .toArray()
    .map((el) => cleanText($(el).text()))
    .filter((text) => text === "-" || OFFSET_RE.test(text));

  if (cells.length >= 6) {
    return cells.slice(0, 6).map(parseOffset);
  }

  return PRAYER_IDS.map((id) => DEFAULT_IQAMA_OFFSETS[id]);
}

function parseOffset(raw: string): number | null {
  if (raw === "-" || raw === "") {
    return null;
  }
  return Number(raw.replace("+", ""));
}

function buildPrayers(azanRaw: string[], offsets: (number | null)[]): PrayerTime[] {
  const prayers: PrayerTime[] = [];
  let previous = -1;

  for (let i = 0; i < PRAYER_IDS.length; i += 1) {
    const id = PRAYER_IDS[i];
    const azan = normalizeClock(azanRaw[i], previous);
    previous = minutesFromClock(azan);
    const iqamaOffsetMinutes = offsets[i] ?? DEFAULT_IQAMA_OFFSETS[id];
    prayers.push({
      id,
      azan,
      iqamaOffsetMinutes,
      iqama:
        iqamaOffsetMinutes == null ? null : addMinutes(azan, iqamaOffsetMinutes),
    });
  }

  return prayers;
}

function minutesFromClock(clock: string): number {
  const [hours, minutes] = clock.split(":").map(Number);
  return hours * 60 + minutes;
}

function extractDates($: cheerio.CheerioAPI): {
  hijriDate: string;
  gregorianDate: string;
  weekday: string | null;
} {
  const weekdayRe =
    /thursday|friday|saturday|sunday|monday|tuesday|wednesday|الخميس|الجمعة|السبت|الأحد|الاحد|الإثنين|الاثنين|الثلاثاء|الأربعاء|الاربعاء/i;

  let hijriDate = "";
  let gregorianDate = "";
  let weekday: string | null = null;

  $(SELECTORS.dateRow).each((_, el) => {
    const cells = $(el)
      .find("td")
      .toArray()
      .map((td) => cleanText($(td).text()))
      .filter(Boolean);

    if (cells.length < 2) {
      return;
    }

    const hijri = cells.find((text) => /14\d{2}|هـ/.test(text) && !/حقوق|Copyright/i.test(text));
    const gregorian = cells.find(
      (text) => /20\d{2}|م$/.test(text) && text !== hijri && !/حقوق|Copyright/i.test(text),
    );
    const day = cells.find((text) => weekdayRe.test(text.replace(/\u0640/g, "")));

    if (hijri && gregorian) {
      hijriDate = hijri;
      gregorianDate = gregorian;
      weekday = day ? day.replace(/\u0640/g, "") : null;
      return false;
    }
  });

  if (!hijriDate || !gregorianDate) {
    throw new Error("Could not parse Hijri/Gregorian dates from prayers.qa");
  }

  return { hijriDate, gregorianDate, weekday };
}

function extractAreaOffsets($: cheerio.CheerioAPI): AreaOffsets {
  const offsets: AreaOffsets = structuredClone(DEFAULT_AREA_OFFSETS);

  $(SELECTORS.areaLabel).each((_, el) => {
    const label = cleanText($(el).text());
    const area = resolveArea(label);
    if (!area || area === "doha") {
      return;
    }

    const numbers = $(el)
      .closest("tr")
      .find("td")
      .toArray()
      .map((td) => cleanText($(td).text()))
      .filter((text) => OFFSET_RE.test(text) && text !== label)
      .map((text) => Number(text.replace("+", "")));

    if (numbers.length >= 2) {
      offsets[area] = { fajr: numbers[0], maghrib: numbers[1] };
    }
  });

  return offsets;
}

function resolveArea(raw: string): AreaId | null {
  const key = raw.replace(/\u0640/g, "").toLowerCase();
  for (const [alias, area] of Object.entries(AREA_ALIASES)) {
    if (key.includes(alias.toLowerCase())) {
      return area;
    }
  }
  return null;
}

function cleanText(value: string): string {
  return value.replace(/\u00a0/g, " ").replace(/\s+/g, " ").trim();
}
