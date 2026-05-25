package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.content.Intent
import com.example.data.database.AppDatabase
import com.example.data.model.Transaction
import com.example.data.api.GeminiApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationService : NotificationListenerService() {
    private val TAG = "NotificationService"
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val notification = sbn ?: return

        val packageName = notification.packageName
        val extras = notification.notification.extras
        val title = extras.getString("android.title", "")
        val text = extras.getCharSequence("android.text", "").toString()

        Log.d(TAG, "Notification received from: $packageName | Title: $title | Text: $text")

        // Intercept banks and common keyword financial notifications
        val isFinancial = packageName.contains("bank", ignoreCase = true) ||
                packageName.contains("nubank", ignoreCase = true) ||
                packageName.contains("itau", ignoreCase = true) ||
                packageName.contains("bradesco", ignoreCase = true) ||
                packageName.contains("santander", ignoreCase = true) ||
                packageName.contains("pagseguro", ignoreCase = true) ||
                packageName.contains("wallet", ignoreCase = true) ||
                text.contains("R$", ignoreCase = true) ||
                text.contains("compra", ignoreCase = true) ||
                text.contains("pago", ignoreCase = true) ||
                text.contains("pagamento", ignoreCase = true) ||
                text.contains("recebeu", ignoreCase = true) ||
                text.contains("transferência", ignoreCase = true)

        if (isFinancial) {
            scope.launch {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val parseResult = GeminiApiClient.parseBankNotification(
                        "App Package: $packageName\nNotification Title: $title\nNotification Text: $text"
                    )
                    if (parseResult != null && parseResult.title != null && parseResult.amount != null) {
                        val transaction = Transaction(
                            title = parseResult.title,
                            amount = parseResult.amount,
                            category = parseResult.category ?: "Outros",
                            isExpense = parseResult.isExpense ?: true,
                            date = System.currentTimeMillis()
                        )
                        db.transactionDao().insertTransaction(transaction)
                        Log.d(TAG, "Successfully structured and saved transaction: $transaction")

                        // Broadcaster message to trigger UI updates or alerts
                        val intent = Intent("com.example.TRANSACTION_ADDED")
                        sendBroadcast(intent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse notification via Gemini AI", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
