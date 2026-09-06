package com.studentclubconnect.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studentclubconnect.R
import com.studentclubconnect.data.model.User
import com.studentclubconnect.databinding.ItemUserBinding

class UserAdapter : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    private var clubNames: Map<String, String> = emptyMap()

    fun setClubNames(names: Map<String, String>) {
        clubNames = names
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                tvUserName.text = user.name
                tvUserEmail.text = user.email
                
                val role = user.role.lowercase()
                tvUserRoleBadge.text = role.replaceFirstChar { it.uppercase() }
                
                // Set color for badge
                val badgeColor = when (role) {
                    "admin" -> root.context.getColor(R.color.primary_navy)
                    "president" -> root.context.getColor(R.color.primary_navy)
                    else -> root.context.getColor(R.color.text_secondary)
                }
                tvUserRoleBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(badgeColor)

                if (role == "president" && !user.presidentOf.isNullOrEmpty()) {
                    val clubName = clubNames[user.presidentOf] ?: "Loading club..."
                    tvUserClubInfo.text = "President of $clubName"
                    tvUserClubInfo.isVisible = true
                } else {
                    tvUserClubInfo.isVisible = false
                }
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
