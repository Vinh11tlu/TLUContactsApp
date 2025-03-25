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

class StaffAdapter(
    private val staffList: List<Staff>,
    private val unitMap: Map<String, Unit>,
    private val onStaffClick: (Staff) -> Task<QuerySnapshot>,
    private val onUnitClick: (Unit) -> Task<QuerySnapshot>
) : RecyclerView.Adapter<StaffAdapter.StaffViewHolder>() {

    class StaffViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPosition: TextView = itemView.findViewById(R.id.tvPosition)
        val tvUnit: TextView = itemView.findViewById(R.id.tvUnit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_staff, parent, false)
        return StaffViewHolder(view)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        val staff = staffList[position]
        holder.tvName.text = staff.name
        holder.tvPosition.text = staff.position
        holder.tvUnit.text = unitMap[staff.unitId]?.name ?: "Không thuộc đơn vị nào"

        staff.photoURL?.let {
            Glide.with(holder.itemView.context)
                .load(it)
                .placeholder(R.drawable.tlu)
                .error(R.drawable.tlu)
                .circleCrop()
                .into(holder.ivPhoto)
        } ?: holder.ivPhoto.setImageResource(R.drawable.tlu)

        holder.itemView.setOnClickListener { onStaffClick(staff) }
        holder.tvUnit.setOnClickListener {
            unitMap[staff.unitId]?.let { unit -> onUnitClick(unit) }
        }
    }

    override fun getItemCount(): Int = staffList.size
}