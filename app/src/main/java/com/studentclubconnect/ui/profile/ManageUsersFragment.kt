package com.studentclubconnect.ui.profile

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
import com.studentclubconnect.databinding.FragmentManageUsersBinding
import com.studentclubconnect.viewmodel.ClubState
import com.studentclubconnect.viewmodel.ClubViewModel
import com.studentclubconnect.viewmodel.ManageUsersState
import com.studentclubconnect.viewmodel.ManageUsersViewModel
import kotlinx.coroutines.launch

class ManageUsersFragment : Fragment() {

    private var _binding: FragmentManageUsersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ManageUsersViewModel by viewModels()
    private val clubViewModel: ClubViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        
        viewModel.loadUsers()
        clubViewModel.getClubs()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter()
        binding.rvUsers.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.usersState.collect { state ->
                        binding.pbLoading.isVisible = state is ManageUsersState.Loading
                        
                        when (state) {
                            is ManageUsersState.Success -> {
                                binding.cardEmptyState.isVisible = false
                                userAdapter.submitList(state.users)
                            }
                            is ManageUsersState.Empty -> {
                                binding.cardEmptyState.isVisible = true
                                userAdapter.submitList(emptyList())
                            }
                            is ManageUsersState.Error -> {
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
                            userAdapter.setClubNames(clubMap)
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
