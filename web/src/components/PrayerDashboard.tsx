"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { AreaSelector } from "@/components/AreaSelector";
import { CountdownTimer } from "@/components/CountdownTimer";
import { DateHeader } from "@/components/DateHeader";
import { PrayerCard } from "@/components/PrayerCard";
import { SettingsPanel } from "@/components/SettingsPanel";
import { PREFS_STORAGE_KEY } from "@/lib/constants";
import { copy } from "@/lib/i18n";
import { applyAreaOffsets, getNextAndCurrent, nowInQatar } from "@/lib/time";
import type {
  AreaId,
  Locale,
  PrayerTimesPayload,
  TimeFormat,
  UserPreferences,
  ViewMode,
} from "@/lib/types";

const DEFAULT_PREFS: UserPreferences = {
  area: "doha",
  timeFormat: "12h",
  viewMode: "both",
  locale: "en",
};

export function PrayerDashboard() {
  const [data, setData] = useState<PrayerTimesPayload | null>(null);
  const [error, setError] = useState(false);
  const [loading, setLoading] = useState(true);
  const [prefs, setPrefs] = useState<UserPreferences>(DEFAULT_PREFS);
  const [prefsReady, setPrefsReady] = useState(false);
  const [nowMinutes, setNowMinutes] = useState(() => nowInQatar().minutes);

  useEffect(() => {
    const stored = window.localStorage.getItem(PREFS_STORAGE_KEY);
    if (stored) {
      try {
        setPrefs({ ...DEFAULT_PREFS, ...JSON.parse(stored) });
      } catch {
        window.localStorage.removeItem(PREFS_STORAGE_KEY);
      }
    }
    setPrefsReady(true);
  }, []);

  useEffect(() => {
    if (!prefsReady) {
      return;
    }
    window.localStorage.setItem(PREFS_STORAGE_KEY, JSON.stringify(prefs));
    document.documentElement.lang = prefs.locale;
    document.documentElement.dir = prefs.locale === "ar" ? "rtl" : "ltr";
  }, [prefs, prefsReady]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(false);
    try {
      const response = await fetch("/api/prayer-times", { cache: "no-store" });
      if (!response.ok) {
        throw new Error("Failed to load prayer times");
      }
      const payload = (await response.json()) as PrayerTimesPayload;
      setData(payload);
    } catch {
      setError(true);
      setData(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    const id = window.setInterval(() => {
      setNowMinutes(nowInQatar().minutes);
    }, 1000);
    return () => window.clearInterval(id);
  }, []);

  const t = copy[prefs.locale];
  const prayers = useMemo(
    () => (data ? applyAreaOffsets(data.prayers, prefs.area, data.areaOffsets) : []),
    [data, prefs.area],
  );
  const highlight = useMemo(
    () => (prayers.length ? getNextAndCurrent(prayers, nowMinutes) : null),
    [prayers, nowMinutes],
  );

  if (loading) {
    return <SkeletonLoader locale={prefs.locale} />;
  }

  if (error || !data || !highlight) {
    return (
      <div className="mx-auto flex min-h-full max-w-xl flex-col justify-center gap-4 px-5 py-16 text-center">
        <h1 className="text-2xl font-semibold">{t.errorTitle}</h1>
        <p className="text-sm text-muted">{t.errorBody}</p>
        <button
          type="button"
          onClick={() => void load()}
          className="mx-auto mt-2 rounded-full bg-accent/15 px-5 py-2 text-sm text-accent"
        >
          {t.retry}
        </button>
      </div>
    );
  }

  return (
    <div
      className="mx-auto flex w-full max-w-2xl flex-col gap-8 px-5 py-10 sm:px-8 sm:py-14"
      lang={prefs.locale === "ar" ? "ar" : "en"}
    >
      <DateHeader
        hijriDate={data.hijriDate}
        gregorianDate={data.gregorianDate}
        weekday={data.weekday}
        locale={prefs.locale}
      />

      <CountdownTimer
        prayers={prayers}
        locale={prefs.locale}
        timeFormat={prefs.timeFormat}
        nextId={highlight.next}
      />

      <AreaSelector
        value={prefs.area}
        locale={prefs.locale}
        onChange={(area) => setPrefs((current) => ({ ...current, area }))}
      />

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {prayers.map((prayer) => (
          <PrayerCard
            key={prayer.id}
            prayer={prayer}
            locale={prefs.locale}
            timeFormat={prefs.timeFormat}
            viewMode={prefs.viewMode}
            isNext={highlight.next === prayer.id}
            isCurrent={highlight.current === prayer.id}
          />
        ))}
      </div>

      <SettingsPanel
        locale={prefs.locale}
        timeFormat={prefs.timeFormat}
        viewMode={prefs.viewMode}
        onTimeFormatChange={(timeFormat: TimeFormat) =>
          setPrefs((current) => ({ ...current, timeFormat }))
        }
        onViewModeChange={(viewMode: ViewMode) =>
          setPrefs((current) => ({ ...current, viewMode }))
        }
        onLocaleChange={(locale: Locale) =>
          setPrefs((current) => ({ ...current, locale }))
        }
      />

      <p className="pb-4 text-center text-xs text-muted">
        {data.source === "prayers.qa" ? t.sourcePrayersQa : t.sourceAladhan}
        <span className="mx-2">·</span>
        {t.timezone}
      </p>
    </div>
  );
}

function SkeletonLoader({ locale }: { locale: Locale }) {
  const t = copy[locale];
  return (
    <div className="mx-auto w-full max-w-2xl px-5 py-10 sm:px-8 sm:py-14" aria-busy="true">
      <p className="sr-only">{t.loading}</p>
      <div className="space-y-8">
        <div className="space-y-3">
          <div className="h-3 w-40 animate-pulse rounded bg-card" />
          <div className="h-8 w-64 animate-pulse rounded bg-card" />
          <div className="h-4 w-48 animate-pulse rounded bg-card" />
        </div>
        <div className="h-32 animate-pulse rounded-2xl bg-card" />
        <div className="flex gap-2">
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="h-8 w-20 animate-pulse rounded-full bg-card" />
          ))}
        </div>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {Array.from({ length: 6 }).map((_, index) => (
            <div key={index} className="h-24 animate-pulse rounded-2xl bg-card" />
          ))}
        </div>
      </div>
    </div>
  );
}
