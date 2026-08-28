package com.example.qatarprayertimes.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qatarprayertimes.data.AppLocale
import com.example.qatarprayertimes.ui.Strings
import com.example.qatarprayertimes.ui.theme.Accent
import com.example.qatarprayertimes.ui.theme.Muted

@Composable
fun DateHeader(
    hijriDate: String,
    gregorianDate: String,
    weekday: String?,
    locale: AppLocale,
) {
    val t = Strings.copy(locale)
    Column {
        Text(
            text = t.appTitle.uppercase(),
            color = Accent.copy(alpha = 0.85f),
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = hijriDate,
            color = com.example.qatarprayertimes.ui.theme.Foreground,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = listOfNotNull(weekday, gregorianDate).joinToString(" · "),
            color = Muted,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(text = t.appSubtitle, color = Muted, fontSize = 14.sp)
    }
}
