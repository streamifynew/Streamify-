package com.streamify.app

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val TMDB_IMAGE = "https://image.tmdb.org/t/p/"

private val Black = Color(0xFF050505)
private val Surface = Color(0xFF111111)
private val Surface2 = Color(0xFF1A1A1A)
private val Red = Color(0xFFFF1F1F)
private val DeepRed = Color(0xFF8A0A0A)
private val Gold = Color(0xFFFFD166)
private val White = Color(0xFFF8F8F8)
private val Grey = Color(0xFF9A9A9A)
private val RedBrush = Brush.linearGradient(listOf(Red, DeepRed))
private val GlassBrush = Brush.linearGradient(listOf(Color.White.copy(alpha = .18f), Color.White.copy(alpha = .05f)))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StreamifyApp() }
    }
}

private enum class Screen { HOME, SEARCH, DETAIL, MY_LIST, DOWNLOADS, SETTINGS, SEE_ALL, PROFILE, PLAYBACK, ABOUT, PLAYER }

private data class PlayReq(val item: TmdbItem)

private data class SeeAllReq(val title: String, val spec: BrowseSpec)

private enum class HomeCategory(val label: String, val icon: ImageVector) {
    TRENDING("Trending", Icons.Outlined.LocalFireDepartment),
    MOVIES("Movies", Icons.Outlined.Movie),
    TV("TV", Icons.Outlined.Tv),
    DRAMA("Drama", Icons.Outlined.TheaterComedy),
    ANIME("Anime", Icons.Outlined.Face)
}

private enum class SubCategory(val label: String) {
    ALL("All"), HOLLYWOOD("Hollywood"), BOLLYWOOD("Bollywood"), SOUTH("South Cinema"),
    MULTI_AUDIO("Multi-Audio"), HINDI_DUBBED("Hindi Dubbed"), WEB_SERIES("Web Series"),
    BOLLYWOOD_SERIES("Bollywood Series"), TV_SHOWS("TV Shows"),
    ANIMATED("Animated Movies/Shows"), ANIME("Anime"), CARTOON("Cartoon Shows"),
    KDRAMA("K-Drama"), TURKISH("Turkish Drama"), PAKISTANI("Pakistani Drama")
}

private fun subcategories(c: HomeCategory): List<SubCategory> = when (c) {
    HomeCategory.TRENDING -> listOf(SubCategory.ALL)
    HomeCategory.MOVIES -> listOf(
        SubCategory.ALL, SubCategory.HOLLYWOOD, SubCategory.BOLLYWOOD,
        SubCategory.SOUTH, SubCategory.MULTI_AUDIO, SubCategory.HINDI_DUBBED
    )
    HomeCategory.TV -> listOf(
        SubCategory.ALL, SubCategory.WEB_SERIES, SubCategory.BOLLYWOOD_SERIES, SubCategory.TV_SHOWS
    )
    HomeCategory.ANIME -> listOf(
        SubCategory.ALL, SubCategory.ANIMATED, SubCategory.ANIME, SubCategory.CARTOON
    )
    HomeCategory.DRAMA -> listOf(
        SubCategory.ALL, SubCategory.KDRAMA, SubCategory.TURKISH, SubCategory.PAKISTANI
    )
}

private const val WEB_NETWORKS = "213|1024|2739|2552|453|49|3186"

// what each category / sub-category loads (null = nothing to load yet)
private fun baseSpec(c: HomeCategory, s: SubCategory): BrowseSpec? = when (c) {
    HomeCategory.TRENDING -> null
    HomeCategory.MOVIES -> when (s) {
        SubCategory.HOLLYWOOD -> BrowseSpec(movie = true, languages = listOf("en"))
        SubCategory.BOLLYWOOD -> BrowseSpec(movie = true, languages = listOf("hi"))
        SubCategory.SOUTH -> BrowseSpec(movie = true, languages = listOf("ta", "te", "ml", "kn"))
        SubCategory.MULTI_AUDIO, SubCategory.HINDI_DUBBED -> null
        else -> BrowseSpec(movie = true)
    }
    HomeCategory.TV -> when (s) {
        SubCategory.WEB_SERIES -> BrowseSpec(tv = true, networks = WEB_NETWORKS, withoutGenres = "10766")
        SubCategory.BOLLYWOOD_SERIES -> BrowseSpec(tv = true, languages = listOf("hi"), networks = "213|1024", withoutGenres = "10766")
        SubCategory.TV_SHOWS -> BrowseSpec(tv = true, languages = listOf("en"))
        else -> BrowseSpec(tv = true)
    }
    HomeCategory.DRAMA -> when (s) {
        SubCategory.KDRAMA -> BrowseSpec(tv = true, languages = listOf("ko"), tvGenre = "18")
        SubCategory.TURKISH -> BrowseSpec(tv = true, languages = listOf("tr"), tvGenre = "18")
        SubCategory.PAKISTANI -> BrowseSpec(tv = true, languages = listOf("ur"), tvGenre = "18")
        else -> BrowseSpec(movie = true, tv = true, movieGenre = "18", tvGenre = "18")
    }
    HomeCategory.ANIME -> when (s) {
        SubCategory.ANIMATED -> BrowseSpec(movie = true, tv = true, movieGenre = "16", tvGenre = "16")
        SubCategory.CARTOON -> BrowseSpec(tv = true, languages = listOf("en"), tvGenre = "16")
        else -> BrowseSpec(movie = true, tv = true, languages = listOf("ja"), movieGenre = "16", tvGenre = "16")
    }
}

private data class GenreDef(val label: String, val movie: String?, val tv: String?)

private val GENRES = listOf(
    GenreDef("Action", "28", "10759"),
    GenreDef("Sci-Fi", "878", "10765"),
    GenreDef("Comedy", "35", "35"),
    GenreDef("Crime", "80", "80"),
    GenreDef("Thriller & Mystery", "53", "9648"),
    GenreDef("Romance", "10749", null),
    GenreDef("Horror", "27", null),
    GenreDef("Drama", "18", "18"),
    GenreDef("Reality", null, "10764"),
    GenreDef("Animation", "16", "16"),
    GenreDef("Family", "10751", "10751")
)

