package com.example.LyricBox

import android.content.SharedPreferences
import org.json.JSONArray

private const val KEY_ENABLED_METADATA_FIELDS = "enabledMetadataFields"
private const val KEY_CUSTOM_METADATA_FIELDS = "customMetadataFields"
private const val KEY_VISIBLE_METADATA_FIELDS_ORDER = "visibleMetadataFieldsOrder"
private const val KEY_HIDDEN_METADATA_FIELDS_ORDER = "hiddenMetadataFieldsOrder"
private val DEFAULT_HIDDEN_CUSTOM_FIELD_NAMES = listOf("language", "rate")

const val CUSTOM_METADATA_FIELD_PREFIX = "custom:"

data class BuiltInMetadataField(
    val key: String,
    val label: String
)

val BUILT_IN_METADATA_FIELDS: List<BuiltInMetadataField> = listOf(
    BuiltInMetadataField("title", "标题"),
    BuiltInMetadataField("artist", "艺术家"),
    BuiltInMetadataField("album", "专辑"),
    BuiltInMetadataField("year", "年份"),
    BuiltInMetadataField("trackDisc", "音轨号 / 碟号"),
    BuiltInMetadataField("genre", "风格"),
    BuiltInMetadataField("albumArtist", "专辑艺术家"),
    BuiltInMetadataField("composer", "作曲"),
    BuiltInMetadataField("lyricist", "作词"),
    BuiltInMetadataField("comment", "注释"),
    BuiltInMetadataField("copyrightInfo", "版权信息"),
    BuiltInMetadataField("accompaniment", "伴奏"),
    BuiltInMetadataField("lyrics", "歌词")
)

private val BUILT_IN_FIELD_KEY_SET = BUILT_IN_METADATA_FIELDS.map { it.key }.toSet()

data class MetadataFieldConfig(
    val visibleFieldKeys: List<String>,
    val hiddenFieldKeys: List<String>,
    val customFieldNames: List<String>
) {
    val visibleCustomFieldNames: List<String>
        get() = visibleFieldKeys
            .filter { isCustomMetadataFieldKey(it) }
            .map { customMetadataFieldNameFromKey(it) }
}

fun customMetadataFieldKey(fieldName: String): String = CUSTOM_METADATA_FIELD_PREFIX + fieldName

fun isCustomMetadataFieldKey(fieldKey: String): Boolean = fieldKey.startsWith(CUSTOM_METADATA_FIELD_PREFIX)

fun customMetadataFieldNameFromKey(fieldKey: String): String = fieldKey.removePrefix(CUSTOM_METADATA_FIELD_PREFIX)

fun metadataFieldLabel(fieldKey: String): String {
    val builtIn = BUILT_IN_METADATA_FIELDS.firstOrNull { it.key == fieldKey }
    if (builtIn != null) return builtIn.label
    if (isCustomMetadataFieldKey(fieldKey)) return customMetadataFieldNameFromKey(fieldKey)
    return fieldKey
}

