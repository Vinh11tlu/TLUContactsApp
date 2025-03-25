package com.example.tlucontactapp

data class User(
    val uid: String = "",
    val role: String = "", // "student" hoặc "staff"
    val studentId: String? = null, // Chỉ có nếu là sinh viên
    val staffId: String? = null, // Chỉ có nếu là CBGV
    val classId: String? = null // Chỉ có nếu là sinh viên
)