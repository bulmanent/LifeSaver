package com.lifesaver.ui.detail

import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lifesaver.LifeSaverApplication
import com.lifesaver.R
import com.lifesaver.databinding.FragmentGroupDetailBinding
import com.lifesaver.model.DocumentPage

class GroupDetailFragment : Fragment() {

    private var _binding: FragmentGroupDetailBinding? = null
    private val binding get() = _binding!!

    private val args: GroupDetailFragmentArgs by navArgs()

    private val viewModel: GroupDetailViewModel by viewModels {
        GroupDetailViewModelFactory(
            (requireActivity().application as LifeSaverApplication).repository,
            args.groupId
        )
    }

    private lateinit var adapter: PageAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGroupDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupMenu()
        setupFab()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = PageAdapter(
            onItemClick = { _, index ->
                val action = GroupDetailFragmentDirections.actionDetailToView(
                    args.groupId,
                    index
                )
                findNavController().navigate(action)
            },
            onItemLongClick = { page ->
                confirmDeletePage(page)
                true
            }
        )
        binding.recyclerPages.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerPages.adapter = adapter

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
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

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                viewModel.reorderPages(adapter.currentList)
            }
        })
        touchHelper.attachToRecyclerView(binding.recyclerPages)
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.menu_detail, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.action_edit_group -> {
                        val action = GroupDetailFragmentDirections.actionDetailToAddEditGroup(args.groupId)
                        findNavController().navigate(action)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupFab() {
        binding.fabAddPage.setOnClickListener {
            showAddPageDialog()
        }
    }

    private fun showAddPageDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_page, null)
        val etUri = dialogView.findViewById<EditText>(R.id.etUri)
        val etCaption = dialogView.findViewById<EditText>(R.id.etCaption)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_page)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val uri = etUri.text.toString().trim()
                val caption = etCaption.text.toString().trim().ifBlank { null }
                if (uri.isNotBlank()) {
                    viewModel.addPage(uri, caption)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun observeData() {
        viewModel.group.observe(viewLifecycleOwner) { group ->
            if (group != null) {
                binding.tvGroupTitle.text = group.title
                binding.tvGroupTags.text = group.tags.joinToString(" • ")
                if (!group.description.isNullOrBlank()) {
                    binding.tvDescription.text = group.description
                    binding.tvDescription.visibility = View.VISIBLE
                } else {
                    binding.tvDescription.visibility = View.GONE
                }
            }
        }

        viewModel.pages.observe(viewLifecycleOwner) { pages ->
            adapter.submitList(pages)
            binding.tvEmpty.visibility = if (pages.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun confirmDeletePage(page: DocumentPage) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_page_title)
            .setMessage(R.string.delete_page_message)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deletePage(page) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
