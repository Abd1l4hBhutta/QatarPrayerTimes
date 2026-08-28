"use client";

import { useEffect, useState } from "react";
import { copy, prayerNames } from "@/lib/i18n";
import { formatClock, formatCountdown, getNextAndCurrent, nowInQatar } from "@/lib/time";
import type { Locale, PrayerId, PrayerTime, TimeFormat } from "@/lib/types";

type CountdownTimerProps = {
  prayers: PrayerTime[];
  locale: Locale;
  timeFormat: TimeFormat;
  nextId: PrayerId;
};

export function CountdownTimer({
  prayers,
  locale,
  timeFormat,
  nextId,
}: CountdownTimerProps) {
  const t = copy[locale];
  const [remaining, setRemaining] = useState(0);

  useEffect(() => {
    const tick = () => {
      const now = nowInQatar();
      const { secondsUntilNext } = getNextAndCurrent(
        prayers,
        now.minutes,
        now.seconds,
      );
      setRemaining(secondsUntilNext);
    };

    tick();
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, [prayers]);

  const next = prayers.find((prayer) => prayer.id === nextId);

  return (
    <section className="rounded-2xl bg-card px-5 py-6 sm:px-8 sm:py-8">
      <p className="text-xs uppercase tracking-[0.18em] text-muted">{t.nextPrayer}</p>
      <div className="mt-3 flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-accent sm:text-3xl">
            {prayerNames[locale][nextId]}
          </h2>
          {next && (
            <p className="mt-1 text-sm text-muted">
              {t.azan} {formatClock(next.azan, timeFormat, locale)}
            </p>
          )}
        </div>
        <p className="font-mono text-3xl tabular-nums tracking-tight text-foreground sm:text-4xl">
          {formatCountdown(remaining)}
        </p>
      </div>
    </section>
  );
}
