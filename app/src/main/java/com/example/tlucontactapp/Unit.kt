package com.example.tlucontactapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Unit(
    val unitId: String = "", // Mã đơn vị
    val name: String = "", // Tên đơn vị
    val phone: String = "", // Số điện thoại
    val email: String = "", // Email
    val address: String = "", // Địa chỉ
    val fax: String = "", // Fax
    val unitType: String = "", // Loại đơn vị
    val parentUnitId: String? = null, // Mã đơn vị cha
    val subUnits: List<String>? = null, // Danh sách đơn vị con
    val photoURL: String? = null // URL ảnh đại diện
) : Parcelable