// the same base list, narrowed to one genre (null = this genre doesn't fit here)
private fun genreSpec(base: BrowseSpec, def: GenreDef, sub: SubCategory): BrowseSpec? {
    val useMovie = base.movie && def.movie != null && def.movie != base.movieGenre
    val useTv = base.tv && def.tv != null && def.tv != base.tvGenre
    if (!useMovie && !useTv) return null
    var spec = base.copy(
        movie = useMovie,
        tv = useTv,
        movieGenre = if (useMovie) listOfNotNull(base.movieGenre, def.movie).joinToString(",") else null,
        tvGenre = if (useTv) listOfNotNull(base.tvGenre, def.tv).joinToString(",") else null
    )
    // Bollywood Series: reality shows come from Hindi TV channels too
    if (def.label == "Reality" && sub == SubCategory.BOLLYWOOD_SERIES) {
        spec = spec.copy(networks = null, withoutGenres = null)
    }
    return spec
}

// ============================================================
// IN-APP UPDATE: checks GitHub Releases, downloads the new APK, opens the installer
// ============================================================

private const val UPDATE_URL = "https://api.github.com/repos/streamifynew/Streamify-/releases/latest"

private data class UpdateInfo(val code: Int, val url: String)

private suspend fun fetchLatestUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
    try {
        val conn = URL(UPDATE_URL).openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "Streamify-App")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        val code = json.getString("tag_name").substringAfterLast("-").toIntOrNull()
        val assets = json.getJSONArray("assets")
        var url: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.getString("name").endsWith(".apk")) url = a.getString("browser_download_url")
        }
        if (code != null && url != null) UpdateInfo(code, url) else null
    } catch (_: Exception) {
        null
    }
}

private suspend fun downloadApk(context: Context, url: String, onProgress: (Float) -> Unit): File =
    withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, "streamify.apk")
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.connect()
        val total = conn.contentLength.toLong()
        conn.inputStream.use { input ->
            file.outputStream().use { out ->
                val buf = ByteArray(16 * 1024)
                var done = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n == -1) break
                    out.write(buf, 0, n)
                    done += n
                    if (total > 0) onProgress(done.toFloat() / total)
                }
            }
        }
        file
    }

// returns false when Android first needs the "install unknown apps" permission
private fun installApk(context: Context, file: File): Boolean {
    if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return false
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    return true
}

private fun installedVersionCode(context: Context): Int =
    try {
        PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(context.packageName, 0)
        ).toInt()
    } catch (_: Exception) { Int.MAX_VALUE }

private fun appVersionName(context: Context): String =
    try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" }
    catch (_: Exception) { "" }

