package com.studentclubconnect.ui.events

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.studentclubconnect.data.model.Club
import com.studentclubconnect.data.model.Event
import com.studentclubconnect.databinding.ActivityAddEditEventBinding
import com.studentclubconnect.viewmodel.ClubState
import com.studentclubconnect.viewmodel.ClubViewModel
import com.studentclubconnect.viewmodel.EventState
import com.studentclubconnect.viewmodel.EventViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AddEditEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditEventBinding
    private val eventViewModel: EventViewModel by viewModels()
    private val clubViewModel: ClubViewModel by viewModels()
    
    private var eventId: String? = null
    private var isEditMode = false
    private var selectedClubId: String = ""
    private var clubsList: List<Club> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getStringExtra("eventId")
        isEditMode = eventId != null

        setupToolbar()
        setupPickers()
        observeViewModels()

        clubViewModel.getClubs()

        if (isEditMode) {
            binding.toolbar.title = "Edit Event"
            binding.btnSubmit.text = "Save Changes"
            eventId?.let { eventViewModel.getEventById(it) }
        }

        binding.btnSubmit.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupPickers() {
        binding.etDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Event Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.etDate.setText(format.format(calendar.time))
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        binding.etTime.setOnClickListener {
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Event Time")
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val hour = timePicker.hour
                val minute = timePicker.minute
                val amPm = if (hour < 12) "AM" else "PM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                binding.etTime.setText(String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm))
            }
            timePicker.show(supportFragmentManager, "TIME_PICKER")
        }
    }

    private fun observeViewModels() {
        // Observe Events
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                eventViewModel.eventState.collect { state ->
                    when (state) {
                        is EventState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.btnSubmit.isEnabled = false
                        }
                        is EventState.SingleSuccess -> {
                            binding.progressBar.isVisible = false
                            binding.btnSubmit.isEnabled = true
                            if (isEditMode && state.event != null) {
                                populateFields(state.event)
                            }
                        }
                        is EventState.ActionSuccess -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(this@AddEditEventActivity, state.message, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        is EventState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.btnSubmit.isEnabled = true
                            Toast.makeText(this@AddEditEventActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }

        // Observe Clubs for Dropdown
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                clubViewModel.clubState.collect { state ->
                    if (state is ClubState.Success) {
                        clubsList = state.clubs
                        val clubNames = clubsList.map { it.name }
                        val adapter = ArrayAdapter(this@AddEditEventActivity, android.R.layout.simple_dropdown_item_1line, clubNames)
                        binding.actvClub.setAdapter(adapter)
                        
                        binding.actvClub.setOnItemClickListener { _, _, position, _ ->
                            selectedClubId = clubsList[position].id
                        }
                        
                        // If editing, re-set the club name to match the loaded ID
                        if (isEditMode && selectedClubId.isNotEmpty()) {
                            val selectedClub = clubsList.find { it.id == selectedClubId }
                            selectedClub?.let { binding.actvClub.setText(it.name, false) }
                        }
                    }
                }
            }
        }
    }

    private fun populateFields(event: Event) {
        binding.apply {
            etEventTitle.setText(event.title)
            etDate.setText(event.date)
            etTime.setText(event.time)
            etLocation.setText(event.location)
            etImageUrl.setText(event.imageUrl)
            etDescription.setText(event.description)
            selectedClubId = event.clubId
            
            // Try to set club name if list is already loaded
            if (clubsList.isNotEmpty()) {
                val club = clubsList.find { it.id == event.clubId }
                club?.let { actvClub.setText(it.name, false) }
            }
        }
    }

    private fun validateAndSubmit() {
        val title = binding.etEventTitle.text.toString().trim()
        val date = binding.etDate.text.toString().trim()
        val time = binding.etTime.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val imageUrl = binding.etImageUrl.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        var isValid = true

        if (title.isEmpty()) {
            binding.tilEventTitle.error = "Title is required"
            isValid = false
        } else {
            binding.tilEventTitle.error = null
        }

        if (date.isEmpty()) {
            binding.tilDate.error = "Date is required"
            isValid = false
        } else {
            binding.tilDate.error = null
        }

        if (location.isEmpty()) {
            binding.tilLocation.error = "Location is required"
            isValid = false
        } else {
            binding.tilLocation.error = null
        }

        if (description.isEmpty()) {
            binding.tilDescription.error = "Description is required"
            isValid = false
        } else {
            binding.tilDescription.error = null
        }
        
        if (selectedClubId.isEmpty()) {
            binding.tilClub.error = "Please select a club"
            isValid = false
        } else {
            binding.tilClub.error = null
        }

        if (isValid) {
            val event = Event(
                id = eventId ?: "",
                title = title,
                description = description,
                date = date,
                time = time,
                location = location,
                clubId = selectedClubId,
                imageUrl = imageUrl
            )

            if (isEditMode) {
                eventViewModel.updateEvent(event)
            } else {
                eventViewModel.createEvent(event)
            }
        }
    }
}
