package com.lifesaver.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
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
    }

    private fun displayPage(page: DocumentPage) {
        val total = viewModel.pages.value?.size ?: 0
        val index = viewModel.currentIndex.value ?: 0
        binding.tvPageCounter.text = getString(R.string.page_counter, index + 1, total)
        binding.tvCaption.text = page.caption ?: ""

        Glide.with(this)
            .load(DriveImageRef(page.driveFileId))
            .error(android.R.drawable.ic_menu_report_image)
            .into(binding.photoView)
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
