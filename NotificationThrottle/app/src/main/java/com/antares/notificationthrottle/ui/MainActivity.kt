package com.antares.notificationthrottle.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.antares.notificationthrottle.R
import com.antares.notificationthrottle.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var rulesAdapter: AppRuleAdapter
    private lateinit var logAdapter: SuppressedLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerViews()
        setupTabs()
        setupFab()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
        viewModel.loadRules()
    }

    private fun setupRecyclerViews() {
        rulesAdapter = AppRuleAdapter(
            onToggle = { pkg, enabled -> viewModel.toggleRule(pkg, enabled) },
            onEdit = { rule ->
                val intent = Intent(this, AppRuleDetailActivity::class.java).apply {
                    putExtra(AppRuleDetailActivity.EXTRA_PACKAGE_NAME, rule.packageName)
                    putExtra(AppRuleDetailActivity.EXTRA_APP_NAME, rule.appName)
                }
                startActivity(intent)
            },
            onDelete = { rule ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Remove Rule")
                    .setMessage("Stop throttling notifications from ${rule.appName}?")
                    .setPositiveButton("Remove") { _, _ -> viewModel.deleteRule(rule.packageName) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.rvRules.apply {
            adapter = rulesAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        logAdapter = SuppressedLogAdapter()
        binding.rvLog.apply {
            adapter = logAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.rvRules.visibility = android.view.View.VISIBLE
                        binding.rvLog.visibility = android.view.View.GONE
                        binding.fab.show()
                    }
                    1 -> {
                        binding.rvRules.visibility = android.view.View.GONE
                        binding.rvLog.visibility = android.view.View.VISIBLE
                        binding.fab.hide()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.rules.observe(this) { rules ->
            rulesAdapter.submitList(rules)
            binding.tvEmptyRules.visibility =
                if (rules.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.suppressedLog.observe(this) { log ->
            logAdapter.submitList(log)
            binding.tvEmptyLog.visibility =
                if (log.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun checkPermission() {
        val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)

        binding.bannerPermission.visibility =
            if (hasPermission) android.view.View.GONE else android.view.View.VISIBLE

        binding.btnGrantPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_log -> {
                viewModel.clearLog()
                Snackbar.make(binding.root, "Log cleared", Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
