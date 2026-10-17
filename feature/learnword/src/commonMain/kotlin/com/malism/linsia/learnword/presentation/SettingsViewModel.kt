package com.malism.linsia.learnword.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.linsia.learnword.data.catalog.CatalogRepository
import com.malism.linsia.learnword.data.study.StudyRepository
import com.malism.linsia.learnword.domain.LearnWordDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUi(
    val dailyNew: Int = 20,
    val accentUk: Boolean = false,
    val activeBookId: String = LearnWordDefaults.BundledBookId,
    val message: String? = null,
    val checking: Boolean = false,
)

class SettingsViewModel(
    private val study: StudyRepository,
    private val catalog: CatalogRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUi())
    val state: StateFlow<SettingsUi> = _state

    init {
        viewModelScope.launch {
            val s = study.settingsOrDefault()
            _state.value = _state.value.copy(dailyNew = s.dailyNewWords, accentUk = s.accentUk, activeBookId = s.activeBookId)
        }
        viewModelScope.launch {
            study.observeActiveBookId().collect { id ->
                _state.value = _state.value.copy(activeBookId = id)
            }
        }
    }

    fun setDailyNew(value: Int) {
        viewModelScope.launch {
            study.updateSettings(dailyNew = value)
            _state.value = _state.value.copy(dailyNew = value.coerceIn(5, 100))
        }
    }

    fun setAccentUk(uk: Boolean) {
        viewModelScope.launch {
            study.updateSettings(accentUk = uk)
            _state.value = _state.value.copy(accentUk = uk)
        }
    }

    fun checkCatalog() {
        viewModelScope.launch {
            _state.value = _state.value.copy(checking = true, message = null)
            val result = catalog.checkRemoteUpdate()
            val msg = when {
                result.isFailure -> "更新失败，已保留本地清单"
                result.getOrNull() == true -> "词书清单已更新"
                else -> "已是最新版本"
            }
            _state.value = _state.value.copy(checking = false, message = msg)
        }
    }
}
