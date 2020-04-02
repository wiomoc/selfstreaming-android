package de.wiomoc.tv.service

import android.net.Uri
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class Channel(val name: String, @SerializedName("link-proxy") val proxyUrl: String)

interface IChannelListService {
    @GET("channels.json")
    fun getChannels(): Call<List<Channel>>
}

object ChannelListService : IChannelListService by
Retrofit.Builder()
    .baseUrl("http://selfnet.tv/sap/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(IChannelListService::class.java);

