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
import com.google.firebase.firestore.FirebaseFirestore

class StaffActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvStaff: RecyclerView
    private lateinit var staffAdapter: StaffAdapter
    private lateinit var drawerLayout: DrawerLayout
    private val staffList = mutableListOf<Staff>()
    private val filteredStaffList = mutableListOf<Staff>()
    private val unitList = mutableListOf<Unit>()
    private val unitMap = mutableMapOf<String, Unit>()
    private var sortBy = "name"
    private var filterUnitId: String? = null
    private val adminEmails = listOf("vinhlovehmuk4@gmail.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Khởi tạo DrawerLayout và NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Khởi tạo RecyclerView
        rvStaff = findViewById(R.id.rvStaff)
        rvStaff.layoutManager = LinearLayoutManager(this)
        staffAdapter = StaffAdapter(
            filteredStaffList,
            unitMap,
            { staff ->
                db.collection("staff").whereEqualTo("staffId", staff.staffId).get()
                    .addOnSuccessListener { result ->
                        if (result.isEmpty) {
                            Toast.makeText(this, "Không tìm thấy thông tin cán bộ giảng viên!", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        val staffDocId = result.documents.firstOrNull()?.id
                        val intent = Intent(this, StaffDetailActivity::class.java).apply {
                            putExtra("staff", staff)
                            putExtra("staffId", staffDocId)
                        }
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Log.e("StaffActivity", "Lỗi khi truy vấn cán bộ giảng viên: ${e.message}")
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            },
            { unit ->
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
                        Log.e("StaffActivity", "Lỗi khi truy vấn đơn vị: ${e.message}")
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        )
        rvStaff.adapter = staffAdapter

        // Xử lý nút thêm cán bộ giảng viên
        val btnAddStaff = findViewById<Button>(R.id.btnAddStaff)
        val currentEmail = auth.currentUser?.email
        if (currentEmail in adminEmails) {
            btnAddStaff.visibility = View.VISIBLE
            btnAddStaff.setOnClickListener { showAddStaffDialog() }
        } else {
            btnAddStaff.visibility = View.GONE
        }

        // Xử lý thanh tìm kiếm
        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterStaff(s.toString())
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
                    showFilterDialog()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        loadUnitData()
        loadStaffData()
    }

    private fun loadUnitData() {
        db.collection("units").get()
            .addOnSuccessListener { result ->
                unitList.clear()
                unitMap.clear()
                for (document in result) {
                    try {
                        val unit = document.toObject(Unit::class.java)
                        unitList.add(unit)
                        unit.unitId.let { unitMap[it] = unit }
                    } catch (e: Exception) {
                        Log.e("StaffActivity", "Lỗi khi ánh xạ dữ liệu đơn vị: ${e.message}")
                    }
                }
                staffAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("StaffActivity", "Lỗi khi tải dữ liệu đơn vị: ${e.message}")
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadStaffData() {
        db.collection("staff").get()
            .addOnSuccessListener { result ->
                staffList.clear()
                for (document in result) {
                    try {
                        val staff = document.toObject(Staff::class.java)
                        staffList.add(staff)
                    } catch (e: Exception) {
                        Log.e("StaffActivity", "Lỗi khi ánh xạ dữ liệu cán bộ giảng viên: ${e.message}")
                    }
                }
                filterStaff(findViewById<EditText>(R.id.etSearch).text.toString())
                Log.d("StaffActivity", "Tải dữ liệu thành công: ${staffList.size} cán bộ giảng viên")
                if (staffList.isEmpty()) {
                    Toast.makeText(this, "Không có cán bộ giảng viên nào!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("StaffActivity", "Lỗi khi tải dữ liệu cán bộ giảng viên: ${e.message}")
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun filterStaff(searchText: String) {
        filteredStaffList.clear()
        filteredStaffList.addAll(staffList.filter {
            (it.name.contains(searchText, ignoreCase = true) ||
                    it.position.contains(searchText, ignoreCase = true) ||
                    it.staffId.contains(searchText, ignoreCase = true)) &&
                    (filterUnitId == null || it.unitId == filterUnitId)
        })

        when (sortBy) {
            "name" -> filteredStaffList.sortBy { it.name }
            "position" -> filteredStaffList.sortBy { it.position }
            "unit" -> filteredStaffList.sortBy { unitMap[it.unitId]?.name ?: "" }
        }

        staffAdapter.notifyDataSetChanged()
    }

    private fun showSortDialog() {
        val options = arrayOf("Theo tên", "Theo chức vụ", "Theo đơn vị")
        AlertDialog.Builder(this)
            .setTitle("Sắp xếp")
            .setItems(options) { _, which ->
                sortBy = when (which) {
                    0 -> "name"
                    1 -> "position"
                    2 -> "unit"
                    else -> "name"
                }
                filterStaff(findViewById<EditText>(R.id.etSearch).text.toString())
            }
            .show()
    }

    private fun showFilterDialog() {
        val unitNames = mutableListOf("Tất cả")
        unitNames.addAll(unitList.map { it.name })
        val unitIds = mutableListOf<String?>(null)
        unitIds.addAll(unitList.map { it.unitId })

        AlertDialog.Builder(this)
            .setTitle("Lọc theo đơn vị")
            .setItems(unitNames.toTypedArray()) { _, which ->
                filterUnitId = unitIds[which]
                filterStaff(findViewById<EditText>(R.id.etSearch).text.toString())
            }
            .show()
    }

    private fun showAddStaffDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_staff, null)
        val etStaffName = dialogView.findViewById<EditText>(R.id.etStaffName)
        val etStaffId = dialogView.findViewById<EditText>(R.id.etStaffId)
        val etStaffPosition = dialogView.findViewById<EditText>(R.id.etStaffPosition)
        val etStaffPhone = dialogView.findViewById<EditText>(R.id.etStaffPhone)
        val etStaffEmail = dialogView.findViewById<EditText>(R.id.etStaffEmail)
        val etUnitId = dialogView.findViewById<EditText>(R.id.etUnitId)
        val etStaffPhotoUrl = dialogView.findViewById<EditText>(R.id.etStaffPhotoUrl)

        AlertDialog.Builder(this)
            .setTitle("Thêm cán bộ giảng viên")
            .setView(dialogView)
            .setPositiveButton("Thêm") { _, _ ->
                val name = etStaffName.text.toString().trim()
                val staffId = etStaffId.text.toString().trim()
                val position = etStaffPosition.text.toString().trim()
                val phone = etStaffPhone.text.toString().trim()
                val email = etStaffEmail.text.toString().trim()
                val unitId = etUnitId.text.toString().trim().ifEmpty { null }
                val photoUrl = etStaffPhotoUrl.text.toString().trim().ifEmpty { null }

                if (name.isEmpty() || staffId.isEmpty() || position.isEmpty()) {
                    Toast.makeText(this, "Vui lòng điền đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newStaff = hashMapOf(
                    "name" to name,
                    "staffId" to staffId,
                    "position" to position,
                    "phone" to phone,
                    "email" to email,
                    "unitId" to unitId,
                    "photoURL" to photoUrl
                )

                db.collection("staff").add(newStaff)
                    .addOnSuccessListener { documentReference ->
                        Log.d("StaffActivity", "Thêm cán bộ giảng viên thành công: ${documentReference.id}")
                        Toast.makeText(this, "Thêm cán bộ giảng viên thành công!", Toast.LENGTH_SHORT).show()
                        loadStaffData()
                    }
                    .addOnFailureListener { e ->
                        Log.e("StaffActivity", "Lỗi khi thêm cán bộ giảng viên: ${e.message}")
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