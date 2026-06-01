package com.accounting.data.repository

import com.accounting.data.db.TransactionDao
import com.accounting.domain.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getDeletedTransactions(): Flow<List<Transaction>> = transactionDao.getDeletedTransactions()

    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startTime, endTime)

    fun getTransactionsBySource(source: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsBySource(source)

    suspend fun insert(transaction: Transaction): Long = transactionDao.insert(transaction)

    suspend fun insertIfNotDuplicate(transaction: Transaction, windowMillis: Long = 2 * 60 * 1000): Long? {
        val duplicateCount = transactionDao.countSimilarTransactions(
            source = transaction.source,
            amount = transaction.amount,
            counterparty = transaction.counterparty,
            description = transaction.description,
            startTime = transaction.timestamp - windowMillis,
            endTime = transaction.timestamp + windowMillis
        )
        return if (duplicateCount == 0) transactionDao.insert(transaction) else null
    }

    suspend fun softDelete(id: Long) = transactionDao.softDelete(id)

    suspend fun restore(id: Long) = transactionDao.restore(id)

    suspend fun hardDelete(id: Long) = transactionDao.hardDelete(id)

    suspend fun deleteExpired() {
        val expiredTime = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        transactionDao.deleteExpired(expiredTime)
    }

    suspend fun deleteAllDeleted() = transactionDao.deleteAllDeleted()

    suspend fun getTotalExpense(startTime: Long, endTime: Long): Double =
        transactionDao.getTotalExpense(startTime, endTime) ?: 0.0

    suspend fun getTotalIncome(startTime: Long, endTime: Long): Double =
        transactionDao.getTotalIncome(startTime, endTime) ?: 0.0

    suspend fun getExpenseByCategory(startTime: Long, endTime: Long) =
        transactionDao.getExpenseByCategory(startTime, endTime)
}