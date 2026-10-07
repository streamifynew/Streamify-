package com.streamify.app

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var exoPlayer: SimpleExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.exoPlayerView)
        webView = findViewById(R.id.embedWebView)

        // Backend se aane wala URL aur type receive karein
        val streamUrl = intent.getStringExtra("STREAM_URL") ?: ""
        val streamType = intent.getStringExtra("STREAM_TYPE") ?: "embed" // "hls", "mp4", ya "embed"

        if (streamType == "embed" || streamUrl.contains("vidsrc") || streamUrl.contains("multiembed")) {
            // Agar web embed link hai, toh WebView chalega
            playerView.visibility = View.GONE
            webView.visibility = View.VISIBLE
            setupWebView(streamUrl)
        } else {
            // Agar direct HLS (.m3u8) ya MP4 hai, toh ExoPlayer chalega
            webView.visibility = View.GONE
            playerView.visibility = View.VISIBLE
            setupExoPlayer(streamUrl)
        }
    }

    private fun setupWebView(url: String) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = false
        }
        webView.webViewClient = WebViewClient()
        webView.loadUrl(url)
    }

    private fun setupExoPlayer(url: String) {
        exoPlayer = SimpleExoPlayer.Builder(this).build().apply {
            playerView.player = this
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
            playWhenReady = true
        }
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
