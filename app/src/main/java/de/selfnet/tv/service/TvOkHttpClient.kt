package de.selfnet.tv.service

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

val tvOkHttpClient by lazy {
    OkHttpClient.Builder()
        .readTimeout(1500, TimeUnit.MILLISECONDS)
        .build()
}

