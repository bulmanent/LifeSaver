package com.lifesaver.ui.home

import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.lifesaver.databinding.ItemDocumentGroupBinding
import com.lifesaver.model.GroupSummary

class GroupAdapter(
    private val onItemClick: (GroupSummary) -> Unit,
    private val onItemLongClick: (GroupSummary) -> Boolean
) : ListAdapter<GroupSummary, GroupAdapter.GroupViewHolder>(DiffCallback) {

    inner class GroupViewHolder(private val binding: ItemDocumentGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(summary: GroupSummary) {
            binding.tvGroupTitle.text = summary.title
            binding.tvPageCount.text = "${summary.pageCount} page${if (summary.pageCount != 1) "s" else ""}"

            if (!summary.description.isNullOrBlank()) {
                binding.tvDescription.text = summary.description
                binding.tvDescription.visibility = View.VISIBLE
            } else {
                binding.tvDescription.visibility = View.GONE
            }

            binding.chipGroupTags.removeAllViews()
            summary.tags.forEach { tag ->
                val chip = Chip(binding.root.context).apply {
                    text = tag
                    isClickable = false
                    isCheckable = false
                }
                binding.chipGroupTags.addView(chip)
            }

            binding.root.setOnClickListener { onItemClick(summary) }
            binding.root.setOnLongClickListener { onItemLongClick(summary) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemDocumentGroupBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<GroupSummary>() {
        override fun areItemsTheSame(old: GroupSummary, new: GroupSummary) = old.id == new.id
        override fun areContentsTheSame(old: GroupSummary, new: GroupSummary) = old == new
    }
}
