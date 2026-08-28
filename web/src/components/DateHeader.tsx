import { copy } from "@/lib/i18n";
import type { Locale } from "@/lib/types";

type DateHeaderProps = {
  hijriDate: string;
  gregorianDate: string;
  weekday: string | null;
  locale: Locale;
};

export function DateHeader({
  hijriDate,
  gregorianDate,
  weekday,
  locale,
}: DateHeaderProps) {
  const t = copy[locale];

  return (
    <header className="space-y-3">
      <p className="text-xs uppercase tracking-[0.22em] text-accent/80">
        {t.appTitle}
      </p>
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">
          {hijriDate}
        </h1>
        <p className="text-sm text-muted">
          {[weekday, gregorianDate].filter(Boolean).join(" · ")}
        </p>
      </div>
      <p className="text-sm text-muted">{t.appSubtitle}</p>
    </header>
  );
}
