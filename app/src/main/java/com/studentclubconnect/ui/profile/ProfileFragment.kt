package com.studentclubconnect.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.studentclubconnect.LoginActivity
import com.studentclubconnect.data.model.User
import com.studentclubconnect.databinding.FragmentProfileBinding
import com.studentclubconnect.viewmodel.ProfileState
import com.studentclubconnect.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                viewModel.loadProfile(uid)
            }

            setupListeners()
            setupDropdowns()
            observeViewModel()
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "Error initializing profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDropdowns() {
        val programmes = arrayOf("B.Sc. Computer Science", "B.Tech IT", "B.A. English", "B.Com", "B.Sc. Mathematics")
        val departments = arrayOf("Information Technology", "Computer Science", "Arts", "Commerce", "Mathematics")
        val semesters = arrayOf("1", "2", "3", "4", "5", "6", "7", "8")

        val programmeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, programmes)
        binding.actvProgramme.setAdapter(programmeAdapter)

        val deptAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, departments)
        binding.actvDepartment.setAdapter(deptAdapter)

        val semesterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, semesters)
        binding.actvSemester.setAdapter(semesterAdapter)
    }

    private fun setupListeners() {
        binding.btnEditProfileItem.setOnClickListener {
            toggleEditMode(true)
        }

        binding.btnSaveChanges.setOnClickListener {
            saveChanges()
        }

        binding.btnLogoutItem.setOnClickListener {
            logout()
        }

        binding.btnMyClubs.setOnClickListener {
            Toast.makeText(requireContext(), "My Clubs coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnMyEvents.setOnClickListener {
            Toast.makeText(requireContext(), "My Events coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profileState.collect { state ->
                    android.util.Log.d("ProfileFragment", "Profile state: $state")
                    when (state) {
                        is ProfileState.Loading -> {
                            binding.pbLoading.isVisible = true
                        }
                        is ProfileState.Success -> {
                            binding.pbLoading.isVisible = false
                            currentUser = state.user
                            android.util.Log.d("ProfileFragment", "Loaded user: $currentUser")
                            displayProfile(state.user)
                        }
                        is ProfileState.UpdateSuccess -> {
                            binding.pbLoading.isVisible = false
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            toggleEditMode(false)
                            // Reload profile to ensure consistency
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            if (uid != null) {
                                viewModel.loadProfile(uid)
                            }
                        }
                        is ProfileState.Error -> {
                            binding.pbLoading.isVisible = false
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                        is ProfileState.Idle -> {
                            binding.pbLoading.isVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun displayProfile(user: User?) {
        if (user == null) {
            android.util.Log.w("ProfileFragment", "displayProfile: User is null")
            return
        }

        android.util.Log.d("ProfileFragment", "Displaying profile for: ${user.name}, ID: ${user.studentId}")
        binding.tvProfileName.text = user.name.ifEmpty { "Student Name" }
        binding.tvProfileStudentId.text = user.studentId.ifEmpty { "Student ID" }
        binding.tvProfileProgramme.text = user.programme.ifEmpty { "Not provided" }
        binding.tvProfileDepartment.text = user.department.ifEmpty { "Not provided" }
        binding.tvProfileSemester.text = user.semester.toString()
        binding.tvProfileEmail.text = user.email
        binding.tvProfilePhone.text = user.phone.ifEmpty { "Not provided" }
        
        // Populate edit fields
        binding.etProfileName.setText(user.name)
        binding.actvProgramme.setText(user.programme, false)
        binding.actvDepartment.setText(user.department, false)
        binding.actvSemester.setText(user.semester.toString(), false)
        binding.etProfilePhone.setText(user.phone)
    }

    private fun toggleEditMode(isEdit: Boolean) {
        // Toggle Buttons/Actions container
        binding.llActions.isVisible = !isEdit
        binding.btnSaveChanges.isVisible = isEdit
        
        // Toggle Header Views
        binding.tvProfileName.isVisible = !isEdit
        binding.tvProfileStudentId.isVisible = !isEdit
        binding.rowEditName.isVisible = isEdit
        
        // Toggle Profile View Rows
        binding.rowProgramme.isVisible = !isEdit
        binding.rowDepartment.isVisible = !isEdit
        binding.rowSemester.isVisible = !isEdit
        binding.rowEmail.isVisible = !isEdit // Hiding Email in Edit Mode as requested
        binding.rowPhone.isVisible = !isEdit
        
        // Toggle Edit Input Rows
        binding.rowEditProgramme.isVisible = isEdit
        binding.rowEditDepartment.isVisible = isEdit
        binding.rowEditSemester.isVisible = isEdit
        binding.rowEditPhone.isVisible = isEdit
    }

    private fun saveChanges() {
        android.util.Log.d("ProfileFragment", "saveChanges called. Current user: $currentUser")
        val name = binding.etProfileName.text.toString().trim()
        val programme = binding.actvProgramme.text.toString().trim()
        val department = binding.actvDepartment.text.toString().trim()
        val semesterStr = binding.actvSemester.text.toString().trim()
        val phone = binding.etProfilePhone.text.toString().trim()

        if (name.isEmpty()) {
            binding.etProfileName.error = "Please enter your name"
            return
        }
        
        val semester = semesterStr.toIntOrNull() ?: 0

        val updatedUser = currentUser?.copy(
            name = name,
            programme = programme,
            department = department,
            semester = semester,
            phone = phone
        ) ?: User( // Fallback if currentUser was null for some reason
            uid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            email = FirebaseAuth.getInstance().currentUser?.email ?: "",
            name = name,
            programme = programme,
            department = department,
            semester = semester,
            phone = phone
        )

        android.util.Log.d("ProfileFragment", "Updating profile with: $updatedUser")
        viewModel.updateProfile(updatedUser)
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}