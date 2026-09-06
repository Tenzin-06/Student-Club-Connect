package com.studentclubconnect.ui.home

import android.content.Intent
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.studentclubconnect.R
import com.studentclubconnect.databinding.FragmentHomeBinding
import com.studentclubconnect.ui.clubs.AnnouncementAdapter
import com.studentclubconnect.ui.clubs.ClubAdapter
import com.studentclubconnect.ui.clubs.ClubDetailsActivity
import com.studentclubconnect.ui.events.EventAdapter
import com.studentclubconnect.ui.events.EventDetailsActivity
import com.studentclubconnect.viewmodel.HomeState
import com.studentclubconnect.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    
    private lateinit var eventAdapter: EventAdapter
    private lateinit var clubAdapter: ClubAdapter
    private lateinit var popularClubAdapter: ClubAdapter
    private lateinit var announcementAdapter: AnnouncementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupListeners()
        observeViewModel()
        
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            viewModel.loadHomeData(userId)
        }
    }

    private fun setupRecyclerViews() {
        // Upcoming Events
        eventAdapter = EventAdapter { event ->
            val intent = Intent(requireContext(), EventDetailsActivity::class.java).apply {
                putExtra("eventId", event.id)
            }
            startActivity(intent)
        }
        binding.rvUpcomingEvents.apply {
            adapter = eventAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // My Clubs
        clubAdapter = ClubAdapter { club ->
            val intent = Intent(requireContext(), ClubDetailsActivity::class.java).apply {
                putExtra("clubId", club.id)
            }
            startActivity(intent)
        }
        binding.rvMyClubs.apply {
            adapter = clubAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Popular Clubs
        popularClubAdapter = ClubAdapter { club ->
            val intent = Intent(requireContext(), ClubDetailsActivity::class.java).apply {
                putExtra("clubId", club.id)
            }
            startActivity(intent)
        }
        binding.rvPopularClubs.apply {
            adapter = popularClubAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Recent Announcements
        announcementAdapter = AnnouncementAdapter()
        binding.rvRecentAnnouncements.apply {
            adapter = announcementAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupListeners() {
        binding.ivNotifications.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.btnViewAllEvents.setOnClickListener {
            navigateToTab(R.id.nav_events)
        }
        binding.btnViewAllClubs.setOnClickListener {
            navigateToTab(R.id.nav_clubs)
        }
    }

    private fun navigateToTab(tabId: Int) {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = tabId
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.homeState.collect { state ->
                    when (state) {
                        is HomeState.Loading -> {
                            binding.shimmerView.isVisible = true
                            binding.shimmerView.startShimmer()
                            binding.rvRecentAnnouncements.isVisible = false
                            binding.rvUpcomingEvents.isVisible = false
                            binding.rvMyClubs.isVisible = false
                            binding.rvPopularClubs.isVisible = false
                            
                            binding.cardNoAnnouncements.isVisible = false
                            binding.cardNoEvents.isVisible = false
                            binding.cardNoClubs.isVisible = false
                        }
                        is HomeState.Success -> {
                            binding.shimmerView.stopShimmer()
                            binding.shimmerView.isVisible = false
                            
                            // Welcome Message
                            val userName = state.user?.name ?: "Student"
                            binding.tvGreeting.text = "Hello, $userName 👋"
                            
                            // Popular Clubs
                            if (state.popularClubs.isEmpty()) {
                                binding.rvPopularClubs.isVisible = false
                                binding.tvPopularClubsLabel.isVisible = false
                            } else {
                                binding.rvPopularClubs.isVisible = true
                                binding.tvPopularClubsLabel.isVisible = true
                                popularClubAdapter.submitList(state.popularClubs)
                            }
                            
                            // Recent Announcements
                            if (state.announcements.isEmpty()) {
                                binding.rvRecentAnnouncements.isVisible = false
                                binding.cardNoAnnouncements.isVisible = true
                            } else {
                                binding.rvRecentAnnouncements.isVisible = true
                                binding.cardNoAnnouncements.isVisible = false
                                announcementAdapter.setClubNames(state.clubNames)
                                announcementAdapter.submitList(state.announcements)
                            }
                            
                            // Upcoming Events
                            if (state.upcomingEvents.isEmpty()) {
                                binding.rvUpcomingEvents.isVisible = false
                                binding.cardNoEvents.isVisible = true
                            } else {
                                binding.rvUpcomingEvents.isVisible = true
                                binding.cardNoEvents.isVisible = false
                                eventAdapter.submitList(state.upcomingEvents)
                            }
                            
                            // My Clubs
                            if (state.joinedClubs.isEmpty()) {
                                binding.rvMyClubs.isVisible = false
                                binding.cardNoClubs.isVisible = true
                            } else {
                                binding.rvMyClubs.isVisible = true
                                binding.cardNoClubs.isVisible = false
                                clubAdapter.submitList(state.joinedClubs)
                            }
                        }
                        is HomeState.Error -> {
                            binding.shimmerView.stopShimmer()
                            binding.shimmerView.isVisible = false
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
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
