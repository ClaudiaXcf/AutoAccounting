package com.accounting.data.db

import androidx.room.*
import com.accounting.domain.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Query("SELECT * FROM transactions WHERE deleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE deleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND deleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE source = :source AND deleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsBySource(source: String): Flow<List<Transaction>>

    @Query("UPDATE transactions SET deleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET deleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM transactions WHERE deleted = 1 AND deletedAt < :expiredTime")
    suspend fun deleteExpired(expiredTime: Long)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE source = :source
            AND amount = :amount
            AND timestamp BETWEEN :startTime AND :endTime
            AND IFNULL(counterparty, '') = IFNULL(:counterparty, '')
            AND IFNULL(description, '') = IFNULL(:description, '')
            AND deleted = 0
        """
    )
    suspend fun countSimilarTransactions(
        source: String,
        amount: Double,
        counterparty: String?,
        description: String?,
        startTime: Long,
        endTime: Long
    ): Int

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND amount < 0 AND deleted = 0")
    suspend fun getTotalExpense(startTime: Long, endTime: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND amount > 0 AND deleted = 0")
    suspend fun getTotalIncome(startTime: Long, endTime: Long): Double?

    @Query("SELECT category, SUM(ABS(amount)) as total FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND amount < 0 AND deleted = 0 GROUP BY category ORDER BY total DESC")
    suspend fun getExpenseByCategory(startTime: Long, endTime: Long): List<CategoryTotal>

    @Query("DELETE FROM transactions WHERE deleted = 1")
    suspend fun deleteAllDeleted()
}

data class CategoryTotal(
    val category: String?,
    val total: Double
)