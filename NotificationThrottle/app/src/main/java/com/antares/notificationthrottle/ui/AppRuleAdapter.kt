package com.antares.notificationthrottle.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.antares.notificationthrottle.databinding.ItemAppRuleBinding
import com.antares.notificationthrottle.model.AppRule
import com.antares.notificationthrottle.model.TimeWindow

class AppRuleAdapter(
    private val onToggle: (String, Boolean) -> Unit,
    private val onEdit: (AppRule) -> Unit,
    private val onDelete: (AppRule) -> Unit
) : ListAdapter<AppRule, AppRuleAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemAppRuleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rule: AppRule) {
            binding.tvAppName.text = rule.appName
            binding.tvRuleSummary.text = buildSummary(rule)
            binding.switchEnabled.isChecked = rule.enabled

            // Suppress listener during bind to avoid feedback loop
            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.isChecked = rule.enabled
            binding.switchEnabled.setOnCheckedChangeListener { _, checked ->
                onToggle(rule.packageName, checked)
            }

            binding.btnEdit.setOnClickListener { onEdit(rule) }
            binding.btnDelete.setOnClickListener { onDelete(rule) }
        }

        private fun buildSummary(rule: AppRule): String {
            val window = TimeWindow.fromMinutes(rule.timeWindowMinutes)
            val notifWord = if (rule.maxNotifications == 1) "notification" else "notifications"
            return "Max ${rule.maxNotifications} $notifWord per ${window.label}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppRule>() {
            override fun areItemsTheSame(a: AppRule, b: AppRule) = a.packageName == b.packageName
            override fun areContentsTheSame(a: AppRule, b: AppRule) = a == b
        }
    }
}
