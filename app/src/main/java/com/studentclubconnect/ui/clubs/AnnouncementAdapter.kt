package com.studentclubconnect.ui.clubs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studentclubconnect.data.model.Announcement
import com.studentclubconnect.databinding.ItemAnnouncementBinding
import com.studentclubconnect.utils.TimeUtils

/**
 * Adapter for displaying announcements in a list.
 */
class AnnouncementAdapter(
    private val currentUserId: String? = null,
    private val currentUserRole: String? = null,
    private val userPresidentOf: String? = null,
    private val onEditClick: ((Announcement) -> Unit)? = null,
    private val onDeleteClick: ((Announcement) -> Unit)? = null
) : ListAdapter<Announcement, AnnouncementAdapter.AnnouncementViewHolder>(AnnouncementDiffCallback()) {

    private var clubNames: Map<String, String> = emptyMap()

    fun setClubNames(names: Map<String, String>) {
        clubNames = names
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val binding = ItemAnnouncementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnnouncementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        val announcement = getItem(position)
        holder.bind(announcement, clubNames[announcement.clubId])
    }

    inner class AnnouncementViewHolder(private val binding: ItemAnnouncementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(announcement: Announcement, clubName: String?) {
            binding.apply {
                tvAnnouncementTitle.text = announcement.title
                tvAnnouncementClub.text = clubName ?: "Unknown Club"
                tvAnnouncementMessage.text = announcement.message
                tvAnnouncementTime.text = TimeUtils.getRelativeTime(announcement.createdAt)

                val isAdmin = currentUserRole?.lowercase() == "admin"
                val isPresident = currentUserRole?.lowercase() == "president" && 
                                announcement.clubId == userPresidentOf
                
                layoutManagement.isVisible = (isAdmin || isPresident) && (onEditClick != null || onDeleteClick != null)
                
                btnEdit.setOnClickListener { onEditClick?.invoke(announcement) }
                btnDelete.setOnClickListener { onDeleteClick?.invoke(announcement) }
            }
        }
    }

    class AnnouncementDiffCallback : DiffUtil.ItemCallback<Announcement>() {
        override fun areItemsTheSame(oldItem: Announcement, newItem: Announcement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Announcement, newItem: Announcement): Boolean {
            return oldItem == newItem
        }
    }
}
