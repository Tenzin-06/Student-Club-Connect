package com.studentclubconnect.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.studentclubconnect.R
import com.studentclubconnect.databinding.FragmentAdminDashboardBinding
import com.studentclubconnect.databinding.ItemAnalyticsBarBinding
import com.studentclubconnect.viewmodel.AdminDashboardViewModel
import com.studentclubconnect.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val viewModel: AdminDashboardViewModel by viewModels()

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
        
        viewModel.loadStats()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupListeners() {
        binding.btnManageClubs.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, com.studentclubconnect.ui.clubs.ClubsFragment.newInstance(true))
                .addToBackStack(null)
                .commit()
        }

        binding.btnManageEvents.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, com.studentclubconnect.ui.events.EventsFragment.newInstance(null, true))
                .addToBackStack(null)
                .commit()
        }

        binding.btnManageAnnouncements.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, ManageAnnouncementsFragment.newInstance(null))
                .addToBackStack(null)
                .commit()
        }

        binding.btnManageUsers.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, ManageUsersFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.userProfile.collect { user ->
                        if (user == null || user.role.lowercase() != "admin") {
                            Toast.makeText(requireContext(), "Unauthorized access", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        } else {
                            binding.tvWelcome.text = "Welcome, ${user.name}"
                        }
                    }
                }

                launch {
                    viewModel.statsState.collect { state ->
                        if (state.error != null) {
                            Toast.makeText(requireContext(), state.error, Toast.LENGTH_SHORT).show()
                        }
                        
                        binding.tvStatStudents.text = state.studentCount.toString()
                        binding.tvStatClubs.text = state.clubCount.toString()
                        binding.tvStatPresidents.text = state.presidentCount.toString()
                        binding.tvStatEvents.text = state.eventCount.toString()
                        binding.tvStatMemberships.text = state.membershipCount.toString()
                        binding.tvStatAnnouncements.text = state.announcementCount.toString()

                        renderClubAnalytics(state.clubMemberships)
                        renderEventAnalytics(state.eventsByMonth)
                    }
                }
            }
        }
    }

    private fun renderClubAnalytics(data: Map<String, Int>) {
        binding.containerClubAnalytics.removeAllViews()
        if (data.isEmpty()) {
            val emptyTv = android.widget.TextView(requireContext()).apply {
                text = "No membership data available"
                gravity = android.view.Gravity.CENTER
            }
            binding.containerClubAnalytics.addView(emptyTv)
            return
        }

        val maxCount = data.values.maxOrNull() ?: 1
        data.forEach { (name, count) ->
            val barBinding = ItemAnalyticsBarBinding.inflate(layoutInflater, binding.containerClubAnalytics, false)
            barBinding.tvLabel.text = name
            barBinding.tvValue.text = count.toString()
            barBinding.pbAnalytics.max = maxCount
            barBinding.pbAnalytics.progress = count
            binding.containerClubAnalytics.addView(barBinding.root)
        }
    }

    private fun renderEventAnalytics(data: Map<String, Int>) {
        binding.containerEventAnalytics.removeAllViews()
        if (data.isEmpty()) {
            val emptyTv = android.widget.TextView(requireContext()).apply {
                text = "No event data available"
                gravity = android.view.Gravity.CENTER
            }
            binding.containerEventAnalytics.addView(emptyTv)
            return
        }

        val maxCount = data.values.maxOrNull() ?: 1
        data.forEach { (month, count) ->
            val barBinding = ItemAnalyticsBarBinding.inflate(layoutInflater, binding.containerEventAnalytics, false)
            barBinding.tvLabel.text = month
            barBinding.tvValue.text = count.toString()
            barBinding.pbAnalytics.max = maxCount
            barBinding.pbAnalytics.progress = count
            binding.containerEventAnalytics.addView(barBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
