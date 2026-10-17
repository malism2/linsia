package com.malism.linsia.home.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.malism.linsia.home.AppFeatureIcon

fun AppFeatureIcon.asImageVector(): ImageVector = when (this) {
    AppFeatureIcon.MenuBook -> Icons.Filled.Star
    AppFeatureIcon.Newspaper -> Icons.Filled.DateRange
    AppFeatureIcon.Keyboard -> Icons.Filled.Edit
    AppFeatureIcon.Library -> Icons.AutoMirrored.Filled.List
    AppFeatureIcon.Dictionary -> Icons.Filled.Search
    AppFeatureIcon.Headphones -> Icons.Filled.PlayArrow
    AppFeatureIcon.Bookmark -> Icons.Filled.Favorite
}
