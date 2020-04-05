package de.wiomoc.tv

import android.app.ProgressDialog
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.extractor.ts.TsExtractor
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_player.*


class PlayerActivity : AppCompatActivity() {

    class EPGViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val eventTimeTextView: TextView = view.findViewById(R.id.epg_event_time)
        private val eventNameTextView: TextView = view.findViewById(R.id.epg_event_name)

        fun applyEvent(event: EPGEvent) {
            eventTimeTextView.text = event.time.toString()
            eventNameTextView.text = event.name
        }
    }

    inner class EPGRecyclerViewAdapter : RecyclerView.Adapter<EPGViewHolder>(), EITReader.EPGEventListener {
        private val events = mutableListOf<EPGEvent>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            EPGViewHolder(LayoutInflater.from(this@PlayerActivity).inflate(R.layout.item_epg_event, parent, false))

        override fun getItemCount() = events.size

        override fun onBindViewHolder(holder: EPGViewHolder, position: Int) = holder.applyEvent(events[position])

        override fun onNewEvent(event: EPGEvent) {
            runOnUiThread {
                if (!event.isInPast) {
                    // Binary Search
                    var left = 0
                    var right = events.size
                    while (left < right) {
                        val middle = (right + left) / 2
                        if (events[middle].time < event.time) {
                            left = middle + 1
                        } else {
                            right = middle
                        }
                    }
                    events.add(left, event)
                    notifyItemInserted(left)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra("name")!!
        val url = intent.getStringExtra("url")!!
        setContentView(R.layout.activity_player)
        player_progress.visibility = View.VISIBLE

        supportActionBar!!.apply {
            title = name
            setDisplayHomeAsUpEnabled(true)
        }

        val rendererFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        val player = SimpleExoPlayer.Builder(this, rendererFactory).build();
        val dataSourceFactory = DefaultDataSourceFactory(
            this,
            Util.getUserAgent(this, "selfnet.tv,android")
        );

        enableFullscreenOnLandscape()

        val epgAdapter = EPGRecyclerViewAdapter()
        epg_list.adapter = epgAdapter

        val videoSource =
            ProgressiveMediaSource.Factory(dataSourceFactory, ExtractorsFactory {
                arrayOf(TsExtractor().withEPGListener(epgAdapter))
            })
                .createMediaSource(Uri.parse(url));

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MOVIE)
            .build();
        player.audioAttributes = audioAttributes
        player.playWhenReady = true
        player.volume = 1.0f
        player.prepare(videoSource);

        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    player_progress.visibility = View.GONE
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                Snackbar.make(findViewById(android.R.id.content), error.message!!, Snackbar.LENGTH_LONG).show()
            }
        })

        main_player.player = player
        main_player.keepScreenOn = true
        // val debugView = TextView(this)
        // main_player.overlayFrameLayout!!.addView(debugView)
        // DebugTextViewHelper(player, debugView).start()
    }

    override fun onStop() {
        super.onStop()
        main_player.player?.stop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_player, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.enter_fullscreen -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            android.R.id.home -> finish()
            else -> return false
        }
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableFullscreenOnLandscape(newConfig)
    }

    private fun enableFullscreenOnLandscape(configuration: Configuration = resources.configuration) {
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            epg_list.visibility = View.GONE
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            supportActionBar!!.hide()
        } else {
            epg_list.visibility = View.VISIBLE
            window.decorView.systemUiVisibility = 0
            supportActionBar!!.show()
        }
    }
}
