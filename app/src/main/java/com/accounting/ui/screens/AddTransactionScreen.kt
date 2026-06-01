package com.accounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounting.domain.TransactionSource
import com.accounting.ui.theme.AlipayBlue
import com.accounting.ui.theme.BankOrange
import com.accounting.ui.theme.ExpenseRed
import com.accounting.ui.theme.QQBlue
import com.accounting.ui.theme.WeChatGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    onSave: (amount: Double, counterparty: String?, source: TransactionSource, description: String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var counterparty by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(TransactionSource.ALIPAY) }
    var isExpense by remember { mutableStateOf(true) }

    val sourceColors = mapOf(
        TransactionSource.WECHAT to WeChatGreen,
        TransactionSource.ALIPAY to AlipayBlue,
        TransactionSource.QQ to QQBlue,
        TransactionSource.BANK to BankOrange
    )

    val sources = listOf(
        TransactionSource.WECHAT to "微信",
        TransactionSource.ALIPAY to "支付宝",
        TransactionSource.QQ to "QQ",
        TransactionSource.BANK to "银行"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "记账",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 金额输入
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // 支出/收入切换
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = isExpense,
                            onClick = { isExpense = true },
                            label = { Text("支出") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseRed.copy(alpha = 0.15f),
                                selectedLabelColor = ExpenseRed
                            )
                        )
                        FilterChip(
                            selected = !isExpense,
                            onClick = { isExpense = false },
                            label = { Text("收入") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = com.accounting.ui.theme.IncomeGreen.copy(alpha = 0.15f),
                                selectedLabelColor = com.accounting.ui.theme.IncomeGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 金额
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¥",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isExpense) ExpenseRed else com.accounting.ui.theme.IncomeGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { newValue ->
                                // 只允许数字和一个小数点
                                if (newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    amount = newValue
                                }
                            },
                            placeholder = {
                                Text(
                                    "0.00",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            },
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 交易方信息
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "交易信息",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = counterparty,
                        onValueChange = { counterparty = it },
                        label = { Text("交易对方") },
                        placeholder = { Text("如：星巴克、超市") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("备注") },
                        placeholder = { Text("可选") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 支付方式
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "支付方式",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        sources.forEach { (source, name) ->
                            val isSelected = selectedSource == source
                            val sourceColor = sourceColors[source] ?: MaterialTheme.colorScheme.primary

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) sourceColor.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable { selectedSource = source }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = when (source) {
                                            TransactionSource.WECHAT -> Icons.Default.Chat
                                            TransactionSource.ALIPAY -> Icons.Default.Payment
                                            TransactionSource.QQ -> Icons.Default.Chat
                                            TransactionSource.BANK -> Icons.Default.AccountBalance
                                            else -> Icons.Default.Payment
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) sourceColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) sourceColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 保存按钮
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0) {
                        val finalAmount = if (isExpense) -amountValue else amountValue
                        onSave(
                            finalAmount,
                            counterparty.takeIf { it.isNotBlank() },
                            selectedSource,
                            description.takeIf { it.isNotBlank() }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "保存",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}