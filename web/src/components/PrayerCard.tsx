"use client";

import { PrayerIcon } from "@/components/PrayerIcon";
import { copy, prayerNames } from "@/lib/i18n";
import { formatClock } from "@/lib/time";
import type { Locale, PrayerTime, TimeFormat, ViewMode } from "@/lib/types";

type PrayerCardProps = {
  prayer: PrayerTime;
  locale: Locale;
  timeFormat: TimeFormat;
  viewMode: ViewMode;
  isNext: boolean;
  isCurrent: boolean;
};

export function PrayerCard({
  prayer,
  locale,
  timeFormat,
  viewMode,
  isNext,
  isCurrent,
}: PrayerCardProps) {
  const t = copy[locale];
  const showIqama = viewMode === "both" && prayer.iqama;

  return (
    <article
      className={`rounded-2xl px-4 py-4 sm:px-5 sm:py-5 transition-colors ${
        isNext
          ? "bg-accent/10 text-foreground"
          : "bg-card text-foreground/90"
      }`}
    >
      <div className="flex items-center gap-4">
        <div
          className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full ${
            isNext ? "text-accent" : "text-muted"
          }`}
        >
          <PrayerIcon id={prayer.id} className="h-6 w-6" />
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <h3
              className={`truncate ${
                isNext ? "text-base font-semibold" : "text-sm font-medium text-muted"
              }`}
            >
              {prayerNames[locale][prayer.id]}
            </h3>
            {isCurrent && !isNext && (
              <span className="rounded-full bg-foreground/8 px-2 py-0.5 text-[10px] uppercase tracking-wide text-muted">
                {t.now}
              </span>
            )}
            {isNext && (
              <span className="rounded-full bg-accent/15 px-2 py-0.5 text-[10px] uppercase tracking-wide text-accent">
                {t.nextPrayer}
              </span>
            )}
          </div>

          <div className="mt-1 flex flex-wrap items-baseline gap-x-5 gap-y-1">
            <p className={isNext ? "text-2xl font-semibold tracking-tight sm:text-3xl" : "text-lg tabular-nums"}>
              <span className="sr-only">{t.azan}: </span>
              {formatClock(prayer.azan, timeFormat, locale)}
            </p>
            {showIqama && (
              <p className="text-sm text-muted">
                <span className="me-1.5 opacity-70">{t.iqama}</span>
                <span className="tabular-nums">
                  {formatClock(prayer.iqama!, timeFormat, locale)}
                </span>
              </p>
            )}
            {viewMode === "both" && !prayer.iqama && (
              <p className="text-xs text-muted">{t.sunriseNote}</p>
            )}
          </div>
        </div>
      </div>
    </article>
  );
}
