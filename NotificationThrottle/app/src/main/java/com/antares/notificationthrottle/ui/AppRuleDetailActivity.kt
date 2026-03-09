package com.antares.notificationthrottle.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.antares.notificationthrottle.data.AppRuleRepository
import com.antares.notificationthrottle.databinding.ActivityAppRuleDetailBinding
import com.antares.notificationthrottle.model.AppRule
import com.antares.notificationthrottle.model.TimeWindow
import com.google.android.material.snackbar.Snackbar

class AppRuleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppRuleDetailBinding
    private lateinit var repository: AppRuleRepository

    private lateinit var packageName: String
    private lateinit var appName: String
    private var existingRule: AppRule? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppRuleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run { finish(); return }
        appName = intent.getStringExtra(EXTRA_APP_NAME) ?: packageName

        title = appName
        repository = AppRuleRepository.getInstance(applicationContext)

        setupTimeWindowDropdown()
        setupMaxNotificationsSlider()
        loadExistingRule()
        setupSaveButton()
    }

    private fun setupTimeWindowDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            TimeWindow.labels()
        )
        binding.actvTimeWindow.setAdapter(adapter)
    }

    private fun setupMaxNotificationsSlider() {
        binding.sliderMax.addOnChangeListener { _, value, _ ->
            val count = value.toInt()
            binding.tvMaxLabel.text = "Max notifications: $count"
        }
        binding.sliderMax.value = 2f // default
        binding.tvMaxLabel.text = "Max notifications: 2"
    }

    private fun loadExistingRule() {
        existingRule = repository.getRuleForPackage(packageName)
        existingRule?.let { rule ->
            // Set time window dropdown
            val window = TimeWindow.fromMinutes(rule.timeWindowMinutes)
            val idx = TimeWindow.values().indexOf(window)
            binding.actvTimeWindow.setText(TimeWindow.labels()[idx], false)

            // Set slider
            val clampedMax = rule.maxNotifications.toFloat().coerceIn(1f, 20f)
            binding.sliderMax.value = clampedMax
            binding.tvMaxLabel.text = "Max notifications: ${rule.maxNotifications}"

            // Set toggle
            binding.switchRuleEnabled.isChecked = rule.enabled
        } ?: run {
            // Defaults for new rule
            val defaultIdx = TimeWindow.values().indexOf(TimeWindow.TEN_MIN)
            binding.actvTimeWindow.setText(TimeWindow.labels()[defaultIdx], false)
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val selectedLabel = binding.actvTimeWindow.text.toString()
            val selectedWindow = TimeWindow.values().find { it.label == selectedLabel }
                ?: TimeWindow.TEN_MIN

            val maxNotif = binding.sliderMax.value.toInt()
            val enabled = binding.switchRuleEnabled.isChecked

            val rule = AppRule(
                packageName = packageName,
                appName = appName,
                maxNotifications = maxNotif,
                timeWindowMinutes = selectedWindow.minutes,
                enabled = enabled
            )

            repository.saveRule(rule)
            Snackbar.make(binding.root, "Rule saved for $appName", Snackbar.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
    }
}
