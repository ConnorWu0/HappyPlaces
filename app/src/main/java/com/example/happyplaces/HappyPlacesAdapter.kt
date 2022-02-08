package com.example.happyplaces

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.activities.AddHappyPlaceActivity
import com.example.happyplaces.activities.MainActivity
import com.example.happyplaces.databinding.ItemHappyPlaceBinding



class HappyPlacesAdapter(private val context: Context, private var items:MutableList<HappyPlaceEntity>):
    RecyclerView.Adapter<HappyPlacesAdapter.ViewHolder>() {

    private var onClickListener: OnClickListener? = null


    class ViewHolder(binding: ItemHappyPlaceBinding) : RecyclerView.ViewHolder(binding.root) {
        val ivPlaceImage = binding.ivPlaceImage
        val tvTitle = binding.tvTitle
        val tvDescription = binding.tvDescription
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
       return ViewHolder(ItemHappyPlaceBinding.inflate(
           LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item =items[position]

        holder.tvTitle.text =item.title
        holder.tvDescription.text = item.description
        holder.ivPlaceImage.setImageURI(Uri.parse(item.image))
        holder.itemView.setOnClickListener {
            if (onClickListener != null){

                onClickListener!!.onClick(position,item)
            }
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }
    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    interface OnClickListener {
        fun onClick(position: Int, happyPlaceEntity: HappyPlaceEntity)
    }

    fun notifyEditItem(activity: Activity, position: Int, requestCode: Int) {
        val intent = Intent(context, AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS, items[position])
        activity.startActivityForResult(intent,requestCode)

        notifyItemChanged(position) // Notify any registered observers that the item at position has changed.
    }

    fun removeAt(position: Int){

        if (items.size > 0) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

}