package com.example.tlucontactapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.QuerySnapshot

class StudentAdapter(
    private val studentList: List<Student>,
    private val unitMap: Map<String, Unit>,
    private val onStudentClick: (Student) -> Task<QuerySnapshot>,
    private val onUnitClick: (Unit) -> Task<QuerySnapshot>
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvStudentId: TextView = itemView.findViewById(R.id.tvStudentId)
        val tvClass: TextView = itemView.findViewById(R.id.tvClass)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = studentList[position]
        holder.tvName.text = student.name
        holder.tvStudentId.text = student.studentId
        holder.tvClass.text = unitMap[student.classId]?.name ?: "Không thuộc lớp nào"

        student.photoURL?.let {
            Glide.with(holder.itemView.context)
                .load(it)
                .placeholder(R.drawable.tlu)
                .error(R.drawable.tlu)
                .circleCrop()
                .into(holder.ivPhoto)
        } ?: holder.ivPhoto.setImageResource(R.drawable.tlu)

        holder.itemView.setOnClickListener { onStudentClick(student) }
        holder.tvClass.setOnClickListener {
            unitMap[student.classId]?.let { unit -> onUnitClick(unit) }
        }
    }

    override fun getItemCount(): Int = studentList.size
}