@Composable
private fun UpdateDialog(info: UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<Float?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val p = progress

    AlertDialog(
        onDismissRequest = { if (p == null) onDismiss() },
        containerColor = Color(0xFF1A0B0E),
        shape = RoundedCornerShape(24.dp),
        title = { Text("New version available", color = White, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                Text(
                    message ?: "A newer Streamify is ready. The update takes only a few seconds.",
                    color = Grey, fontSize = 13.sp
                )
                if (p != null) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { p },
                        color = Red,
                        trackColor = White.copy(alpha = .12f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = p == null,
                onClick = {
                    scope.launch {
                        message = null
                        progress = 0f
                        try {
                            val file = downloadApk(context, info.url) { progress = it }
                            progress = null
                            if (installApk(context, file)) onDismiss()
                            else message = "Allow Install unknown apps for Streamify, then tap Update now again."
                        } catch (_: Exception) {
                            progress = null
                            message = "Download failed. Check your internet and try again."
                        }
                    }
                }
            ) { Text("Update now", color = Red, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(enabled = p == null, onClick = onDismiss) { Text("Later", color = Grey) }
        }
    )
}

@Composable
private fun UpdatePrompt() {
    val context = LocalContext.current
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val latest = fetchLatestUpdate()
        if (latest != null && latest.code > installedVersionCode(context)) update = latest
    }

    val info = update
    if (info != null && !dismissed) UpdateDialog(info) { dismissed = true }
}

// ============================================================
// PLAYER
// Asks fetchStreamingLinksForAnyMedia (TmdbRepository.kt) for the sources of a title,
// reads the link out of each source automatically, and plays it:
//   direct video links (.m3u8 / .mp4 ...) -> built-in player (tries the next source if one fails)
//   web page links                         -> locked-down web player
// ============================================================

private data class Playable(val label: String, val url: String, val headers: Map<String, String>)

// reads link / name / headers out of a source object, whatever its field names are
private fun toPlayable(src: Any): Playable? {
    var url: String? = null
    val labels = mutableListOf<String>()
    var headers: Map<String, String> = emptyMap()
    for (f in src.javaClass.declaredFields) {
        if (java.lang.reflect.Modifier.isStatic(f.modifiers)) continue
        try {
            f.isAccessible = true
            val v = f.get(src) ?: continue
            val text: String? = when (v) {
                is String -> v
                is Uri, is java.net.URL -> v.toString()
                else -> null
            }
            when {
                text != null && text.startsWith("http", ignoreCase = true) -> if (url == null) url = text
                text != null -> if (text.isNotBlank() && text.length <= 32) labels += text
                v is Map<*, *> -> {
                    val m = v.entries
                        .filter { it.key is String && it.value is String }
                        .associate { it.key as String to it.value as String }
                    if (m.isNotEmpty()) headers = m
                }
            }
        } catch (_: Exception) { }
    }
    val u = url ?: return null
    return Playable(labels.take(2).joinToString("  •  ").ifEmpty { "Source" }, u, headers)
}

private fun isDirectVideo(url: String): Boolean {
    val lower = url.lowercase()
    if (lower.contains(".m3u8")) return true
    val path = lower.substringBefore("?").substringBefore("#")
    return listOf(".mp4", ".mkv", ".webm", ".mov").any { path.endsWith(it) }
}

@Composable
private fun PlayerScreen(req: PlayReq, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val repo = remember { TmdbRepository() }
    var loading by remember(req) { mutableStateOf(true) }
    var error by remember(req) { mutableStateOf<String?>(null) }
    var playables by remember(req) { mutableStateOf<List<Playable>>(emptyList()) }
    var index by remember(req) { mutableIntStateOf(0) }
    var picker by remember { mutableStateOf(false) }
    val title = req.item.title ?: req.item.name ?: ""

    LaunchedEffect(req) {
        loading = true
        error = null
        try {
            val raw: List<Any> = with(repo) { fetchStreamingLinksForAnyMedia(req.item) }
            playables = raw.mapNotNull { toPlayable(it) }.distinctBy { it.url }
            if (playables.isEmpty()) error = "No playable source found for this title."
        } catch (_: Exception) {
            error = "Could not load sources. Check your internet and try again."
        }
        loading = false
    }

    // landscape + full screen while watching, back to normal afterwards
    DisposableEffect(Unit) {
        val oldOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.let { w ->
            val c = WindowInsetsControllerCompat(w, w.decorView)
            c.hide(WindowInsetsCompat.Type.systemBars())
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.requestedOrientation = oldOrientation
            activity?.window?.let { w ->
                WindowInsetsControllerCompat(w, w.decorView).show(WindowInsetsCompat.Type.systemBars())
                w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    BackHandler { onBack() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val current = playables.getOrNull(index)
        when {
            loading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Red, strokeWidth = 3.dp)
                Spacer(Modifier.height(10.dp))
                Text("Finding sources...", color = Grey, fontSize = 12.sp)
            }
            error != null || current == null -> Text(
                error ?: "No playable source found for this title.",
                color = White, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center).padding(32.dp)
            )
            isDirectVideo(current.url) -> key(current.url) {
                ExoPlayerView(current) {
                    if (index < playables.lastIndex) index++
                    else error = "This source could not be played. Try again later."
                }
            }
            else -> key(current.url) { WebPlayer(current.url) }
        }

        Row(
            Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.displayCutout).padding(10.dp)
                .clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = .45f))
                .clickable { onBack() }.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.ArrowBack, "Back", tint = White, modifier = Modifier.size(20.dp))
            if (title.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    title, color = White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 260.dp)
                )
            }
        }
        if (playables.size > 1) {
            Box(
                Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.displayCutout).padding(10.dp)
                    .clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = .45f))
                    .clickable { picker = true }.padding(horizontal = 14.dp, vertical = 9.dp)
            ) { Text("Sources", color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        }
    }

    if (picker) {
        AlertDialog(
            onDismissRequest = { picker = false },
            containerColor = Color(0xFF1A0B0E),
            shape = RoundedCornerShape(24.dp),
            title = { Text("Choose a source", color = White, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    playables.forEachIndexed { i, p ->
                        TextButton(onClick = { index = i; error = null; picker = false }) {
                            Text(
                                "${p.label}${if (i == index) "   ✓" else ""}",
                                color = if (i == index) Red else White
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picker = false }) { Text("Close", color = Grey) } }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoPlayerView(src: Playable, onError: () -> Unit) {
    val context = LocalContext.current
    val onErr by rememberUpdatedState(onError)
    val player = remember(src.url) {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(src.headers)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(http))
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) { onErr() }
                })
                setMediaItem(MediaItem.fromUri(src.url))
                prepare()
                playWhenReady = true
            }
    }
    DisposableEffect(player) {
        val owner = context as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player.pause()
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose {
            owner?.lifecycle?.removeObserver(observer)
            player.release()
        }
    }
    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun WebPlayer(url: String) {
    val host = remember(url) { Uri.parse(url).host }
    AndroidView(
        factory = { ctx ->
            val root = FrameLayout(ctx)
            val web = WebView(ctx)
            root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            var custom: View? = null
            var customCallback: WebChromeClient.CustomViewCallback? = null

            web.settings.javaScriptEnabled = true
            web.settings.domStorageEnabled = true
            web.settings.mediaPlaybackRequiresUserGesture = false
            web.settings.setSupportMultipleWindows(false)
            web.settings.javaScriptCanOpenWindowsAutomatically = false
            web.setBackgroundColor(android.graphics.Color.BLACK)

            // stay on the player's own site: redirects/ads to other sites are blocked
            web.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    request.isForMainFrame && request.url.host != host
            }
            web.webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    custom = view
                    customCallback = callback
                    web.visibility = View.GONE
                    root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }

                override fun onHideCustomView() {
                    custom?.let { root.removeView(it) }
                    custom = null
                    customCallback?.onCustomViewHidden()
                    customCallback = null
                    web.visibility = View.VISIBLE
                }
            }
            web.loadUrl(url)
            root
        },
        onRelease = { root -> (root.getChildAt(0) as? WebView)?.destroy() },
        modifier = Modifier.fillMaxSize()
    )
}

// My List / Downloads are kept on the phone, so they survive closing the app
private fun saveItems(prefs: SharedPreferences, key: String, items: List<TmdbItem>) {
    val arr = JSONArray()
    items.forEach {
        arr.put(
            JSONObject().apply {
                put("id", it.id)
                put("type", it.media_type ?: "movie")
                put("title", it.title ?: "")
                put("name", it.name ?: "")
                put("poster", it.poster_path ?: "")
                put("backdrop", it.backdrop_path ?: "")
                put("overview", it.overview ?: "")
                put("vote", it.vote_average ?: 0.0)
            }
        )
    }
    prefs.edit().putString(key, arr.toString()).apply()
}

