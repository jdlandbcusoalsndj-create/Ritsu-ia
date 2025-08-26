package com.ritsu.aiassistant.data.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isSystemApp: Boolean = false,
    val category: String = "Other",
    val lastUsed: Long = 0L,
    val usageCount: Int = 0
)