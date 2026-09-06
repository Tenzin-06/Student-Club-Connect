package com.studentclubconnect.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.studentclubconnect.databinding.FragmentAdminDashboardBinding
import com.studentclubconnect.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupListeners() {
        binding.btnManageClubs.setOnClickListener {
            // Already implemented via bottom nav, but can navigate specifically if needed
            Toast.makeText(requireContext(), "Use Clubs tab for management", Toast.LENGTH_SHORT).show()
        }

        binding.btnManageEvents.setOnClickListener {
            // Already implemented via bottom nav
            Toast.makeText(requireContext(), "Use Events tab for management", Toast.LENGTH_SHORT).show()
        }

        binding.btnManageAnnouncements.setOnClickListener {
            // Future feature: Global announcements list for admin
            Toast.makeText(requireContext(), "Global announcement management coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnManageUsers.setOnClickListener {
            Toast.makeText(requireContext(), "User management coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.userProfile.collect { user ->
                    if (user == null || user.role.lowercase() != "admin") {
                        // Secure redirection if unauthorized access
                        Toast.makeText(requireContext(), "Unauthorized access", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        binding.tvWelcome.text = "Welcome, ${user.name}"
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
