package com.example.domain

import android.graphics.drawable.Drawable

/**
 * Model representing any installed app on the system.
 */
data class AppInfoItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystem: Boolean,
    val isBoosted: Boolean = false,
    val boostMode: String = "BALANCED"
)
