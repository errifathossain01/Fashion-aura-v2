package com.example.data.remote

import com.example.data.model.SteadfastBalanceResponse
import com.example.data.model.SteadfastCreateOrderRequest
import com.example.data.model.SteadfastCreateOrderResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface SteadfastApiService {

    @GET("get_balance")
    suspend fun getBalance(
        @Header("Api-Key") apiKey: String,
        @Header("Secret-Key") secretKey: String,
        @Header("Accept") accept: String = "application/json",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    ): SteadfastBalanceResponse

    @POST("create_order")
    suspend fun createOrder(
        @Header("Api-Key") apiKey: String,
        @Header("Secret-Key") secretKey: String,
        @Header("Accept") accept: String = "application/json",
        @Header("Content-Type") contentType: String = "application/json",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        @Body request: SteadfastCreateOrderRequest
    ): SteadfastCreateOrderResponse

    companion object {
        private const val BASE_URL = "https://portal.packzy.com/api/v1/"

        fun create(): SteadfastApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(SteadfastApiService::class.java)
        }
    }
}
