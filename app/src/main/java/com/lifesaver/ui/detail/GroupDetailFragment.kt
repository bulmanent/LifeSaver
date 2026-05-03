package com.lifesaver.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lifesaver.LifeSaverApplication
import com.lifesaver.R
import com.lifesaver.data.remote.GmailAttachmentSummary
import com.lifesaver.data.remote.GmailMessageSummary
import com.lifesaver.databinding.FragmentGroupDetailBinding
import com.lifesaver.model.DocumentPage
import java.io.File

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
    private var pendingCameraUri: Uri? = null

    private val pickDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            requireContext().contentResolver.takePersistableReadPermissionIfPossible(it)
            showCaptionDialog(it)
        }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingCameraUri?.let(::showCaptionDialog)
        }
    }

    private val gmailAccessLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val app = requireActivity().application as LifeSaverApplication
            runCatching { app.authManager.handleSignInResult(result.data) }
                .onSuccess { viewModel.loadRecentGmailMessages() }
                .onFailure {
                    Toast.makeText(
                        requireContext(),
                        app.authManager.describeSignInFailure(it),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

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
        observeErrors()
        observeGmailMessages()
        viewModel.refresh()
    }

    private fun setupRecyclerView() {
        adapter = PageAdapter(
            onItemClick = { _, index ->
                val action = GroupDetailFragmentDirections.actionDetailToView(args.groupId, index)
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
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
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

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) = Unit

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
            showImageSourceDialog()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf(
            getString(R.string.pick_file),
            getString(R.string.capture_photo),
            getString(R.string.add_text_entry),
            getString(R.string.import_from_gmail)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_page)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickDocumentLauncher.launch(arrayOf("*/*"))
                    1 -> {
                        val file = File(requireContext().cacheDir, "captured_${System.currentTimeMillis()}.jpg")
                        pendingCameraUri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            file
                        )
                        takePhotoLauncher.launch(pendingCameraUri)
                    }
                    2 -> showTextEntryDialog()
                    3 -> startGmailImport()
                }
            }
            .show()
    }

    private fun startGmailImport() {
        if (!viewModel.hasGmailAccess()) {
            Toast.makeText(requireContext(), R.string.gmail_access_required, Toast.LENGTH_LONG).show()
            val app = requireActivity().application as LifeSaverApplication
            gmailAccessLauncher.launch(app.authManager.gmailAccessSignInIntent())
            return
        }
        viewModel.loadRecentGmailMessages()
    }

    private fun showCaptionDialog(uri: Uri) {
        val input = TextInputEditText(requireContext()).apply {
            hint = getString(R.string.caption_hint)
        }
        val sequenceLayout = buildSequenceInput()
        val container = buildDialogContainer(input, sequenceLayout)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_page)
            .setView(container)
            .setPositiveButton(R.string.upload) { _, _ ->
                val caption = input.text?.toString()?.trim().takeUnless { it.isNullOrBlank() }
                viewModel.addPage(uri, caption, parseSequence(sequenceLayout))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showTextEntryDialog() {
        val input = TextInputEditText(requireContext()).apply {
            hint = getString(R.string.text_entry_hint)
            minLines = 4
        }
        val sequenceLayout = buildSequenceInput()
        val container = buildDialogContainer(input, sequenceLayout)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_text_entry)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val text = input.text?.toString()?.trim().orEmpty()
                if (text.isNotBlank()) {
                    viewModel.addTextPage(text, null, parseSequence(sequenceLayout))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun buildSequenceInput(): TextInputLayout {
        return TextInputLayout(requireContext()).apply {
            hint = getString(R.string.sequence_hint)
            addView(
                TextInputEditText(context).apply {
                    setText(nextSequenceValue().toString())
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                }
            )
        }
    }

    private fun buildDialogContainer(
        primaryInput: TextInputEditText,
        sequenceLayout: TextInputLayout
    ): View {
        return android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(primaryInput)
            addView(sequenceLayout)
        }
    }

    private fun parseSequence(sequenceLayout: TextInputLayout): Int? {
        return sequenceLayout.editText?.text?.toString()?.trim()?.toIntOrNull()
    }

    private fun nextSequenceValue(): Int {
        return (viewModel.pages.value?.maxOfOrNull { it.sequence } ?: -1) + 1
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

    private fun observeErrors() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.consumeError()
            }
        }
    }

    private fun observeGmailMessages() {
        viewModel.gmailMessages.observe(viewLifecycleOwner) { messages ->
            messages ?: return@observe
            if (messages.isEmpty()) {
                Toast.makeText(requireContext(), R.string.no_gmail_messages_found, Toast.LENGTH_LONG).show()
                viewModel.consumeGmailMessages()
                return@observe
            }
            showGmailMessagePicker(messages)
            viewModel.consumeGmailMessages()
        }
    }

    private fun showGmailMessagePicker(messages: List<GmailMessageSummary>) {
        val items = messages.map { message ->
            buildString {
                append(message.subject)
                append("\n")
                append(message.from)
                if (message.dateText.isNotBlank()) {
                    append(" • ")
                    append(message.dateText)
                }
                append(" • ")
                append(message.attachments.size)
            }
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_email)
            .setItems(items) { _, which ->
                showGmailAttachmentPicker(messages[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showGmailAttachmentPicker(message: GmailMessageSummary) {
        val items = message.attachments.map { attachment ->
            val sizeText = Formatter.formatShortFileSize(requireContext(), attachment.sizeBytes.toLong())
            getString(R.string.gmail_attachment_item, attachment.fileName, sizeText)
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_attachment)
            .setItems(items) { _, which ->
                showGmailCaptionDialog(message.attachments[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showGmailCaptionDialog(attachment: GmailAttachmentSummary) {
        val input = TextInputEditText(requireContext()).apply {
            hint = getString(R.string.caption_hint)
            setText(attachment.subject ?: attachment.fileName)
        }
        val sequenceLayout = buildSequenceInput()
        val container = buildDialogContainer(input, sequenceLayout)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_from_gmail)
            .setView(container)
            .setPositiveButton(R.string.upload) { _, _ ->
                val caption = input.text?.toString()?.trim().takeUnless { it.isNullOrBlank() }
                viewModel.importGmailAttachment(attachment, caption, parseSequence(sequenceLayout))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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

private fun android.content.ContentResolver.takePersistableReadPermissionIfPossible(uri: Uri) {
    try {
        takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
    } catch (_: IllegalArgumentException) {
    }
}
