import type { PrayerId } from "@/lib/types";

const stroke = {
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.5,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
};

export function PrayerIcon({
  id,
  className,
}: {
  id: PrayerId;
  className?: string;
}) {
  return (
    <svg
      viewBox="0 0 24 24"
      className={className}
      aria-hidden="true"
      focusable="false"
    >
      {id === "fajr" && (
        <>
          <path {...stroke} d="M4 16.5h16" />
          <path {...stroke} d="M12 16.5V11" />
          <path {...stroke} d="M8 13.5 12 11l4 2.5" />
          <path {...stroke} d="M6.5 8.5 5 7.5M12 6.5V5M17.5 8.5 19 7.5" />
        </>
      )}
      {id === "sunrise" && (
        <>
          <path {...stroke} d="M4 17h16" />
          <path {...stroke} d="M12 14a4 4 0 0 1 4-4 4 4 0 0 1-4-4" />
          <path {...stroke} d="M12 14a4 4 0 0 0-4-4 4 4 0 0 0 4-4" />
          <path {...stroke} d="M12 6V4" />
        </>
      )}
      {id === "dhuhr" && (
        <>
          <circle {...stroke} cx="12" cy="12" r="3.5" />
          <path {...stroke} d="M12 4v2.5M12 17.5V20M4 12h2.5M17.5 12H20M6.2 6.2l1.8 1.8M16 16l1.8 1.8M6.2 17.8 8 16M16 8l1.8-1.8" />
        </>
      )}
      {id === "asr" && (
        <>
          <circle {...stroke} cx="12" cy="11" r="3.25" />
          <path {...stroke} d="M4 18.5h16" />
          <path {...stroke} d="M12 4.5v2M6.4 6.8l1.5 1.5M17.6 6.8l-1.5 1.5" />
        </>
      )}
      {id === "maghrib" && (
        <>
          <path {...stroke} d="M4 16.5h16" />
          <path {...stroke} d="M7 16.5a5 5 0 0 1 10 0" />
          <path {...stroke} d="M12 7.5V6M7.2 9.2 6 8M16.8 9.2 18 8" />
        </>
      )}
      {id === "isha" && (
        <>
          <path {...stroke} d="M14.5 6.5A6 6 0 1 0 17 16.2 4.75 4.75 0 1 1 14.5 6.5Z" />
          <circle cx="18.4" cy="7.1" r="0.7" fill="currentColor" />
          <circle cx="20.3" cy="9.2" r="0.55" fill="currentColor" />
        </>
      )}
    </svg>
  );
}
