package com.antares.notificationthrottle.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.antares.notificationthrottle.data.AppRuleRepository
import com.antares.notificationthrottle.databinding.ActivityAppPickerBinding
import com.antares.notificationthrottle.databinding.ItemAppPickerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppPickerBinding
    private lateinit var adapter: PickerAdapter
    private lateinit var repository: AppRuleRepository
    private var allApps: List<AppInfo> = emptyList()

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val alreadyHasRule: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Choose App to Throttle"

        repository = AppRuleRepository.getInstance(applicationContext)

        adapter = PickerAdapter { appInfo ->
            val intent = Intent(this, AppRuleDetailActivity::class.java).apply {
                putExtra(AppRuleDetailActivity.EXTRA_PACKAGE_NAME, appInfo.packageName)
                putExtra(AppRuleDetailActivity.EXTRA_APP_NAME, appInfo.appName)
            }
            startActivity(intent)
            finish()
        }

        binding.rvApps.apply {
            adapter = this@AppPickerActivity.adapter
            layoutManager = LinearLayoutManager(this@AppPickerActivity)
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filterApps(s?.toString() ?: "")
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadApps()
    }

    private fun loadApps() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val pm = packageManager
            val existingRules = repository.getAllRules().map { it.packageName }.toSet()

            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // user apps only
                .map { info ->
                    AppInfo(
                        packageName = info.packageName,
                        appName = pm.getApplicationLabel(info).toString(),
                        alreadyHasRule = info.packageName in existingRules
                    )
                }
                .sortedBy { it.appName }

            withContext(Dispatchers.Main) {
                allApps = apps
                adapter.submitList(apps)
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
        adapter.submitList(filtered)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class PickerAdapter(private val onClick: (AppInfo) -> Unit) :
        ListAdapter<AppInfo, PickerAdapter.VH>(DIFF) {

        inner class VH(private val binding: ItemAppPickerBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(app: AppInfo) {
                binding.tvAppName.text = app.appName
                binding.tvPackageName.text = app.packageName
                binding.tvHasRule.visibility =
                    if (app.alreadyHasRule) android.view.View.VISIBLE else android.view.View.GONE
                binding.root.setOnClickListener { onClick(app) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemAppPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
                override fun areItemsTheSame(a: AppInfo, b: AppInfo) = a.packageName == b.packageName
                override fun areContentsTheSame(a: AppInfo, b: AppInfo) = a == b
            }
        }
    }
}
