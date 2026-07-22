package com.example.model

import androidx.compose.runtime.Stable
import com.squareup.moshi.JsonClass

@Stable
@JsonClass(generateAdapter = true)
data class TouchPoint(
    val number: Int,
    var x: Float,
    var y: Float,
    var holdDurationMs: Long = 50L,
    var delayAfterMs: Long = 1000L,
    var isSwipe: Boolean = false,
    var endX: Float? = null,
    var endY: Float? = null
)
