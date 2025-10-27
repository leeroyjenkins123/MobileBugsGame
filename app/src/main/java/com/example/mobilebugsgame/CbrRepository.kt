package com.example.mobilebugsgame

import retrofit2.Retrofit
import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class CbrRepository {
    private val apiService: CbrApiService
    private val TAG = "CbrRepository"

    init {
        val tikXml = TikXml.Builder()
            .exceptionOnUnreadXml(false)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.cbr.ru/")
            .addConverterFactory(TikXmlConverterFactory.create(tikXml))
            .build()

        apiService = retrofit.create(CbrApiService::class.java)
    }
    suspend fun getCurrentGoldRate(): Double {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val calendar = Calendar.getInstance()

            calendar.add(Calendar.DAY_OF_MONTH, -2)
            val dateFrom = dateFormat.format(calendar.time)

            calendar.add(Calendar.DAY_OF_MONTH, 2) // Текущая дата
            val dateTo = dateFormat.format(calendar.time)

            Log.d(TAG, "Requesting gold rates from $dateFrom to $dateTo")

            val response = apiService.getMetallRates(dateFrom, dateTo)
            Log.d(TAG, "Response received: ${response.records?.size ?: 0} records")

            if (response.records == null || response.records.isEmpty()) {
                Log.w(TAG, "No records in response for dates $dateFrom - $dateTo")
                return getFallbackGoldRate()
            }

            response.records.forEachIndexed { index, record ->
                Log.d(TAG, "Record $index - Date: ${record.date}, Code: ${record.code}, " +
                        "Buy: ${record.buy?.value}, Sell: ${record.sell?.value}")
            }

            val goldRecords = response.records
                .filter { it.code == "1" && it.sell?.value != null && it.sell.value.isNotBlank() }
                .sortedByDescending { record ->
                    try {
                        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(record.date)?.time ?: 0
                    } catch (e: Exception) {
                        0
                    }
                }

            if (goldRecords.isEmpty()) {
                Log.w(TAG, "No gold records found (code=1)")
                return getFallbackGoldRate()
            }

            val latestGoldRecord = goldRecords.first()
            Log.d(TAG, "Using latest gold record: ${latestGoldRecord.date}")

            val rateString = latestGoldRecord.sell?.value?.replace(",", ".")
            val rate = rateString?.toDoubleOrNull()

            if (rate == null) {
                Log.e(TAG, "Failed to parse rate: $rateString")
                return getFallbackGoldRate()
            }

            Log.i(TAG, "Successfully got current gold rate: $rate")
            rate

        } catch (e: Exception) {
            Log.e(TAG, "Error in getCurrentGoldRate: ${e.message}", e)
            return getFallbackGoldRate()
        }
    }

    private fun getFallbackGoldRate(): Double {
        return 7500.0
    }
}