package com.bugra.campussync.network

import com.google.gson.annotations.SerializedName

data class ScheduleItem(
    val id: Int,
    @SerializedName("course") val courseId: Int,

    val course_name: String, // YENİ: Django'dan gelecek dersin gerçek adı

    @SerializedName("day_of_week") val day: String,
    val start_time: String,
    val end_time: String,

    @SerializedName("classroom") val classroomId: Int?, // Bazen sınıf boş olabilir diye Nullable (?) yapıyoruz

    val classroom_name: String? // YENİ: Django'dan gelecek sınıfın gerçek adı
)

data class ImportedCourse(
    val department: String,
    val code: String,
    val name: String,
    val lecturer: String
)