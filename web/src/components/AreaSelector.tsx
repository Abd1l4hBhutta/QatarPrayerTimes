"use client";

import { areaNames, copy } from "@/lib/i18n";
import { AREA_IDS, type AreaId, type Locale } from "@/lib/types";

type AreaSelectorProps = {
  value: AreaId;
  locale: Locale;
  onChange: (area: AreaId) => void;
};

export function AreaSelector({ value, locale, onChange }: AreaSelectorProps) {
  const t = copy[locale];

  return (
    <fieldset>
      <legend className="mb-3 text-xs uppercase tracking-[0.18em] text-muted">
        {t.area}
      </legend>
      <div className="flex flex-wrap gap-2">
        {AREA_IDS.map((area) => {
          const selected = area === value;
          return (
            <button
              key={area}
              type="button"
              onClick={() => onChange(area)}
              className={`rounded-full px-3.5 py-1.5 text-sm transition-colors ${
                selected
                  ? "bg-accent/15 text-accent"
                  : "bg-card text-muted hover:text-foreground"
              }`}
            >
              {areaNames[locale][area]}
            </button>
          );
        })}
      </div>
    </fieldset>
  );
}
