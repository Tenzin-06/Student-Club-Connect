package com.studentclubconnect.ui.clubs

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.studentclubconnect.databinding.ActivityClubDetailsBinding
import com.studentclubconnect.viewmodel.AuthViewModel
import com.studentclubconnect.viewmodel.ClubState
import com.studentclubconnect.viewmodel.ClubViewModel
import com.studentclubconnect.viewmodel.MembershipState
import com.studentclubconnect.viewmodel.MembershipViewModel
import kotlinx.coroutines.launch

class ClubDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClubDetailsBinding
    private val viewModel: ClubViewModel by viewModels()
    private val membershipViewModel: MembershipViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private var isMember = false
    private var currentClub: com.studentclubconnect.data.model.Club? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClubDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val clubId = intent.getStringExtra("clubId")
        if (clubId.isNullOrEmpty()) {
            Toast.makeText(this, "Unable to load club details.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        observeViewModels(clubId)
        
        viewModel.getClubById(clubId)
        membershipViewModel.checkMembership(clubId)
        authViewModel.getCurrentUser()?.uid?.let { authViewModel.loadUserProfile(it) }

        binding.btnJoinClub.setOnClickListener {
            if (isMember) {
                membershipViewModel.leaveClub(clubId)
            } else {
                membershipViewModel.joinClub(clubId)
            }
        }

        binding.btnEditClub.setOnClickListener {
            val intent = android.content.Intent(this, AddEditClubActivity::class.java).apply {
                putExtra("clubId", clubId)
            }
            startActivity(intent)
        }

        binding.btnDeleteClub.setOnClickListener {
            showDeleteConfirmation(clubId)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
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
                            state.club?.let { displayClub(it) } ?: run {
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

        // Observe User Role for Admin Actions
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.userProfile.collect { user ->
                    binding.adminActionContainer.isVisible = user?.role?.lowercase() == "admin"
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
                        is MembershipState.AuthExpired -> {
                            Toast.makeText(this@ClubDetailsActivity, "Authentication expired. Please log in again.", Toast.LENGTH_LONG).show()
                            // In a real app, redirect to login
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateJoinButtonUI(member: Boolean) {
        binding.btnJoinClub.text = if (member) "Leave Club" else "Join Club"
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
            tvPresidentName.text = club.president.ifEmpty { "Information unavailable" }
            
            // Image loading would go here (e.g. Glide.with(this).load(club.imageUrl)...)
            // For now it uses the placeholder in XML
        }
    }
}
