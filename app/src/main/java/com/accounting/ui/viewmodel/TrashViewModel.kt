package com.accounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.data.repository.TransactionRepository
import com.accounting.domain.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val deletedTransactions: List<Transaction> = emptyList()
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        loadDeletedTransactions()
    }

    private fun loadDeletedTransactions() {
        viewModelScope.launch {
            repository.getDeletedTransactions().collect { transactions ->
                _uiState.update { it.copy(deletedTransactions = transactions) }
            }
        }
    }

    fun restore(id: Long) {
        viewModelScope.launch {
            repository.restore(id)
        }
    }

    fun hardDelete(id: Long) {
        viewModelScope.launch {
            repository.hardDelete(id)
        }
    }

    fun deleteAllExpired() {
        viewModelScope.launch {
            repository.deleteExpired()
        }
    }
}