private fun loadItems(prefs: SharedPreferences, key: String): List<TmdbItem> =
    try {
        val arr = JSONArray(prefs.getString(key, "[]") ?: "[]")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TmdbItem(
                id = o.optInt("id"),
                media_type = o.optString("type").ifEmpty { null },
                title = o.optString("title").ifEmpty { null },
                name = o.optString("name").ifEmpty { null },
                poster_path = o.optString("poster").ifEmpty { null },
                backdrop_path = o.optString("backdrop").ifEmpty { null },
                overview = o.optString("overview").ifEmpty { null },
                vote_average = o.optDouble("vote", 0.0)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

private fun List<TmdbItem>.containsSame(item: TmdbItem) = any { it.sameAs(item) }
private fun TmdbItem.sameAs(o: TmdbItem) = id == o.id && media_type == o.media_type

@Composable
private fun StreamifyApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var category by remember { mutableStateOf(HomeCategory.TRENDING) }
    var sub by remember { mutableStateOf(SubCategory.ALL) }
    var seeAll by remember { mutableStateOf<SeeAllReq?>(null) }
    var selected by remember { mutableStateOf<TmdbItem?>(null) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("streamify", Context.MODE_PRIVATE) }
    var myList by remember { mutableStateOf(loadItems(prefs, "my_list")) }
    var downloads by remember { mutableStateOf(loadItems(prefs, "downloads")) }
    var profileName by remember { mutableStateOf(prefs.getString("profile_name", "Guest") ?: "Guest") }
    var quality by remember { mutableStateOf(prefs.getString("quality", "Auto") ?: "Auto") }
    var autoplay by remember { mutableStateOf(prefs.getBoolean("autoplay", true)) }
    var wifiOnly by remember { mutableStateOf(prefs.getBoolean("wifi_only", false)) }
    var profileBack by remember { mutableStateOf(Screen.HOME) }
    var playReq by remember { mutableStateOf<PlayReq?>(null) }
    val homeList = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val version = remember { appVersionName(context) }

    val goBack: () -> Unit = {
        screen = when (screen) {
            Screen.PLAYBACK, Screen.ABOUT -> Screen.SETTINGS
            Screen.PROFILE -> profileBack
            Screen.PLAYER -> Screen.DETAIL
            else -> Screen.HOME
        }
    }
    BackHandler(enabled = screen != Screen.HOME || category != HomeCategory.TRENDING) {
        if (screen != Screen.HOME) goBack()
        else { category = HomeCategory.TRENDING; sub = SubCategory.ALL; scope.launch { homeList.scrollToItem(0) } }
    }
    val open: (TmdbItem) -> Unit = { selected = it; screen = Screen.DETAIL }
    val showBar = screen != Screen.DETAIL && screen != Screen.SEARCH && screen != Screen.SEE_ALL && screen != Screen.PLAYER

    UpdatePrompt()

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF34060B), Black), endY = 1500f))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    category = category,
                    sub = sub,
                    onCategory = { category = it; sub = SubCategory.ALL; scope.launch { homeList.scrollToItem(0) } },
                    onSub = { sub = it },
                    onSearch = { screen = Screen.SEARCH },
                    onProfile = { profileBack = Screen.HOME; screen = Screen.PROFILE },
                    onPlay = { selected = it; playReq = PlayReq(it); screen = Screen.PLAYER },
                    onOpen = open,
                    onSeeAll = { t, sp -> seeAll = SeeAllReq(t, sp); screen = Screen.SEE_ALL },
                    listState = homeList
                )
                Screen.SEARCH -> SearchScreen({ screen = Screen.HOME }, open)
                Screen.SEE_ALL -> seeAll?.let { SeeAllScreen(it.title, it.spec, { screen = Screen.HOME }, open) }
                Screen.DETAIL -> selected?.let { item ->
                    DetailScreen(
                        item = item,
                        inList = myList.containsSame(item),
                        onBack = { screen = Screen.HOME },
                        onToggleList = {
                            val updated =
                                if (myList.containsSame(item)) myList.filterNot { it.sameAs(item) } else myList + item
                            myList = updated
                            saveItems(prefs, "my_list", updated)
                        },
                        onDownload = {
                            if (!downloads.containsSame(item)) {
                                downloads = downloads + item
                                saveItems(prefs, "downloads", downloads)
                            }
                        },
                        onOpenSimilar = { selected = it },
                        onPlay = { playReq = PlayReq(item); screen = Screen.PLAYER }
                    )
                }
                Screen.PLAYER -> playReq?.let { PlayerScreen(it) { screen = Screen.DETAIL } }
                Screen.MY_LIST -> CollectionScreen("My List", myList, "Your watchlist is empty.", { screen = Screen.HOME }, open)
                Screen.DOWNLOADS -> CollectionScreen("Downloads", downloads, "No downloads yet.", { screen = Screen.HOME }, open)
                Screen.SETTINGS -> SettingsScreen(
                    version = version,
                    onBack = { screen = Screen.HOME },
                    onAccount = { profileBack = Screen.SETTINGS; screen = Screen.PROFILE },
                    onPlayback = { screen = Screen.PLAYBACK },
                    onDownloads = { screen = Screen.DOWNLOADS },
                    onAbout = { screen = Screen.ABOUT }
                )
                Screen.PROFILE -> ProfileScreen(
                    name = profileName,
                    onName = { profileName = it; prefs.edit().putString("profile_name", it).apply() },
                    myCount = myList.size,
                    dlCount = downloads.size,
                    onBack = goBack,
                    onMyList = { screen = Screen.MY_LIST },
                    onDownloads = { screen = Screen.DOWNLOADS }
                )
                Screen.PLAYBACK -> PlaybackScreen(
                    quality = quality,
                    onQuality = { quality = it; prefs.edit().putString("quality", it).apply() },
                    autoplay = autoplay,
                    onAutoplay = { autoplay = it; prefs.edit().putBoolean("autoplay", it).apply() },
                    wifiOnly = wifiOnly,
                    onWifiOnly = { wifiOnly = it; prefs.edit().putBoolean("wifi_only", it).apply() },
                    onBack = goBack
                )
                Screen.ABOUT -> AboutScreen(version, goBack)
            }
        }
        if (showBar) {
            BottomBar(
                selected = when (screen) {
                    Screen.PLAYBACK, Screen.ABOUT -> Screen.SETTINGS
                    Screen.PROFILE -> profileBack
                    else -> screen
                },
                onHome = {
                    category = HomeCategory.TRENDING; sub = SubCategory.ALL
                    screen = Screen.HOME
                    scope.launch { homeList.scrollToItem(0) }
                },
                onMyList = { screen = Screen.MY_LIST },
                onDownloads = { screen = Screen.DOWNLOADS },
                onSettings = { screen = Screen.SETTINGS }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(
    category: HomeCategory,
    sub: SubCategory,
    onCategory: (HomeCategory) -> Unit,
    onSub: (SubCategory) -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
    onPlay: (TmdbItem) -> Unit,
    onOpen: (TmdbItem) -> Unit,
    onSeeAll: (String, BrowseSpec) -> Unit,
    listState: LazyListState
) {
    val repo = remember { TmdbRepository() }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var headerH by remember { mutableStateOf(150.dp) }
    var trending by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }
    val base = remember(category, sub) { baseSpec(category, sub) }
    var newItems by remember(category, sub) { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var newLoaded by remember(category, sub) { mutableStateOf(false) }

    LaunchedEffect(Unit) { trending = try { repo.getTrending() } catch (_: Exception) { emptyList() } }
    LaunchedEffect(category, sub) {
        if (base != null) {
            newItems = try { repo.browse(base.copy(newRelease = true), 1) } catch (_: Exception) { emptyList() }
        }
        newLoaded = true
    }

    val heroIsNew = category != HomeCategory.TRENDING && newItems.size >= 3
    val hero = remember(trending, newItems, category) {
        (if (category != HomeCategory.TRENDING && newItems.size >= 3) newItems else trending).take(5)
    }
    val pagerState = rememberPagerState(pageCount = { hero.size })
    LaunchedEffect(hero) { if (hero.isNotEmpty()) pagerState.scrollToPage(0) }
    LaunchedEffect(hero, pagerState.currentPage) {
        if (hero.size > 1) {
            delay(3000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % hero.size)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = headerH, bottom = 24.dp)
        ) {
            item {
                if (hero.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(18.dp)).background(Surface),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Red, strokeWidth = 3.dp) }
                } else {
                    HeroPager(
                        hero, pagerState, onOpen, if (heroIsNew) "NEW RELEASE" else "TRENDING", onPlay,
                        onPrev = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1 + hero.size) % hero.size) } },
                        onNext = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1) % hero.size) } }
                    )
                }
            }
            if (category == HomeCategory.TRENDING) {
                listOf(HomeCategory.TRENDING, HomeCategory.MOVIES, HomeCategory.TV, HomeCategory.ANIME).forEach { c ->
                    item(key = c.name) { CategorySection(c, onOpen) }
                }
            } else {
                item(key = "head-${category.name}") { GridHeader(category, sub, onSub) }
                val where = if (sub == SubCategory.ALL) category.label else sub.label
                if (base == null) {
                    item(key = "soon") {
                        Text(
                            "Connect your playback source metadata to show ${sub.label} titles.",
                            color = Grey, fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                        )
                    }
                } else {
                    item(key = "new-${category.name}-${sub.name}") {
                        GridBlock("New Releases", newItems, newLoaded, 9, "New", onOpen) {
                            onSeeAll("New Releases  •  $where", base.copy(newRelease = true))
                        }
                    }
                    GENRES.forEach { def ->
                        val spec = genreSpec(base, def, sub)
                        if (spec != null) {
                            item(key = "g-${category.name}-${sub.name}-${def.label}") {
                                SectionGrid(def.label, spec, 6, onOpen) {
                                    onSeeAll("${def.label}  •  $where", spec)
                                }
                            }
                        }
                    }
                }
            }
        }
        // Pinned header: logo, search and categories stay on top; content fades under them
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .onSizeChanged { headerH = with(density) { it.height.toDp() } }
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFF2E0509).copy(alpha = .97f),
                        .78f to Color(0xFF2E0509).copy(alpha = .9f),
                        1f to Color.Transparent
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            TopBar(onSearch, onProfile)
            CategoryPills(category, onCategory)
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun GridHeader(cat: HomeCategory, sub: SubCategory, onSub: (SubCategory) -> Unit) {
    Column(Modifier.padding(top = 18.dp, bottom = 4.dp)) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(cat.icon, null, tint = Red, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text(cat.label, color = White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.padding(horizontal = 14.dp)) { SubPills(subcategories(cat), sub, onSub) }
    }
}

