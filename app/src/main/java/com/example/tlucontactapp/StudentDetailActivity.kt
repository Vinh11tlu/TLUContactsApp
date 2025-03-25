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

class StudentDetailActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var studentId: String? = null
    private val adminEmails = listOf("vinhlovehmuk4@gmail.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_detail)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val student = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("student", Student::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("student")
        }
        studentId = intent.getStringExtra("studentId")

        if (student == null) {
            Toast.makeText(this, "Không tìm thấy thông tin sinh viên!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val ivPhoto = findViewById<ImageView>(R.id.ivPhoto)
        val tvStudentId = findViewById<TextView>(R.id.tvStudentId)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvPhone = findViewById<TextView>(R.id.tvPhone)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvAddress = findViewById<TextView>(R.id.tvAddress)
        val tvClass = findViewById<TextView>(R.id.tvClass)
        val btnEdit = findViewById<Button>(R.id.btnEdit)
        val btnDelete = findViewById<Button>(R.id.btnDelete)

        tvStudentId.text = student.studentId.ifEmpty { "Không có mã sinh viên" }
        tvName.text = student.name.ifEmpty { "Không có họ và tên" }
        tvPhone.text = student.phone.ifEmpty { "Không có số điện thoại" }
        tvEmail.text = student.email.ifEmpty { "Không có email" }
        tvAddress.text = student.address.ifEmpty { "Không có địa chỉ" }

        student.photoURL?.let {
            Glide.with(this).load(it).placeholder(R.drawable.tlu).error(R.drawable.tlu).into(ivPhoto)
        } ?: ivPhoto.setImageResource(R.drawable.tlu)

        tvPhone.setOnClickListener {
            val phoneNumber = student.phone
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
            val emailAddress = student.email
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

        student.classId?.let { classId ->
            db.collection("units").whereEqualTo("unitId", classId).get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        tvClass.text = "Không thuộc lớp nào"
                        return@addOnSuccessListener
                    }
                    val classUnit = result.documents.firstOrNull()?.toObject(Unit::class.java)
                    tvClass.text = classUnit?.name ?: "Không thuộc lớp nào"
                    tvClass.setOnClickListener {
                        if (classUnit != null) {
                            val intent = Intent(this, UnitDetailActivity::class.java).apply {
                                putExtra("unit", classUnit)
                                putExtra("unitId", result.documents.firstOrNull()?.id)
                            }
                            startActivity(intent)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    tvClass.text = "Lỗi khi tải thông tin lớp"
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } ?: run { tvClass.text = "Không thuộc lớp nào" }

        // Phân quyền nút sửa và xóa
        val currentEmail = auth.currentUser?.email
        if (currentEmail in adminEmails) {
            // Admin: Có thể chỉnh sửa và xóa
            btnEdit.visibility = View.VISIBLE
            btnDelete.visibility = View.VISIBLE
            btnEdit.setOnClickListener { showEditDialog(student) }
            btnDelete.setOnClickListener { deleteStudent() }
        } else if (currentEmail == student.email) {
            // Sinh viên: Chỉ có thể chỉnh sửa thông tin của chính mình
            btnEdit.visibility = View.VISIBLE
            btnDelete.visibility = View.GONE
            btnEdit.setOnClickListener { showEditDialog(student) }
        } else {
            // Không có quyền chỉnh sửa hoặc xóa
            btnEdit.visibility = View.GONE
            btnDelete.visibility = View.GONE
        }
    }

    private fun showEditDialog(student: Student) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_student, null)
        val etStudentName = dialogView.findViewById<EditText>(R.id.etStudentName).apply { setText(student.name) }
        val etStudentId = dialogView.findViewById<EditText>(R.id.etStudentId).apply { setText(student.studentId) }
        val etStudentPhone = dialogView.findViewById<EditText>(R.id.etStudentPhone).apply { setText(student.phone) }
        val etStudentEmail = dialogView.findViewById<EditText>(R.id.etStudentEmail).apply { setText(student.email) }
        val etStudentAddress = dialogView.findViewById<EditText>(R.id.etStudentAddress).apply { setText(student.address) }
        val etClassId = dialogView.findViewById<EditText>(R.id.etClassId).apply { setText(student.classId) }
        val etStudentPhotoUrl = dialogView.findViewById<EditText>(R.id.etStudentPhotoUrl).apply { setText(student.photoURL) }

        AlertDialog.Builder(this)
            .setTitle("Sửa thông tin sinh viên")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val updatedStudent = hashMapOf(
                    "name" to etStudentName.text.toString().trim(),
                    "studentId" to etStudentId.text.toString().trim(),
                    "phone" to etStudentPhone.text.toString().trim(),
                    "email" to etStudentEmail.text.toString().trim(),
                    "address" to etStudentAddress.text.toString().trim(),
                    "classId" to etClassId.text.toString().trim().ifEmpty { null },
                    "photoURL" to etStudentPhotoUrl.text.toString().trim().ifEmpty { null }
                )
                studentId?.let {
                    db.collection("students").document(it).update(updatedStudent as Map<String, Any>)
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

    private fun deleteStudent() {
        AlertDialog.Builder(this)
            .setTitle("Xóa sinh viên")
            .setMessage("Bạn có chắc chắn muốn xóa sinh viên này không?")
            .setPositiveButton("Xóa") { _, _ ->
                studentId?.let {
                    db.collection("students").document(it).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Xóa sinh viên thành công!", Toast.LENGTH_SHORT).show()
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