package com.example.tlucontactapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Student(
    val studentId: String = "", // Mã sinh viên
    val name: String = "", // Họ và tên
    val phone: String = "", // Số điện thoại
    val email: String = "", // Email
    val address: String = "", // Địa chỉ nơi ở
    val classId: String? = null, // Mã lớp (hoặc đơn vị trực thuộc)
    val photoURL: String? = null // URL ảnh đại diện
) : Parcelable