package de.wiomoc.tv

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.extractor.ts.TsExtractor
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_player.*


class PlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("url")!!

        setContentView(R.layout.activity_player)

        val rendererFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        val player = SimpleExoPlayer.Builder(this, rendererFactory).build();
        val dataSourceFactory = DefaultDataSourceFactory(
            this,
            Util.getUserAgent(this, "selfnet.tv,android")
        );

        enableFullscreenOnLandscape()


        val events = mutableListOf<EPGEvent>()
        val adapter = object : BaseAdapter() {
            override fun getView(position: Int, oldView: View?, viewGroup: ViewGroup): View {
                val view = oldView ?: View.inflate(this@PlayerActivity, R.layout.item_epg_event, null)
                view.findViewById<TextView>(R.id.epg_event_name).text = events[position].name
                view.findViewById<TextView>(R.id.epg_event_time).text = events[position].time.toString()
                return view
            }

            override fun getItem(position: Int) = events[position]

            override fun getItemId(position: Int) = events[position].id.toLong()

            override fun getCount() = events.size
        }
        epg_list.adapter = adapter

        val epgListener = { event: EPGEvent ->
            runOnUiThread {
                events.add(event)
                events.sortBy { it.time }
                adapter.notifyDataSetChanged()
            }
        }

        val videoSource =
            ProgressiveMediaSource.Factory(dataSourceFactory, ExtractorsFactory {
                arrayOf(TsExtractor().withEPGListener(epgListener))
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
        } else {
            epg_list.visibility = View.VISIBLE
            window.decorView.systemUiVisibility = 0
        }
    }

}
