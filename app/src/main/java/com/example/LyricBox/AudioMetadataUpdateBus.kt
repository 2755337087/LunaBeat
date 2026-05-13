package com.example.LyricBox

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AudioMetadataUpdateBus {
    private val _updates = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 32
    )
    val updates = _updates.asSharedFlow()

    fun notifyPathUpdated(path: String?) {
        val normalized = path?.trim().orEmpty()
        if (normalized.isEmpty()) return
        _updates.tryEmit(normalized)
    }
}

