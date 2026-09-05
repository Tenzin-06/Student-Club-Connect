package com.studentclubconnect.ui.clubs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.studentclubconnect.data.model.Club
import com.studentclubconnect.databinding.FragmentClubsBinding
import com.studentclubconnect.viewmodel.AuthViewModel
import com.studentclubconnect.viewmodel.ClubState
import com.studentclubconnect.viewmodel.ClubViewModel
import kotlinx.coroutines.launch

class ClubsFragment : Fragment() {

    private var _binding: FragmentClubsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ClubViewModel by viewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var clubAdapter: ClubAdapter

    private var allClubs: List<Club> = emptyList()
    private var currentCategory: String = "All"
    private var currentSearchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClubsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupFilters()
        setupSearch()
        observeViewModel()

        binding.btnRetry.setOnClickListener {
            viewModel.getClubs()
        }

        binding.fabAddClub.setOnClickListener {
            val intent = android.content.Intent(requireContext(), AddEditClubActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        // Refresh the list every time the fragment becomes visible
        viewModel.getClubs()
    }

    private fun setupRecyclerView() {
        clubAdapter = ClubAdapter { club ->
            val intent = android.content.Intent(requireContext(), ClubDetailsActivity::class.java).apply {
                putExtra("clubId", club.id)
            }
            startActivity(intent)
        }
        
        binding.rvClubs.apply {
            adapter = clubAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupFilters() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            currentCategory = when (checkedId) {
                binding.chipTechnology.id -> "Technology"
                binding.chipSports.id -> "Sports"
                binding.chipArts.id -> "Arts"
                binding.chipCultural.id -> "Cultural"
                else -> "All"
            }
            filterClubs()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                filterClubs()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.clubState.collect { state ->
                        when (state) {
                            is ClubState.Loading -> showLoading(true)
                            is ClubState.Success -> {
                                showLoading(false)
                                allClubs = state.clubs
                                filterClubs()
                            }
                            is ClubState.Empty -> {
                                showLoading(false)
                                allClubs = emptyList()
                                clubAdapter.submitList(emptyList())
                                binding.emptyState.isVisible = true
                            }
                            is ClubState.Error -> {
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
                        binding.fabAddClub.isVisible = isAdmin
                    }
                }
            }
        }
    }

    private fun filterClubs() {
        val filteredList = allClubs.filter { club ->
            val matchesCategory = currentCategory == "All" || 
                club.category.equals(currentCategory, ignoreCase = true)
            
            val matchesSearch = currentSearchQuery.isEmpty() || 
                club.name.contains(currentSearchQuery, ignoreCase = true) || 
                club.description.contains(currentSearchQuery, ignoreCase = true)
            
            matchesCategory && matchesSearch
        }
        
        clubAdapter.submitList(filteredList)
        
        // Only show empty state if we actually have no clubs after filtering, 
        // OR if the original list was empty.
        binding.emptyState.isVisible = filteredList.isEmpty() && !binding.progressBar.isVisible
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.rvClubs.isVisible = !isLoading && !binding.emptyState.isVisible
        if (isLoading) {
            binding.emptyState.isVisible = false
            binding.errorState.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}