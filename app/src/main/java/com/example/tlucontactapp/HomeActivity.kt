package com.example.tlucontactapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val adminEmails = listOf("vinhlovehmuk4@gmail.com")

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val ivProfile = findViewById<ImageView>(R.id.ivProfile)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnStaff = findViewById<Button>(R.id.btnStaff)
        val btnStudents = findViewById<Button>(R.id.btnStudents)
        val btnUnits = findViewById<Button>(R.id.btnUnits)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val cardStaff = findViewById<View>(R.id.cardStaff)

        val userId = auth.currentUser?.uid
        val userEmail = auth.currentUser?.email

        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "Người dùng"
                        val photoUrl = document.getString("photoURL")
                        val role = if (userEmail in adminEmails) "Admin" else document.getString("role") ?: "SV"
                        tvWelcome.text = "Chào $name"
                        Log.d("HomeActivity", "Lấy tên thành công: $name")
                        if (photoUrl != null) {
                            Glide.with(this).load(photoUrl).into(ivProfile)
                        }

                        // Phân quyền giao diện
                        when {
                            userEmail in adminEmails -> {
                                // Admin: Xem và chỉnh sửa tất cả
                                cardStaff.visibility = View.VISIBLE
                                btnStaff.isEnabled = true
                                btnStudents.isEnabled = true
                                btnUnits.isEnabled = true
                            }
                            role == "CBGV" -> {
                                // CBGV: Xem tất cả nhưng không sửa danh sách sinh viên và đơn vị
                                cardStaff.visibility = View.VISIBLE
                                btnStaff.isEnabled = true
                                btnStudents.isEnabled = true
                                btnUnits.isEnabled = true
                            }
                            role == "SV" -> {
                                // Sinh viên: Chỉ xem danh sách sinh viên và đơn vị, ẩn danh bạ CBGV
                                cardStaff.visibility = View.GONE
                                btnStudents.isEnabled = true
                                btnUnits.isEnabled = true
                            }
                        }
                    } else {
                        // Nếu không có dữ liệu trong Firestore, kiểm tra admin cố định
                        if (userEmail in adminEmails) {
                            tvWelcome.text = "Chào Admin"
                            cardStaff.visibility = View.VISIBLE
                            btnStaff.isEnabled = true
                            btnStudents.isEnabled = true
                            btnUnits.isEnabled = true
                            saveDefaultAdmin(userId, userEmail!!)
                        } else {
                            tvWelcome.text = "Chào người dùng"
                            Log.e("HomeActivity", "Document không tồn tại")
                            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    tvWelcome.text = "Chào người dùng"
                    Log.e("HomeActivity", "Lỗi khi lấy dữ liệu: ${e.message}")
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            tvWelcome.text = "Chào người dùng"
            Log.e("HomeActivity", "User ID là null")
            Toast.makeText(this, "Không tìm thấy thông tin đăng nhập", Toast.LENGTH_SHORT).show()
        }

        btnStaff.setOnClickListener {
            startActivity(Intent(this, StaffActivity::class.java))
        }
        btnStudents.setOnClickListener {
            startActivity(Intent(this, StudentActivity::class.java))
        }
        btnUnits.setOnClickListener {
            startActivity(Intent(this, UnitActivity::class.java))
        }
        btnLogout.setOnClickListener {
            auth.signOut()
            Log.d("HomeActivity", "Đăng xuất thành công")
            Toast.makeText(this, "Đăng xuất thành công!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun saveDefaultAdmin(userId: String, email: String) {
        val userData = hashMapOf(
            "name" to "Admin",
            "email" to email,
            "role" to "Admin",
            "code" to "ADMIN_$userId"
        )
        db.collection("users").document(userId).set(userData)
            .addOnSuccessListener {
                Log.d("HomeActivity", "Lưu thông tin admin mặc định thành công: $email")
            }
            .addOnFailureListener { e ->
                Log.e("HomeActivity", "Lỗi khi lưu thông tin admin: ${e.message}")
            }
    }
}