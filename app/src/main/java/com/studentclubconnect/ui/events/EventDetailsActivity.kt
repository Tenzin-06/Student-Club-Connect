package com.studentclubconnect.ui.events

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.studentclubconnect.databinding.ActivityEventDetailsBinding
import com.studentclubconnect.viewmodel.AuthViewModel
import com.studentclubconnect.viewmodel.ClubState
import com.studentclubconnect.viewmodel.ClubViewModel
import com.studentclubconnect.viewmodel.EventState
import com.studentclubconnect.viewmodel.EventViewModel
import com.studentclubconnect.viewmodel.EventReminderState
import com.studentclubconnect.viewmodel.EventReminderViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailsBinding
    private val eventViewModel: EventViewModel by viewModels()
    private val clubViewModel: ClubViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val reminderViewModel: EventReminderViewModel by viewModels()

    private var currentReminderId: String? = null
    private var isReminderSet = false

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
        observeViewModels(eventId)
        
        eventViewModel.getEventById(eventId)
        reminderViewModel.checkReminder(eventId)
        authViewModel.getCurrentUser()?.uid?.let { authViewModel.loadUserProfile(it) }

        binding.btnSetReminder.setOnClickListener {
            toggleReminder()
        }

        binding.btnEditEvent.setOnClickListener {
            val intent = android.content.Intent(this, AddEditEventActivity::class.java).apply {
                putExtra("eventId", eventId)
            }
            startActivity(intent)
        }

        binding.btnDeleteEvent.setOnClickListener {
            showDeleteConfirmation(eventId)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun observeViewModels(eventId: String) {
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
                        is EventState.ActionSuccess -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(this@EventDetailsActivity, state.message, Toast.LENGTH_SHORT).show()
                            finish()
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

        // Observe Reminders
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                reminderViewModel.reminderState.collect { state ->
                    when (state) {
                        is EventReminderState.Loading -> {
                            binding.btnSetReminder.isEnabled = false
                        }
                        is EventReminderState.SingleSuccess -> {
                            binding.btnSetReminder.isEnabled = true
                            isReminderSet = state.reminder != null
                            currentReminderId = state.reminder?.id
                            updateReminderButtonUI()
                        }
                        is EventReminderState.ActionSuccess -> {
                            binding.btnSetReminder.isEnabled = true
                            Toast.makeText(this@EventDetailsActivity, state.message, Toast.LENGTH_SHORT).show()
                            // Re-check to update UI correctly
                            reminderViewModel.checkReminder(eventId)
                        }
                        is EventReminderState.Error -> {
                            binding.btnSetReminder.isEnabled = true
                            Toast.makeText(this@EventDetailsActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }

        // Observe User Role for Admin Actions
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.userProfile.collect { user ->
                    binding.adminActionContainer.isVisible = user?.role?.lowercase() == "admin"
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

    private fun updateReminderButtonUI() {
        binding.btnSetReminder.text = if (isReminderSet) "✓ Reminder already set" else "🔔 Set Reminder"
        // If already set, we can keep it disabled or allow removal. 
        // The prompt says "Do not create another reminder when the user taps the button again".
    }

    private fun toggleReminder() {
        if (isReminderSet) {
            // Option: Allow removal if tapped again, or just show toast
            AlertDialog.Builder(this)
                .setTitle("Reminder Already Set")
                .setMessage("A reminder is already set for this event. Would you like to remove it?")
                .setPositiveButton("Remove") { _, _ ->
                    currentReminderId?.let { reminderViewModel.deleteReminder(it) }
                }
                .setNegativeButton("Keep", null)
                .show()
            return
        }

        val event = (eventViewModel.eventState.value as? EventState.SingleSuccess)?.event
        if (event == null) {
            Toast.makeText(this, "Unable to load event data", Toast.LENGTH_SHORT).show()
            return
        }

        showReminderOptionsDialog(event)
    }

    private fun showReminderOptionsDialog(event: com.studentclubconnect.data.model.Event) {
        val options = arrayOf("1 hour before", "1 day before")
        var selectedOption = 0

        AlertDialog.Builder(this)
            .setTitle("Set Reminder")
            .setSingleChoiceItems(options, 0) { _, which ->
                selectedOption = if (which == 0) 0 else 1
            }
            .setPositiveButton("Set Reminder") { _, _ ->
                calculateAndSaveReminder(event, selectedOption)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun calculateAndSaveReminder(event: com.studentclubconnect.data.model.Event, option: Int) {
        try {
            val dateTimeStr = "${event.date} ${event.time}"
            val format = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
            val eventDate = format.parse(dateTimeStr)

            if (eventDate == null) {
                Toast.makeText(this, "Invalid event date/time", Toast.LENGTH_SHORT).show()
                return
            }

            val calendar = Calendar.getInstance()
            calendar.time = eventDate

            if (option == 0) { // 1 hour before
                calendar.add(Calendar.HOUR_OF_DAY, -1)
            } else { // 1 day before
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }

            val reminderTime = calendar.time
            val now = Calendar.getInstance().time

            if (reminderTime.before(now)) {
                Toast.makeText(this, "Reminder time has already passed", Toast.LENGTH_SHORT).show()
                return
            }

            val userId = authViewModel.getCurrentUser()?.uid
            if (userId == null) {
                Toast.makeText(this, "Please log in to set reminders", Toast.LENGTH_SHORT).show()
                return
            }

            val reminder = com.studentclubconnect.data.model.EventReminder(
                eventId = event.id,
                userId = userId,
                reminderTime = reminderTime
            )
            reminderViewModel.saveReminder(reminder)

        } catch (e: Exception) {
            Toast.makeText(this, "Unable to set reminder. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation(eventId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete this event?")
            .setPositiveButton("Delete") { _, _ ->
                eventViewModel.deleteEvent(eventId)
            }
            .setNegativeButton("Cancel", null)
            .show()
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
