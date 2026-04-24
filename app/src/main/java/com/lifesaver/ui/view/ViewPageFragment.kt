package com.lifesaver.ui.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.github.chrisbanes.photoview.PhotoView
import com.lifesaver.LifeSaverApplication
import com.lifesaver.R
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

        viewModel.pages.observe(viewLifecycleOwner) { pages ->
            val index = viewModel.currentIndex.value ?: 0
            updateNavButtons(index, pages.size)
        }

        viewModel.currentIndex.observe(viewLifecycleOwner) { index ->
            val size = viewModel.pages.value?.size ?: 0
            updateNavButtons(index, size)
        }

        viewModel.currentPage.observe(viewLifecycleOwner) { page ->
            if (page != null) displayPage(page)
        }
    }

    private fun displayPage(page: DocumentPage) {
        val total = viewModel.pages.value?.size ?: 0
        val index = viewModel.currentIndex.value ?: 0
        binding.tvPageCounter.text = getString(R.string.page_counter, index + 1, total)
        binding.tvCaption.text = page.caption ?: ""

        val uri = Uri.parse(page.uri)
        val isLocal = uri.scheme == "content" || uri.scheme == "file"

        if (isLocal) {
            binding.photoView.visibility = View.VISIBLE
            binding.tvRemoteHint.visibility = View.GONE
            binding.btnOpenExternal.visibility = View.GONE
            try {
                binding.photoView.setImageURI(uri)
            } catch (e: Exception) {
                binding.photoView.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        } else {
            binding.photoView.visibility = View.GONE
            binding.tvRemoteHint.visibility = View.VISIBLE
            binding.btnOpenExternal.visibility = View.VISIBLE
            binding.btnOpenExternal.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }
    }

    private fun updateNavButtons(index: Int, size: Int) {
        binding.btnPrev.isEnabled = index > 0
        binding.btnNext.isEnabled = index < size - 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
