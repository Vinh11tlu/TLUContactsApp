package com.example.tlucontactapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StaffDetailActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var staffId: String? = null
    private val adminEmails = listOf("vinhlovehmuk4@gmail.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_detail)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val staff = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("staff", Staff::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("staff")
        }
        staffId = intent.getStringExtra("staffId")

        if (staff == null) {
            Toast.makeText(this, "Không tìm thấy thông tin cán bộ giảng viên!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val ivPhoto = findViewById<ImageView>(R.id.ivPhoto)
        val tvStaffId = findViewById<TextView>(R.id.tvStaffId)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvPosition = findViewById<TextView>(R.id.tvPosition)
        val tvPhone = findViewById<TextView>(R.id.tvPhone)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvUnit = findViewById<TextView>(R.id.tvUnit)
        val btnEdit = findViewById<Button>(R.id.btnEdit)
        val btnDelete = findViewById<Button>(R.id.btnDelete)

        tvStaffId.text = staff.staffId.ifEmpty { "Không có mã cán bộ" }
        tvName.text = staff.name.ifEmpty { "Không có họ và tên" }
        tvPosition.text = staff.position.ifEmpty { "Không có chức vụ" }
        tvPhone.text = staff.phone.ifEmpty { "Không có số điện thoại" }
        tvEmail.text = staff.email.ifEmpty { "Không có email" }

        staff.photoURL?.let {
            Glide.with(this).load(it).placeholder(R.drawable.tlu).error(R.drawable.tlu).into(ivPhoto)
        } ?: ivPhoto.setImageResource(R.drawable.tlu)

        tvPhone.setOnClickListener {
            val phoneNumber = staff.phone
            if (!phoneNumber.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Không có số điện thoại để gọi!", Toast.LENGTH_SHORT).show()
            }
        }

        tvEmail.setOnClickListener {
            val emailAddress = staff.email
            if (!emailAddress.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$emailAddress")
                    putExtra(Intent.EXTRA_SUBJECT, "Liên hệ từ TLUContact")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Không tìm thấy ứng dụng email!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Không có email để gửi!", Toast.LENGTH_SHORT).show()
            }
        }

        staff.unitId?.let { unitId ->
            db.collection("units").whereEqualTo("unitId", unitId).get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        tvUnit.text = "Không thuộc đơn vị nào"
                        return@addOnSuccessListener
                    }
                    val unit = result.documents.firstOrNull()?.toObject(Unit::class.java)
                    tvUnit.text = unit?.name ?: "Không thuộc đơn vị nào"
                    tvUnit.setOnClickListener {
                        if (unit != null) {
                            val intent = Intent(this, UnitDetailActivity::class.java).apply {
                                putExtra("unit", unit)
                                putExtra("unitId", result.documents.firstOrNull()?.id)
                            }
                            startActivity(intent)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    tvUnit.text = "Lỗi khi tải thông tin đơn vị"
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } ?: run { tvUnit.text = "Không thuộc đơn vị nào" }

        // Phân quyền nút sửa và xóa
        val currentEmail = auth.currentUser?.email
        if (currentEmail in adminEmails) {
            btnEdit.visibility = View.VISIBLE
            btnDelete.visibility = View.VISIBLE
            btnEdit.setOnClickListener { showEditDialog(staff) }
            btnDelete.setOnClickListener { deleteStaff() }
        } else if (currentEmail == staff.email) {
            btnEdit.visibility = View.VISIBLE
            btnDelete.visibility = View.GONE
            btnEdit.setOnClickListener { showEditDialog(staff) }
        } else {
            btnEdit.visibility = View.GONE
            btnDelete.visibility = View.GONE
        }
    }

    private fun showEditDialog(staff: Staff) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_staff, null)
        val etStaffName = dialogView.findViewById<EditText>(R.id.etStaffName).apply { setText(staff.name) }
        val etStaffId = dialogView.findViewById<EditText>(R.id.etStaffId).apply { setText(staff.staffId) }
        val etStaffPosition = dialogView.findViewById<EditText>(R.id.etStaffPosition).apply { setText(staff.position) }
        val etStaffPhone = dialogView.findViewById<EditText>(R.id.etStaffPhone).apply { setText(staff.phone) }
        val etStaffEmail = dialogView.findViewById<EditText>(R.id.etStaffEmail).apply { setText(staff.email) }
        val etUnitId = dialogView.findViewById<EditText>(R.id.etUnitId).apply { setText(staff.unitId) }
        val etStaffPhotoUrl = dialogView.findViewById<EditText>(R.id.etStaffPhotoUrl).apply { setText(staff.photoURL) }

        AlertDialog.Builder(this)
            .setTitle("Sửa thông tin cán bộ giảng viên")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val updatedStaff = hashMapOf(
                    "name" to etStaffName.text.toString().trim(),
                    "staffId" to etStaffId.text.toString().trim(),
                    "position" to etStaffPosition.text.toString().trim(),
                    "phone" to etStaffPhone.text.toString().trim(),
                    "email" to etStaffEmail.text.toString().trim(),
                    "unitId" to etUnitId.text.toString().trim().ifEmpty { null },
                    "photoURL" to etStaffPhotoUrl.text.toString().trim().ifEmpty { null }
                )
                staffId?.let {
                    db.collection("staff").document(it).update(updatedStaff as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteStaff() {
        AlertDialog.Builder(this)
            .setTitle("Xóa cán bộ giảng viên")
            .setMessage("Bạn có chắc chắn muốn xóa cán bộ giảng viên này không?")
            .setPositiveButton("Xóa") { _, _ ->
                staffId?.let {
                    db.collection("staff").document(it).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Xóa cán bộ giảng viên thành công!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}