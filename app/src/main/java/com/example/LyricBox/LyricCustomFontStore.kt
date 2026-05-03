package com.example.LyricBox

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.FontFamily
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class LyricCustomFontOption(
    val id: String,
    val displayName: String,
    val isDefault: Boolean
)

private data class LyricCustomFontEntry(
    val id: String,
    val displayName: String,
    val fileName: String
)

object LyricCustomFontStore {
    private const val PREFS_NAME = "LyricCustomFontStore"
    private const val KEY_ENTRIES = "entries_json"
    private const val KEY_SELECTED = "selected_font_id"
    private const val CACHE_DIR_NAME = "lyric_custom_fonts"
    const val DEFAULT_FONT_ID = "default"
    private const val DEFAULT_FONT_NAME = "默认字体"

    fun loadOptions(context: Context): List<LyricCustomFontOption> {
        val entries = loadEntries(context)
        return buildList {
            add(
                LyricCustomFontOption(
                    id = DEFAULT_FONT_ID,
                    displayName = DEFAULT_FONT_NAME,
                    isDefault = true
                )
            )
            entries.forEach { entry ->
                add(
                    LyricCustomFontOption(
                        id = entry.id,
                        displayName = entry.displayName,
                        isDefault = false
                    )
                )
            }
        }
    }

    fun getSelectedFontId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val selected = prefs.getString(KEY_SELECTED, DEFAULT_FONT_ID) ?: DEFAULT_FONT_ID
        if (selected == DEFAULT_FONT_ID) return DEFAULT_FONT_ID
        val exists = loadEntries(context).any { it.id == selected }
        if (!exists) {
            prefs.edit().putString(KEY_SELECTED, DEFAULT_FONT_ID).apply()
            return DEFAULT_FONT_ID
        }
        return selected
    }

    fun setSelectedFontId(context: Context, fontId: String) {
        val validId = if (fontId == DEFAULT_FONT_ID || loadEntries(context).any { it.id == fontId }) {
            fontId
        } else {
            DEFAULT_FONT_ID
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED, validId)
            .apply()
    }

    fun importFont(context: Context, uri: Uri): Result<LyricCustomFontOption> {
        return runCatching {
            val displayName = queryDisplayName(context, uri)?.takeIf { it.isNotBlank() }
                ?: "自定义字体.ttf"
            if (!displayName.lowercase().endsWith(".ttf")) {
                error("仅支持TTF字体文件")
            }

            val fontId = "font_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
            val cacheFileName = "$fontId.ttf"
            val cacheFile = File(getCacheDir(context), cacheFileName)

            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "无法读取字体文件" }
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            require(cacheFile.exists() && cacheFile.length() > 0L) { "字体文件缓存失败" }

            val entry = LyricCustomFontEntry(
                id = fontId,
                displayName = displayName,
                fileName = cacheFileName
            )
            val current = loadEntries(context).toMutableList().apply { add(entry) }
            saveEntries(context, current)
            setSelectedFontId(context, fontId)

            LyricCustomFontOption(
                id = fontId,
                displayName = displayName,
                isDefault = false
            )
        }
    }

    fun deleteFont(context: Context, fontId: String): Boolean {
        if (fontId == DEFAULT_FONT_ID) return false
        val entries = loadEntries(context).toMutableList()
        val target = entries.firstOrNull { it.id == fontId } ?: return false
        val cacheFile = File(getCacheDir(context), target.fileName)
        if (cacheFile.exists()) {
            runCatching { cacheFile.delete() }
        }
        entries.removeAll { it.id == fontId }
        saveEntries(context, entries)
        if (getSelectedFontId(context) == fontId) {
            setSelectedFontId(context, DEFAULT_FONT_ID)
        }
        return true
    }

    fun resolveSelectedFontFamily(context: Context): FontFamily? {
        val typeface = resolveSelectedTypeface(context) ?: return null
        return FontFamily(typeface)
    }

    fun resolveSelectedTypeface(context: Context): Typeface? {
        val selectedId = getSelectedFontId(context)
        if (selectedId == DEFAULT_FONT_ID) return null
        val entry = loadEntries(context).firstOrNull { it.id == selectedId } ?: return null
        val file = File(getCacheDir(context), entry.fileName)
        if (!file.exists()) return null
        return runCatching { Typeface.createFromFile(file) }.getOrNull()
    }

    private fun getCacheDir(context: Context): File {
        val dir = File(context.filesDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    cursor.getString(idx)
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun loadEntries(context: Context): List<LyricCustomFontEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ENTRIES, null).orEmpty()
        if (raw.isBlank()) return emptyList()

        val parsed = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    val displayName = obj.optString("displayName")
                    val fileName = obj.optString("fileName")
                    if (id.isBlank() || displayName.isBlank() || fileName.isBlank()) continue
                    add(
                        LyricCustomFontEntry(
                            id = id,
                            displayName = displayName,
                            fileName = fileName
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }

        val valid = parsed.filter { File(getCacheDir(context), it.fileName).exists() }
        if (valid.size != parsed.size) {
            saveEntries(context, valid)
        }
        return valid
    }

    private fun saveEntries(context: Context, entries: List<LyricCustomFontEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("displayName", entry.displayName)
                    put("fileName", entry.fileName)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENTRIES, array.toString())
            .apply()
    }
}

