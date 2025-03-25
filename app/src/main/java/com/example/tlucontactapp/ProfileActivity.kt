package com.example.tlucontactapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class ProfileActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Lấy thông tin hiện tại
        db.collection("users").document(userId!!).get().addOnSuccessListener {
            etPhone.setText(it.getString("phoneNumber"))
        }

        // Lưu thông tin
        btnSave.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            db.collection("users").document(userId)
                .update("phoneNumber", phone)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val imageUri = data.data
            val storageRef = storage.reference.child("profile_images/$userId/${UUID.randomUUID()}.jpg")

            imageUri?.let {
                storageRef.putFile(it).addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        db.collection("users").document(userId!!)
                            .update("photoURL", uri.toString())
                            .addOnSuccessListener {
                                Toast.makeText(this, "Cập nhật ảnh thành công!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi upload ảnh: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}