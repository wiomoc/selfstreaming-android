package de.selfnet.tv

import androidx.lifecycle.*
import de.selfnet.tv.service.Channel
import de.selfnet.tv.service.ChannelListService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.RuntimeException

class ChannelListViewModel : ViewModel() {
    val search = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()
    private val allChannelsResponse: MutableLiveData<Result<List<Channel>>?> = MutableLiveData()

    init {
        loadChannels()
    }

    fun loadChannels(showLoading: Boolean = true) {
        isLoading.value = showLoading
        ChannelListService.getChannels().enqueue(object : Callback<List<Channel>> {
            override fun onFailure(call: Call<List<Channel>>, t: Throwable) {
                allChannelsResponse.value = Result.failure(t)
                isLoading.value = false
            }

            override fun onResponse(call: Call<List<Channel>>, response: Response<List<Channel>>) {
                allChannelsResponse.value = if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(RuntimeException(response.message()))
                }
                isLoading.value = false
            }
        })
    }

    val filteredChannelsResponse by lazy {
        merge(allChannelsResponse, search) { rawResponse, search ->
            if (rawResponse == null) return@merge null
            rawResponse.map { allChannels ->
                if (search.isNullOrEmpty()) {
                    allChannels
                } else {
                    allChannels.filter {
                        it.name.contains(search, ignoreCase = true)
                    }
                }
            }
        }
    }
}

fun <A, B, C> merge(liveData1: LiveData<A>, liveData2: LiveData<B>, transformer: (A?, B?) -> C): LiveData<C> =
    MediatorLiveData<C>().apply {
        val onChange = Observer<Any?> {
            value = transformer(liveData1.value, liveData2.value)
        }
        addSource(liveData1, onChange)
        addSource(liveData2, onChange)
    }


