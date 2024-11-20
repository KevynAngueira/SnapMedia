package com.example.snapmedia.old

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.snapmedia.R
import com.example.snapmedia.SharedStoragePhoto

class SharedPhotoAdapter(private val photos: List<SharedStoragePhoto>) : RecyclerView.Adapter<SharedPhotoAdapter.PhotoViewHolder>() {

    // ViewHolder to hold image views and text views
    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView) // Image view for the image
        val textView: TextView = itemView.findViewById(R.id.textView) // Text view for the image name
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]

        // Load the image using Glide or any image loading library
        Glide.with(holder.imageView.context)
            .load(photo.contentUri)
            .into(holder.imageView)

        // Set the image name or display info
        holder.textView.text = photo.name
    }

    override fun getItemCount(): Int {
        return photos.size
    }
}
