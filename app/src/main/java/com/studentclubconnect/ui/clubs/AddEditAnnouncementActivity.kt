package com.studentclubconnect.ui.clubs

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.studentclubconnect.data.model.Announcement
import com.studentclubconnect.data.model.Club
import com.studentclubconnect.databinding.ActivityAddEditAnnouncementBinding
import com.studentclubconnect.viewmodel.AnnouncementState
import com.studentclubconnect.viewmodel.AnnouncementViewModel
import com.studentclubconnect.viewmodel.AuthViewModel
import com.studentclubconnect.viewmodel.ClubState
import com.studentclubconnect.viewmodel.ClubViewModel
import kotlinx.coroutines.launch

class AddEditAnnouncementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditAnnouncementBinding
    private val announcementViewModel: AnnouncementViewModel by viewModels()
    private val clubViewModel: ClubViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private var announcementId: String? = null
    private var isEditMode = false
    private var selectedClubId: String = ""
    private var clubsList: List<Club> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditAnnouncementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        announcementId = intent.getStringExtra("announcementId")
        isEditMode = announcementId != null

        setupToolbar()
        observeViewModels()

        authViewModel.getCurrentUser()?.uid?.let { authViewModel.loadUserProfile(it) }
        clubViewModel.getClubs()

        if (isEditMode) {
            binding.toolbar.title = "Edit Announcement"
            binding.btnSubmit.text = "Save Changes"
            
            // Populate fields from extras
            binding.etTitle.setText(intent.getStringExtra("title"))
            binding.etMessage.setText(intent.getStringExtra("message"))
            selectedClubId = intent.getStringExtra("clubId") ?: ""
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

    private fun observeViewModels() {
        // Observe Current User for Authorization
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.userProfile.collect { user ->
                    val userRole = user?.role?.lowercase()
                    val presidentOf = user?.presidentOf

                    if (userRole == "president") {
                        binding.tilClub.isEnabled = false
                        binding.actvClub.isEnabled = false
                        if (!isEditMode) {
                            selectedClubId = presidentOf ?: ""
                        }
                    }
                    
                    updateClubDropdown(userRole, presidentOf)
                }
            }
        }

        // Observe Clubs
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                clubViewModel.clubState.collect { state ->
                    if (state is ClubState.Success) {
                        clubsList = state.clubs
                        val user = authViewModel.userProfile.value
                        updateClubDropdown(user?.role?.lowercase(), user?.presidentOf)
                    }
                }
            }
        }

        // Observe Announcement Actions
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                announcementViewModel.announcementState.collect { state ->
                    when (state) {
                        is AnnouncementState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.btnSubmit.isEnabled = false
                        }
                        is AnnouncementState.ActionSuccess -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(this@AddEditAnnouncementActivity, state.message, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        is AnnouncementState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.btnSubmit.isEnabled = true
                            Toast.makeText(this@AddEditAnnouncementActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateClubDropdown(role: String?, presidentOf: String?) {
        val filteredClubs = when (role) {
            "president" -> clubsList.filter { it.id == presidentOf }
            "admin" -> clubsList
            else -> emptyList()
        }

        val names = filteredClubs.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        binding.actvClub.setAdapter(adapter)

        binding.actvClub.setOnItemClickListener { _, _, position, _ ->
            selectedClubId = filteredClubs[position].id
        }

        // If editing or only one club available (President), select it
        if (selectedClubId.isNotEmpty()) {
            val club = filteredClubs.find { it.id == selectedClubId }
            club?.let { binding.actvClub.setText(it.name, false) }
        } else if (role == "president" && filteredClubs.size == 1) {
            val club = filteredClubs[0]
            binding.actvClub.setText(club.name, false)
            selectedClubId = club.id
        }
    }

    private fun validateAndSubmit() {
        val title = binding.etTitle.text.toString().trim()
        val message = binding.etMessage.text.toString().trim()
        val userId = authViewModel.getCurrentUser()?.uid ?: ""

        var isValid = true

        if (selectedClubId.isEmpty()) {
            binding.tilClub.error = "Please select a club"
            isValid = false
        } else {
            binding.tilClub.error = null
        }

        if (title.isEmpty()) {
            binding.tilTitle.error = "Title is required"
            isValid = false
        } else {
            binding.tilTitle.error = null
        }

        if (message.isEmpty()) {
            binding.tilMessage.error = "Message is required"
            isValid = false
        } else {
            binding.tilMessage.error = null
        }

        if (isValid) {
            val announcement = Announcement(
                id = announcementId ?: "",
                clubId = selectedClubId,
                title = title,
                message = message,
                createdBy = userId
            )

            if (isEditMode) {
                announcementViewModel.updateAnnouncement(announcement)
            } else {
                announcementViewModel.createAnnouncement(announcement)
            }
        }
    }
}
