package de.wiomoc.tv

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import de.wiomoc.tv.service.Channel
import de.wiomoc.tv.service.ChannelListService
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ChannelListService.getChannels().enqueue(object : Callback<List<Channel>> {
            override fun onFailure(call: Call<List<Channel>>, t: Throwable) {}

            override fun onResponse(call: Call<List<Channel>>, response: Response<List<Channel>>) {
                val list = response.body()!!
                val names = list.map { it.name }
                channel_list.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, names)
                channel_list.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
                    val url = list[i].proxyUrl
                    startActivity(Intent(this@MainActivity, PlayerActivity::class.java).apply {
                        putExtra("url", url)
                    })
                }
            }
        })

    }
}
