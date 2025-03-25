package com.example.tlucontactapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val adminEmails = listOf("vinhlovehmuk4@gmail.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Vui lòng nhập email!"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Vui lòng nhập mật khẩu!"
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            db.collection("users").document(user.uid).get()
                                .addOnSuccessListener { document ->
                                    if (!document.exists() && email !in adminEmails) {
                                        // Nếu không phải Admin và chưa có dữ liệu, yêu cầu xác minh email
                                        if (user.isEmailVerified) {
                                            val role = if (email.endsWith("@gmail.com")) "CBGV" else "SV"
                                            saveUserToFirestore(user.uid, email, "Tên mặc định", "Mã mặc định", role)
                                            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, HomeActivity::class.java))
                                            finish()
                                        } else {
                                            Toast.makeText(this, "Vui lòng xác minh email trước khi đăng nhập lần đầu!", Toast.LENGTH_LONG).show()
                                            auth.signOut()
                                        }
                                    } else {
                                        // Admin hoặc tài khoản đã tồn tại, không cần kiểm tra xác minh
                                        Log.d("LoginActivity", "Đăng nhập thành công: ${user.email}")
                                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, HomeActivity::class.java))
                                        finish()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Lỗi kiểm tra thông tin: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    } else {
                        Log.e("LoginActivity", "Đăng nhập thất bại: ${task.exception?.message}")
                        Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                etEmail.error = "Vui lòng nhập email!"
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Đã gửi email đặt lại mật khẩu!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
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
                Log.d("LoginActivity", "Lưu thông tin vào Firestore thành công: $userData")
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Lưu thông tin thất bại: ${e.message}")
                Toast.makeText(this, "Lỗi khi lưu thông tin: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}