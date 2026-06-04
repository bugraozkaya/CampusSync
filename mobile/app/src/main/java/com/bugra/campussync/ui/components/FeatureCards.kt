package com.bugra.campussync.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugra.campussync.network.CourseMaterialItem
import com.bugra.campussync.ui.theme.CSTheme

@Composable
fun MaterialCard(
    material: CourseMaterialItem,
    canDelete: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val typeColor = CSTheme.materialTypeColor(material.material_type)
            val typeIcon = CSTheme.materialTypeIcon(material.material_type)
            
            Surface(
                color = typeColor.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(24.dp),
                    tint = typeColor
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Text(
                    text = "${material.course_code} · ${material.material_type_display}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = material.uploaded_by_name,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (material.file_url != null) {
                IconButton(onClick = onDownload) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GradeCardItem(
    grade: com.bugra.campussync.network.GradeItem,
    isStudent: Boolean,
    onDelete: (() -> Unit)? = null
) {
    val pct = grade.percentage
    val color = CSTheme.gradeColor(pct)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = color.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "%.0f".format(grade.score),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = color
                    )
                    Text(
                        text = "/${grade.max_score.toInt()}",
                        fontSize = 10.sp,
                        color = color.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    text = grade.grade_type_display,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (!isStudent) {
                    Text(
                        text = grade.student_name.ifBlank { grade.student_username },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = grade.course_code,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (grade.notes.isNotBlank()) {
                    Text(
                        text = grade.notes,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = color.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "%.1f%%".format(pct),
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                onDelete?.let {
                    IconButton(
                        onClick = it,
                        modifier = Modifier.padding(top = 4.dp).size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
