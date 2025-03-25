package com.example.tlucontactapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class UnitActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvUnits: RecyclerView
    private lateinit var unitAdapter: UnitAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var etSearch: EditText
    private val unitList = mutableListOf<Unit>()
    private val filteredUnitList = mutableListOf<Unit>()
    private var sortBy = "name"
    private val adminEmails = listOf("vinhlovehmuk4@gmail.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unit)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Khởi tạo DrawerLayout và NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Khởi tạo RecyclerView
        rvUnits = findViewById(R.id.rvUnits)
        rvUnits.layoutManager = LinearLayoutManager(this)
        unitAdapter = UnitAdapter(filteredUnitList) { unit ->
            db.collection("units").whereEqualTo("unitId", unit.unitId).get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        Toast.makeText(this, "Không tìm thấy thông tin đơn vị!", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    val unitDocId = result.documents.firstOrNull()?.id
                    val intent = Intent(this, UnitDetailActivity::class.java).apply {
                        putExtra("unit", unit)
                        putExtra("unitId", unitDocId)
                    }
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    Log.e("UnitActivity", "Lỗi khi truy vấn đơn vị: ${e.message}")
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
        rvUnits.adapter = unitAdapter

        // Ánh xạ thanh tìm kiếm
        etSearch = findViewById(R.id.etSearch)

        // Xử lý nút thêm đơn vị
        val btnAddUnit = findViewById<Button>(R.id.btnAddUnit)
        val currentEmail = auth.currentUser?.email
        if (currentEmail in adminEmails) {
            btnAddUnit.visibility = View.VISIBLE
            btnAddUnit.setOnClickListener { showAddUnitDialog() }
        } else {
            btnAddUnit.visibility = View.GONE
        }

        // Xử lý thanh tìm kiếm
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterUnits(s.toString())
            }
        })

        // Xử lý biểu tượng menu
        findViewById<ImageView>(R.id.ivMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Xử lý sự kiện chọn mục trong Navigation Drawer
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_sort -> {
                    showSortDialog()
                }
                R.id.nav_filter -> {
                    Toast.makeText(this, "Không có bộ lọc cho đơn vị", Toast.LENGTH_SHORT).show()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        loadUnitData()
    }

    private fun loadUnitData() {
        db.collection("units").get()
            .addOnSuccessListener { result ->
                unitList.clear()
                for (document in result) {
                    try {
                        val unit = document.toObject(Unit::class.java)
                        unitList.add(unit)
                    } catch (e: Exception) {
                        Log.e("UnitActivity", "Lỗi khi ánh xạ dữ liệu đơn vị: ${e.message}")
                    }
                }
                filterUnits(etSearch.text.toString())
                Log.d("UnitActivity", "Tải dữ liệu thành công: ${unitList.size} đơn vị")
                if (unitList.isEmpty()) {
                    Toast.makeText(this, "Không có đơn vị nào!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("UnitActivity", "Lỗi khi tải dữ liệu đơn vị: ${e.message}")
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun filterUnits(searchText: String) {
        filteredUnitList.clear()
        filteredUnitList.addAll(unitList.filter {
            it.name.contains(searchText, ignoreCase = true) ||
                    it.unitId.contains(searchText, ignoreCase = true)
        })

        when (sortBy) {
            "name" -> filteredUnitList.sortBy { it.name }
            "unitId" -> filteredUnitList.sortBy { it.unitId }
        }

        unitAdapter.notifyDataSetChanged()
    }

    private fun showSortDialog() {
        val options = arrayOf("Theo tên", "Theo mã đơn vị")
        AlertDialog.Builder(this)
            .setTitle("Sắp xếp")
            .setItems(options) { _, which ->
                sortBy = when (which) {
                    0 -> "name"
                    1 -> "unitId"
                    else -> "name"
                }
                filterUnits(etSearch.text.toString())
            }
            .show()
    }

    private fun showAddUnitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_unit, null)
        val etUnitName = dialogView.findViewById<EditText>(R.id.etUnitName)
        val etUnitId = dialogView.findViewById<EditText>(R.id.etUnitId)
        val etUnitPhone = dialogView.findViewById<EditText>(R.id.etUnitPhone)
        val etUnitAddress = dialogView.findViewById<EditText>(R.id.etUnitAddress)
        val etUnitPhotoUrl = dialogView.findViewById<EditText>(R.id.etUnitPhotoUrl)
        val etParentUnitId = dialogView.findViewById<EditText>(R.id.etParentUnitId) // Thêm trường parentUnitId

        AlertDialog.Builder(this)
            .setTitle("Thêm đơn vị")
            .setView(dialogView)
            .setPositiveButton("Thêm") { _, _ ->
                val name = etUnitName.text.toString().trim()
                val unitId = etUnitId.text.toString().trim()
                val phone = etUnitPhone.text.toString().trim()
                val address = etUnitAddress.text.toString().trim()
                val photoUrl = etUnitPhotoUrl.text.toString().trim().ifEmpty { null }
                val parentUnitId = etParentUnitId.text.toString().trim().ifEmpty { null }

                if (name.isEmpty() || unitId.isEmpty()) {
                    Toast.makeText(this, "Vui lòng điền đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Kiểm tra trùng lặp unitId
                db.collection("units").whereEqualTo("unitId", unitId).get()
                    .addOnSuccessListener { result ->
                        if (!result.isEmpty) {
                            Toast.makeText(this, "Mã đơn vị đã tồn tại!", Toast.LENGTH_SHORT).show()
                        } else {
                            val newUnit = hashMapOf(
                                "name" to name,
                                "unitId" to unitId,
                                "phone" to phone,
                                "email" to "",
                                "address" to address,
                                "fax" to "",
                                "parentUnitId" to parentUnitId,
                                "photoURL" to photoUrl,
                                "subUnits" to emptyList<String>()
                            )

                            db.collection("units").add(newUnit)
                                .addOnSuccessListener { documentReference ->
                                    Log.d("UnitActivity", "Thêm đơn vị thành công: ${documentReference.id}")
                                    Toast.makeText(this, "Thêm đơn vị thành công!", Toast.LENGTH_SHORT).show()

                                    // Nếu có parentUnitId, cập nhật subUnits của đơn vị cha
                                    if (!parentUnitId.isNullOrEmpty()) {
                                        db.collection("units").whereEqualTo("unitId", parentUnitId).get()
                                            .addOnSuccessListener { parentResult ->
                                                if (!parentResult.isEmpty) {
                                                    val parentDoc = parentResult.documents.first()
                                                    db.collection("units").document(parentDoc.id)
                                                        .update("subUnits", FieldValue.arrayUnion(unitId))
                                                        .addOnSuccessListener {
                                                            Log.d("UnitActivity", "Cập nhật subUnits cho đơn vị cha thành công")
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.e("UnitActivity", "Lỗi cập nhật subUnits: ${e.message}")
                                                        }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("UnitActivity", "Lỗi tìm đơn vị cha: ${e.message}")
                                            }
                                    }

                                    loadUnitData() // Tải lại dữ liệu
                                }
                                .addOnFailureListener { e ->
                                    Log.e("UnitActivity", "Lỗi khi thêm đơn vị: ${e.message}")
                                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("UnitActivity", "Lỗi kiểm tra trùng lặp: ${e.message}")
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}