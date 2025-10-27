package com.example.mobilebugsgame

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class GoldRateWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_gold_rate)

        // Загружаем актуальный курс золота
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val repository = CbrRepository()
                val goldRate = repository.getCurrentGoldRate()

                // Форматируем цену
                val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
                formatter.maximumFractionDigits = 2
                val formattedRate = formatter.format(goldRate)

                // Обновляем виджет в главном потоке
                GlobalScope.launch(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_gold_rate_text, "$formattedRate ₽/г")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                // В случае ошибки показываем заглушку
                GlobalScope.launch(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_gold_rate_text, "Ошибка")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        // Intent для обновления по клику
        val intent = Intent(context, GoldRateWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}