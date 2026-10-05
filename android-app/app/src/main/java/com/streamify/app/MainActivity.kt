package com.streamify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

private enum class Screen { HOME, SEARCH, DETAIL, MY_LIST, DOWNLOADS, SETTINGS }

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

private suspend fun loadContent(repo: TmdbRepository, c: HomeCategory, s: SubCategory): List<TmdbItem> =
    when (c) {
        HomeCategory.TRENDING -> repo.getTrending()
        HomeCategory.MOVIES -> when (s) {
            SubCategory.HOLLYWOOD -> repo.getHollywoodMovies()
            SubCategory.BOLLYWOOD -> repo.getBollywoodMovies()
            SubCategory.SOUTH -> repo.getSouthMovies()
            SubCategory.MULTI_AUDIO, SubCategory.HINDI_DUBBED -> emptyList()
            else -> repo.getMovies()
        }
        HomeCategory.TV -> when (s) {
            SubCategory.WEB_SERIES -> repo.getWebSeries()
            SubCategory.BOLLYWOOD_SERIES -> repo.getBollywoodSeries()
            SubCategory.TV_SHOWS -> repo.getEnglishTvShows()
            else -> repo.getTvShows()
        }
        HomeCategory.DRAMA -> when (s) {
            SubCategory.KDRAMA -> repo.getKDrama()
            SubCategory.TURKISH -> repo.getTurkishDrama()
            SubCategory.PAKISTANI -> repo.getPakistaniDrama()
            else -> repo.getDrama()
        }
        HomeCategory.ANIME -> when (s) {
            SubCategory.ANIMATED -> repo.getAnimatedContent()
            SubCategory.CARTOON -> repo.getCartoonShows()
            else -> repo.getAnime()
        }
    }

private fun List<TmdbItem>.containsSame(item: TmdbItem) = any { it.sameAs(item) }
private fun TmdbItem.sameAs(o: TmdbItem) = id == o.id && media_type == o.media_type

