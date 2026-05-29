package com.accounting.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.accounting.data.repository.TransactionRepository
import com.accounting.domain.Transaction
import com.accounting.domain.TransactionSource
import com.accounting.model.Category
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TransactionNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var repository: TransactionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val wechatPackages = setOf("com.tencent.mm")
    private val qqPackages = setOf("com.tencent.mobileqq")
    private val alipayPackages = setOf("com.eg.android.AlipayGphone")

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName ?: return

        // Only process WeChat and Alipay notifications
        if (packageName !in wechatPackages && packageName !in qqPackages && packageName !in alipayPackages) {
            // Check if it's a bank notification (common bank package patterns)
            if (!isBankNotification(packageName)) {
                return
            }
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isEmpty() && text.isEmpty()) return

        scope.launch {
            val source: TransactionSource = when (packageName) {
                in wechatPackages -> TransactionSource.WECHAT
                in qqPackages -> TransactionSource.QQ
                in alipayPackages -> TransactionSource.ALIPAY
                else -> TransactionSource.BANK
            }

            // Parse transaction from notification
            val transaction = parseTransaction(source, title, text, packageName)
            transaction?.let {
                repository.insertIfNotDuplicate(it)
            }
        }
    }

    private fun isBankNotification(packageName: String): Boolean {
        val bankKeywords = listOf(
            "bank", "bankcomm", "ccb", "icbc", "boc", "abc", "cmb", "citic"
        )
        return bankKeywords.any { packageName.lowercase().contains(it) }
    }

    private fun parseTransaction(
        source: TransactionSource,
        title: String,
        text: String,
        packageName: String
    ): Transaction? {
        val fullText = "$title $text"

        val rawAmount = extractAmount(fullText) ?: return null
        val amount = applyDirection(rawAmount, fullText, source)

        // Extract counterparty if possible
        val counterparty = extractCounterparty(fullText, source)

        // Extract description
        val description = if (title.isNotEmpty()) title else text
        val category = Category.categorize("$description $counterparty $fullText")

        return Transaction(
            source = source.name,
            amount = amount,
            counterparty = counterparty,
            category = category,
            timestamp = System.currentTimeMillis(),
            description = description,
            rawNotification = sanitizeRawText(fullText)
        )
    }

    private fun extractAmount(text: String): Double? {
        // Match patterns like ¥128.00, ¥1,234.56, 128.00元, etc.
        val patterns = listOf(
            """¥\s*([\d,]+\.?\d*)""".toRegex(),
            """([\d,]+\.?\d*)\s*元""".toRegex(),
            """\$\s*([\d,]+\.?\d*)""".toRegex()
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                return amountStr.toDoubleOrNull()
            }
        }
        return null
    }

    private fun applyDirection(amount: Double, text: String, source: TransactionSource): Double {
        val incomeKeywords = listOf("收入", "收款", "收到", "入账", "转入", "退款", "退回", "红包收入")
        val expenseKeywords = listOf("支出", "支付", "付款", "扣款", "消费", "转出", "已付款", "订单支付", "交易成功")

        return when {
            expenseKeywords.any { text.contains(it, ignoreCase = true) } -> -kotlin.math.abs(amount)
            incomeKeywords.any { text.contains(it, ignoreCase = true) } -> kotlin.math.abs(amount)
            source in setOf(
                TransactionSource.JD,
                TransactionSource.TAOBAO,
                TransactionSource.PINDUODUO,
                TransactionSource.MEITUAN,
                TransactionSource.ELE,
                TransactionSource.METRO
            ) -> -kotlin.math.abs(amount)
            else -> amount
        }
    }

    private fun sanitizeRawText(text: String): String {
        return text
            .replace(Regex("""\d{12,}"""), "***")
            .replace(Regex("""\b\d{4}(?:\s?\d{4}){2,}\b"""), "***")
            .take(300)
    }

    private fun extractCounterparty(text: String, source: TransactionSource): String? {
        // Source-specific counterparty extraction
        return when (source) {
            TransactionSource.WECHAT -> {
                // WeChat notifications often contain "收到钱 from XXX" or "Payment from XXX"
                val patterns = listOf(
                    """(?:收到|from|来自)\s*([^\s¥\d]+)""".toRegex(),
                    """([^\s¥\d]+)\s*(?:向你付款|给你转账)""".toRegex()
                )
                for (pattern in patterns) {
                    val match = pattern.find(text)
                    if (match != null) {
                        return match.groupValues[1].trim()
                    }
                }
                null
            }
            TransactionSource.QQ -> {
                val patterns = listOf(
                    """(?:收到|from|来自)\s*([^\s¥\d]+)""".toRegex(),
                    """([^\s¥\d]+)\s*(?:向你付款|给你转账)""".toRegex()
                )
                for (pattern in patterns) {
                    val match = pattern.find(text)
                    if (match != null) {
                        return match.groupValues[1].trim()
                    }
                }
                null
            }
            TransactionSource.ALIPAY -> {
                // Alipay notifications often contain the counterparty name
                val patterns = listOf(
                    """([^\s¥\d]+)\s*(?:收款|转账给你)""".toRegex(),
                    """(?:收款方|对方)\s*[:：]\s*([^\s¥\d]+)""".toRegex()
                )
                for (pattern in patterns) {
                    val match = pattern.find(text)
                    if (match != null) {
                        return match.groupValues[1].trim()
                    }
                }
                null
            }
            TransactionSource.BANK -> {
                // Banks usually don't show counterparty in notification
                null
            }
            TransactionSource.JD, TransactionSource.TAOBAO, TransactionSource.PINDUODUO,
            TransactionSource.MEITUAN, TransactionSource.ELE, TransactionSource.METRO -> {
                // These apps don't send notification with counterparty info
                null
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed for this implementation
    }
}
