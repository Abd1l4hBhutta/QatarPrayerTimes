package com.example.qatarprayertimes.ui

import android.media.MediaPlayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qatarprayertimes.azan.CustomAzan
import com.example.qatarprayertimes.azan.StockAzan
import com.example.qatarprayertimes.data.AppLocale
import com.example.qatarprayertimes.ui.theme.Accent
import com.example.qatarprayertimes.ui.theme.Background
import com.example.qatarprayertimes.ui.theme.Card
import com.example.qatarprayertimes.ui.theme.Foreground
import com.example.qatarprayertimes.ui.theme.Muted

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AzanSelectionScreen(
    locale: AppLocale,
    stockSounds: List<StockAzan>,
    customSounds: List<CustomAzan>,
    selectedSoundId: String?,
    onSelectStock: (StockAzan) -> Unit,
    onSelectCustom: (CustomAzan) -> Unit,
    onDeleteCustom: (CustomAzan) -> Unit,
    onUploadCustom: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var playingId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var soundToDelete by remember { mutableStateOf<CustomAzan?>(null) }

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
        playingId = null
    }

    fun play(id: String, resId: Int?, filePath: String?) {
        if (playingId == id) {
            stop()
            return
        }
        stop()
        mediaPlayer = when {
            resId != null -> MediaPlayer.create(context, resId)
            filePath != null -> try {
                MediaPlayer().apply {
                    setDataSource(filePath)
                    prepare()
                }
            } catch (e: Exception) { null }
            else -> null
        }?.apply {
            setOnCompletionListener { stop() }
            start()
        }
        playingId = id
    }

    DisposableEffect(Unit) {
        onDispose { stop() }
    }

    // Delete Confirmation Dialog
    if (soundToDelete != null) {
        AlertDialog(
            onDismissRequest = { soundToDelete = null },
            title = { Text("Delete Adhaan") },
            text = { Text("Are you sure you want to delete \"${soundToDelete?.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        soundToDelete?.let { onDeleteCustom(it) }
                        soundToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { soundToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 22.dp, vertical = 24.dp),
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
                text = "Select Adhaan",
                color = Foreground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Accent.copy(alpha = 0.1f))
                        .border(1.dp, Accent.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .clickable(onClick = onUploadCustom)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = Accent)
                    Text(text = "Add Your Own Adhaan", color = Accent, fontWeight = FontWeight.Bold)
                }
            }

            if (customSounds.isNotEmpty()) {
                item {
                    Text(
                        text = "YOUR ADHAANS",
                        color = Muted,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )
                }

                items(customSounds) { custom ->
                    SoundItem(
                        name = custom.name,
                        arabicName = null,
                        isSelected = custom.id == selectedSoundId,
                        isPlaying = playingId == custom.id,
                        onPlay = { play(custom.id, null, custom.file.absolutePath) },
                        onSelect = { onSelectCustom(custom) },
                        onLongClick = { soundToDelete = custom }
                    )
                }
            }

            item {
                Text(
                    text = "GALLERY",
                    color = Muted,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            }

            items(stockSounds) { stock ->
                SoundItem(
                    name = stock.englishName,
                    arabicName = stock.arabicName,
                    isSelected = stock.id == selectedSoundId,
                    isPlaying = playingId == stock.id,
                    onPlay = { play(stock.id, stock.resId, null) },
                    onSelect = { onSelectStock(stock) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SoundItem(
    name: String,
    arabicName: String?,
    isSelected: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onSelect: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Accent.copy(alpha = 0.1f) else Card)
            .border(
                1.dp, 
                if (isSelected) Accent.copy(alpha = 0.4f) else Color.Transparent, 
                RoundedCornerShape(20.dp)
            )
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onLongClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPlay,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isPlaying) Accent else Foreground.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isPlaying) Color.White else Foreground,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Foreground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (arabicName != null) {
                Text(
                    text = arabicName,
                    color = Muted,
                    fontSize = 14.sp
                )
            }
        }

        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(24.dp)
            )
        } else if (onLongClick != null) {
             Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Muted.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
