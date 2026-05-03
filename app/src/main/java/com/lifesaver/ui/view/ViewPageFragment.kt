package com.lifesaver.ui.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.Gravity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lifesaver.LifeSaverApplication
import com.lifesaver.R
import com.lifesaver.data.remote.DriveImageRef
import com.lifesaver.databinding.FragmentViewPageBinding
import com.lifesaver.model.DocumentPage

class ViewPageFragment : Fragment() {

    private var _binding: FragmentViewPageBinding? = null
    private val binding get() = _binding!!

    private val args: ViewPageFragmentArgs by navArgs()

    private val viewModel: ViewPageViewModel by viewModels {
        ViewPageViewModelFactory(
            (requireActivity().application as LifeSaverApplication).repository,
            args.groupId
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentViewPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setIndex(args.pageIndex)

        binding.btnPrev.setOnClickListener { viewModel.goPrev() }
        binding.btnNext.setOnClickListener { viewModel.goNext() }
        binding.btnEdit.setOnClickListener {
            viewModel.currentPage.value?.let(::showEditDialog)
        }
        binding.btnOpenFile.setOnClickListener {
            viewModel.openCurrentFile()
        }
        binding.btnShare.setOnClickListener {
            viewModel.shareCurrentItem()
        }

        viewModel.pages.observe(viewLifecycleOwner) { pages ->
            updateNavButtons(viewModel.currentIndex.value ?: 0, pages.size)
        }

        viewModel.currentIndex.observe(viewLifecycleOwner) { index ->
            updateNavButtons(index, viewModel.pages.value?.size ?: 0)
        }

        viewModel.currentPage.observe(viewLifecycleOwner) { page ->
            if (page != null) {
                displayPage(page)
            }
        }

        viewModel.openFileUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                openFile(uri, viewModel.currentPage.value?.mimeType)
                viewModel.consumeOpenFileUri()
            }
        }

        viewModel.shareRequest.observe(viewLifecycleOwner) { request ->
            when (request) {
                is ShareRequest.File -> {
                    shareFile(request.uri, request.mimeType)
                    viewModel.consumeShareRequest()
                }
                is ShareRequest.Text -> {
                    shareText(request.content)
                    viewModel.consumeShareRequest()
                }
                null -> Unit
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.consumeError()
            }
        }
    }

    private fun displayPage(page: DocumentPage) {
        val total = viewModel.pages.value?.size ?: 0
        val index = viewModel.currentIndex.value ?: 0
        binding.tvPageCounter.text = getString(R.string.page_counter, index + 1, total)
        binding.tvCaption.text = page.caption ?: ""

        if (page.isTextOnly) {
            binding.photoView.visibility = View.GONE
            binding.btnOpenFile.visibility = View.GONE
            binding.btnShare.visibility = View.VISIBLE
            binding.tvTextContent.visibility = View.VISIBLE
            binding.tvTextContent.gravity = Gravity.CENTER
            binding.tvTextContent.text = page.textContent.orEmpty()
        } else if (page.isImage) {
            binding.photoView.visibility = View.VISIBLE
            binding.btnOpenFile.visibility = View.GONE
            binding.btnShare.visibility = View.VISIBLE
            binding.tvTextContent.visibility = View.GONE
            Glide.with(this)
                .load(DriveImageRef(page.driveFileId.orEmpty()))
                .error(android.R.drawable.ic_menu_report_image)
                .into(binding.photoView)
        } else {
            binding.photoView.visibility = View.GONE
            binding.btnOpenFile.visibility = View.VISIBLE
            binding.btnShare.visibility = View.VISIBLE
            binding.tvTextContent.visibility = View.VISIBLE
            binding.tvTextContent.gravity = Gravity.CENTER
            binding.tvTextContent.text = buildString {
                append(page.fileName ?: getString(R.string.file_entry))
                page.mimeType?.takeIf { it.isNotBlank() }?.let {
                    append("\n")
                    append(it)
                }
            }
        }
    }

    private fun updateNavButtons(index: Int, size: Int) {
        binding.btnPrev.isEnabled = index > 0
        binding.btnNext.isEnabled = index < size - 1
    }

    private fun showEditDialog(page: DocumentPage) {
        val captionLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.caption_hint)
            addView(
                TextInputEditText(context).apply {
                    setText(page.caption.orEmpty())
                }
            )
        }

        val sequenceLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.sequence_hint)
            addView(
                TextInputEditText(context).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    setText(page.sequence.toString())
                }
            )
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(captionLayout)
            addView(sequenceLayout)
        }

        var textLayout: TextInputLayout? = null
        if (page.isTextOnly) {
            textLayout = TextInputLayout(requireContext()).apply {
                hint = getString(R.string.text_entry_hint)
                addView(
                    TextInputEditText(context).apply {
                        minLines = 4
                        setText(page.textContent.orEmpty())
                    }
                )
            }
            container.addView(textLayout)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val updated = page.copy(
                    sequence = sequenceLayout.editText?.text?.toString()?.trim()?.toIntOrNull() ?: page.sequence,
                    caption = captionLayout.editText?.text?.toString()?.trim().takeUnless { it.isNullOrBlank() },
                    textContent = if (page.isTextOnly) {
                        textLayout?.editText?.text?.toString()?.trim().takeUnless { it.isNullOrBlank() }
                    } else {
                        page.textContent
                    }
                )
                viewModel.updatePage(updated)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openFile(uri: android.net.Uri, mimeType: String?) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.no_app_found_for_file, Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(uri: android.net.Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_item)))
    }

    private fun shareText(content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_item)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
