package com.example.qatarprayertimes.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qatarprayertimes.azan.DndAccess
import com.example.qatarprayertimes.data.AppLocale
import com.example.qatarprayertimes.data.DataSource
import com.example.qatarprayertimes.data.PrayerId
import com.example.qatarprayertimes.data.PrayerTime
import com.example.qatarprayertimes.data.TimeFormat
import com.example.qatarprayertimes.data.TimeUtils
import com.example.qatarprayertimes.ui.components.CountdownTimer
import com.example.qatarprayertimes.ui.components.DateHeader
import com.example.qatarprayertimes.ui.components.PrayerCard
import com.example.qatarprayertimes.ui.theme.Accent
import com.example.qatarprayertimes.ui.theme.Background
import com.example.qatarprayertimes.ui.theme.Card
import com.example.qatarprayertimes.ui.theme.Foreground
import com.example.qatarprayertimes.ui.theme.Muted
import kotlinx.coroutines.delay
import java.util.Calendar

enum class Screen { Dashboard, Qibla, Settings, AzanSelection }

@Composable
fun PrayerDashboard(viewModel: PrayerViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = state.prefs.locale
    val direction = if (locale == AppLocale.AR) LayoutDirection.Rtl else LayoutDirection.Ltr
    val context = LocalContext.current

    var nowMinutes by remember { mutableIntStateOf(TimeUtils.nowInQatar().first) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = TimeUtils.nowInQatar().first
            if (nowMinutes != current) {
                nowMinutes = current
            }
            delay(5000)
        }
    }

    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    
    var dashboardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { dashboardVisible = true }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.setGlobalCustomSound(uri)
            DndAccess.request(context)
        }
    }

    BackHandler(enabled = currentScreen != Screen.Dashboard) {
        currentScreen = when (currentScreen) {
            Screen.AzanSelection -> Screen.Settings
            else -> Screen.Dashboard
        }
    }

    val t = Strings.copy(locale)

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Scaffold(
            bottomBar = {
                if (currentScreen != Screen.AzanSelection) {
                    NavigationBar(
                        containerColor = Card,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == Screen.Dashboard,
                            onClick = { currentScreen = Screen.Dashboard },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text(t.home) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent,
                                selectedTextColor = Accent,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted,
                                indicatorColor = Accent.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Qibla,
                            onClick = { currentScreen = Screen.Qibla },
                            icon = { Icon(Icons.Default.Place, contentDescription = null) },
                            label = { Text(t.qibla) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent,
                                selectedTextColor = Accent,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted,
                                indicatorColor = Accent.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Settings,
                            onClick = { currentScreen = Screen.Settings },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text(t.settings) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent,
                                selectedTextColor = Accent,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted,
                                indicatorColor = Accent.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (targetState != Screen.Dashboard) {
                            (fadeIn(tween(300)) + slideInVertically(tween(400)) { it / 8 })
                                .togetherWith(fadeOut(tween(250)))
                        } else {
                            (fadeIn(tween(350)))
                                .togetherWith(fadeOut(tween(250)))
                        }
                    },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        Screen.Dashboard -> DashboardContent(
                            state = state,
                            nowMinutes = nowMinutes,
                            isEntranceVisible = dashboardVisible,
                            onRetry = { viewModel.refresh(force = true) },
                            onToggleMute = viewModel::toggleMute
                        )
                        Screen.Qibla -> QiblaCompassScreen(locale)
                        Screen.Settings -> SettingsScreen(
                            locale = locale,
                            timeFormat = state.prefs.timeFormat,
                            viewMode = state.prefs.viewMode,
                            soundState = state.soundStates[PrayerId.FAJR],
                            onTimeFormatChange = viewModel::setTimeFormat,
                            onViewModeChange = viewModel::setViewMode,
                            onLocaleChange = viewModel::setLocale,
                            onOpenAzanSelection = { currentScreen = Screen.AzanSelection },
                            onToggleMute = viewModel::toggleGlobalMute,
                            onBack = { currentScreen = Screen.Dashboard }
                        )
                        Screen.AzanSelection -> AzanSelectionScreen(
                            locale = locale,
                            stockSounds = state.stockSounds,
                            customSounds = state.customSounds,
                            selectedSoundId = state.soundStates[PrayerId.FAJR]?.soundId,
                            onSelectStock = { 
                                viewModel.selectStockSound(it)
                                currentScreen = Screen.Settings
                            },
                            onSelectCustom = {
                                viewModel.selectCustomSound(it)
                                currentScreen = Screen.Settings
                            },
                            onDeleteCustom = viewModel::deleteCustomSound,
                            onUploadCustom = { picker.launch("audio/*") },
                            onBack = { currentScreen = Screen.Settings }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: PrayerUiState,
    nowMinutes: Int,
    isEntranceVisible: Boolean,
    onRetry: () -> Unit,
    onToggleMute: (PrayerId) -> Unit,
) {
    val locale = state.prefs.locale
    val t = Strings.copy(locale)
    var showSunriseInsteadOfFajr by remember { mutableStateOf(false) }
    var showFirstCallInsteadOfDhuhr by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        when {
            state.loading -> SkeletonLoader(t.loading)
            state.error || state.payload == null -> ErrorState(
                title = t.errorTitle,
                body = t.errorBody,
                retry = t.retry,
                onRetry = onRetry,
            )
            else -> {
                val payload = state.payload!!
                val prayers = TimeUtils.applyAreaOffsets(
                    payload.prayers,
                    state.prefs.area,
                    payload.areaOffsets,
                )
                val highlight = TimeUtils.getNextAndCurrent(prayers, nowMinutes)
                
                val mainPrayers = prayers.filter { it.id != PrayerId.SUNRISE }
                val sunrise = prayers.find { it.id == PrayerId.SUNRISE }
                
                // Robust Friday Check
                val isFriday = Calendar.getInstance(TimeUtils.QATAR).get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY ||
                              payload.weekday?.contains("Friday", ignoreCase = true) == true ||
                              payload.weekday?.contains("الجمعة") == true

                // Ensure jummahFirstCall is calculated if missing (for legacy cache)
                val effectiveJummahFirstCall = payload.jummahFirstCall ?: run {
                    val dhuhr = prayers.find { it.id == PrayerId.DHUHR }
                    if (dhuhr != null) {
                        val azanMinutes = TimeUtils.clockToMinutes(dhuhr.azan)
                        val offset = dhuhr.iqamaOffsetMinutes ?: 20
                        TimeUtils.minutesToClock(azanMinutes + offset - 60)
                    } else null
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateHeader(
                            hijriDate = payload.hijriDate,
                            gregorianDate = payload.gregorianDate,
                            weekday = payload.weekday,
                            locale = locale,
                        )
                        if (isFriday && effectiveJummahFirstCall != null) {
                            val time = TimeUtils.formatClock(effectiveJummahFirstCall, state.prefs.timeFormat, locale)
                            Text(
                                text = "${t.jummahFirstCall} $time",
                                color = Accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Accent.copy(alpha = 0.1f))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }

                    CountdownTimer(
                        prayers = prayers,
                        locale = locale,
                        timeFormat = state.prefs.timeFormat,
                        nextId = highlight.next,
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        mainPrayers.forEachIndexed { index, prayer ->
                            val currentPrayer = when {
                                prayer.id == PrayerId.FAJR && showSunriseInsteadOfFajr && sunrise != null -> sunrise
                                prayer.id == PrayerId.DHUHR && isFriday && showFirstCallInsteadOfDhuhr && effectiveJummahFirstCall != null -> {
                                    PrayerTime(
                                        id = PrayerId.DHUHR,
                                        azan = effectiveJummahFirstCall,
                                        iqama = null,
                                        iqamaOffsetMinutes = null
                                    )
                                }
                                else -> prayer
                            }

                            val customName = if (prayer.id == PrayerId.DHUHR && isFriday && showFirstCallInsteadOfDhuhr) {
                                t.firstCallLabel
                            } else null

                            AnimatedVisibility(
                                visible = isEntranceVisible,
                                enter = fadeIn(tween(700, delayMillis = index * 60)) + 
                                        slideInVertically(tween(700, delayMillis = index * 60)) { it / 4 }
                            ) {
                                PrayerCard(
                                    prayer = currentPrayer,
                                    locale = locale,
                                    timeFormat = state.prefs.timeFormat,
                                    viewMode = state.prefs.viewMode,
                                    isNext = if (prayer.id == PrayerId.DHUHR && showFirstCallInsteadOfDhuhr) false else (highlight.next == currentPrayer.id),
                                    isCurrent = if (prayer.id == PrayerId.DHUHR && showFirstCallInsteadOfDhuhr) false else (highlight.current == currentPrayer.id),
                                    soundState = state.soundStates[currentPrayer.id],
                                    customName = customName,
                                    onClick = when (prayer.id) {
                                        PrayerId.FAJR -> { { showSunriseInsteadOfFajr = !showSunriseInsteadOfFajr } }
                                        PrayerId.DHUHR -> if (isFriday && effectiveJummahFirstCall != null) {
                                            { showFirstCallInsteadOfDhuhr = !showFirstCallInsteadOfDhuhr }
                                        } else null
                                        else -> null
                                    },
                                    onToggleMute = if ((prayer.id == PrayerId.FAJR && showSunriseInsteadOfFajr) || (prayer.id == PrayerId.DHUHR && showFirstCallInsteadOfDhuhr)) null else {
                                        { onToggleMute(currentPrayer.id) }
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = listOf(
                                if (payload.source == DataSource.PRAYERS_QA) t.sourcePrayersQa else t.sourceAladhan,
                                t.timezone,
                            ).joinToString(" · "),
                            color = Muted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Made by Abdullah Bhutta",
                            color = Muted.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonLoader(loading: String) {
    val pulse = rememberInfiniteTransition(label = "skeleton")
    val alpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Text(text = loading, color = Muted, fontSize = 13.sp)
        Box(Modifier.height(12.dp).width(140.dp).clip(RoundedCornerShape(6.dp)).background(Card.copy(alpha = alpha)))
        Box(Modifier.height(28.dp).width(220.dp).clip(RoundedCornerShape(6.dp)).background(Card.copy(alpha = alpha)))
        Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(20.dp)).background(Card.copy(alpha = alpha)))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                Box(Modifier.height(32.dp).width(72.dp).clip(CircleShape).background(Card.copy(alpha = alpha)))
            }
        }
        repeat(6) {
            Box(Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(20.dp)).background(Card.copy(alpha = alpha)))
        }
    }
}

@Composable
private fun ErrorState(title: String, body: String, retry: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
            verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, color = Foreground, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(text = body, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Text(
            text = retry,
            color = Accent,
            modifier = Modifier
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.15f))
                .clickable(onClick = onRetry)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}
