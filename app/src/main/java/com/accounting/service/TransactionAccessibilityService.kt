package com.accounting.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
class TransactionAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var repository: TransactionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastTransactionTime = 0L
    private val debounceInterval = 3000L // 3 seconds

    // Supported apps
    private val supportedApps = mapOf(
        "com.tencent.mm" to TransactionSource.WECHAT,
        "com.tencent.mobileqq" to TransactionSource.QQ,
        "com.eg.android.AlipayGphone" to TransactionSource.ALIPAY,
        "com.jingdong.app.mall" to TransactionSource.JD,
        "com.taobao.taobao" to TransactionSource.TAOBAO,
        "com.xunmeng.pinduoduo" to TransactionSource.PINDUODUO,
        "com.sankuai.meituan" to TransactionSource.MEITUAN,
        "me.ele" to TransactionSource.ELE,
        "com.zhicall.ztk" to TransactionSource.METRO
    )

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
            packageNames = supportedApps.keys.toTypedArray()
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val packageName = event.packageName?.toString() ?: return

        // Check if this app is supported
        val source = supportedApps[packageName] ?: return

        // Debounce to avoid duplicate processing
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTransactionTime < debounceInterval) return

        // Get the root node of the active window
        val sourceNode = event.source ?: return

        // Scan the entire screen for amounts
        val screenText = collectVisibleText(sourceNode)
        val amounts = findAllAmounts(sourceNode)
            .distinct()
            .filter { it > 0 && it < 100000 }

        val amount = amounts.firstOrNull()
        if (amount != null) {
            saveTransaction(source, applyDirection(amount, screenText, source), null, screenText.take(120))
            lastTransactionTime = currentTime
        }

        sourceNode.recycle()
    }

    private fun findAllAmounts(node: AccessibilityNodeInfo?): List<Double> {
        val amounts = mutableListOf<Double>()

        if (node == null) return amounts

        // Check this node's text and content description
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val combinedText = text + " " + desc

        // Look for ¥ amounts
        val amountPatterns = listOf(
            """[¥￥]\s*([\d,]+\.?\d*)""".toRegex(),
            """([\d,]+\.\d{2})\s*元""".toRegex()
        )

        for (pattern in amountPatterns) {
            val matches = pattern.findAll(combinedText)
            for (match in matches) {
                val amountStr = match.groupValues[1].replace(",", "")
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0 && amount < 100000) {
                    amounts.add(amount)
                }
            }
        }

        // Recurse into children
        for (i in 0 until (node.childCount ?: 0)) {
            val child = node.getChild(i)
            if (child != null) {
                amounts.addAll(findAllAmounts(child))
                child.recycle()
            }
        }

        return amounts
    }

    private fun saveTransaction(source: TransactionSource, amount: Double, counterparty: String?, description: String?) {
        scope.launch {
            val transaction = Transaction(
                source = source.name,
                amount = amount,
                counterparty = counterparty,
                category = Category.categorize(description),
                timestamp = System.currentTimeMillis(),
                description = description,
                rawNotification = null
            )
            repository.insertIfNotDuplicate(transaction)
        }
    }

    private fun collectVisibleText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""

        val parts = mutableListOf<String>()
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let(parts::add)
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(parts::add)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                parts.add(collectVisibleText(child))
                child.recycle()
            }
        }

        return parts.joinToString(" ").trim()
    }

    private fun applyDirection(amount: Double, text: String, source: TransactionSource): Double {
        val incomeKeywords = listOf("收入", "收款", "收到", "入账", "转入", "退款", "退回", "红包收入")
        val expenseKeywords = listOf("支出", "支付", "付款", "扣款", "消费", "转出", "已付款", "确认支付", "立即支付")

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

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