@Composable
private fun PosterGridRow(row: List<TmdbItem>, tag: String?, onOpen: (TmdbItem) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        row.forEach { PosterCard(it, onOpen, tag, Modifier.weight(1f).aspectRatio(0.68f)) }
        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

// one genre block: loads its own titles, then fades in
@Composable
private fun SectionGrid(
    title: String, spec: BrowseSpec, count: Int,
    onOpen: (TmdbItem) -> Unit, onSeeAll: () -> Unit
) {
    val repo = remember { TmdbRepository() }
    val first = remember(spec) { repo.cached(spec, 1) }
    var items by remember(spec) { mutableStateOf(first ?: emptyList()) }
    var loaded by remember(spec) { mutableStateOf(first != null) }
    LaunchedEffect(spec) {
        if (!loaded) {
            items = try { repo.browse(spec, 1) } catch (_: Exception) { emptyList() }
            loaded = true
        }
    }
    GridBlock(title, items, loaded, count, null, onOpen, onSeeAll)
}

@Composable
private fun GridBlock(
    title: String, items: List<TmdbItem>, loaded: Boolean, count: Int, tag: String?,
    onOpen: (TmdbItem) -> Unit, onSeeAll: () -> Unit
) {
    // a genre with nothing in it simply doesn't show up
    if (loaded && items.isEmpty()) return

    Column(Modifier.padding(top = 24.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(RedBrush))
            Spacer(Modifier.width(9.dp))
            Text(
                title, color = White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Row(
                Modifier.clip(RoundedCornerShape(14.dp)).background(GlassBrush)
                    .border(1.dp, White.copy(alpha = .25f), RoundedCornerShape(14.dp))
                    .clickable { onSeeAll() }
                    .padding(start = 11.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("See all", color = White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Outlined.KeyboardArrowRight, null, tint = White, modifier = Modifier.size(14.dp))
            }
        }
        if (!loaded) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                repeat(3) {
                    Box(
                        Modifier.weight(1f).aspectRatio(0.68f).clip(RoundedCornerShape(12.dp))
                            .background(White.copy(alpha = .05f))
                    )
                }
            }
        }
        AnimatedVisibility(visible = loaded, enter = fadeIn(tween(600))) {
            Column {
                items.take(count).chunked(3).forEach { PosterGridRow(it, tag, onOpen) }
            }
        }
    }
}

