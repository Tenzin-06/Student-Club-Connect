package com.studentclubconnect.ui.profile

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
import com.studentclubconnect.data.model.Club
import com.studentclubconnect.databinding.FragmentMyClubBinding
import com.studentclubconnect.ui.clubs.AddEditClubActivity
import com.studentclubconnect.viewmodel.MyClubViewModel
import kotlinx.coroutines.launch

class MyClubFragment : Fragment() {

    private var _binding: FragmentMyClubBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyClubViewModel by viewModels()
    private var currentClubId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyClubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        currentClubId = arguments?.getString("clubId")
        
        setupToolbar()
        setupListeners()
        observeViewModel()
        
        currentClubId?.let { viewModel.loadDashboardData(it) }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupListeners() {
        binding.btnEditClub.setOnClickListener {
            currentClubId?.let { id ->
                val intent = Intent(requireContext(), AddEditClubActivity::class.java).apply {
                    putExtra("clubId", id)
                }
                startActivity(intent)
            }
        }

        binding.btnManageMembers.setOnClickListener {
            currentClubId?.let { id ->
                parentFragmentManager.beginTransaction()
                    .replace(com.studentclubconnect.R.id.nav_host_fragment, ManageMembersFragment.newInstance(id))
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.btnManageEvents.setOnClickListener {
            currentClubId?.let { id ->
                parentFragmentManager.beginTransaction()
                    .replace(com.studentclubconnect.R.id.nav_host_fragment, com.studentclubconnect.ui.events.EventsFragment.newInstance(id, true))
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.btnManageAnnouncements.setOnClickListener {
            currentClubId?.let { id ->
                parentFragmentManager.beginTransaction()
                    .replace(com.studentclubconnect.R.id.nav_host_fragment, ManageAnnouncementsFragment.newInstance(id))
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dashboardState.collect { state ->
                    binding.pbLoading.isVisible = state.isLoading
                    binding.tvError.isVisible = state.error != null
                    state.error?.let { binding.tvError.text = it }
                    
                    state.club?.let { displayClubInfo(it) }
                    
                    binding.tvMemberCount.text = state.memberCount.toString()
                    binding.tvEventCount.text = state.eventCount.toString()
                    binding.tvAnnouncementCount.text = state.announcementCount.toString()
                }
            }
        }
    }

    private fun displayClubInfo(club: Club) {
        binding.tvClubName.text = club.name
        binding.tvClubCategory.text = club.category
        // Image loading could go here
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(clubId: String): MyClubFragment {
            return MyClubFragment().apply {
                arguments = Bundle().apply {
                    putString("clubId", clubId)
                }
            }
        }
    }
}
