package com.accounting.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val source: String,          // WECHAT, ALIPAY, BANK
    val amount: Double,
    val counterparty: String?,   // 交易对方
    val category: String?,       // 自动分类
    val timestamp: Long,         // 交易时间
    val description: String?,   // 交易描述
    val rawNotification: String? // 原始通知内容
)