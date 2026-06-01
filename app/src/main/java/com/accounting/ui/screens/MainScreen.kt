package com.accounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounting.domain.Transaction
import com.accounting.domain.TransactionSource
import com.accounting.ui.theme.AlipayBlue
import com.accounting.ui.theme.BankOrange
import com.accounting.ui.theme.ExpenseRed
import com.accounting.ui.theme.QQBlue
import com.accounting.ui.theme.WeChatGreen
import com.accounting.ui.viewmodel.TimeFilter
import java.text.SimpleDateFormat
import java.util.*

data class TransactionGroup(
    val date: String,
    val transactions: List<Transaction>
)

@Composable
fun MainScreen(
    transactions: List<Transaction>,
    timeFilter: TimeFilter,
    onTimeFilterChange: (TimeFilter) -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Group transactions by date
    val groupedTransactions = remember(transactions) {
        val dateFormat = SimpleDateFormat("MM月dd日 EEEE", Locale.CHINA)
        transactions.groupBy { dateFormat.format(Date(it.timestamp)) }
            .map { (date, txns) -> TransactionGroup(date, txns) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部状态栏预留
            Spacer(modifier = Modifier.height(50.dp))

            // 大标题
            Text(
                text = "本月账单",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // 概览卡片
            OverviewCard(
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 时间筛选器
            TimeFilterChips(
                selectedFilter = timeFilter,
                onFilterChange = onTimeFilterChange,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 交易列表
            if (groupedTransactions.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    groupedTransactions.forEach { group ->
                        item {
                            Text(
                                text = group.date,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = surfaceColor)
                            ) {
                                Column {
                                    group.transactions.forEachIndexed { index, transaction ->
                                        TransactionRow(
                                            transaction = transaction,
                                            onDelete = { onDelete(transaction.id) }
                                        )
                                        if (index < group.transactions.size - 1) {
                                            Divider(
                                                modifier = Modifier.padding(start = 66.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }

            // 底部导航栏高度
            Spacer(modifier = Modifier.height(83.dp))
        }

        // 底部毛玻璃导航栏
        FrostedTabBar(
            onStatisticsClick = onNavigateToStatistics,
            onTrashClick = onNavigateToTrash,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除？") },
            text = { Text("删除后可在垃圾箱中恢复，30天后自动清除") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("删除", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(transaction.timestamp))

    val sourceColor = when (transaction.source) {
        TransactionSource.WECHAT.name -> WeChatGreen
        TransactionSource.QQ.name -> QQBlue
        TransactionSource.ALIPAY.name -> AlipayBlue
        TransactionSource.BANK.name -> BankOrange
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标容器
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(sourceColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (transaction.source) {
                    TransactionSource.WECHAT.name -> Icons.Default.Chat
                    TransactionSource.QQ.name -> Icons.Default.Chat
                    TransactionSource.ALIPAY.name -> Icons.Default.Payment
                    TransactionSource.BANK.name -> Icons.Default.AccountBalance
                    else -> Icons.Default.Payment
                },
                contentDescription = null,
                tint = sourceColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.counterparty ?: "未知",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$formattedTime · ${TransactionSource.fromName(transaction.source)?.displayName ?: transaction.source}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "${if (transaction.amount >= 0) "+" else "-"}¥ ${String.format("%.2f", kotlin.math.abs(transaction.amount))}",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = if (transaction.amount >= 0) com.accounting.ui.theme.IncomeGreen else ExpenseRed
        )

        // 删除按钮
        IconButton(onClick = { showDeleteDialog = true }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OverviewCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OverviewItem(
                title = "本月支出",
                amount = "4,520.00",
                isExpense = true
            )
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            OverviewItem(
                title = "本月收入",
                amount = "9,800.00",
                isExpense = false
            )
        }
    }
}

@Composable
private fun OverviewItem(
    title: String,
    amount: String,
    isExpense: Boolean
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "¥ $amount",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isExpense) ExpenseRed else com.accounting.ui.theme.IncomeGreen
        )
    }
}

@Composable
private fun TimeFilterChips(
    selectedFilter: TimeFilter,
    onFilterChange: (TimeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        TimeFilter.ALL to "全部",
        TimeFilter.TODAY to "今天",
        TimeFilter.WEEK to "本周",
        TimeFilter.MONTH to "本月"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (filter, label) ->
            val isSelected = selectedFilter == filter
            val backgroundColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            }
            val textColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .clickable { onFilterChange(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 83.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "暂无交易记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请开启通知访问和无障碍服务",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun FrostedTabBar(
    onStatisticsClick: () -> Unit,
    onTrashClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabBarBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(83.dp)
            .background(tabBarBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // 账单 tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = "账单",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "账单",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 记账 tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = "记账",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "记账",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 统计/垃圾箱 tab
            Column(
                modifier = Modifier.clickable { onTrashClick() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "垃圾箱",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "垃圾箱",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}