@Composable
private fun StreamifyApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var category by remember { mutableStateOf(HomeCategory.TRENDING) }
    var homeTick by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<TmdbItem?>(null) }
    var myList by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var downloads by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }

    BackHandler(enabled = screen != Screen.HOME || category != HomeCategory.TRENDING) {
        if (screen != Screen.HOME) screen = Screen.HOME else category = HomeCategory.TRENDING
    }
    val open: (TmdbItem) -> Unit = { selected = it; screen = Screen.DETAIL }
    val showBar = screen == Screen.HOME || screen == Screen.MY_LIST ||
        screen == Screen.DOWNLOADS || screen == Screen.SETTINGS

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF34060B), Black), endY = 1500f))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (screen) {
                Screen.HOME -> HomeScreen(category, { category = it }, { screen = Screen.SEARCH }, open, homeTick)
                Screen.SEARCH -> SearchScreen({ screen = Screen.HOME }, open)
                Screen.DETAIL -> selected?.let { item ->
                    DetailScreen(
                        item = item,
                        inList = myList.containsSame(item),
                        onBack = { screen = Screen.HOME },
                        onToggleList = {
                            myList = if (myList.containsSame(item)) myList.filterNot { it.sameAs(item) } else myList + item
                        },
                        onDownload = { if (!downloads.containsSame(item)) downloads = downloads + item },
                        onOpenSimilar = { selected = it }
                    )
                }
                Screen.MY_LIST -> CollectionScreen("My List", myList, "Your watchlist is empty.", { screen = Screen.HOME }, open)
                Screen.DOWNLOADS -> CollectionScreen("Downloads", downloads, "No downloads yet.", { screen = Screen.HOME }, open)
                Screen.SETTINGS -> SettingsScreen { screen = Screen.HOME }
            }
        }
        if (showBar) {
            BottomBar(
                selected = screen,
                onHome = { category = HomeCategory.TRENDING; homeTick++; screen = Screen.HOME },
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
    onCategory: (HomeCategory) -> Unit,
    onSearch: () -> Unit,
    onOpen: (TmdbItem) -> Unit,
    resetTick: Int
) {
    val repo = remember { TmdbRepository() }
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var headerH by remember { mutableStateOf(150.dp) }
    var trending by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var sub by remember(category) { mutableStateOf(SubCategory.ALL) }
    var gridItems by remember(category, sub) { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var gridLoading by remember(category, sub) { mutableStateOf(true) }

    LaunchedEffect(Unit) { trending = try { repo.getTrending() } catch (_: Exception) { emptyList() } }
    LaunchedEffect(category, sub) {
        if (category != HomeCategory.TRENDING) {
            gridLoading = true
            gridItems = try { loadContent(repo, category, sub) } catch (_: Exception) { emptyList() }
            gridLoading = false
        }
    }
    LaunchedEffect(resetTick, category) { listState.scrollToItem(0) }

    val hero = remember(trending, gridItems, category) {
        (if (category == HomeCategory.TRENDING || gridItems.isEmpty()) trending else gridItems).take(5)
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
            contentPadding = PaddingValues(top = headerH, bottom = 16.dp)
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
                        hero, pagerState, onOpen,
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
                item(key = "grid-header") { GridHeader(category, sub) { sub = it } }
                if (gridLoading) {
                    item(key = "grid-loading") {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Red, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                        }
                    }
                } else if (gridItems.isEmpty()) {
                    item(key = "grid-empty") {
                        Text(
                            if (sub == SubCategory.MULTI_AUDIO || sub == SubCategory.HINDI_DUBBED)
                                "Connect your playback source metadata to show ${sub.label} titles."
                            else "No content available.",
                            color = Grey, fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                        )
                    }
                } else {
                    items(gridItems.chunked(3)) { row ->
                        Row(
                            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            row.forEach { PosterCard(it, onOpen, null, Modifier.weight(1f).aspectRatio(0.68f)) }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
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
            TopBar(onSearch)
            CategoryPills(category, onCategory)
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun GridHeader(cat: HomeCategory, sub: SubCategory, onSub: (SubCategory) -> Unit) {
    Column(Modifier.padding(top = 18.dp, bottom = 12.dp)) {
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
private fun CategorySection(cat: HomeCategory, onOpen: (TmdbItem) -> Unit) {
    val repo = remember { TmdbRepository() }
    var sub by remember(cat) { mutableStateOf(SubCategory.ALL) }
    var content by remember(cat) { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var loading by remember(cat) { mutableStateOf(true) }

    LaunchedEffect(cat, sub) {
        loading = true
        content = try { loadContent(repo, cat, sub) } catch (_: Exception) { emptyList() }
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
private fun TopBar(onSearch: () -> Unit) {
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
            Modifier.size(44.dp).clip(CircleShape).background(Surface2).border(2.dp, Red, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Person, "Profile", tint = White, modifier = Modifier.size(24.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroPager(
    items: List<TmdbItem>, pagerState: PagerState, onOpen: (TmdbItem) -> Unit,
    onPrev: () -> Unit, onNext: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Red.copy(alpha = .6f), RoundedCornerShape(18.dp))
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            HeroSlide(items[page.coerceIn(0, items.lastIndex)], onOpen)
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
private fun HeroSlide(item: TmdbItem, onOpen: (TmdbItem) -> Unit) {
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
                Text("TRENDING", color = White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
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
                    .clickable { onOpen(item) }.padding(horizontal = 14.dp, vertical = 8.dp),
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
    onDownload: () -> Unit, onOpenSimilar: (TmdbItem) -> Unit
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
                        .background(Black.copy(alpha = .5f)).border(2.dp, White, CircleShape),
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
                            .clickable { /* PLAYBACK SOURCE INTEGRATION SLOT */ },
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
private fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Black)) {
        Row(Modifier.fillMaxWidth().padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = White) }
            Text("Settings", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Setting("Account", "Login and subscription")
        Setting("Playback", "Video quality and playback settings")
        Setting("Downloads", "Manage downloaded content")
        Setting("About Streamify", "App information")
    }
}

@Composable
private fun Setting(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 15.dp)) {
        Text(title, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = Grey, fontSize = 11.sp)
    }
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
