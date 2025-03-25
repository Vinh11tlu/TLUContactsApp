package com.example.tlucontactapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Staff(
    val staffId: String = "", // Mã cán bộ
    val name: String = "", // Họ và tên
    val position: String = "", // Chức vụ
    val phone: String = "", // Số điện thoại
    val email: String = "", // Email
    val unitId: String? = null, // Mã đơn vị trực thuộc
    val photoURL: String? = null // URL ảnh đại diện
) : Parcelable