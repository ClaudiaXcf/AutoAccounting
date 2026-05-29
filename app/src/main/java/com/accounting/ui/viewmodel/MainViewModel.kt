package com.accounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.data.db.CategoryTotal
import com.accounting.data.repository.TransactionRepository
import com.accounting.domain.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class MainUiState(
    val transactions: List<Transaction> = emptyList(),
    val monthlyExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val categoryTotals: List<CategoryTotal> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            repository.getAllTransactions().collect { transactions ->
                _uiState.update { it.copy(transactions = transactions) }
                calculateStatistics(transactions)
            }
        }
    }

    private fun calculateStatistics(transactions: List<Transaction>) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val monthStart = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            val monthEnd = calendar.timeInMillis - 1

            val monthlyTransactions = transactions.filter {
                it.timestamp in monthStart..monthEnd
            }

            val expense = monthlyTransactions.filter { it.amount < 0 }.sumOf { it.amount }
            val income = monthlyTransactions.filter { it.amount > 0 }.sumOf { it.amount }
            val categories = repository.getExpenseByCategory(monthStart, monthEnd)

            _uiState.update {
                it.copy(
                    monthlyExpense = expense,
                    monthlyIncome = income,
                    categoryTotals = categories
                )
            }
        }
    }
}