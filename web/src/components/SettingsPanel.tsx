"use client";

import { copy } from "@/lib/i18n";
import type { Locale, TimeFormat, ViewMode } from "@/lib/types";

type SettingsPanelProps = {
  locale: Locale;
  timeFormat: TimeFormat;
  viewMode: ViewMode;
  onTimeFormatChange: (value: TimeFormat) => void;
  onViewModeChange: (value: ViewMode) => void;
  onLocaleChange: (value: Locale) => void;
};

export function SettingsPanel({
  locale,
  timeFormat,
  viewMode,
  onTimeFormatChange,
  onViewModeChange,
  onLocaleChange,
}: SettingsPanelProps) {
  const t = copy[locale];

  return (
    <section className="space-y-6 rounded-2xl bg-card px-5 py-6">
      <h2 className="text-xs uppercase tracking-[0.18em] text-muted">{t.settings}</h2>

      <ToggleGroup
        label={t.timeFormat}
        value={timeFormat}
        options={[
          { value: "12h", label: t.hour12 },
          { value: "24h", label: t.hour24 },
        ]}
        onChange={onTimeFormatChange}
      />

      <ToggleGroup
        label={t.display}
        value={viewMode}
        options={[
          { value: "azan", label: t.azanOnly },
          { value: "both", label: t.azanAndIqama },
        ]}
        onChange={onViewModeChange}
      />

      <ToggleGroup
        label={t.language}
        value={locale}
        options={[
          { value: "en", label: t.english },
          { value: "ar", label: t.arabic },
        ]}
        onChange={onLocaleChange}
      />
    </section>
  );
}

function ToggleGroup<T extends string>({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: T;
  options: { value: T; label: string }[];
  onChange: (value: T) => void;
}) {
  return (
    <div>
      <p className="mb-2 text-sm text-muted">{label}</p>
      <div className="flex gap-2">
        {options.map((option) => {
          const selected = option.value === value;
          return (
            <button
              key={option.value}
              type="button"
              onClick={() => onChange(option.value)}
              className={`flex-1 rounded-full px-3 py-2 text-sm transition-colors ${
                selected
                  ? "bg-accent/15 text-accent"
                  : "bg-background text-muted hover:text-foreground"
              }`}
            >
              {option.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
