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

class UnitAdapter(
    private val unitList: List<Unit>,
    private val onUnitClick: (Unit) -> Task<QuerySnapshot>
) : RecyclerView.Adapter<UnitAdapter.UnitViewHolder>() {

    class UnitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvUnitId: TextView = itemView.findViewById(R.id.tvUnitId)
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_unit, parent, false)
        return UnitViewHolder(view)
    }

    override fun onBindViewHolder(holder: UnitViewHolder, position: Int) {
        val unit = unitList[position]
        holder.tvName.text = unit.name
        holder.tvUnitId.text = unit.unitId
        holder.tvAddress.text = unit.address ?: "Không có địa chỉ"

        unit.photoURL?.let {
            Glide.with(holder.itemView.context)
                .load(it)
                .placeholder(R.drawable.tlu)
                .error(R.drawable.tlu)
                .circleCrop()
                .into(holder.ivPhoto)
        } ?: holder.ivPhoto.setImageResource(R.drawable.tlu)

        holder.itemView.setOnClickListener { onUnitClick(unit) }
    }

    override fun getItemCount(): Int = unitList.size
}