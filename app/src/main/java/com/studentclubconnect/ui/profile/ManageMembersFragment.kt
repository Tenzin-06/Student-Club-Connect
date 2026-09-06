package com.studentclubconnect.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.studentclubconnect.data.model.User
import com.studentclubconnect.databinding.FragmentManageMembersBinding
import com.studentclubconnect.viewmodel.ManageMembersState
import com.studentclubconnect.viewmodel.ManageMembersViewModel
import kotlinx.coroutines.launch

class ManageMembersFragment : Fragment() {

    private var _binding: FragmentManageMembersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ManageMembersViewModel by viewModels()
    private var clubId: String? = null
    private lateinit var memberAdapter: MemberAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        clubId = arguments?.getString("clubId")
        if (clubId == null) {
            Toast.makeText(requireContext(), "Club ID missing", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        clubId?.let { viewModel.loadMembers(it) }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        memberAdapter = MemberAdapter(currentUserId) { user ->
            showRemoveConfirmation(user)
        }
        binding.rvMembers.apply {
            adapter = memberAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showRemoveConfirmation(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Member?")
            .setMessage("Are you sure you want to remove ${user.name} from the club?")
            .setPositiveButton("Remove") { _, _ ->
                clubId?.let { viewModel.removeMember(user.uid, it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.membersState.collect { state ->
                    binding.pbLoading.isVisible = state is ManageMembersState.Loading
                    
                    when (state) {
                        is ManageMembersState.Success -> {
                            binding.cardEmptyState.isVisible = false
                            memberAdapter.submitList(state.members)
                        }
                        is ManageMembersState.Empty -> {
                            binding.cardEmptyState.isVisible = true
                            memberAdapter.submitList(emptyList())
                        }
                        is ManageMembersState.ActionSuccess -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        is ManageMembersState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
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
        fun newInstance(clubId: String): ManageMembersFragment {
            return ManageMembersFragment().apply {
                arguments = Bundle().apply {
                    putString("clubId", clubId)
                }
            }
        }
    }
}
