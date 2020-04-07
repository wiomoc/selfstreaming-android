package de.wiomoc.tv

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import de.wiomoc.tv.service.Channel
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var viewModel: ChannelListViewModel
    lateinit var searchView: SearchView

    class ChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val channelNameTextView: TextView = view.findViewById(R.id.channel_name)
        private val channelCurrentEventTextView: TextView = view.findViewById(R.id.channel_current_event)
        private var channel: Channel? = null

        init {
            view.setOnClickListener {
                val context = it.context
                context.startActivity(Intent(context, PlayerActivity::class.java).apply {
                    putExtra("name", channel!!.name)
                    putExtra("url", channel!!.proxyUrl)
                })
            }
        }

        fun applyChannel(channel: Channel) {
            channelNameTextView.text = channel.name
            channelCurrentEventTextView.text =
                if (channel.currentEventName.isNullOrEmpty()) channel.currentEventDescription
                else channel.currentEventName
            this.channel = channel
        }
    }

    inner class ChannelsAdapter : RecyclerView.Adapter<ChannelViewHolder>() {
        private var channels = emptyList<Channel>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ChannelViewHolder(
                LayoutInflater.from(this@MainActivity)
                    .inflate(R.layout.item_channel, parent, false)
            )

        override fun getItemCount() = channels.size

        override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) =
            holder.applyChannel(channels[position])

        fun onChanged(newChannels: List<Channel>) {
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    newChannels[newItemPosition].proxyUrl == channels[oldItemPosition].proxyUrl

                override fun getOldListSize() = channels.size
                override fun getNewListSize() = newChannels.size
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    newChannels[newItemPosition].proxyUrl == channels[oldItemPosition].proxyUrl
            })
            channels = newChannels
            result.dispatchUpdatesTo(this)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))
            .get(ChannelListViewModel::class.java)

        val channelsAdapter = ChannelsAdapter()

        var snackbar: Snackbar? = null
        viewModel.filteredChannels.observe(this, Observer {
            if (it == null) return@Observer
            when {
                it.isSuccess -> {
                    channelsAdapter.onChanged(it.getOrThrow())
                    snackbar?.dismiss()
                }
                else -> {
                    snackbar = Snackbar.make(
                        findViewById(android.R.id.content),
                        "Error",
                        Snackbar.LENGTH_INDEFINITE
                    ).apply {
                        setAction("Retry") {
                            viewModel.loadChannels()
                        }
                        show()
                    }
                }
            }
        })
        viewModel.isLoading.observe(this, Observer {
            main_progress.visibility = if (it) View.VISIBLE else View.GONE
            if (!it) channel_refresh.isRefreshing = false
        })
        channel_list.adapter = channelsAdapter

        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.recycler_view_divider)!!)
        channel_list.addItemDecoration(divider)

        channel_refresh.setOnRefreshListener {
            viewModel.loadChannels(false)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        searchView = menu.findItem(R.id.channel_search).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(filter: String) = true

            override fun onQueryTextChange(filter: String?): Boolean {
                viewModel.search.value = filter
                return true
            }
        })

        return true
    }

    override fun onBackPressed() {
        if (!searchView.isIconified) {
            searchView.clearFocus()
            searchView.setQuery("", false)
            searchView.isIconified = true
            viewModel.search.value = null
        } else {
            super.onBackPressed()
        }
    }
}
