package com.example.qatarprayertimes.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.qatarprayertimes.azan.DndAccess
import com.example.qatarprayertimes.azan.ExactAlarmAccess
import com.example.qatarprayertimes.data.AppLocale
import com.example.qatarprayertimes.data.AzanSoundState
import com.example.qatarprayertimes.data.TimeFormat
import com.example.qatarprayertimes.data.ViewMode
import com.example.qatarprayertimes.ui.theme.Accent
import com.example.qatarprayertimes.ui.theme.Background
import com.example.qatarprayertimes.ui.theme.Card
import com.example.qatarprayertimes.ui.theme.Foreground
import com.example.qatarprayertimes.ui.theme.Muted

@Composable
fun SettingsScreen(
    locale: AppLocale,
    timeFormat: TimeFormat,
    viewMode: ViewMode,
    soundState: AzanSoundState?,
    onTimeFormatChange: (TimeFormat) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onLocaleChange: (AppLocale) -> Unit,
    onOpenAzanSelection: () -> Unit,
    onToggleMute: () -> Unit,
    onBack: () -> Unit,
) {
    val t = Strings.copy(locale)
    val isMuted = soundState?.muted == true
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Local state to track DND permission
    var hasDndPermission by remember { mutableStateOf(DndAccess.hasAccess(context)) }
    var hasExactAlarmPermission by remember { mutableStateOf(ExactAlarmAccess.hasAccess(context)) }

    // Observe lifecycle to refresh permission status when returning to app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasDndPermission = DndAccess.hasAccess(context)
                hasExactAlarmPermission = ExactAlarmAccess.hasAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Card)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Foreground
                )
            }
            Text(
                text = t.settings,
                color = Foreground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Mute / Unmute Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (isMuted) Color.Red.copy(alpha = 0.05f) else Accent.copy(alpha = 0.05f))
                .clickable(onClick = onToggleMute)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color.Red.copy(alpha = 0.1f) else Accent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (isMuted) Color.Red else Accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isMuted) t.unmute else t.mute,
                            color = Foreground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMuted) "Azan is currently silenced" else "Azan will play normally",
                            color = Muted,
                            fontSize = 13.sp
                        )
                    }
                }
                Switch(
                    checked = !isMuted,
                    onCheckedChange = { onToggleMute() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Black
                    )
                )
            }
        }

        // Change Azan Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Card)
                .clickable(onClick = onOpenAzanSelection)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Change Adhaan",
                    color = Foreground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = soundState?.fileName ?: "Select your preferred sound",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Muted
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Card, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ToggleGroup(
                label = t.timeFormat,
                options = listOf(TimeFormat.H12 to t.hour12, TimeFormat.H24 to t.hour24),
                value = timeFormat,
                onChange = onTimeFormatChange,
            )
            ToggleGroup(
                label = t.display,
                options = listOf(ViewMode.AZAN to t.azanOnly, ViewMode.BOTH to t.azanAndIqama),
                value = viewMode,
                onChange = onViewModeChange,
            )
            ToggleGroup(
                label = t.language,
                options = listOf(AppLocale.EN to t.english, AppLocale.AR to t.arabic),
                value = locale,
                onChange = onLocaleChange,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Card, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "System", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Background.copy(alpha = 0.5f))
                    .clickable { ExactAlarmAccess.request(context) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Exact prayer alarms", color = Foreground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (hasExactAlarmPermission) {
                            "Prayer alerts are scheduled for their exact time."
                        } else {
                            "Allow Alarms & reminders to play the Adhaan on time."
                        },
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
                Switch(
                    checked = hasExactAlarmPermission,
                    onCheckedChange = { ExactAlarmAccess.request(context) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Black
                    ),
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Background.copy(alpha = 0.5f))
                    .clickable { DndAccess.request(context) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = t.dndOverride, color = Foreground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(text = t.dndNote, color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
                }
                Switch(
                    checked = hasDndPermission,
                    onCheckedChange = { DndAccess.request(context) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Black
                    ),
                )
            }
        }

        Button(
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ABOUT_ME_URL)))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Background,
            ),
        ) {
            Text(
                text = "About me",
                modifier = Modifier.padding(vertical = 4.dp),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private const val ABOUT_ME_URL = "https://linktr.ee/Abdullah.Bhutta"

@Composable
private fun <T> ToggleGroup(
    label: String,
    options: List<Pair<T, String>>,
    value: T,
    onChange: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = Muted, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { (option, title) ->
                val selected = option == value
                Text(
                    text = title,
                    color = if (selected) Accent else Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (selected) Accent.copy(alpha = 0.15f) else Background)
                        .clickable { onChange(option) }
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}
