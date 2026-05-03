package com.lifesaver.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lifesaver.LifeSaverApplication
import com.lifesaver.R
import com.lifesaver.databinding.FragmentHomeBinding
import com.lifesaver.model.GroupSummary

class HomeFragment : Fragment() {

    private companion object {
        const val LOADING_MESSAGE_DURATION_MS = 2_000L
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory((requireActivity().application as LifeSaverApplication).repository)
    }

    private lateinit var adapter: GroupAdapter
    private var hasShownInitialLoadingMessage = false
    private val hideLoadingMessage = Runnable {
        val binding = _binding ?: return@Runnable
        if (adapter.currentList.isEmpty() && viewModel.isLoading.value != true && !viewModel.needsSetup()) {
            binding.tvEmpty.visibility = View.GONE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupMenu()
        setupFab()
        observeGroups()
        observeLoading()
        observeErrors()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.needsSetup()) {
            binding.fabAddGroup.hide()
            binding.recyclerGroups.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = getString(R.string.setup_required_message)
        } else {
            binding.fabAddGroup.show()
            val shouldShowInitialLoading = !hasShownInitialLoadingMessage && adapter.currentList.isEmpty()
            binding.recyclerGroups.visibility = if (shouldShowInitialLoading) View.GONE else View.VISIBLE
            if (shouldShowInitialLoading) {
                binding.tvEmpty.text = getString(R.string.loading_groups)
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.removeCallbacks(hideLoadingMessage)
                binding.tvEmpty.postDelayed(hideLoadingMessage, LOADING_MESSAGE_DURATION_MS)
                hasShownInitialLoadingMessage = true
            } else {
                binding.tvEmpty.visibility = View.GONE
            }
            viewModel.refresh()
        }
    }

    private fun setupRecyclerView() {
        adapter = GroupAdapter(
            onItemClick = { summary ->
                val action = HomeFragmentDirections.actionHomeToDetail(summary.id)
                findNavController().navigate(action)
            },
            onItemLongClick = { summary ->
                confirmDelete(summary)
                true
            }
        )
        binding.recyclerGroups.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerGroups.adapter = adapter

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = vh.adapterPosition
                val to = target.adapterPosition
                val list = adapter.currentList.toMutableList()
                val moved = list.removeAt(from)
                list.add(to, moved)
                adapter.submitList(list)
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val summary = adapter.currentList[vh.adapterPosition]
                confirmDelete(summary)
                adapter.notifyItemChanged(vh.adapterPosition)
            }

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                viewModel.reorderSummaries(adapter.currentList)
            }
        })
        touchHelper.attachToRecyclerView(binding.recyclerGroups)
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.menu_home, menu)
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView
                searchView.queryHint = getString(R.string.search_hint)
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?) = true

                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.setSearchQuery(newText ?: "")
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.action_settings -> {
                        findNavController().navigate(R.id.action_home_to_settings)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupFab() {
        binding.fabAddGroup.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_add_edit_group)
        }
    }

    private fun observeGroups() {
        viewModel.filteredGroups.observe(viewLifecycleOwner) { summaries ->
            adapter.submitList(summaries)
            val isLoading = viewModel.isLoading.value == true
            val shouldShowLoading = !hasShownInitialLoadingMessage && summaries.isEmpty() && isLoading
            binding.recyclerGroups.visibility = if (shouldShowLoading) View.GONE else View.VISIBLE
            binding.tvEmpty.visibility = if (shouldShowLoading) View.VISIBLE else View.GONE
            if (shouldShowLoading) {
                binding.tvEmpty.text = getString(R.string.loading_groups)
            }
        }
    }

    private fun observeLoading() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val summaries = adapter.currentList
            if (summaries.isEmpty() && !hasShownInitialLoadingMessage) {
                binding.recyclerGroups.visibility = if (isLoading) View.GONE else View.VISIBLE
                if (isLoading) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = getString(R.string.loading_groups)
                    binding.tvEmpty.removeCallbacks(hideLoadingMessage)
                    binding.tvEmpty.postDelayed(hideLoadingMessage, LOADING_MESSAGE_DURATION_MS)
                } else {
                    binding.tvEmpty.removeCallbacks(hideLoadingMessage)
                    binding.tvEmpty.postDelayed(hideLoadingMessage, LOADING_MESSAGE_DURATION_MS)
                    hasShownInitialLoadingMessage = true
                }
            }
        }
    }

    private fun observeErrors() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.consumeError()
            }
        }
    }

    private fun confirmDelete(summary: GroupSummary) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_group_title)
            .setMessage(getString(R.string.delete_group_message, summary.title))
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteSummary(summary) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        _binding?.tvEmpty?.removeCallbacks(hideLoadingMessage)
        super.onDestroyView()
        _binding = null
    }
}
