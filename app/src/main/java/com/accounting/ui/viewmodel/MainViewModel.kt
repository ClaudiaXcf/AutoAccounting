package com.accounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.data.db.CategoryTotal
import com.accounting.data.repository.TransactionRepository
import com.accounting.domain.Transaction
import com.accounting.domain.TransactionSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class TimeFilter {
    ALL, TODAY, WEEK, MONTH
}

data class MainUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val monthlyExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val timeFilter: TimeFilter = TimeFilter.ALL
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
        cleanExpiredTrash()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            repository.getAllTransactions().collect { transactions ->
                _uiState.update { state ->
                    state.copy(
                        transactions = transactions,
                        filteredTransactions = filterTransactions(transactions, state.timeFilter)
                    )
                }
                calculateStatistics(transactions)
            }
        }
    }

    fun setTimeFilter(filter: TimeFilter) {
        _uiState.update { state ->
            state.copy(
                timeFilter = filter,
                filteredTransactions = filterTransactions(state.transactions, filter)
            )
        }
    }

    fun softDelete(id: Long) {
        viewModelScope.launch {
            repository.softDelete(id)
        }
    }

    private fun filterTransactions(transactions: List<Transaction>, filter: TimeFilter): List<Transaction> {
        val calendar = Calendar.getInstance()

        return when (filter) {
            TimeFilter.ALL -> transactions
            TimeFilter.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                transactions.filter { it.timestamp >= startOfDay }
            }
            TimeFilter.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfWeek = calendar.timeInMillis
                transactions.filter { it.timestamp >= startOfWeek }
            }
            TimeFilter.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis
                transactions.filter { it.timestamp >= startOfMonth }
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

    private fun cleanExpiredTrash() {
        viewModelScope.launch {
            repository.deleteExpired()
        }
    }

    fun addTransaction(amount: Double, counterparty: String?, source: TransactionSource, description: String?) {
        viewModelScope.launch {
            val transaction = Transaction(
                source = source.name,
                amount = amount,
                counterparty = counterparty,
                category = null,
                timestamp = System.currentTimeMillis(),
                description = description,
                rawNotification = null
            )
            repository.insert(transaction)
        }
    }
}