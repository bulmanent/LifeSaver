package com.lifesaver.ui.detail

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lifesaver.databinding.ItemDocumentPageBinding
import com.lifesaver.model.DocumentPage

class PageAdapter(
    private val onItemClick: (DocumentPage, Int) -> Unit,
    private val onItemLongClick: (DocumentPage) -> Boolean
) : ListAdapter<DocumentPage, PageAdapter.PageViewHolder>(DiffCallback) {

    inner class PageViewHolder(private val binding: ItemDocumentPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(page: DocumentPage, position: Int) {
            val seqLabel = "${position + 1}"
            binding.tvPageSequence.text = seqLabel
            binding.tvCaption.text = page.caption ?: ""

            try {
                val uri = Uri.parse(page.uri)
                if (uri.scheme == "content" || uri.scheme == "file") {
                    binding.ivPageThumb.setImageURI(uri)
                } else {
                    binding.ivPageThumb.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } catch (e: Exception) {
                binding.ivPageThumb.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            binding.root.setOnClickListener { onItemClick(page, position) }
            binding.root.setOnLongClickListener { onItemLongClick(page) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemDocumentPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DocumentPage>() {
        override fun areItemsTheSame(old: DocumentPage, new: DocumentPage) = old.id == new.id
        override fun areContentsTheSame(old: DocumentPage, new: DocumentPage) = old == new
    }
}
