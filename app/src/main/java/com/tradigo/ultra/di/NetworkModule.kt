package com.tradigo.ultra.di

import com.tradigo.ultra.BuildConfig
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * NetworkModule — production-grade HTTP client with SSL pinning.
 *
 * SETUP:
 * 1. Replace HOST_NAME and BASE_URL with your real API domain.
 * 2. Replace the placeholder SHA-256 pin with the real certificate hash:
 *    openssl s_client -connect api.tradigoultra.com:443 | \
 *    openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | \
 *    openssl dgst -sha256 -binary | base64
 * 3. Add a backup pin (second .add() call) for certificate rotation safety.
 * 4. Set ENABLE_LOGGING = false before releasing to the Play Store.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.tradigoultra.com/"
    private const val HOST_NAME = "api.tradigoultra.com"
    
    // Set false in release builds — logging must never run in production
    private const val ENABLE_LOGGING = false

    /**
     * Certificate pinner — defends against MITM attacks.
     * Replace the placeholder with your real leaf + backup public-key pins.
     */
    private val certificatePinner: CertificatePinner = CertificatePinner.Builder()
        .add(HOST_NAME, BuildConfig.SSL_LEAF_CERT_PIN.also {
            require(it.startsWith("sha256/") && it.length > 10) {
                "SSL_LEAF_CERT_PIN not configured. Set ssl.leaf.cert.pin in local.properties. " +
                "Run: openssl s_client -connect api.tradigoultra.com:443 </dev/null | " +
                "openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | " +
                "openssl dgst -sha256 -binary | base64"
            }
        })
        .add(HOST_NAME, BuildConfig.SSL_BACKUP_CERT_PIN.also {
            require(it.startsWith("sha256/") && it.length > 10) {
                "SSL_BACKUP_CERT_PIN not configured. Set ssl.backup.cert.pin in local.properties."
            }
        })
        .build()

    /**
     * Provides a secured OkHttpClient.
     * Call once and reuse — OkHttpClient is thread-safe and expensive to create.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        
        if (ENABLE_LOGGING) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }
        
        return builder.build()
    }

    /**
     * Provides a Retrofit instance backed by the secured OkHttpClient.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}
