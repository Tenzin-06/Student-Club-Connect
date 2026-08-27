package com.studentclubconnect.ui.events

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.studentclubconnect.databinding.ActivityEventDetailsBinding
import com.studentclubconnect.viewmodel.ClubState
import com.studentclubconnect.viewmodel.ClubViewModel
import com.studentclubconnect.viewmodel.EventState
import com.studentclubconnect.viewmodel.EventViewModel
import kotlinx.coroutines.launch

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailsBinding
    private val eventViewModel: EventViewModel by viewModels()
    private val clubViewModel: ClubViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val eventId = intent.getStringExtra("eventId")
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        observeViewModels()
        
        eventViewModel.getEventById(eventId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun observeViewModels() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                eventViewModel.eventState.collect { state ->
                    when (state) {
                        is EventState.Loading -> binding.progressBar.isVisible = true
                        is EventState.SingleSuccess -> {
                            binding.progressBar.isVisible = false
                            val event = state.event
                            if (event != null) {
                                displayEventDetails(event)
                                clubViewModel.getClubById(event.clubId)
                            } else {
                                Toast.makeText(this@EventDetailsActivity, "Event not found", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        is EventState.Error -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(this@EventDetailsActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                clubViewModel.clubState.collect { state ->
                    if (state is ClubState.SingleSuccess) {
                        binding.tvClubName.text = state.club?.name ?: "Unknown Club"
                    }
                }
            }
        }
    }

    private fun displayEventDetails(event: com.studentclubconnect.data.model.Event) {
        binding.apply {
            tvEventTitle.text = event.title
            tvEventDate.text = event.date
            tvEventTime.text = event.time
            tvEventLocation.text = event.location
            tvEventDescription.text = event.description
            
            // In a real app, use Glide/Coil for event.imageUrl
        }
    }
}
