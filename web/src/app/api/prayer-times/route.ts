import { NextResponse } from "next/server";
import { getPrayerTimes } from "@/lib/get-prayer-times";

export const runtime = "nodejs";
export const revalidate = 86400;

export async function GET() {
  try {
    const data = await getPrayerTimes();
    return NextResponse.json(data, {
      headers: {
        "Cache-Control": "public, s-maxage=86400, stale-while-revalidate=3600",
      },
    });
  } catch {
    return NextResponse.json(
      {
        error:
          "Could not load prayer times from prayers.qa or the Aladhan fallback.",
      },
      { status: 503 },
    );
  }
}
