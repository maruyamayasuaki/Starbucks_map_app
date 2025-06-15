package com.example.starbucksmapapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class StarbucksStore(
    val name: String,
    val address: String,
    val placeId: String,
    val latitude: Double,
    val longitude: Double
)

class SearchResultAdapter(
    private var stores: List<StarbucksStore>,
    private val onStoreClick: (StarbucksStore) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val storeName: TextView = view.findViewById(R.id.store_name)
        val storeAddress: TextView = view.findViewById(R.id.store_address)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_result_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val store = stores[position]
        holder.storeName.text = store.name
        holder.storeAddress.text = store.address

        holder.itemView.setOnClickListener {
            onStoreClick(store)
        }
    }

    override fun getItemCount() = stores.size

    fun updateStores(newStores: List<StarbucksStore>) {
        stores = newStores
        notifyDataSetChanged()
    }
}