// full list of one genre / new releases: keeps loading pages while you scroll
@Composable
private fun SeeAllScreen(
    title: String, spec: BrowseSpec,
    onBack: () -> Unit, onOpen: (TmdbItem) -> Unit
) {
    val repo = remember { TmdbRepository() }
    val state = rememberLazyListState()
    var shown by remember(spec) { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var page by remember(spec) { mutableIntStateOf(0) }
    var loading by remember(spec) { mutableStateOf(false) }
    var ended by remember(spec) { mutableStateOf(false) }

    val nearEnd by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= info.totalItemsCount - 3
        }
    }

    LaunchedEffect(spec, page, nearEnd) {
        if (!loading && !ended && (page == 0 || nearEnd)) {
            loading = true
            val next = try { repo.browse(spec, page + 1) } catch (_: Exception) { emptyList() }
            if (next.isEmpty()) ended = true
            else shown = (shown + next).distinctBy { "${it.media_type}-${it.id}" }
            page += 1
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = White) }
            Text(
                title, color = White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            state = state,
            contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
        ) {
            items(shown.chunked(3)) { PosterGridRow(it, null, onOpen) }
            item {
                if (loading) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Red, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySection(cat: HomeCategory, onOpen: (TmdbItem) -> Unit) {
    val repo = remember { TmdbRepository() }
    var sub by remember(cat) { mutableStateOf(SubCategory.ALL) }
    var content by remember(cat) { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var loading by remember(cat) { mutableStateOf(true) }

    LaunchedEffect(cat, sub) {
        loading = true
        content = try {
            if (cat == HomeCategory.TRENDING) repo.getTrending()
            else baseSpec(cat, sub)?.let { repo.browse(it, 1) } ?: emptyList()
        } catch (_: Exception) { emptyList() }
        loading = false
    }

    Column(Modifier.padding(top = 18.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(cat.icon, null, tint = Red, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text(cat.label, color = White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            if (cat != HomeCategory.TRENDING) {
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) { SubPills(subcategories(cat), sub) { sub = it } }
            } else Spacer(Modifier.weight(1f))
            if (cat == HomeCategory.TRENDING) {
                Text("See All", color = Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Outlined.KeyboardArrowRight, null, tint = Red, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Red, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
            }
            content.isEmpty() -> Text(
                if (sub == SubCategory.MULTI_AUDIO || sub == SubCategory.HINDI_DUBBED)
                    "Connect your playback source metadata to show ${sub.label} titles."
                else "No content available.",
                color = Grey, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            )
            else -> PosterRow(content.take(12), onOpen, if (cat == HomeCategory.TRENDING) "Trending" else null)
        }
    }
}

@Composable
private fun TopBar(onSearch: () -> Unit, onProfile: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(R.drawable.streamify_logo), "Streamify",
            modifier = Modifier.height(46.dp).width(112.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.width(10.dp))
        Row(
            Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(24.dp))
                .background(GlassBrush)
                .border(1.dp, White.copy(alpha = .3f), RoundedCornerShape(24.dp))
                .clickable { onSearch() }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, null, tint = White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
            Text("Search movies, series, anime...", color = Grey, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Surface2).border(2.dp, Red, CircleShape)
                .clickable { onProfile() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Person, "Profile", tint = White, modifier = Modifier.size(24.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroPager(
    items: List<TmdbItem>, pagerState: PagerState, onOpen: (TmdbItem) -> Unit, tag: String,
    onPlay: (TmdbItem) -> Unit,
    onPrev: () -> Unit, onNext: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Red.copy(alpha = .6f), RoundedCornerShape(18.dp))
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            HeroSlide(items[page.coerceIn(0, items.lastIndex)], tag, onOpen, onPlay)
        }
        Icon(
            Icons.Outlined.KeyboardArrowLeft, "Previous", tint = White,
            modifier = Modifier.align(Alignment.CenterStart).size(30.dp).clickable { onPrev() }
        )
        Icon(
            Icons.Outlined.KeyboardArrowRight, "Next", tint = White,
            modifier = Modifier.align(Alignment.CenterEnd).size(30.dp).clickable { onNext() }
        )
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(items.size) { i ->
                val active = i == pagerState.currentPage
                Box(
                    Modifier.size(if (active) 16.dp else 6.dp, 6.dp).clip(CircleShape)
                        .background(if (active) Red else White.copy(alpha = .45f))
                )
            }
        }
    }
}

@Composable
private fun HeroSlide(item: TmdbItem, tag: String, onOpen: (TmdbItem) -> Unit, onPlay: (TmdbItem) -> Unit) {
    val title = item.title ?: item.name ?: "Untitled"
    Box(Modifier.fillMaxSize().clickable { onOpen(item) }) {
        AsyncImage(
            model = item.backdrop_path?.let { TMDB_IMAGE + "w780" + it },
            contentDescription = title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Black.copy(alpha = .92f), Black.copy(alpha = .35f), Color.Transparent))
            )
        )
        Column(Modifier.align(Alignment.CenterStart).padding(start = 30.dp, end = 60.dp)) {
            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(Red).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(tag, color = White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(title, color = White, fontSize = 26.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                "${if (item.media_type == "tv") "TV Series" else "Movie"}  •  ★ ${String.format("%.1f", item.vote_average ?: 0.0)}",
                color = White.copy(alpha = .85f), fontSize = 11.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).background(RedBrush)
                    .clickable { onPlay(item) }.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.PlayArrow, null, tint = White, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text("Watch Now", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CategoryPills(selected: HomeCategory, onSelect: (HomeCategory) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HomeCategory.values().toList()) { c ->
            val on = selected == c
            Row(
                Modifier.clip(RoundedCornerShape(24.dp))
                    .background(if (on) Brush.linearGradient(listOf(Red.copy(alpha = .5f), DeepRed.copy(alpha = .25f))) else GlassBrush)
                    .border(1.dp, if (on) Red else White.copy(alpha = .28f), RoundedCornerShape(24.dp))
                    .clickable { onSelect(c) }.padding(horizontal = 15.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(c.icon, null, tint = if (on) Red else White, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(7.dp))
                Text(c.label, color = White, fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SubPills(options: List<SubCategory>, selected: SubCategory, onSelect: (SubCategory) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(options) { s ->
            val on = selected == s
            Box(
                Modifier.clip(RoundedCornerShape(16.dp))
                    .background(if (on) RedBrush else GlassBrush)
                    .border(1.dp, if (on) Color.Transparent else White.copy(alpha = .22f), RoundedCornerShape(16.dp))
                    .clickable { onSelect(s) }.padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Text(s.label, color = White, fontSize = 10.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title, color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun PosterRow(items: List<TmdbItem>, onOpen: (TmdbItem) -> Unit, tag: String? = null) {
    LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items(items) { PosterCard(it, onOpen, tag) }
    }
}

@Composable
private fun PosterCard(
    item: TmdbItem, onOpen: (TmdbItem) -> Unit, tag: String?,
    modifier: Modifier = Modifier.width(108.dp).height(160.dp)
) {
    val title = item.title ?: item.name ?: "Untitled"
    Box(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Red.copy(alpha = .35f), RoundedCornerShape(12.dp))
            .clickable { onOpen(item) }
    ) {
        AsyncImage(
            model = item.poster_path?.let { TMDB_IMAGE + "w342" + it },
            contentDescription = title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Black.copy(alpha = .95f)))
            )
        )
        if (tag != null) {
            Box(
                Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(bottomStart = 8.dp, topEnd = 12.dp))
                    .background(Red).padding(horizontal = 6.dp, vertical = 2.dp)
            ) { Text(tag, color = White, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(7.dp)) {
            Text(title, color = White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                (if (item.media_type == "tv") "TV" else "Movie") +
                    ((item.vote_average ?: 0.0).takeIf { it > 0 }?.let { " • ★ " + String.format("%.1f", it) } ?: ""),
                color = Grey, fontSize = 8.sp
            )
            Spacer(Modifier.height(3.dp))
            Box(Modifier.clip(RoundedCornerShape(3.dp)).background(White.copy(alpha = .18f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                Text("HD", color = White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SearchScreen(onBack: () -> Unit, onOpen: (TmdbItem) -> Unit) {
    val repo = remember { TmdbRepository() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(Black).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = White) }
            TextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.weight(1f), singleLine = true,
                placeholder = { Text("Search movies, series, anime...", color = Grey) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Red) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Surface, unfocusedContainerColor = Surface,
                    focusedTextColor = White, unfocusedTextColor = White, cursorColor = Red,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(onClick = {
                if (query.isNotBlank()) {
                    loading = true
                    scope.launch {
                        results = try { repo.search(query) } catch (_: Exception) { emptyList() }
                        loading = false
                    }
                }
            }) { Icon(Icons.Outlined.Search, "Search", tint = White) }
        }
        Spacer(Modifier.height(12.dp))
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Red) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) { items(results) { ListRow(it, onOpen) } }
    }
}

@Composable
private fun ListRow(item: TmdbItem, onOpen: (TmdbItem) -> Unit) {
    val title = item.title ?: item.name ?: "Untitled"
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GlassBrush)
            .border(1.dp, White.copy(alpha = .22f), RoundedCornerShape(14.dp))
            .clickable { onOpen(item) }.padding(8.dp)
    ) {
        AsyncImage(
            model = item.poster_path?.let { TMDB_IMAGE + "w185" + it }, contentDescription = title,
            modifier = Modifier.size(62.dp, 90.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text(if (item.media_type == "tv") "TV Series" else "Movie", color = Red, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Text(item.overview ?: "No description available.", color = Grey, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DetailScreen(
    item: TmdbItem, inList: Boolean, onBack: () -> Unit, onToggleList: () -> Unit,
    onDownload: () -> Unit, onOpenSimilar: (TmdbItem) -> Unit, onPlay: () -> Unit
) {
    val repo = remember { TmdbRepository() }
    var details by remember { mutableStateOf(item) }
    var similar by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }

    LaunchedEffect(item.id, item.media_type) {
        details = item
        try { details = repo.getDetails(item); similar = repo.getSimilar(item).take(12) } catch (_: Exception) {}
    }
    val title = details.title ?: details.name ?: "Untitled"

    LazyColumn(Modifier.fillMaxSize().background(Black), contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                AsyncImage(
                    model = details.backdrop_path?.let { TMDB_IMAGE + "w780" + it }, contentDescription = title,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Black.copy(alpha = .25f), Black))))
                IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp, start = 8.dp)) {
                    Icon(Icons.Outlined.ArrowBack, "Back", tint = White)
                }
                Box(
                    Modifier.align(Alignment.Center).size(58.dp).clip(CircleShape)
                        .background(Black.copy(alpha = .5f)).border(2.dp, White, CircleShape)
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.PlayArrow, "Play", tint = White, modifier = Modifier.size(34.dp)) }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Text(title, color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag("HD"); Spacer(Modifier.width(6.dp)); Tag("13+")
                    Spacer(Modifier.width(10.dp))
                    Text("★ ${String.format("%.1f", details.vote_average ?: 0.0)}", color = Gold, fontSize = 11.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(if (details.media_type == "tv") "TV Series" else "Movie", color = Grey, fontSize = 11.sp)
                }
                if (!details.genres.isNullOrEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(details.genres!!.joinToString("  •  ") { it.name }, color = Grey, fontSize = 11.sp)
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(24.dp)).background(RedBrush)
                            .clickable { onPlay() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.PlayArrow, null, tint = White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    IconAction(if (inList) Icons.Outlined.Check else Icons.Outlined.Add, if (inList) "Added" else "My List", onToggleList)
                    IconAction(Icons.Outlined.Download, "Download", onDownload)
                    IconAction(Icons.Outlined.Share, "Share") {}
                }
                Spacer(Modifier.height(20.dp))
                Text("About", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(details.overview ?: "No description available.", color = Grey, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        if (similar.isNotEmpty()) {
            item {
                Spacer(Modifier.height(22.dp))
                SectionTitle("Because You Watched This")
                Spacer(Modifier.height(9.dp))
                PosterRow(similar, onOpenSimilar)
                Spacer(Modifier.height(20.dp))
                SectionTitle("Similar to $title")
                Spacer(Modifier.height(9.dp))
                PosterRow(similar.reversed(), onOpenSimilar)
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Box(Modifier.clip(RoundedCornerShape(4.dp)).border(1.dp, White.copy(alpha = .6f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
        Text(text, color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IconAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = Grey, fontSize = 8.sp)
    }
}

@Composable
private fun CollectionScreen(
    title: String, items: List<TmdbItem>, emptyText: String,
    onBack: () -> Unit, onOpen: (TmdbItem) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Black)) {
        Row(Modifier.fillMaxWidth().padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = White) }
            Text(title, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        if (items.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = Grey, fontSize = 13.sp)
        } else LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)
        ) { items(items) { ListRow(it, onOpen) } }
    }
}

@Composable
private fun ScreenTop(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = White) }
        Text(title, color = White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun SettingsScreen(
    version: String,
    onBack: () -> Unit,
    onAccount: () -> Unit,
    onPlayback: () -> Unit,
    onDownloads: () -> Unit,
    onAbout: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ScreenTop("Settings", onBack)
        Setting("Account", "Profile, plan and subscription", onAccount)
        Setting("Playback", "Video quality and autoplay", onPlayback)
        Setting("Downloads", "Manage downloaded content", onDownloads)
        Setting("About Streamify", "Version $version  •  check for updates", onAbout)
    }
}

@Composable
private fun Setting(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = Grey, fontSize = 11.sp)
        }
        Icon(Icons.Outlined.KeyboardArrowRight, null, tint = Grey)
    }
}

@Composable
private fun ProfileScreen(
    name: String,
    onName: (String) -> Unit,
    myCount: Int,
    dlCount: Int,
    onBack: () -> Unit,
    onMyList: () -> Unit,
    onDownloads: () -> Unit
) {
    val context = LocalContext.current
    val initial = name.trim().firstOrNull()?.uppercase() ?: "G"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTop("Profile", onBack)
        Box(
            Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp).size(96.dp)
                .clip(CircleShape).background(RedBrush)
                .border(2.dp, White.copy(alpha = .4f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(initial, color = White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold) }

        Spacer(Modifier.height(16.dp))
        TextField(
            value = name,
            onValueChange = { if (it.length <= 24) onName(it) },
            singleLine = true,
            label = { Text("Your name", color = Grey) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Surface, unfocusedContainerColor = Surface,
                focusedTextColor = White, unfocusedTextColor = White, cursorColor = Red,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(16.dp))
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("My List", myCount, Modifier.weight(1f), onMyList)
            StatCard("Downloads", dlCount, Modifier.weight(1f), onDownloads)
        }

        Spacer(Modifier.height(18.dp))
        Column(
            Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)).background(GlassBrush)
                .border(1.dp, White.copy(alpha = .25f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Text("FREE PLAN", color = Red, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text("480p  •  up to 3 downloads", color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Text("PREMIUM  •  ₹99 / month", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text("720p to 1080p  •  unlimited downloads", color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.clip(RoundedCornerShape(22.dp)).background(RedBrush)
                    .clickable {
                        Toast.makeText(context, "Premium payments are not live yet", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 22.dp, vertical = 11.dp)
            ) { Text("Get Premium", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(label: String, count: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(GlassBrush)
            .border(1.dp, White.copy(alpha = .25f), RoundedCornerShape(18.dp))
            .clickable { onClick() }.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$count", color = White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Grey, fontSize = 11.sp)
    }
}

@Composable
private fun PlaybackScreen(
    quality: String, onQuality: (String) -> Unit,
    autoplay: Boolean, onAutoplay: (Boolean) -> Unit,
    wifiOnly: Boolean, onWifiOnly: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTop("Playback", onBack)
        Text(
            "Video quality", color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Row(
            Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Auto", "480p", "720p", "1080p").forEach { opt ->
                val premium = opt == "720p" || opt == "1080p"
                val on = quality == opt
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (on) RedBrush else GlassBrush)
                        .border(1.dp, if (on) Color.Transparent else White.copy(alpha = .28f), RoundedCornerShape(20.dp))
                        .clickable {
                            if (premium) Toast.makeText(context, "$opt is a Premium feature", Toast.LENGTH_SHORT).show()
                            else onQuality(opt)
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(opt, color = White, fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                    if (premium) {
                        Spacer(Modifier.width(5.dp))
                        Text("PRO", color = Gold, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        ToggleRow("Autoplay next episode", "Start the next episode automatically", autoplay, onAutoplay)
        ToggleRow("Stream on Wi-Fi only", "Don't use mobile data for video", wifiOnly, onWifiOnly)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = Grey, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Red, checkedThumbColor = White,
                uncheckedTrackColor = Surface2, uncheckedThumbColor = Grey,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun AboutScreen(version: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<UpdateInfo?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenTop("About Streamify", onBack)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painterResource(R.drawable.streamify_logo), "Streamify",
                modifier = Modifier.height(96.dp).width(230.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(6.dp))
            Text("Version $version", color = Grey, fontSize = 13.sp)
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.clip(RoundedCornerShape(24.dp)).background(RedBrush)
                    .clickable(enabled = !checking) {
                        scope.launch {
                            checking = true
                            status = null
                            val latest = fetchLatestUpdate()
                            checking = false
                            when {
                                latest == null -> status = "Couldn't check right now. Try again later."
                                latest.code > installedVersionCode(context) -> pending = latest
                                else -> status = "You are on the latest version."
                            }
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (checking) {
                    CircularProgressIndicator(color = White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text("Check for updates", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            status?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Grey, fontSize = 12.sp)
            }
            Spacer(Modifier.height(36.dp))
            Text(
                "This product uses the TMDB API but is not endorsed or certified by TMDB.",
                color = Grey, fontSize = 10.sp, lineHeight = 15.sp
            )
        }
    }
    pending?.let { UpdateDialog(it) { pending = null } }
}

@Composable
private fun BottomBar(
    selected: Screen, onHome: () -> Unit, onMyList: () -> Unit, onDownloads: () -> Unit, onSettings: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(30.dp)).background(GlassBrush)
            .border(1.5.dp, Red.copy(alpha = .8f), RoundedCornerShape(30.dp))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(Icons.Outlined.Home, "Home", selected == Screen.HOME, onHome)
        NavItem(Icons.Outlined.List, "My List", selected == Screen.MY_LIST, onMyList)
        Box(
            Modifier.size(54.dp).clip(CircleShape).background(RedBrush).border(2.dp, White.copy(alpha = .25f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.PlayArrow, "Play", tint = White, modifier = Modifier.size(30.dp)) }
        NavItem(Icons.Outlined.Download, "Downloads", selected == Screen.DOWNLOADS, onDownloads)
        NavItem(Icons.Outlined.Settings, "Settings", selected == Screen.SETTINGS, onSettings)
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable { onClick() }.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = if (selected) Red else White, modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(1.dp))
        Text(label, color = if (selected) Red else White, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
