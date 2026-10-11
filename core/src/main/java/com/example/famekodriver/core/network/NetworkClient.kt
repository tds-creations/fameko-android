package com.example.famekodriver.core.network

import com.example.famekodriver.core.BuildConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Singleton client for Retrofit services (Production Railway Cluster)
 */
object NetworkClient {
    // In-memory cookie storage for sessions
    private val cookieStore = mutableMapOf<String, List<Cookie>>()

    private val sessionCookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: listOf()
        }
    }

    private const val PRODUCTION_URL = "https://fameko-android-production.up.railway.app/"
    private const val PRODUCTION_ROUTING_URL = "https://go-routing-production.up.railway.app/"

    private val BASE_URL = PRODUCTION_URL
    private val ROUTING_URL = PRODUCTION_ROUTING_URL

    private val resilientDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                Dns.SYSTEM.lookup(hostname)
            } catch (e: UnknownHostException) {
                try {
                    val addresses = InetAddress.getAllByName(hostname)
                    if (addresses.isNotEmpty()) addresses.toList() else throw e
                } catch (_: Exception) {
                    throw e
                }
            }
        }
    }

    val okHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(resilientDns)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .cookieJar(sessionCookieJar)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val routingHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(resilientDns)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val osmHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(resilientDns)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val routingRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ROUTING_URL)
            .client(routingHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // TomTom API Key - Accessed via Secrets Gradle Plugin
    val TOMTOM_API_KEY = BuildConfig.TOMTOM_API_KEY

    val famekoApi: FamekoApiService by lazy {
        retrofit.create(FamekoApiService::class.java)
    }

    val routingApi: FamekoApiService by lazy {
        routingRetrofit.create(FamekoApiService::class.java)
    }

    val osmService: OpenStreetMapService by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(osmHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenStreetMapService::class.java)
    }

    val tomTomService: TomTomApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.tomtom.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TomTomApiService::class.java)
    }

    fun getWebSocketUrl(userId: String): String {
        val base = BASE_URL.removeSuffix("/")
        return base.replace("http", "ws") + "/ws/$userId"
    }

    // Backward compatibility alias
    val geocodingService: FamekoApiService by lazy { famekoApi }
}
