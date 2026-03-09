package com.antares.notificationthrottle.data

import android.content.Context
import android.content.SharedPreferences
import com.antares.notificationthrottle.model.AppRule
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppRuleRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAllRules(): List<AppRule> {
        val json = prefs.getString(KEY_RULES, null) ?: return emptyList()
        val type = object : TypeToken<List<AppRule>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getRuleForPackage(packageName: String): AppRule? {
        return getAllRules().find { it.packageName == packageName }
    }

    fun saveRule(rule: AppRule) {
        val rules = getAllRules().toMutableList()
        val index = rules.indexOfFirst { it.packageName == rule.packageName }
        if (index >= 0) {
            rules[index] = rule
        } else {
            rules.add(rule)
        }
        persistRules(rules)
    }

    fun deleteRule(packageName: String) {
        val rules = getAllRules().toMutableList()
        rules.removeAll { it.packageName == packageName }
        persistRules(rules)
    }

    fun updateRuleEnabled(packageName: String, enabled: Boolean) {
        val rule = getRuleForPackage(packageName) ?: return
        saveRule(rule.copy(enabled = enabled))
    }

    private fun persistRules(rules: List<AppRule>) {
        prefs.edit().putString(KEY_RULES, gson.toJson(rules)).apply()
    }

    companion object {
        private const val PREFS_NAME = "notification_throttle_prefs"
        private const val KEY_RULES = "app_rules"

        @Volatile
        private var instance: AppRuleRepository? = null

        fun getInstance(context: Context): AppRuleRepository {
            return instance ?: synchronized(this) {
                instance ?: AppRuleRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
