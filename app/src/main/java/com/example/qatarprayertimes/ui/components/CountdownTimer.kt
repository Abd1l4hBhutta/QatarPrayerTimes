package com.example.qatarprayertimes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qatarprayertimes.data.AppLocale
import com.example.qatarprayertimes.data.PrayerId
import com.example.qatarprayertimes.data.PrayerTime
import com.example.qatarprayertimes.data.TimeFormat
import com.example.qatarprayertimes.data.TimeUtils
import com.example.qatarprayertimes.ui.Strings
import com.example.qatarprayertimes.ui.theme.Accent
import com.example.qatarprayertimes.ui.theme.Card
import com.example.qatarprayertimes.ui.theme.Foreground
import com.example.qatarprayertimes.ui.theme.Muted
import kotlinx.coroutines.delay

@Composable
fun CountdownTimer(
    prayers: List<PrayerTime>,
    locale: AppLocale,
    timeFormat: TimeFormat,
    nextId: PrayerId,
) {
    val t = Strings.copy(locale)
    var remaining by remember { mutableIntStateOf(0) }

    LaunchedEffect(prayers) {
        while (true) {
            val (minutes, seconds) = TimeUtils.nowInQatar()
            remaining = TimeUtils.getNextAndCurrent(prayers, minutes, seconds).secondsUntilNext
            delay(1000)
        }
    }

    val next = prayers.find { it.id == nextId }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(20.dp))
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = t.nextPrayer.uppercase(),
            color = Muted,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = Strings.prayerName(locale, nextId),
                    color = Accent,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (next != null) {
                    Text(
                        text = "${t.azan} ${TimeUtils.formatClock(next.azan, timeFormat, locale)}",
                        color = Muted,
                        fontSize = 14.sp,
                    )
                }
            }
            Text(
                text = TimeUtils.formatCountdown(remaining),
                color = Foreground,
                fontSize = 30.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
