package com.streamify.app

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
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
import kotlinx.coroutines.delay

@Composable
fun UniversalPlayerScreen(streamUrl: String, streamType: String) {
    val context = LocalContext.current
    var extractedVideoUrl by remember { mutableStateOf<String?>(null) }
    
    // Agar URL pehle se hi direct .mp4 ya .m3u8 hai
    val isDirectFile = streamUrl.endsWith(".mp4") || streamUrl.endsWith(".m3u8")

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isDirectFile || extractedVideoUrl != null) {
            // ==========================================
            // EXOPLAYER: Jab direct video link mil jaye
            // ==========================================
            val playUrl = extractedVideoUrl ?: streamUrl
            val exoPlayer = remember(playUrl) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(Uri.parse(playUrl)))
                    prepare()
                    playWhenReady = true
                }
            }

            DisposableEffect(playUrl) {
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
        } else {
            // ==========================================
            // WEBVIEW SNIFFER: Embed page load karke link nikalne ke liye
            // ==========================================
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }
                        
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // JavaScript inject karke video tag ka src dhoondna
                                view?.evaluateJavascript(
                                    "(function() { " +
                                    "  var v = document.querySelector('video'); " +
                                    "  return v ? v.src : ''; " +
                                    "})();"
                                ) { result ->
                                    if (!result.isNullOrEmpty() && result != "null" && result.length > 2) {
                                        val cleanUrl = result.removeSurrounding("\"")
                                        if (cleanUrl.startsWith("http")) {
                                            extractedVideoUrl = cleanUrl
                                        }
                                    }
                                }
                            }
                        }
                        loadUrl(streamUrl)
                    }
                },
                update = { webView ->
                    // Agar URL change ho toh dobara load karein
                    if (webView.url != streamUrl) {
                        webView.loadUrl(streamUrl)
                    }
                }
            )
        }
    }
}