object MetadataFieldConfigStore {
    fun load(prefs: SharedPreferences): MetadataFieldConfig {
        val customFields = (prefs.getStringSet(KEY_CUSTOM_METADATA_FIELDS, emptySet()) ?: emptySet())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toMutableList()

        val legacyEnabledCustomFields = (prefs.getStringSet(KEY_ENABLED_METADATA_FIELDS, emptySet()) ?: emptySet())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        val storedVisible = readOrderedStringList(prefs, KEY_VISIBLE_METADATA_FIELDS_ORDER)
        val storedHidden = readOrderedStringList(prefs, KEY_HIDDEN_METADATA_FIELDS_ORDER)

        val hasStoredOrder = storedVisible.isNotEmpty() || storedHidden.isNotEmpty()
        if (!hasStoredOrder) {
            val existingCustomLower = customFields.map { it.lowercase() }.toMutableSet()
            for (defaultField in DEFAULT_HIDDEN_CUSTOM_FIELD_NAMES) {
                if (existingCustomLower.add(defaultField.lowercase())) {
                    customFields.add(defaultField)
                }
            }
        }

        val customKeys = customFields.map { customMetadataFieldKey(it) }
        val allAllowedKeys = buildList {
            addAll(BUILT_IN_METADATA_FIELDS.map { it.key })
            addAll(customKeys)
        }.toSet()

        val filteredStoredVisible = storedVisible.filter { it in allAllowedKeys }
        val filteredStoredHidden = storedHidden.filter { it in allAllowedKeys }

        val visible = if (hasStoredOrder) {
            dedupePreserveOrder(filteredStoredVisible)
        } else {
            buildList {
                addAll(BUILT_IN_METADATA_FIELDS.map { it.key })
                addAll(customFields.filter { it in legacyEnabledCustomFields }.map { customMetadataFieldKey(it) })
            }
        }.toMutableList()

        val hidden = if (hasStoredOrder) {
            dedupePreserveOrder(filteredStoredHidden)
        } else {
            customFields
                .filter { it !in legacyEnabledCustomFields }
                .map { customMetadataFieldKey(it) }
        }
            .toMutableList()

        val used = (visible + hidden).toMutableSet()
        val builtInDefaultVisible = BUILT_IN_METADATA_FIELDS.map { it.key }

        for (builtInKey in builtInDefaultVisible) {
            if (!used.contains(builtInKey)) {
                val insertionIndex = BUILT_IN_METADATA_FIELDS
                    .dropWhile { it.key != builtInKey }
                    .drop(1)
                    .mapNotNull { nextField ->
                        visible.indexOf(nextField.key).takeIf { it >= 0 }
                    }
                    .minOrNull()
                    ?: visible.size
                visible.add(insertionIndex, builtInKey)
                used.add(builtInKey)
            }
        }
        for (customName in customFields) {
            val key = customMetadataFieldKey(customName)
            if (!used.contains(key)) {
                if (customName in legacyEnabledCustomFields) {
                    visible.add(key)
                } else {
                    hidden.add(key)
                }
                used.add(key)
            }
        }

        val finalVisible = dedupePreserveOrder(visible).filter { it in allAllowedKeys }
        val finalHidden = dedupePreserveOrder(hidden).filter { it in allAllowedKeys && it !in finalVisible.toSet() }

        return MetadataFieldConfig(
            visibleFieldKeys = finalVisible,
            hiddenFieldKeys = finalHidden,
            customFieldNames = customFields
        )
    }

    fun save(
        prefs: SharedPreferences,
        visibleFieldKeys: List<String>,
        hiddenFieldKeys: List<String>,
        customFieldNames: List<String>
    ) {
        val trimmedCustom = customFieldNames
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val customKeySet = trimmedCustom.map { customMetadataFieldKey(it) }.toSet()
        val allowedKeys = BUILT_IN_FIELD_KEY_SET + customKeySet

        val finalVisible = dedupePreserveOrder(visibleFieldKeys).filter { it in allowedKeys }
        val finalHidden = dedupePreserveOrder(hiddenFieldKeys).filter { it in allowedKeys && it !in finalVisible.toSet() }

        val visibleCustomNames = finalVisible
            .filter { isCustomMetadataFieldKey(it) }
            .map { customMetadataFieldNameFromKey(it) }
            .toSet()

        prefs.edit()
            .putString(KEY_VISIBLE_METADATA_FIELDS_ORDER, JSONArray(finalVisible).toString())
            .putString(KEY_HIDDEN_METADATA_FIELDS_ORDER, JSONArray(finalHidden).toString())
            .putStringSet(KEY_CUSTOM_METADATA_FIELDS, trimmedCustom.toSet())
            .putStringSet(KEY_ENABLED_METADATA_FIELDS, visibleCustomNames)
            .apply()
    }

    private fun readOrderedStringList(prefs: SharedPreferences, key: String): List<String> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            buildList {
                for (i in 0 until jsonArray.length()) {
                    val value = jsonArray.optString(i).trim()
                    if (value.isNotEmpty()) add(value)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun dedupePreserveOrder(items: List<String>): List<String> {
        val seen = LinkedHashSet<String>()
        for (item in items) {
            if (item.isNotEmpty()) {
                seen.add(item)
            }
        }
        return seen.toList()
    }
}
