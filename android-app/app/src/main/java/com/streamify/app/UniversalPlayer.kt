package com.streamify.app

import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun UniversalPlayerScreen(streamUrl: String, streamType: String) {
    val context = LocalContext.current
    
    val isEmbed = streamType == "embed" || 
                  streamUrl.contains("vidsrc") || 
                  streamUrl.contains("multiembed") || 
                  streamUrl.contains("embed") ||
                  (!streamUrl.endsWith(".mp4") && !streamUrl.endsWith(".m3u8"))

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isEmbed) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = WebViewClient()
                        loadUrl(streamUrl)
                    }
                },
                update = { webView ->
                    webView.loadUrl(streamUrl)
                }
            )
        } else {
            val exoPlayer = remember(streamUrl) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
                    prepare()
                    playWhenReady = true
                }
            }

            DisposableEffect(streamUrl) {
                onDispose {
                    exoPlayer.release()
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                }
            )
        }
    }
}
