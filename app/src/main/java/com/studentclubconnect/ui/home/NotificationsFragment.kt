package com.studentclubconnect.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.studentclubconnect.databinding.FragmentNotificationsBinding
import com.studentclubconnect.viewmodel.AnnouncementState
import com.studentclubconnect.viewmodel.AnnouncementViewModel
import com.studentclubconnect.viewmodel.ClubState
import com.studentclubconnect.viewmodel.ClubViewModel
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    
    private val announcementViewModel: AnnouncementViewModel by viewModels()
    private val clubViewModel: ClubViewModel by viewModels()
    
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupToolbar()
        observeViewModels()
        
        announcementViewModel.getAnnouncements()
        clubViewModel.getClubs()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter()
        binding.rvNotifications.apply {
            adapter = notificationAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModels() {
        // Observe Announcements
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                announcementViewModel.announcementState.collect { state ->
                    when (state) {
                        is AnnouncementState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.cardEmptyState.isVisible = false
                        }
                        is AnnouncementState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.cardEmptyState.isVisible = false
                            // Sort by date (already likely sorted by repo, but just to be sure)
                            val sorted = state.announcements.sortedByDescending { it.createdAt }
                            notificationAdapter.submitList(sorted)
                        }
                        is AnnouncementState.Empty -> {
                            binding.progressBar.isVisible = false
                            binding.cardEmptyState.isVisible = true
                            notificationAdapter.submitList(emptyList())
                        }
                        is AnnouncementState.Error -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }

        // Observe Clubs to resolve names
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                clubViewModel.clubState.collect { state ->
                    if (state is ClubState.Success) {
                        val clubNames = state.clubs.associate { it.id to it.name }
                        notificationAdapter.setClubNames(clubNames)
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
