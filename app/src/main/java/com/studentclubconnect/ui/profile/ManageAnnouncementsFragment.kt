package com.studentclubconnect.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.studentclubconnect.data.model.Announcement
import com.studentclubconnect.databinding.FragmentManageAnnouncementsBinding
import com.studentclubconnect.ui.clubs.AddEditAnnouncementActivity
import com.studentclubconnect.ui.clubs.AnnouncementAdapter
import com.studentclubconnect.viewmodel.AnnouncementState
import com.studentclubconnect.viewmodel.AnnouncementViewModel
import com.studentclubconnect.viewmodel.AuthViewModel
import com.studentclubconnect.viewmodel.ClubState
import com.studentclubconnect.viewmodel.ClubViewModel
import kotlinx.coroutines.launch

class ManageAnnouncementsFragment : Fragment() {

    private var _binding: FragmentManageAnnouncementsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnnouncementViewModel by viewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private val clubViewModel: ClubViewModel by viewModels()
    private var clubId: String? = null
    private lateinit var announcementAdapter: AnnouncementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageAnnouncementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        clubId = arguments?.getString("clubId")
        
        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        binding.fabAddAnnouncement.setOnClickListener {
            val intent = Intent(requireContext(), AddEditAnnouncementActivity::class.java).apply {
                if (clubId != null) {
                    putExtra("clubId", clubId)
                }
            }
            startActivity(intent)
        }
        
        clubViewModel.getClubs()
    }

    override fun onStart() {
        super.onStart()
        loadAnnouncements()
    }

    private fun loadAnnouncements() {
        if (clubId != null) {
            viewModel.getAnnouncementsByClub(clubId!!)
        } else {
            viewModel.getAnnouncements()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        if (clubId == null) {
            binding.toolbar.title = "Global Announcements"
        }
    }

    private fun setupRecyclerView() {
        val user = authViewModel.userProfile.value
        announcementAdapter = AnnouncementAdapter(
            currentUserId = user?.uid,
            currentUserRole = user?.role,
            userPresidentOf = user?.presidentOf,
            onEditClick = { announcement ->
                val intent = Intent(requireContext(), AddEditAnnouncementActivity::class.java).apply {
                    putExtra("announcementId", announcement.id)
                    putExtra("clubId", announcement.clubId)
                    putExtra("title", announcement.title)
                    putExtra("message", announcement.message)
                }
                startActivity(intent)
            },
            onDeleteClick = { announcement ->
                showDeleteConfirmation(announcement)
            }
        )
        binding.rvAnnouncements.apply {
            adapter = announcementAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showDeleteConfirmation(announcement: Announcement) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Announcement")
            .setMessage("Are you sure you want to delete this announcement?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteAnnouncement(announcement.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.userProfile.collect { user ->
                        setupRecyclerView()
                    }
                }

                launch {
                    viewModel.announcementState.collect { state ->
                        binding.pbLoading.isVisible = state is AnnouncementState.Loading
                        
                        when (state) {
                            is AnnouncementState.Success -> {
                                binding.cardEmptyState.isVisible = false
                                announcementAdapter.submitList(state.announcements)
                            }
                            is AnnouncementState.Empty -> {
                                binding.cardEmptyState.isVisible = true
                                announcementAdapter.submitList(emptyList())
                            }
                            is AnnouncementState.ActionSuccess -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                loadAnnouncements()
                            }
                            is AnnouncementState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    clubViewModel.clubState.collect { state ->
                        if (state is ClubState.Success) {
                            val clubMap = state.clubs.associate { it.id to it.name }
                            announcementAdapter.setClubNames(clubMap)
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

    companion object {
        fun newInstance(clubId: String? = null): ManageAnnouncementsFragment {
            return ManageAnnouncementsFragment().apply {
                arguments = Bundle().apply {
                    putString("clubId", clubId)
                }
            }
        }
    }
}
