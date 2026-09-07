package com.studentclubconnect.ui.clubs

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.studentclubconnect.databinding.ActivityClubDetailsBinding
import coil.load
import com.studentclubconnect.ui.events.EventAdapter
import com.studentclubconnect.ui.events.EventDetailsActivity
import com.studentclubconnect.viewmodel.AuthViewModel
import com.studentclubconnect.viewmodel.ClubState
import com.studentclubconnect.viewmodel.ClubViewModel
import com.studentclubconnect.viewmodel.MembershipState
import com.studentclubconnect.viewmodel.MembershipViewModel
import com.studentclubconnect.viewmodel.AnnouncementState
import com.studentclubconnect.viewmodel.AnnouncementViewModel
import com.studentclubconnect.viewmodel.EventState
import com.studentclubconnect.viewmodel.EventViewModel
import kotlinx.coroutines.launch

class ClubDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClubDetailsBinding
    private val viewModel: ClubViewModel by viewModels()
    private val membershipViewModel: MembershipViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val announcementViewModel: AnnouncementViewModel by viewModels()
    private val eventViewModel: EventViewModel by viewModels()

    private lateinit var announcementAdapter: AnnouncementAdapter
    private lateinit var eventAdapter: EventAdapter
    
    private var isMember = false
    private var currentClubId: String? = null
    private var currentClub: com.studentclubconnect.data.model.Club? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClubDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentClubId = intent.getStringExtra("clubId")
        if (currentClubId.isNullOrEmpty()) {
            Toast.makeText(this, "Unable to load club details.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupTabs()
        setupAdapters()
        observeViewModels(currentClubId!!)

        binding.btnJoinClub.setOnClickListener {
            if (isMember) {
                membershipViewModel.leaveClub(currentClubId!!)
            } else {
                membershipViewModel.joinClub(currentClubId!!)
            }
        }

        binding.btnEditClub.setOnClickListener {
            val intent = Intent(this, AddEditClubActivity::class.java).apply {
                putExtra("clubId", currentClubId)
            }
            startActivity(intent)
        }

        binding.btnDeleteClub.setOnClickListener {
            showDeleteConfirmation(currentClubId!!)
        }
    }

    override fun onStart() {
        super.onStart()
        currentClubId?.let { id ->
            viewModel.getClubById(id)
            membershipViewModel.checkMembership(id)
            announcementViewModel.getAnnouncementsByClub(id)
            eventViewModel.getEventsByClub(id)
            authViewModel.getCurrentUser()?.uid?.let { authViewModel.loadUserProfile(it) }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showSection(about = true)
                    1 -> showSection(events = true)
                    2 -> showSection(announcements = true)
                    3 -> showSection(manage = true)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showSection(
        about: Boolean = false,
        events: Boolean = false,
        announcements: Boolean = false,
        manage: Boolean = false
    ) {
        binding.layoutAbout.isVisible = about
        binding.layoutEvents.isVisible = events
        binding.layoutAnnouncements.isVisible = announcements
        binding.adminActionContainer.isVisible = manage
    }

    private fun setupAdapters() {
        // Announcements
        announcementAdapter = AnnouncementAdapter(
            onEditClick = { announcement ->
                val intent = Intent(this, AddEditAnnouncementActivity::class.java).apply {
                    putExtra("announcementId", announcement.id)
                    putExtra("clubId", announcement.clubId)
                    putExtra("title", announcement.title)
                    putExtra("message", announcement.message)
                }
                startActivity(intent)
            },
            onDeleteClick = { announcement ->
                showDeleteAnnouncementConfirmation(announcement.id)
            }
        )
        binding.rvAnnouncements.apply {
            adapter = announcementAdapter
            layoutManager = LinearLayoutManager(this@ClubDetailsActivity)
        }

        // Events
        eventAdapter = EventAdapter(showFooter = true) { event ->
            val intent = Intent(this, EventDetailsActivity::class.java).apply {
                putExtra("eventId", event.id)
            }
            startActivity(intent)
        }
        binding.rvEvents.apply {
            adapter = eventAdapter
            layoutManager = LinearLayoutManager(this@ClubDetailsActivity)
        }
    }

    private fun showDeleteAnnouncementConfirmation(announcementId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Announcement")
            .setMessage("Are you sure you want to delete this announcement?")
            .setPositiveButton("Delete") { _, _ ->
                announcementViewModel.deleteAnnouncement(announcementId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModels(clubId: String) {
        // Observe Club Details
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.clubState.collect { state ->
                    when (state) {
                        is ClubState.Loading -> {
                            binding.progressBar.isVisible = true
                        }
                        is ClubState.SingleSuccess -> {
                            binding.progressBar.isVisible = false
                            currentClub = state.club
                            state.club?.let { 
                                displayClub(it)
                                announcementAdapter.setClubNames(mapOf(it.id to it.name))
                            } ?: run {
                                Toast.makeText(this@ClubDetailsActivity, "Club not found.", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        is ClubState.ActionSuccess -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(this@ClubDetailsActivity, state.message, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        is ClubState.Error -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(this@ClubDetailsActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }

        // Observe User Role for Manage Tab Visibility
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.userProfile.collect { user ->
                    val isAdmin = user?.role?.lowercase() == "admin"
                    val isPresident = user?.role?.lowercase() == "president" && user.presidentOf == clubId
                    val hasManageAccess = isAdmin || isPresident
                    
                    // Show/Hide Manage Tab
                    val manageTab = binding.tabLayout.getTabAt(3)
                    if (hasManageAccess) {
                        if (manageTab == null) {
                            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(com.studentclubconnect.R.string.manage))
                        }
                    } else {
                        if (manageTab != null) {
                            binding.tabLayout.removeTabAt(3)
                        }
                    }
                    
                    // Update announcement adapter with new user context
                    announcementAdapter.updateUserContext(
                        uid = user?.uid,
                        role = user?.role,
                        presidentOf = user?.presidentOf
                    )
                }
            }
        }

        // Observe Membership Status
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                membershipViewModel.membershipState.collect { state ->
                    when (state) {
                        is MembershipState.Loading -> {
                            binding.btnJoinClub.isEnabled = false
                        }
                        is MembershipState.Status -> {
                            binding.btnJoinClub.isEnabled = true
                            isMember = state.isMember
                            updateJoinButtonUI(isMember)
                        }
                        is MembershipState.Success -> {
                            binding.btnJoinClub.isEnabled = true
                            isMember = state.isMember
                            updateJoinButtonUI(isMember)
                            Toast.makeText(this@ClubDetailsActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        is MembershipState.Error -> {
                            binding.btnJoinClub.isEnabled = true
                            Toast.makeText(this@ClubDetailsActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }

        // Observe Events
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                eventViewModel.eventState.collect { state ->
                    when (state) {
                        is EventState.Success -> {
                            binding.tvNoEvents.isVisible = false
                            eventAdapter.submitList(state.events)
                        }
                        is EventState.Empty -> {
                            binding.tvNoEvents.isVisible = true
                            eventAdapter.submitList(emptyList())
                        }
                        is EventState.Error -> {
                            Toast.makeText(this@ClubDetailsActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }

        // Observe Announcements
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                announcementViewModel.announcementState.collect { state ->
                    when (state) {
                        is AnnouncementState.Loading -> {
                            binding.pbAnnouncements.isVisible = true
                            binding.tvNoAnnouncements.isVisible = false
                        }
                        is AnnouncementState.Success -> {
                            binding.pbAnnouncements.isVisible = false
                            binding.tvNoAnnouncements.isVisible = false
                            announcementAdapter.submitList(state.announcements)
                        }
                        is AnnouncementState.Empty -> {
                            binding.pbAnnouncements.isVisible = false
                            binding.tvNoAnnouncements.isVisible = true
                            announcementAdapter.submitList(emptyList())
                        }
                        is AnnouncementState.Error -> {
                            binding.pbAnnouncements.isVisible = false
                            Toast.makeText(this@ClubDetailsActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateJoinButtonUI(member: Boolean) {
        binding.btnJoinClub.text = if (member) "Leave Club" else "Join Club"
        // Also update member count when joining/leaving
        currentClubId?.let { viewModel.getClubById(it) }
    }

    private fun showDeleteConfirmation(clubId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Club")
            .setMessage("Are you sure you want to delete this club?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteClub(clubId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun displayClub(club: com.studentclubconnect.data.model.Club) {
        binding.apply {
            tvClubName.text = club.name
            tvClubCategory.text = club.category.ifEmpty { "General" }
            tvClubDescription.text = club.description.ifEmpty { "No description available." }
            tvPresidentName.text = club.president.ifEmpty { "No President assigned" }
            tvMemberCount.text = getString(com.studentclubconnect.R.string.members_count, club.memberCount)
            
            ivClubImage.load(club.imageUrl) {
                crossfade(true)
                placeholder(com.studentclubconnect.R.drawable.ic_clubs)
                error(com.studentclubconnect.R.drawable.ic_clubs)
            }
        }
    }
}
