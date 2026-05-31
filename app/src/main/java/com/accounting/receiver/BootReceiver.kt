package com.accounting.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.accounting.service.TransactionAccessibilityService
import com.accounting.service.TransactionNotificationListener

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 确保服务在开机后启动
            val serviceIntent = Intent(context, TransactionNotificationListener::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            // Accessibility Service 由系统自动管理，不需要手动启动
            // 但可以发送广播让应用检查服务状态
        }
    }
}