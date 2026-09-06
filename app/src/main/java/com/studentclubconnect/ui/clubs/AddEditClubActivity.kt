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
import com.studentclubconnect.data.model.User
import com.studentclubconnect.databinding.ActivityAddEditClubBinding
import com.studentclubconnect.viewmodel.AuthViewModel
import com.studentclubconnect.viewmodel.ClubState
import com.studentclubconnect.viewmodel.ClubViewModel
import kotlinx.coroutines.launch

class AddEditClubActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditClubBinding
    private val viewModel: ClubViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private var clubId: String? = null
    private var isEditMode = false
    
    private var eligibleStudents: List<User> = emptyList()
    private var selectedPresidentId: String = ""
    private var selectedPresidentName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditClubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        clubId = intent.getStringExtra("clubId")
        isEditMode = clubId != null

        setupToolbar()
        setupCategoryDropdown()
        observeViewModel()

        val uid = authViewModel.getCurrentUser()?.uid
        if (uid != null) {
            authViewModel.loadUserProfile(uid)
        }
        viewModel.getEligibleStudents()

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
                launch {
                    authViewModel.userProfile.collect { user ->
                        val isPresident = user?.role?.lowercase() == "president"
                        if (isPresident) {
                            binding.tilPresident.isEnabled = false
                            binding.actvPresident.isEnabled = false
                        }
                    }
                }
                
                launch {
                    viewModel.clubState.collect { state ->
                        when (state) {
                            is ClubState.Loading -> {
                                binding.progressBar.isVisible = true
                                binding.btnSubmit.isEnabled = false
                            }
                            is ClubState.StudentsLoaded -> {
                                binding.progressBar.isVisible = false
                                binding.btnSubmit.isEnabled = true
                                eligibleStudents = state.students
                                setupPresidentDropdown()
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
    }

    private fun setupPresidentDropdown() {
        val displayList = mutableListOf<User>()
        // Add "None" option at the top
        displayList.add(User(uid = "", name = "None (No President)"))
        
        displayList.addAll(eligibleStudents)
        
        // Add current president if in edit mode and not in list
        if (isEditMode && selectedPresidentId.isNotEmpty() && eligibleStudents.none { it.uid == selectedPresidentId }) {
            displayList.add(1, User(uid = selectedPresidentId, name = selectedPresidentName))
        }

        val names = displayList.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        binding.actvPresident.setAdapter(adapter)
        
        binding.actvPresident.setOnItemClickListener { _, _, position, _ ->
            val user = displayList[position]
            selectedPresidentId = user.uid
            selectedPresidentName = if (user.uid.isEmpty()) "" else user.name
        }
    }

    private fun populateFields(club: Club) {
        binding.apply {
            etClubName.setText(club.name)
            actvCategory.setText(club.category, false)
            actvPresident.setText(club.president, false)
            selectedPresidentId = club.presidentId
            selectedPresidentName = club.president
            etImageUrl.setText(club.imageUrl)
            etDescription.setText(club.description)
            
            // Re-setup dropdown to include current president if necessary
            setupPresidentDropdown()
        }
    }

    private fun validateAndSubmit() {
        val name = binding.etClubName.text.toString().trim()
        val category = binding.actvCategory.text.toString().trim()
        val president = binding.actvPresident.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val imageUrl = binding.etImageUrl.text.toString().trim()

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

        if (president.isEmpty()) {
            binding.tilPresident.error = "President selection is required"
            isValid = false
        } else {
            binding.tilPresident.error = null
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
                president = selectedPresidentName,
                presidentId = selectedPresidentId,
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
