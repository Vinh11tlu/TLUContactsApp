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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class UnitDetailActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var unitId: String? = null
    private lateinit var subUnitAdapter: UnitAdapter
    private val subUnitList = mutableListOf<Unit>()
    private val adminEmails = listOf("vinhlovehmuk4@gmail.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unit_detail)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val unit = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("unit", Unit::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("unit")
        }
        unitId = intent.getStringExtra("unitId")

        if (unit == null || unitId == null) {
            Toast.makeText(this, "Không tìm thấy thông tin đơn vị!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Ánh xạ các view
        val ivPhoto = findViewById<ImageView>(R.id.ivPhoto)
        val tvUnitId = findViewById<TextView>(R.id.tvUnitId)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvPhone = findViewById<TextView>(R.id.tvPhone)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvAddress = findViewById<TextView>(R.id.tvAddress)
        val tvFax = findViewById<TextView>(R.id.tvFax)
        val tvParentUnit = findViewById<TextView>(R.id.tvParentUnit)
        val tvSubUnitsLabel = findViewById<TextView>(R.id.tvSubUnitsLabel)
        val rvSubUnits = findViewById<RecyclerView>(R.id.rvSubUnits)
        val btnEdit = findViewById<Button>(R.id.btnEdit)
        val btnDelete = findViewById<Button>(R.id.btnDelete)

        // Gán dữ liệu cơ bản
        tvUnitId.text = unit.unitId.ifEmpty { "Không có mã đơn vị" }
        tvName.text = unit.name.ifEmpty { "Không có tên" }
        tvPhone.text = unit.phone.ifEmpty { "Không có số điện thoại" }
        tvEmail.text = unit.email.ifEmpty { "Không có email" }
        tvAddress.text = unit.address.ifEmpty { "Không có địa chỉ" }
        tvFax.text = unit.fax.ifEmpty { "Không có fax" }
        unit.photoURL?.let {
            Glide.with(this).load(it).placeholder(R.drawable.tlu).error(R.drawable.tlu).into(ivPhoto)
        } ?: ivPhoto.setImageResource(R.drawable.tlu)

        // Xử lý sự kiện gọi điện
        tvPhone.setOnClickListener {
            val phoneNumber = unit.phone
            if (!phoneNumber.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Không có số điện thoại để gọi!", Toast.LENGTH_SHORT).show()
            }
        }

        // Xử lý sự kiện gửi email
        tvEmail.setOnClickListener {
            val emailAddress = unit.email
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

        // Tải thông tin đơn vị cha
        loadParentUnit(unit, tvParentUnit)

        // Tải danh sách đơn vị con
        setupSubUnitsRecyclerView(unit, rvSubUnits, tvSubUnitsLabel)

        // Phân quyền nút sửa và xóa
        val currentEmail = auth.currentUser?.email
        if (currentEmail in adminEmails) {
            btnEdit.visibility = View.VISIBLE
            btnDelete.visibility = View.VISIBLE
            btnEdit.setOnClickListener { showEditDialog(unit) }
            btnDelete.setOnClickListener { deleteUnit() }
        } else {
            btnEdit.visibility = View.GONE
            btnDelete.visibility = View.GONE
        }
    }

    private fun loadParentUnit(unit: Unit, tvParentUnit: TextView) {
        unit.parentUnitId?.let { parentId ->
            db.collection("units").whereEqualTo("unitId", parentId).limit(1).get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        tvParentUnit.text = "Không có đơn vị cha"
                        return@addOnSuccessListener
                    }
                    val parentUnit = result.documents.firstOrNull()?.toObject(Unit::class.java)
                    if (parentUnit != null) {
                        tvParentUnit.text = parentUnit.name
                        tvParentUnit.setOnClickListener {
                            val intent = Intent(this, UnitDetailActivity::class.java).apply {
                                putExtra("unit", parentUnit)
                                putExtra("unitId", result.documents.first().id)
                            }
                            startActivity(intent)
                        }
                    } else {
                        tvParentUnit.text = "Không có đơn vị cha"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("UnitDetailActivity", "Lỗi tải đơn vị cha: ${e.message}")
                    tvParentUnit.text = "Lỗi khi tải đơn vị cha"
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } ?: run {
            tvParentUnit.text = "Không có đơn vị cha"
        }
    }

    private fun setupSubUnitsRecyclerView(unit: Unit, rvSubUnits: RecyclerView, tvSubUnitsLabel: TextView) {
        rvSubUnits.layoutManager = LinearLayoutManager(this)
        subUnitAdapter = UnitAdapter(subUnitList) { subUnit ->
            db.collection("units").whereEqualTo("unitId", subUnit.unitId).limit(1).get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        Toast.makeText(this, "Không tìm thấy đơn vị con!", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    val subUnitDocId = result.documents.first().id
                    val intent = Intent(this, UnitDetailActivity::class.java).apply {
                        putExtra("unit", subUnit)
                        putExtra("unitId", subUnitDocId)
                    }
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    Log.e("UnitDetailActivity", "Lỗi truy vấn đơn vị con: ${e.message}")
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
        rvSubUnits.adapter = subUnitAdapter

        unit.subUnits?.let { subUnitIds ->
            if (subUnitIds.isNotEmpty()) {
                db.collection("units").whereIn("unitId", subUnitIds).get()
                    .addOnSuccessListener { result ->
                        subUnitList.clear()
                        for (document in result) {
                            try {
                                val subUnit = document.toObject(Unit::class.java)
                                subUnitList.add(subUnit)
                            } catch (e: Exception) {
                                Log.e("UnitDetailActivity", "Lỗi ánh xạ đơn vị con: ${e.message}")
                            }
                        }
                        subUnitAdapter.notifyDataSetChanged()
                        tvSubUnitsLabel.visibility = if (subUnitList.isNotEmpty()) View.VISIBLE else View.GONE
                        rvSubUnits.visibility = if (subUnitList.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                    .addOnFailureListener { e ->
                        Log.e("UnitDetailActivity", "Lỗi tải đơn vị con: ${e.message}")
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                        tvSubUnitsLabel.visibility = View.GONE
                        rvSubUnits.visibility = View.GONE
                    }
            } else {
                tvSubUnitsLabel.visibility = View.GONE
                rvSubUnits.visibility = View.GONE
            }
        } ?: run {
            tvSubUnitsLabel.visibility = View.GONE
            rvSubUnits.visibility = View.GONE
        }
    }

    private fun showEditDialog(unit: Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_unit, null)
        val etUnitName = dialogView.findViewById<EditText>(R.id.etUnitName).apply { setText(unit.name) }
        val etUnitId = dialogView.findViewById<EditText>(R.id.etUnitId).apply { setText(unit.unitId) }
        val etUnitPhone = dialogView.findViewById<EditText>(R.id.etUnitPhone).apply { setText(unit.phone) }
        val etUnitEmail = dialogView.findViewById<EditText>(R.id.etUnitEmail).apply { setText(unit.email) }
        val etUnitAddress = dialogView.findViewById<EditText>(R.id.etUnitAddress).apply { setText(unit.address) }
        val etUnitFax = dialogView.findViewById<EditText>(R.id.etUnitFax).apply { setText(unit.fax) }
        val etParentUnitId = dialogView.findViewById<EditText>(R.id.etParentUnitId).apply { setText(unit.parentUnitId) }
        val etUnitPhotoUrl = dialogView.findViewById<EditText>(R.id.etUnitPhotoUrl).apply { setText(unit.photoURL) }
        val etSubUnits = dialogView.findViewById<EditText>(R.id.etSubUnits)?.apply { setText(unit.subUnits?.joinToString(",")) } // Thêm trường subUnits

        AlertDialog.Builder(this)
            .setTitle("Sửa thông tin đơn vị")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val name = etUnitName.text.toString().trim()
                val unitIdText = etUnitId.text.toString().trim()
                val phone = etUnitPhone.text.toString().trim()
                val email = etUnitEmail.text.toString().trim()
                val address = etUnitAddress.text.toString().trim()
                val fax = etUnitFax.text.toString().trim()
                val parentUnitId = etParentUnitId.text.toString().trim().ifEmpty { null }
                val photoUrl = etUnitPhotoUrl.text.toString().trim().ifEmpty { null }
                val subUnits = etSubUnits?.text.toString().trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }

                if (name.isEmpty() || unitIdText.isEmpty()) {
                    Toast.makeText(this, "Vui lòng điền đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedUnit = hashMapOf(
                    "name" to name,
                    "unitId" to unitIdText,
                    "phone" to phone,
                    "email" to email,
                    "address" to address,
                    "fax" to fax,
                    "parentUnitId" to parentUnitId,
                    "photoURL" to photoUrl,
                    "subUnits" to subUnits
                )

                unitId?.let {
                    db.collection("units").document(it).update(updatedUnit as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show()
                            // Cập nhật parentUnitId của các đơn vị con nếu có thay đổi
                            updateSubUnitsParent(unit.subUnits ?: emptyList(), subUnits, unitIdText)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("UnitDetailActivity", "Lỗi cập nhật đơn vị: ${e.message}")
                            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updateSubUnitsParent(oldSubUnits: List<String>, newSubUnits: List<String>, currentUnitId: String) {
        // Xóa currentUnitId khỏi parentUnitId của các đơn vị con cũ không còn trong danh sách mới
        val removedSubUnits = oldSubUnits.filter { it !in newSubUnits }
        removedSubUnits.forEach { subUnitId ->
            db.collection("units").whereEqualTo("unitId", subUnitId).get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val docId = result.documents.first().id
                        db.collection("units").document(docId).update("parentUnitId", null)
                    }
                }
        }

        // Cập nhật parentUnitId cho các đơn vị con mới
        newSubUnits.forEach { subUnitId ->
            db.collection("units").whereEqualTo("unitId", subUnitId).get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val docId = result.documents.first().id
                        db.collection("units").document(docId).update("parentUnitId", currentUnitId)
                    }
                }
        }
    }

    private fun deleteUnit() {
        AlertDialog.Builder(this)
            .setTitle("Xóa đơn vị")
            .setMessage("Bạn có chắc chắn muốn xóa đơn vị này không?")
            .setPositiveButton("Xóa") { _, _ ->
                unitId?.let {
                    db.collection("units").document(it).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Xóa đơn vị thành công!", Toast.LENGTH_SHORT).show()
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