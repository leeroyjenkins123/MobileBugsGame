package com.example.mobilebugsgame

import retrofit2.http.GET
import retrofit2.http.Query

interface CbrApiService {

    @GET("scripts/xml_metall.asp")
    suspend fun getMetallRates(
        @Query("date_req1") dateFrom: String,
        @Query("date_req2") dateTo: String
    ): MetallResponse
}