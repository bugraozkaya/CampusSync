package com.bugra.campussync.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CSTheme {
    @Composable
    fun materialTypeColor(type: String): Color = when (type) {
        "LECTURE_NOTES" -> MaterialTheme.colorScheme.primary
        "ASSIGNMENT"    -> MaterialTheme.colorScheme.tertiary
        "EXAM"          -> MaterialTheme.colorScheme.error
        "RESOURCE"      -> MaterialTheme.colorScheme.secondary
        else            -> Color.Gray
    }

    fun materialTypeIcon(type: String): ImageVector = when (type) {
        "LECTURE_NOTES" -> Icons.Default.MenuBook
        "ASSIGNMENT"    -> Icons.Default.Assignment
        "EXAM"          -> Icons.Default.Quiz
        "RESOURCE"      -> Icons.Default.Link
        else            -> Icons.Default.AttachFile
    }

    @Composable
    fun gradeColor(percentage: Double): Color = when {
        percentage >= 85 -> MaterialTheme.colorScheme.primary
        percentage >= 60 -> MaterialTheme.colorScheme.tertiary
        else             -> MaterialTheme.colorScheme.error
    }
}
