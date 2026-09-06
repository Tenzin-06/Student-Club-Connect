package com.studentclubconnect.ui.events

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.studentclubconnect.databinding.FragmentEventsBinding
import com.studentclubconnect.viewmodel.AuthViewModel
import com.studentclubconnect.viewmodel.EventState
import com.studentclubconnect.viewmodel.EventViewModel
import kotlinx.coroutines.launch

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EventViewModel by viewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var eventAdapter: EventAdapter
    private var filterClubId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        filterClubId = arguments?.getString("clubId")
        
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        
        loadEvents()

        binding.btnRetry.setOnClickListener {
            loadEvents()
        }

        binding.fabAddEvent.setOnClickListener {
            val intent = android.content.Intent(requireContext(), AddEditEventActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupToolbar() {
        if (filterClubId != null) {
            binding.toolbar.isVisible = true
            binding.toolbar.title = "Manage Events"
            binding.tvUpcomingEvents.text = "Club Events"
            binding.toolbar.setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        } else {
            binding.toolbar.isVisible = false
            binding.tvUpcomingEvents.text = "Upcoming Events"
        }
    }

    override fun onStart() {
        super.onStart()
        loadEvents()
    }

    private fun loadEvents() {
        if (filterClubId != null) {
            viewModel.getEventsByClub(filterClubId!!)
        } else {
            viewModel.getEvents()
        }
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            val intent = Intent(requireContext(), EventDetailsActivity::class.java).apply {
                putExtra("eventId", event.id)
            }
            startActivity(intent)
        }
        
        binding.rvEvents.apply {
            adapter = eventAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.eventState.collect { state ->
                        when (state) {
                            is EventState.Loading -> showLoading(true)
                            is EventState.Success -> {
                                showLoading(false)
                                eventAdapter.submitList(state.events)
                            }
                            is EventState.Empty -> {
                                showLoading(false)
                                eventAdapter.submitList(emptyList())
                                binding.emptyState.isVisible = true
                            }
                            is EventState.Error -> {
                                showLoading(false)
                                binding.errorState.isVisible = true
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    authViewModel.userProfile.collect { user ->
                        val isAdmin = user?.role?.lowercase() == "admin"
                        val isPresident = user?.role?.lowercase() == "president"
                        binding.fabAddEvent.isVisible = isAdmin || isPresident
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.rvEvents.isVisible = !isLoading && !binding.emptyState.isVisible
        if (isLoading) {
            binding.emptyState.isVisible = false
            binding.errorState.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(clubId: String? = null): EventsFragment {
            return EventsFragment().apply {
                arguments = Bundle().apply {
                    putString("clubId", clubId)
                }
            }
        }
    }
}
