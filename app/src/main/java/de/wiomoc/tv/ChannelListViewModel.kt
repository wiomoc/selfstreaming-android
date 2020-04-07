package de.wiomoc.tv

import androidx.lifecycle.*
import de.wiomoc.tv.service.Channel
import de.wiomoc.tv.service.ChannelListService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChannelListViewModel : ViewModel() {
    val search = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()
    private val rawResponse: MutableLiveData<Result<List<Channel>>?> = MutableLiveData()

    init {
        loadChannels()
    }

    fun loadChannels(showLoading: Boolean = true) {
        isLoading.value = showLoading
        ChannelListService.getChannels().enqueue(object : Callback<List<Channel>> {
            override fun onFailure(call: Call<List<Channel>>, t: Throwable) {
                rawResponse.value = Result.failure(t)
                isLoading.value = false
            }

            override fun onResponse(call: Call<List<Channel>>, response: Response<List<Channel>>) {
                rawResponse.value = Result.success(response.body()!!)
                isLoading.value = false
            }
        })
    }

    val filteredChannels by lazy {
        merge(rawResponse, search) { rawResponse, search ->
            val tmp = rawResponse // XXX
            if (tmp == null) return@merge null
            tmp.map { allChannels ->
                if (search != null) {
                    allChannels.filter {
                        val needle = search.toLowerCase()
                        it.name.toLowerCase().indexOf(needle) != -1
                    }
                } else {
                    allChannels
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

