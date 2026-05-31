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
        // 更严格的金额匹配模式，避免误匹配
        val patterns = listOf(
            // ¥123.45 或 ¥ 123.45（人民币符号开头）
            """¥\s*(\d{1,6}(?:\.\d{1,2})?)""".toRegex(),
            // 123.45元 或 123.45 元（元结尾）
            """(\d{1,6}(?:\.\d{1,2})?)\s*元""".toRegex(),
            // $123.45（美元符号开头，需要区分处理）
            """\$\s*(\d{1,6}(?:\.\d{1,2})?)""".toRegex()
        )

        var bestAmount: Double? = null
        var maxAmount = 0.0

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0 && amount <= 999999.99) {
                    // 优先选择较大的金额（更可能是实际交易金额）
                    if (amount > maxAmount) {
                        maxAmount = amount
                        bestAmount = amount
                    }
                }
            }
        }
        return bestAmount
    }

    private fun applyDirection(amount: Double, text: String, source: TransactionSource): Double {
        val lowerText = text.lowercase()
        
        // 收入相关关键词（正数）
        val incomeKeywords = listOf(
            "收入", "收款", "收到", "入账", "转入", "退款", "退回", "红包收入",
            "转账收入", "收到转账", "对方转账", "确认收款"
        )
        
        // 支出相关关键词（负数）
        val expenseKeywords = listOf(
            "支出", "支付", "付款", "扣款", "消费", "转出", "已付款", "订单支付",
            "交易成功", "已支付", "支付成功", "扫码支付", "转账支出", "付给"
        )
        
        // 先检查关键词
        when {
            incomeKeywords.any { lowerText.contains(it) } -> return kotlin.math.abs(amount)
            expenseKeywords.any { lowerText.contains(it) } -> return -kotlin.math.abs(amount)
        }
        
        // 基于来源判断（电商/外卖平台默认为支出）
        return when (source) {
            TransactionSource.JD, TransactionSource.TAOBAO, TransactionSource.PINDUODUO,
            TransactionSource.MEITUAN, TransactionSource.ELE, TransactionSource.METRO -> -kotlin.math.abs(amount)
            // 微信和支付宝需要更多上下文判断
            TransactionSource.WECHAT, TransactionSource.ALIPAY -> {
                // 检查是否包含二维码等支付相关词汇
                if (lowerText.contains("二维码") || lowerText.contains("扫码")) {
                    -kotlin.math.abs(amount)
                } else {
                    // 微信/支付宝默认视为支出（更常见）
                    -kotlin.math.abs(amount)
                }
            }
            else -> amount
        }
    }

    private fun sanitizeRawText(text: String): String {
        // 脱敏所有敏感信息
        return text
            // 银行卡号 (16-19位)
            .replace(Regex("""\b\d{16,19}\b"""), "****")
            // 信用卡安全码 (3-4位)
            .replace(Regex("""[cvv]\s*[:：]?\s*\d{3,4}""", RegexOption.IGNORE_CASE), "***")
            // 手机号
            .replace(Regex("""\b1[3-9]\d{9}\b"""), "***")
            // 身份证号
            .replace(Regex("""\b\d{15}|\d{17}[\dXx]\b"""), "***")
            // 账号中的数字序列 (连续12位以上)
            .replace(Regex("""\d{12,}"""), "***")
            // 分隔的银行卡号 (如 1234 5678 9012 3456)
            .replace(Regex("""\b\d{4}(\s?\d{4}){2,}\b"""), "****")
            // 交易流水号/订单号
            .replace(Regex("""[A-Z]{0,3}\d{8,}[A-Z]{0,3}"""), "***")
            // 限制长度
            .take(200)
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
