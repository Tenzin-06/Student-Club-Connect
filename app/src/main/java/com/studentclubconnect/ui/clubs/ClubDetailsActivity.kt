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
import kotlinx.coroutines.launch

class ClubDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClubDetailsBinding
    private val viewModel: ClubViewModel by viewModels()

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
        observeViewModel()
        
        viewModel.getClubById(clubId)

        binding.btnJoinClub.setOnClickListener {
            Toast.makeText(this, "Join Club functionality coming soon.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun observeViewModel() {
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
                        else -> {
                            // Handle other states if necessary
                        }
                    }
                }
            }
        }
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
