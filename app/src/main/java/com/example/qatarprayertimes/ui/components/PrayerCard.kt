package com.example.qatarprayertimes.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qatarprayertimes.data.AppLocale
import com.example.qatarprayertimes.data.AzanSoundState
import com.example.qatarprayertimes.data.PrayerTime
import com.example.qatarprayertimes.data.TimeFormat
import com.example.qatarprayertimes.data.TimeUtils
import com.example.qatarprayertimes.data.ViewMode
import com.example.qatarprayertimes.ui.Strings
import com.example.qatarprayertimes.ui.theme.Accent
import com.example.qatarprayertimes.ui.theme.Card
import com.example.qatarprayertimes.ui.theme.Foreground
import com.example.qatarprayertimes.ui.theme.Muted

@Composable
fun PrayerCard(
    prayer: PrayerTime,
    locale: AppLocale,
    timeFormat: TimeFormat,
    viewMode: ViewMode,
    isNext: Boolean,
    isCurrent: Boolean,
    soundState: AzanSoundState?,
    customName: String? = null,
    onClick: (() -> Unit)? = null,
    onToggleMute: (() -> Unit)? = null,
) {
    val t = Strings.copy(locale)
    var showMenu by remember { mutableStateOf(false) }
    val showIqama = viewMode == ViewMode.BOTH && prayer.iqama != null
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val targetScale = when {
        isPressed -> 0.96f
        isNext -> 1.02f
        else -> 1f
    }
    
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "card_scale"
    )

    val cardShape = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shape = cardShape
                clip = true
            }
            .then(if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier)
            .background(if (isNext) Accent.copy(alpha = 0.15f) else Card)
            .then(if (isNext) Modifier.border(1.dp, Accent.copy(alpha = 0.3f), cardShape) else Modifier)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                PrayerIcon(id = prayer.id, color = if (isNext) Accent else Muted)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedContent(
                        targetState = customName ?: Strings.prayerName(locale, prayer.id),
                        transitionSpec = { 
                            fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
                        },
                        label = "name"
                    ) { name ->
                        Text(
                            text = name,
                            color = if (isNext) Foreground else Muted,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                            fontSize = if (isNext) 17.sp else 15.sp,
                        )
                    }
                    if (isCurrent && !isNext) {
                        Text(
                            text = t.now.uppercase(),
                            color = Muted,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Foreground.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    if (isNext) {
                        Text(
                            text = t.nextPrayer.uppercase(),
                            color = Accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Accent.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    AnimatedContent(
                        targetState = TimeUtils.formatClock(prayer.azan, timeFormat, locale),
                        transitionSpec = {
                            fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
                        },
                        label = "time"
                    ) { time ->
                        Text(
                            text = time,
                            color = Foreground,
                            fontSize = if (isNext) 30.sp else 20.sp,
                            fontWeight = if (isNext) FontWeight.ExtraBold else FontWeight.SemiBold,
                        )
                    }
                    if (showIqama) {
                        Text(
                            text = "${t.iqama} ${TimeUtils.formatClock(prayer.iqama!!, timeFormat, locale)}",
                            color = Muted,
                            fontSize = 14.sp,
                        )
                    } else if (viewMode == ViewMode.BOTH && prayer.iqama == null && customName == null) {
                        Text(text = t.sunriseNote, color = Muted, fontSize = 12.sp)
                    }
                }
            }
            
            if (prayer.id != com.example.qatarprayertimes.data.PrayerId.SUNRISE && onToggleMute != null) {
                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (soundState?.muted == true) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            contentDescription = "Sound options",
                            tint = if (isNext) Accent else Muted.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    MaterialTheme(
                        colorScheme = MaterialTheme.colorScheme.copy(
                            surface = Card,
                            onSurface = Foreground
                        ),
                        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
                    ) {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(Card)
                                .border(1.dp, Foreground.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                .padding(4.dp)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = if (soundState?.muted == true) t.unmute else t.mute,
                                        fontWeight = FontWeight.SemiBold
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (soundState?.muted == true) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (soundState?.muted == true) Accent else Color.Red.copy(alpha = 0.7f)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onToggleMute()
                                }
                            )
                            
                            if (soundState?.fileName != null) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MusicNote, 
                                            contentDescription = null, 
                                            modifier = Modifier.size(16.dp),
                                            tint = Accent
                                        )
                                        Text(
                                            text = soundState.fileName,
                                            fontSize = 12.sp,
                                            color = Muted,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
