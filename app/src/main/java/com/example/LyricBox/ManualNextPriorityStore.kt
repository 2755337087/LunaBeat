package com.example.LyricBox

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

const val EXTRA_MANUAL_NEXT_TOKEN = "manual_next_token"

private const val MANUAL_NEXT_PREFS = "manual_next_priority_store"
private const val KEY_MANUAL_NEXT_TOKENS = "manual_next_tokens"
private const val TOKEN_SEPARATOR = "\n"

object ManualNextPriorityStore {
    @Synchronized
    fun load(context: Context): MutableList<String> {
        val raw = context.getSharedPreferences(MANUAL_NEXT_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MANUAL_NEXT_TOKENS, "")
            .orEmpty()
        if (raw.isBlank()) return mutableListOf()
        return raw.split(TOKEN_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()
    }

    @Synchronized
    fun enqueue(context: Context, token: String) {
        val normalized = token.trim()
        if (normalized.isEmpty()) return
        val tokens = load(context)
        tokens.removeAll { it == normalized }
        tokens.add(normalized)
        save(context, tokens)
    }

    @Synchronized
    fun save(context: Context, tokens: List<String>) {
        val normalized = tokens.map { it.trim() }.filter { it.isNotEmpty() }
        context.getSharedPreferences(MANUAL_NEXT_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MANUAL_NEXT_TOKENS, normalized.joinToString(TOKEN_SEPARATOR))
            .apply()
    }
}

fun resolveManualNextIndex(
    context: Context,
    player: Player,
    consume: Boolean
): Int {
    val storedTokens = ManualNextPriorityStore.load(context)
    if (storedTokens.isEmpty()) return -1
    val currentIndex = player.currentMediaItemIndex
    var changed = false
    var foundIndex = -1
    val normalizedTokens = mutableListOf<String>()

    storedTokens.forEach { token ->
        val index = findIndexByManualNextToken(player, token)
        when {
            index !in 0 until player.mediaItemCount -> {
                changed = true
            }
            index == currentIndex -> {
                changed = true
            }
            else -> {
                normalizedTokens += token
                if (foundIndex < 0) {
                    foundIndex = index
                    if (consume) {
                        normalizedTokens.removeAt(normalizedTokens.lastIndex)
                        changed = true
                    }
                }
            }
        }
    }

    if (foundIndex < 0) {
        if (changed) {
            ManualNextPriorityStore.save(context, normalizedTokens)
        }
        return -1
    }

    if (changed) {
        ManualNextPriorityStore.save(context, normalizedTokens)
    }
    return foundIndex
}

private fun findIndexByManualNextToken(player: Player, token: String): Int {
    if (token.isBlank()) return -1
    for (index in 0 until player.mediaItemCount) {
        val item = player.getMediaItemAt(index)
        if (item.resolveManualNextToken() == token) {
            return index
        }
    }
    return -1
}

fun MediaItem.resolveManualNextToken(): String? {
    return mediaMetadata.extras?.getString(EXTRA_MANUAL_NEXT_TOKEN)
        ?.takeIf { it.isNotBlank() }
}
