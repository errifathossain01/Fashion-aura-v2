package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesManager
import com.example.data.model.AppLanguage
import com.example.data.model.UserPreferences
import com.example.data.repository.CourierRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val courierRepository: CourierRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesManager.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserPreferences()
        )

    private val _balanceState = MutableStateFlow<String?>(null)
    val balanceState: StateFlow<String?> = _balanceState.asStateFlow()

    private val _isCheckingBalance = MutableStateFlow(false)
    val isCheckingBalance: StateFlow<Boolean> = _isCheckingBalance.asStateFlow()

    fun setDarkMode(isDark: Boolean?) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(isDark)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesManager.setAppLanguage(language)
            preferencesManager.setUseBanglaDigits(language == AppLanguage.BANGLA)
        }
    }

    fun setUseBanglaDigits(useBangla: Boolean) {
        viewModelScope.launch {
            preferencesManager.setUseBanglaDigits(useBangla)
        }
    }

    fun saveSteadfastCredentials(apiKey: String, secretKey: String) {
        viewModelScope.launch {
            preferencesManager.setSteadfastCredentials(apiKey, secretKey)
        }
    }

    fun setAutoSyncCourier(autoSync: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoSyncCourier(autoSync)
        }
    }

    fun checkSteadfastBalance(apiKey: String, secretKey: String) {
        viewModelScope.launch {
            _isCheckingBalance.value = true
            _balanceState.value = "ব্যালেন্স চেক করা হচ্ছে..."
            val result = courierRepository.getBalance(apiKey, secretKey)
            if (result.isSuccess) {
                val bal = result.getOrNull() ?: 0.0
                _balanceState.value = "বর্তমান ব্যালেন্স: ৳$bal"
            } else {
                _balanceState.value = "ব্যর্থ: ${result.exceptionOrNull()?.message}"
            }
            _isCheckingBalance.value = false
        }
    }

    class Factory(
        private val preferencesManager: PreferencesManager,
        private val courierRepository: CourierRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(preferencesManager, courierRepository) as T
        }
    }
}
