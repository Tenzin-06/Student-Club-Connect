package com.studentclubconnect.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studentclubconnect.data.model.Announcement
import com.studentclubconnect.databinding.ItemNotificationBinding
import com.studentclubconnect.utils.TimeUtils

/**
 * Adapter for displaying notifications derived from announcements.
 */
class NotificationAdapter : ListAdapter<Announcement, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    private var clubNames: Map<String, String> = emptyMap()

    fun setClubNames(names: Map<String, String>) {
        clubNames = names
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val announcement = getItem(position)
        holder.bind(announcement, clubNames[announcement.clubId])
    }

    class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(announcement: Announcement, clubName: String?) {
            binding.apply {
                tvNotificationClub.text = clubName ?: "Unknown Club"
                tvNotificationTitle.text = "New announcement: ${announcement.title}"
                tvNotificationTime.text = TimeUtils.getRelativeTime(announcement.createdAt)
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Announcement>() {
        override fun areItemsTheSame(oldItem: Announcement, newItem: Announcement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Announcement, newItem: Announcement): Boolean {
            return oldItem == newItem
        }
    }
}
