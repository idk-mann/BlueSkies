package com.example.myapplication2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication2.R

class LocationAdapter(
    private var items: MutableList<String>,
    private val onDeleteModeChanged: (Boolean) -> Unit = {}
) : RecyclerView.Adapter<LocationAdapter.ViewHolder>() {

    var deleteMode = false
        set(value) {
            field = value
            onDeleteModeChanged(value)
            if (!value) selectedItems.clear()
            notifyDataSetChanged()
        }

    private val selectedItems = mutableSetOf<String>()
    var onItemClick: ((String) -> Unit)? = null
    var onItemLongClick: ((String) -> Boolean)? = null

    fun getSelectedItems(): List<String> = selectedItems.toList()

    fun updateList(newList: List<String>) {
        items = newList.toMutableList()
        if (!deleteMode) selectedItems.clear()
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.location_name)
        private val checkbox: CheckBox = view.findViewById(R.id.checkbox)

        fun bind(label: String) {
            name.text = label
            val isProtected = (label in listOf("Current Location", "Kuala Lumpur") || label == "+ Add Location")

            checkbox.visibility = if (deleteMode && !isProtected) View.VISIBLE else View.GONE
            checkbox.isEnabled = !isProtected

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = selectedItems.contains(label)

            itemView.setOnClickListener {
                if (deleteMode && !isProtected) {
                    checkbox.isChecked = !checkbox.isChecked
                } else {
                    onItemClick?.invoke(label)
                }
            }

            itemView.setOnLongClickListener {
                if (!isProtected) {
                    deleteMode = true
                    true
                } else {
                    false
                }
            }

            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedItems.add(label) else selectedItems.remove(label)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.location_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}