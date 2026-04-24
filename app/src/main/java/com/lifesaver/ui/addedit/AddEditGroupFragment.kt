package com.lifesaver.ui.addedit

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.lifesaver.LifeSaverApplication
import com.lifesaver.R
import com.lifesaver.databinding.FragmentAddEditGroupBinding

class AddEditGroupFragment : Fragment() {

    private var _binding: FragmentAddEditGroupBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditGroupFragmentArgs by navArgs()

    private val viewModel: AddEditGroupViewModel by viewModels {
        AddEditGroupViewModelFactory(
            (requireActivity().application as LifeSaverApplication).repository,
            args.groupId.ifEmpty { null }
        )
    }

    private val currentTags = mutableListOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddEditGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTagInput()
        setupSaveButton()
        observeExistingGroup()
    }

    private fun setupTagInput() {
        binding.etTagInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTagFromInput()
                true
            } else false
        }
        binding.btnAddTag.setOnClickListener { addTagFromInput() }
    }

    private fun addTagFromInput() {
        val tag = binding.etTagInput.text.toString().trim()
        if (tag.isNotBlank() && !currentTags.contains(tag)) {
            currentTags.add(tag)
            addChip(tag)
            binding.etTagInput.text?.clear()
        }
    }

    private fun addChip(tag: String) {
        val chip = Chip(requireContext()).apply {
            text = tag
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                currentTags.remove(tag)
                binding.chipGroupTags.removeView(this)
            }
        }
        binding.chipGroupTags.addView(chip)
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            if (title.isBlank()) {
                binding.tilTitle.error = getString(R.string.title_required)
                return@setOnClickListener
            }
            binding.tilTitle.error = null
            val description = binding.etDescription.text.toString().trim().ifBlank { null }
            viewModel.saveGroup(title, currentTags.toList(), description) {
                activity?.runOnUiThread {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun observeExistingGroup() {
        viewModel.existingGroup.observe(viewLifecycleOwner) { group ->
            if (group != null) {
                binding.etTitle.setText(group.title)
                binding.etDescription.setText(group.description ?: "")
                currentTags.clear()
                currentTags.addAll(group.tags)
                binding.chipGroupTags.removeAllViews()
                group.tags.forEach { addChip(it) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
