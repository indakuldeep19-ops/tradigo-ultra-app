package com.tradigo.ultra.di

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

object NetworkModule {
    private const val BASE_URL = "https://api.tradigoultra.com/"
    private const val HOST_NAME = "api.tradigoultra.com"
    private const val ENABLE_LOGGING = false

    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        
        if (ENABLE_LOGGING) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }
        
        return builder.build()
    }

    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}
