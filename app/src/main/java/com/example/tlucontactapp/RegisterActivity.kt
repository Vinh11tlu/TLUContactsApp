package com.example.tlucontactapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val adminEmails = listOf("vinhlovehmuk4@gmail.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val etName = findViewById<EditText>(R.id.etName)
        val etCode = findViewById<EditText>(R.id.etCode)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val name = etName.text.toString().trim()
            val code = etCode.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Vui lòng nhập email!"
                return@setOnClickListener
            }
            val emailPattern = if (email.endsWith("@gmail.com") && email !in adminEmails) "CBGV"
            else if (email.endsWith("@e.tlu.edu.vn")) "SV"
            else if (email in adminEmails) "Admin"
            else null
            if (emailPattern == null) {
                etEmail.error = "Email phải thuộc @gmail.com (CBGV) hoặc @e.tlu.edu.vn (SV)!"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Vui lòng nhập mật khẩu!"
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                etConfirmPassword.error = "Vui lòng xác nhận mật khẩu!"
                return@setOnClickListener
            }
            if (name.isEmpty()) {
                etName.error = "Vui lòng nhập họ và tên!"
                return@setOnClickListener
            }
            if (code.isEmpty()) {
                etCode.error = "Vui lòng nhập mã cán bộ/sinh viên!"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                etConfirmPassword.error = "Mật khẩu không khớp!"
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val role = when {
                            email in adminEmails -> "Admin"
                            email.endsWith("@gmail.com") -> "CBGV"
                            else -> "SV"
                        }
                        if (user != null) {
                            // Lưu thông tin người dùng vào Firestore
                            saveUserToFirestore(user.uid, email, name, code, role)
                            if (email in adminEmails) {
                                // Admin không cần xác minh email
                                Toast.makeText(this, "Đăng ký Admin thành công!", Toast.LENGTH_LONG).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            } else {
                                // CBGV và SV cần xác minh email
                                user.sendEmailVerification()?.addOnSuccessListener {
                                    Toast.makeText(this, "Đã gửi email xác minh! Vui lòng xác minh email để hoàn tất đăng ký.", Toast.LENGTH_LONG).show()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }?.addOnFailureListener { e ->
                                    Toast.makeText(this, "Lỗi gửi email xác minh: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } else {
                        Log.e("RegisterActivity", "Đăng ký thất bại: ${task.exception?.message}")
                        Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun saveUserToFirestore(userId: String, email: String, name: String, code: String, role: String) {
        val userData = hashMapOf(
            "name" to name,
            "email" to email,
            "role" to role,
            "code" to code
        )
        db.collection("users").document(userId).set(userData)
            .addOnSuccessListener {
                Log.d("RegisterActivity", "Lưu thông tin vào Firestore thành công: $userData")
            }
            .addOnFailureListener { e ->
                Log.e("RegisterActivity", "Lưu thông tin thất bại: ${e.message}")
                Toast.makeText(this, "Lỗi khi lưu thông tin: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}