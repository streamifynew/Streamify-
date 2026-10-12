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
    
    // Check if the link is a web embed or direct video stream (.mp4 / .m3u8)
    val isEmbed = streamType == "embed" || 
                  streamUrl.contains("vidsrc") || 
                  streamUrl.contains("multiembed") || 
                  streamUrl.contains("embed") ||
                  !streamUrl.endsWith(".mp4") && !streamUrl.endsWith(".m3u8")

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isEmbed) {
            // WebView for HTML / Embed / Vidsrc / EmbedSu links
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
                    webView.load
