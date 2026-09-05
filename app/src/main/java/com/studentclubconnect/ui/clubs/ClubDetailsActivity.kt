package com.studentclubconnect.ui.clubs

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.studentclubconnect.databinding.ActivityClubDetailsBinding
import com.studentclubconnect.viewmodel.ClubState
import com.studentclubconnect.viewmodel.ClubViewModel
import com.studentclubconnect.viewmodel.MembershipState
import com.studentclubconnect.viewmodel.MembershipViewModel
import kotlinx.coroutines.launch

class ClubDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClubDetailsBinding
    private val viewModel: ClubViewModel by viewModels()
    private val membershipViewModel: MembershipViewModel by viewModels()

    private var isMember = false

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

        binding.btnJoinClub.setOnClickListener {
            if (isMember) {
                membershipViewModel.leaveClub(clubId)
            } else {
                membershipViewModel.joinClub(clubId)
            }
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
                            state.club?.let { displayClub(it) } ?: run {
                                Toast.makeText(this@ClubDetailsActivity, "Club not found.", Toast.LENGTH_SHORT).show()
                                finish()
                            }
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
