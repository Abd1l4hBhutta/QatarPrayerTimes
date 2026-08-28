import { unstable_cache } from "next/cache";
import { fetchAladhanTimes } from "./aladhan";
import { scrapePrayersQa } from "./scrape-prayers-qa";
import { qatarDateKey } from "./time";
import type { PrayerTimesPayload } from "./types";

export async function getPrayerTimes(): Promise<PrayerTimesPayload> {
  const dateKey = qatarDateKey();

  return unstable_cache(
    async () => loadWithFallback(),
    ["prayer-times", dateKey],
    { revalidate: 86400 },
  )();
}

async function loadWithFallback(): Promise<PrayerTimesPayload> {
  try {
    return await scrapePrayersQa();
  } catch (scrapeError) {
    console.warn("prayers.qa scrape failed, using Aladhan fallback", scrapeError);
    try {
      return await fetchAladhanTimes();
    } catch (fallbackError) {
      console.error("Aladhan fallback failed", fallbackError);
      throw fallbackError;
    }
  }
}
