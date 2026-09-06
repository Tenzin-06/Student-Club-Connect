package com.studentclubconnect.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studentclubconnect.data.model.User
import com.studentclubconnect.databinding.ItemMemberBinding

class MemberAdapter(
    private val currentPresidentId: String?,
    private val onRemoveClick: (User) -> Unit
) : ListAdapter<User, MemberAdapter.MemberViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemberViewHolder(private val binding: ItemMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                tvMemberName.text = user.name
                tvStudentId.text = "Student ID: ${user.studentId}"
                
                // Don't allow president to remove themselves
                btnRemove.isVisible = user.uid != currentPresidentId
                
                btnRemove.setOnClickListener { onRemoveClick(user) }
            }
        }
    }

    class MemberDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
