package com.lifesaver.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lifesaver.data.remote.DriveImageRef
import com.lifesaver.databinding.ItemDocumentPageBinding
import com.lifesaver.model.DocumentPage

class PageAdapter(
    private val onItemClick: (DocumentPage, Int) -> Unit,
    private val onItemLongClick: (DocumentPage) -> Boolean
) : ListAdapter<DocumentPage, PageAdapter.PageViewHolder>(DiffCallback) {

    inner class PageViewHolder(private val binding: ItemDocumentPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(page: DocumentPage, position: Int) {
            binding.tvPageSequence.text = page.sequence.toString()
            binding.tvCaption.text = when {
                page.isTextOnly -> page.textContent.orEmpty()
                !page.caption.isNullOrBlank() -> page.caption
                else -> binding.root.context.getString(com.lifesaver.R.string.image_entry)
            }

            if (page.isTextOnly) {
                binding.ivPageThumb.setImageResource(android.R.drawable.ic_menu_edit)
            } else {
                Glide.with(binding.ivPageThumb)
                    .load(DriveImageRef(page.driveFileId.orEmpty()))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .centerCrop()
                    .into(binding.ivPageThumb)
            }

            binding.root.setOnClickListener { onItemClick(page, position) }
            binding.root.setOnLongClickListener { onItemLongClick(page) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemDocumentPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
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
