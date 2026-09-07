package com.studentclubconnect.ui.clubs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.studentclubconnect.R
import com.studentclubconnect.data.model.Club
import com.studentclubconnect.databinding.ItemClubBinding

/**
 * Adapter for displaying the list of clubs using ListAdapter and DiffUtil.
 */
class ClubAdapter(
    private val showFooter: Boolean = true,
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
                
                // Bind member count if available (assuming it comes from the model now)
                tvMemberCount.text = root.context.getString(R.string.members_count, club.memberCount)
                
                // Hide footer for home screen if requested
                footerGroup.isVisible = showFooter
                
                // Set up click listener for the entire card
                root.setOnClickListener { onClubClick(club) }
                
                // Load club image
                ivClubIcon.load(club.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_clubs)
                    error(R.drawable.ic_clubs)
                }
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
