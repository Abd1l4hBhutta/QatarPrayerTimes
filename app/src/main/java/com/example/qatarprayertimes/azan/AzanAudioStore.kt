package com.example.qatarprayertimes.azan

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.qatarprayertimes.R
import com.example.qatarprayertimes.data.AzanSoundState
import com.example.qatarprayertimes.data.PrayerId
import com.example.qatarprayertimes.data.SoundType
import java.io.File

data class StockAzan(
    val id: String,
    val resId: Int,
    val englishName: String,
    val arabicName: String?,
)

data class CustomAzan(
    val id: String,
    val name: String,
    val file: File,
)

class AzanAudioStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("azan_audio", Context.MODE_PRIVATE)

    fun state(id: PrayerId): AzanSoundState {
        if (id == PrayerId.SUNRISE) return AzanSoundState(null, true)
        
        val soundId = prefs.getString(KEY_GLOBAL_ID, DEFAULT_AZAN_ID)
        val typeString = prefs.getString(KEY_GLOBAL_TYPE, SoundType.STOCK.name)
        val type = runCatching { SoundType.valueOf(typeString!!) }.getOrDefault(SoundType.STOCK)
        val name = prefs.getString(KEY_GLOBAL_NAME, DEFAULT_AZAN_NAME)
        
        return AzanSoundState(
            fileName = name,
            muted = prefs.getBoolean(muteKey(id), false),
            soundId = soundId,
            type = type
        )
    }

    fun snapshot(): Map<PrayerId, AzanSoundState> = PrayerId.entries.associateWith(::state)

    fun shouldPlay(id: PrayerId): Boolean {
        val s = state(id)
        return s.soundId != null && !s.muted
    }

    fun getStockSounds(): List<StockAzan> {
        val list = mutableListOf<StockAzan>()
        val fields = R.raw::class.java.fields
        for (field in fields) {
            val name = field.name
            if (name == "README" || name.contains("data_extraction_rules") || name.contains("backup_rules")) continue
            val resId = field.getInt(null)
            
            val (english, arabic) = parseStockName(name)
            list.add(StockAzan(name, resId, english, arabic))
        }
        return list.sortedBy { it.englishName }
    }

    fun getCustomSounds(): List<CustomAzan> {
        val files = dir().listFiles()?.filter { it.isFile && it.name.startsWith("custom_") } ?: emptyList()
        return files.map { file ->
            CustomAzan(
                id = file.name,
                name = prefs.getString("name_${file.name}", null) ?: file.name.removePrefix("custom_"),
                file = file
            )
        }.sortedByDescending { it.file.lastModified() }
    }

    private fun parseStockName(raw: String): Pair<String, String?> {
        val clean = raw.trim('_')
        if (clean.contains("yasser_al_dosari")) {
            return "Yasser Al-Dosari" to "ياسر الدوسري"
        }
        val parts = clean.split("_")
        val englishParts = parts.filter { it.isNotEmpty() && it.first().isLetter() }
        val pretty = englishParts.joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        return pretty.ifEmpty { clean } to null
    }

    fun saveGlobalCustom(uri: Uri): Boolean {
        val displayName = queryDisplayName(uri) ?: "Custom Azan"
        val fileName = "custom_${System.currentTimeMillis()}"
        val dest = File(dir(), fileName)
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return false
            
            // Backup current sound to "previous" before switching
            backupCurrentSound()
            
            prefs.edit()
                .putString(KEY_GLOBAL_TYPE, SoundType.CUSTOM.name)
                .putString(KEY_GLOBAL_NAME, displayName)
                .putString(KEY_GLOBAL_ID, fileName)
                .putString("name_$fileName", displayName)
                .apply()
            
            unmuteAll()
            true
        }.getOrDefault(false)
    }

    private fun backupCurrentSound() {
        val currentId = prefs.getString(KEY_GLOBAL_ID, null)
        val currentType = prefs.getString(KEY_GLOBAL_TYPE, null)
        val currentName = prefs.getString(KEY_GLOBAL_NAME, null)
        
        if (currentId != null && currentType != null && currentName != null) {
            prefs.edit()
                .putString(KEY_PREV_ID, currentId)
                .putString(KEY_PREV_TYPE, currentType)
                .putString(KEY_PREV_NAME, currentName)
                .apply()
        }
    }

    fun deleteCustomAzan(custom: CustomAzan) {
        val currentId = prefs.getString(KEY_GLOBAL_ID, null)
        
        // Delete the file
        if (custom.file.exists()) {
            custom.file.delete()
        }
        
        // Remove metadata
        prefs.edit().remove("name_${custom.id}").apply()
        
        // If this was the active sound, switch to previous or default
        if (currentId == custom.id) {
            val prevId = prefs.getString(KEY_PREV_ID, DEFAULT_AZAN_ID)
            val prevType = prefs.getString(KEY_PREV_TYPE, SoundType.STOCK.name)
            val prevName = prefs.getString(KEY_PREV_NAME, DEFAULT_AZAN_NAME)
            
            // If prev was also this sound (somehow), fallback to absolute default
            if (prevId == custom.id) {
                resetToDefault()
            } else {
                prefs.edit()
                    .putString(KEY_GLOBAL_ID, prevId)
                    .putString(KEY_GLOBAL_TYPE, prevType)
                    .putString(KEY_GLOBAL_NAME, prevName)
                    .apply()
            }
        }
    }

    private fun resetToDefault() {
        prefs.edit()
            .putString(KEY_GLOBAL_ID, DEFAULT_AZAN_ID)
            .putString(KEY_GLOBAL_TYPE, SoundType.STOCK.name)
            .putString(KEY_GLOBAL_NAME, DEFAULT_AZAN_NAME)
            .apply()
    }

    fun setGlobalStock(stock: StockAzan) {
        backupCurrentSound()
        prefs.edit()
            .putString(KEY_GLOBAL_TYPE, SoundType.STOCK.name)
            .putString(KEY_GLOBAL_NAME, stock.englishName)
            .putString(KEY_GLOBAL_ID, stock.id)
            .apply()
        unmuteAll()
    }

    fun setGlobalCustom(custom: CustomAzan) {
        backupCurrentSound()
        prefs.edit()
            .putString(KEY_GLOBAL_TYPE, SoundType.CUSTOM.name)
            .putString(KEY_GLOBAL_NAME, custom.name)
            .putString(KEY_GLOBAL_ID, custom.id)
            .apply()
        unmuteAll()
    }

    private fun unmuteAll() {
        val editor = prefs.edit()
        PrayerId.entries.forEach { editor.putBoolean(muteKey(it), false) }
        editor.apply()
    }

    fun setMuted(id: PrayerId, muted: Boolean) {
        prefs.edit().putBoolean(muteKey(id), muted).apply()
    }

    fun toggleAllMute() {
        val allMuted = isAllMuted()
        val next = !allMuted
        val editor = prefs.edit()
        PrayerId.entries.forEach { editor.putBoolean(muteKey(it), next) }
        editor.apply()
    }

    fun isAllMuted(): Boolean = PrayerId.entries.all { prefs.getBoolean(muteKey(it), false) }

    private fun dir(): File = File(context.filesDir, "azan").apply { mkdirs() }
    private fun muteKey(id: PrayerId) = "mute_${id.name}"

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        return cursor?.use { if (it.moveToFirst()) it.getString(0) else null }
    }

    companion object {
        private const val KEY_GLOBAL_NAME = "global_name"
        private const val KEY_GLOBAL_ID = "global_id"
        private const val KEY_GLOBAL_TYPE = "global_type"
        
        private const val KEY_PREV_NAME = "prev_name"
        private const val KEY_PREV_ID = "prev_id"
        private const val KEY_PREV_TYPE = "prev_type"
        
        private const val DEFAULT_AZAN_ID = "yasser_al_dosari_saudi_arabia_"
        private const val DEFAULT_AZAN_NAME = "Yasser Al-Dosari"
    }
}
