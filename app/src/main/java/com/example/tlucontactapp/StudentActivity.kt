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

class StudentActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvStudents: RecyclerView
    private lateinit var studentAdapter: StudentAdapter
    private lateinit var drawerLayout: DrawerLayout
    private val studentList = mutableListOf<Student>()
    private val filteredStudentList = mutableListOf<Student>()
    private val unitList = mutableListOf<Unit>()
    private val unitMap = mutableMapOf<String, Unit>()
    private var sortBy = "name"
    private var filterUnitId: String? = null
    private val adminEmails = listOf("vinhlovehmuk4@gmail.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Khởi tạo DrawerLayout và NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Khởi tạo RecyclerView
        rvStudents = findViewById(R.id.rvStudents)
        rvStudents.layoutManager = LinearLayoutManager(this)
        studentAdapter = StudentAdapter(
            filteredStudentList,
            unitMap,
            { student ->
                db.collection("students").whereEqualTo("studentId", student.studentId).get()
                    .addOnSuccessListener { result ->
                        if (result.isEmpty) {
                            Toast.makeText(this, "Không tìm thấy thông tin sinh viên!", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        val studentDocId = result.documents.firstOrNull()?.id
                        val intent = Intent(this, StudentDetailActivity::class.java).apply {
                            putExtra("student", student)
                            putExtra("studentId", studentDocId)
                        }
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Log.e("StudentActivity", "Lỗi khi truy vấn sinh viên: ${e.message}")
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
                        Log.e("StudentActivity", "Lỗi khi truy vấn đơn vị: ${e.message}")
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        )
        rvStudents.adapter = studentAdapter

        // Xử lý nút thêm sinh viên
        val btnAddStudent = findViewById<Button>(R.id.btnAddStudent)
        val currentEmail = auth.currentUser?.email
        if (currentEmail in adminEmails) {
            btnAddStudent.visibility = View.VISIBLE
            btnAddStudent.setOnClickListener { showAddStudentDialog() }
        } else {
            btnAddStudent.visibility = View.GONE
        }

        // Xử lý thanh tìm kiếm
        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterStudents(s.toString())
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
        loadStudentData()
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
                        unit.unitId?.let { unitMap[it] = unit }
                    } catch (e: Exception) {
                        Log.e("StudentActivity", "Lỗi khi ánh xạ dữ liệu đơn vị: ${e.message}")
                    }
                }
                studentAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("StudentActivity", "Lỗi khi tải dữ liệu đơn vị: ${e.message}")
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadStudentData() {
        db.collection("students").get()
            .addOnSuccessListener { result ->
                studentList.clear()
                for (document in result) {
                    try {
                        val student = document.toObject(Student::class.java)
                        studentList.add(student)
                    } catch (e: Exception) {
                        Log.e("StudentActivity", "Lỗi khi ánh xạ dữ liệu sinh viên: ${e.message}")
                    }
                }
                filterStudents(findViewById<EditText>(R.id.etSearch).text.toString())
                Log.d("StudentActivity", "Tải dữ liệu thành công: ${studentList.size} sinh viên")
                if (studentList.isEmpty()) {
                    Toast.makeText(this, "Không có sinh viên nào!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("StudentActivity", "Lỗi khi tải dữ liệu sinh viên: ${e.message}")
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun filterStudents(searchText: String) {
        filteredStudentList.clear()
        filteredStudentList.addAll(studentList.filter {
            (it.name.contains(searchText, ignoreCase = true) ||
                    it.studentId.contains(searchText, ignoreCase = true)) &&
                    (filterUnitId == null || it.classId == filterUnitId)
        })

        when (sortBy) {
            "name" -> filteredStudentList.sortBy { it.name }
            "studentId" -> filteredStudentList.sortBy { it.studentId }
            "class" -> filteredStudentList.sortBy { unitMap[it.classId]?.name ?: "" }
        }

        studentAdapter.notifyDataSetChanged()
    }

    private fun showSortDialog() {
        val options = arrayOf("Theo tên", "Theo mã sinh viên", "Theo lớp")
        AlertDialog.Builder(this)
            .setTitle("Sắp xếp")
            .setItems(options) { _, which ->
                sortBy = when (which) {
                    0 -> "name"
                    1 -> "studentId"
                    2 -> "class"
                    else -> "name"
                }
                filterStudents(findViewById<EditText>(R.id.etSearch).text.toString())
            }
            .show()
    }

    private fun showFilterDialog() {
        val unitNames = mutableListOf("Tất cả")
        unitNames.addAll(unitList.map { it.name })
        val unitIds = mutableListOf<String?>(null)
        unitIds.addAll(unitList.map { it.unitId })

        AlertDialog.Builder(this)
            .setTitle("Lọc theo lớp")
            .setItems(unitNames.toTypedArray()) { _, which ->
                filterUnitId = unitIds[which]
                filterStudents(findViewById<EditText>(R.id.etSearch).text.toString())
            }
            .show()
    }

    private fun showAddStudentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_student, null)
        val etStudentName = dialogView.findViewById<EditText>(R.id.etStudentName)
        val etStudentId = dialogView.findViewById<EditText>(R.id.etStudentId)
        val etStudentPhone = dialogView.findViewById<EditText>(R.id.etStudentPhone)
        val etStudentEmail = dialogView.findViewById<EditText>(R.id.etStudentEmail)
        val etStudentAddress = dialogView.findViewById<EditText>(R.id.etStudentAddress)
        val etClassId = dialogView.findViewById<EditText>(R.id.etClassId)
        val etStudentPhotoUrl = dialogView.findViewById<EditText>(R.id.etStudentPhotoUrl)

        AlertDialog.Builder(this)
            .setTitle("Thêm sinh viên")
            .setView(dialogView)
            .setPositiveButton("Thêm") { _, _ ->
                val name = etStudentName.text.toString().trim()
                val studentId = etStudentId.text.toString().trim()
                val phone = etStudentPhone.text.toString().trim()
                val email = etStudentEmail.text.toString().trim()
                val address = etStudentAddress.text.toString().trim()
                val classId = etClassId.text.toString().trim().ifEmpty { null }
                val photoUrl = etStudentPhotoUrl.text.toString().trim().ifEmpty { null }

                if (name.isEmpty() || studentId.isEmpty()) {
                    Toast.makeText(this, "Vui lòng điền đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newStudent = hashMapOf(
                    "name" to name,
                    "studentId" to studentId,
                    "phone" to phone,
                    "email" to email,
                    "address" to address,
                    "classId" to classId,
                    "photoURL" to photoUrl
                )

                db.collection("students").add(newStudent)
                    .addOnSuccessListener { documentReference ->
                        Log.d("StudentActivity", "Thêm sinh viên thành công: ${documentReference.id}")
                        Toast.makeText(this, "Thêm sinh viên thành công!", Toast.LENGTH_SHORT).show()
                        loadStudentData()
                    }
                    .addOnFailureListener { e ->
                        Log.e("StudentActivity", "Lỗi khi thêm sinh viên: ${e.message}")
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
