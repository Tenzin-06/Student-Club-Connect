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
import com.studentclubconnect.data.model.Club
import com.studentclubconnect.databinding.ActivityAddEditClubBinding
import com.studentclubconnect.viewmodel.ClubState
import com.studentclubconnect.viewmodel.ClubViewModel
import kotlinx.coroutines.launch

class AddEditClubActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditClubBinding
    private val viewModel: ClubViewModel by viewModels()
    private var clubId: String? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditClubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        clubId = intent.getStringExtra("clubId")
        isEditMode = clubId != null

        setupToolbar()
        setupCategoryDropdown()
        observeViewModel()

        if (isEditMode) {
            binding.toolbar.title = "Edit Club"
            binding.btnSubmit.text = "Save Changes"
            clubId?.let { viewModel.getClubById(it) }
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

    private fun setupCategoryDropdown() {
        val categories = arrayOf("Technology", "Sports", "Arts", "Cultural", "Social", "Academic")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.clubState.collect { state ->
                    when (state) {
                        is ClubState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.btnSubmit.isEnabled = false
                        }
                        is ClubState.SingleSuccess -> {
                            binding.progressBar.isVisible = false
                            binding.btnSubmit.isEnabled = true
                            if (isEditMode && state.club != null) {
                                populateFields(state.club)
                            }
                        }
                        is ClubState.ActionSuccess -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(this@AddEditClubActivity, state.message, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        is ClubState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.btnSubmit.isEnabled = true
                            Toast.makeText(this@AddEditClubActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun populateFields(club: Club) {
        binding.apply {
            etClubName.setText(club.name)
            actvCategory.setText(club.category, false)
            etPresident.setText(club.president)
            etImageUrl.setText(club.imageUrl)
            etDescription.setText(club.description)
        }
    }

    private fun validateAndSubmit() {
        val name = binding.etClubName.text.toString().trim()
        val category = binding.actvCategory.text.toString().trim()
        val president = binding.etPresident.text.toString().trim()
        val imageUrl = binding.etImageUrl.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        var isValid = true

        if (name.isEmpty()) {
            binding.tilClubName.error = "Name is required"
            isValid = false
        } else {
            binding.tilClubName.error = null
        }

        if (category.isEmpty()) {
            binding.tilCategory.error = "Category is required"
            isValid = false
        } else {
            binding.tilCategory.error = null
        }

        if (description.isEmpty()) {
            binding.tilDescription.error = "Description is required"
            isValid = false
        } else {
            binding.tilDescription.error = null
        }

        if (isValid) {
            val club = Club(
                id = clubId ?: "",
                name = name,
                category = category,
                president = president,
                imageUrl = imageUrl,
                description = description
            )

            if (isEditMode) {
                viewModel.updateClub(club)
            } else {
                viewModel.createClub(club)
            }
        }
    }
}
