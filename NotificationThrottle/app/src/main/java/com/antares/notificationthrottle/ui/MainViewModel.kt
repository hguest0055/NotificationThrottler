package com.antares.notificationthrottle.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.antares.notificationthrottle.data.AppRuleRepository
import com.antares.notificationthrottle.data.SuppressedLogRepository
import com.antares.notificationthrottle.model.AppRule
import com.antares.notificationthrottle.model.SuppressedNotification

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRuleRepository.getInstance(application)

    private val _rules = MutableLiveData<List<AppRule>>()
    val rules: LiveData<List<AppRule>> = _rules

    private val _suppressedLog = MutableLiveData<List<SuppressedNotification>>()
    val suppressedLog: LiveData<List<SuppressedNotification>> = _suppressedLog

    private val logListener: () -> Unit = {
        _suppressedLog.postValue(SuppressedLogRepository.log)
    }

    init {
        loadRules()
        _suppressedLog.value = SuppressedLogRepository.log
        SuppressedLogRepository.addListener(logListener)
    }

    fun loadRules() {
        _rules.value = repository.getAllRules().sortedBy { it.appName }
    }

    fun toggleRule(packageName: String, enabled: Boolean) {
        repository.updateRuleEnabled(packageName, enabled)
        loadRules()
    }

    fun deleteRule(packageName: String) {
        repository.deleteRule(packageName)
        loadRules()
    }

    fun clearLog() {
        SuppressedLogRepository.clear()
    }

    override fun onCleared() {
        super.onCleared()
        SuppressedLogRepository.removeListener(logListener)
    }
}
