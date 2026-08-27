package com.studentclubconnect.ui.clubs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studentclubconnect.data.model.Club
import com.studentclubconnect.databinding.ItemClubBinding

/**
 * Adapter for displaying the list of clubs using ListAdapter and DiffUtil.
 */
class ClubAdapter(
    private val onClubClick: (Club) -> Unit
) : ListAdapter<Club, ClubAdapter.ClubViewHolder>(ClubDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClubViewHolder {
        val binding = ItemClubBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClubViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClubViewHolder, position: Int) {
        val club = getItem(position)
        holder.bind(club)
    }

    inner class ClubViewHolder(private val binding: ItemClubBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(club: Club) {
            binding.apply {
                tvClubName.text = club.name
                tvClubCategory.text = club.category
                tvClubDescription.text = club.description
                
                // Set up click listener for the entire card or the "View Club" action
                root.setOnClickListener { onClubClick(club) }
                
                // In a real app, we would use Glide or Coil to load club.imageUrl into ivClubIcon
                // For now, we use the default placeholder from XML
            }
        }
    }

    class ClubDiffCallback : DiffUtil.ItemCallback<Club>() {
        override fun areItemsTheSame(oldItem: Club, newItem: Club): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Club, newItem: Club): Boolean {
            return oldItem == newItem
        }
    }
}
