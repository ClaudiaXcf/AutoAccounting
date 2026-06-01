package com.accounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounting.data.db.CategoryTotal
import com.accounting.ui.theme.ExpenseRed
import com.accounting.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    monthlyExpense: Double,
    monthlyIncome: Double,
    categoryTotals: List<CategoryTotal>,
    onNavigateBack: () -> Unit
) {
    val backgroundColor = Color(0xFFF2F2F7)
    val surfaceColor = Color.White
    val dateFormat = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
    val currentMonth = dateFormat.format(Date())

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

            // 顶部导航
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(surfaceColor)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "统计报表",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // 概览卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF007AFF).copy(alpha = 0.06f),
                                    Color(0xFF5856D6).copy(alpha = 0.06f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Text(
                        text = currentMonth,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            color = Color(0xFF8E8E93)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "支出",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFF8E8E93)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "¥ ${String.format("%,.2f", kotlin.math.abs(monthlyExpense))}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ExpenseRed
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "收入",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFF8E8E93)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "¥ ${String.format("%,.2f", monthlyIncome)}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = IncomeGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 支出分类标题
            Text(
                text = "支出分类",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // 分类列表
            if (categoryTotals.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无分类数据",
                            color = Color(0xFF8E8E93),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = surfaceColor)
                        ) {
                            Column {
                                categoryTotals.forEachIndexed { index, categoryTotal ->
                                    CategoryRow(
                                        categoryTotal = categoryTotal,
                                        totalExpense = kotlin.math.abs(monthlyExpense)
                                    )
                                    if (index < categoryTotals.size - 1) {
                                        Divider(
                                            modifier = Modifier.padding(start = 66.dp),
                                            color = Color(0xFFE5E5EA),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }

        // 底部毛玻璃导航栏
        FrostedTabBarStatistics(
            onBackClick = onNavigateBack,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CategoryRow(
    categoryTotal: CategoryTotal,
    totalExpense: Double
) {
    val percentage = if (totalExpense > 0) (categoryTotal.total / totalExpense * 100) else 0.0
    val categoryName = categoryTotal.category ?: "未分类"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Text(
                text = "¥ ${String.format("%.2f", categoryTotal.total)}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF1C1C1E)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        // 进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE5E5EA))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage.toFloat().coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF007AFF),
                                Color(0xFF5856D6)
                            )
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${String.format("%.1f", percentage)}%",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                color = Color(0xFF8E8E93)
            )
        )
    }
}

@Composable
private fun FrostedTabBarStatistics(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(83.dp)
            .background(Color.White.copy(alpha = 0.8f))
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
                modifier = Modifier.clickable { onBackClick() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = "账单",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "账单",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF8E8E93)
                )
            }

            // 记账 tab (核心大按钮)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = "记账",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "记账",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF007AFF)
                )
            }

            // 统计 tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = "统计",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "统计",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF007AFF)
                )
            }
        }
    }
}