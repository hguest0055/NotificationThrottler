package com.antares.notificationthrottle.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.antares.notificationthrottle.databinding.ItemLogEntryBinding
import com.antares.notificationthrottle.model.SuppressedNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SuppressedLogAdapter :
    ListAdapter<SuppressedNotification, SuppressedLogAdapter.ViewHolder>(DIFF) {

    private val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemLogEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: SuppressedNotification) {
            binding.tvAppName.text = entry.appName
            binding.tvTitle.text = entry.title ?: "(no title)"
            binding.tvText.text = entry.text ?: "(no text)"
            binding.tvTime.text = formatTime(entry.timestamp)
            binding.tvReason.text = entry.reason
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diffMs = now - timestamp
            return when {
                diffMs < 60_000 -> "just now"
                diffMs < 3_600_000 -> "${diffMs / 60_000}m ago"
                else -> dateFormat.format(Date(timestamp))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLogEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SuppressedNotification>() {
            override fun areItemsTheSame(a: SuppressedNotification, b: SuppressedNotification) =
                a.timestamp == b.timestamp && a.packageName == b.packageName
            override fun areContentsTheSame(a: SuppressedNotification, b: SuppressedNotification) =
                a == b
        }
    }
}
