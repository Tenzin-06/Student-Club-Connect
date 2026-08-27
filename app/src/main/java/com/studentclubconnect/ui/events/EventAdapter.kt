package com.studentclubconnect.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studentclubconnect.data.model.Event
import com.studentclubconnect.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for displaying the list of events using ListAdapter and DiffUtil.
 */
class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
    }

    inner class EventViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.apply {
                tvEventTitle.text = event.title
                tvEventTime.text = "${event.date}, ${event.time}"
                tvEventLocation.text = event.location
                
                // Parse date for badge
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = inputFormat.parse(event.date)
                    if (date != null) {
                        tvEventMonth.text = SimpleDateFormat("MMM", Locale.getDefault()).format(date).uppercase()
                        tvEventDay.text = SimpleDateFormat("dd", Locale.getDefault()).format(date)
                    } else {
                        tvEventMonth.text = "EVENT"
                        tvEventDay.text = "--"
                    }
                } catch (e: Exception) {
                    tvEventMonth.text = "EVENT"
                    tvEventDay.text = "--"
                }

                root.setOnClickListener { onEventClick(event) }
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }
    }
}
