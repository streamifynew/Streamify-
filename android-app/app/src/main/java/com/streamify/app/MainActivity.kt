package com.streamify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private const val TMDB_IMAGE = "https://image.tmdb.org/t/p/"

private val Black = Color(0xFF070707)
private val Surface = Color(0xFF151515)
private val Surface2 = Color(0xFF202020)
private val Red = Color(0xFFFF3B30)
private val Orange = Color(0xFFFF6A00)
private val Gold = Color(0xFFFFD166)
private val White = Color(0xFFF8F8F8)
private val Grey = Color(0xFF969696)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StreamifyApp() }
    }
}

private enum class Screen { HOME, SEARCH, DETAIL, MY_LIST, DOWNLOADS, SETTINGS }

private enum class HomeCategory(val label: String) {
    TRENDING("Trending"),
    MOVIES("Movies"),
    TV("TV"),
    DRAMA("Drama"),
    ANIME("Anime")
}

private enum class SubCategory(val label: String) {
    ALL("All"),
    HOLLYWOOD("Hollywood"),
    BOLLYWOOD("Bollywood"),
    SOUTH("South Cinema"),
    MULTI_AUDIO("Multi-Audio"),
    HINDI_DUBBED("Hindi Dubbed"),
    WEB_SERIES("Web Series"),
    BOLLYWOOD_SERIES("Bollywood Series"),
    TV_SHOWS("TV Shows"),
    ANIMATED("Animated Movies/Shows"),
    ANIME("Anime"),
    CARTOON("Cartoon Shows"),
    KDRAMA("K-Drama"),
    TURKISH("Turkish Drama"),
    PAKISTANI("Pakistani Drama")
}

@Composable
private fun StreamifyApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var category by remember { mutableStateOf(HomeCategory.TRENDING) }
    var subcategory by remember { mutableStateOf(SubCategory.ALL) }
    var selected by remember { mutableStateOf<TmdbItem?>(null) }
    var myList by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var downloads by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }

    BackHandler(enabled = screen != Screen.HOME) {
        screen = Screen.HOME
    }

    Box(Modifier.fillMaxSize().background(Black)) {
        when (screen) {
            Screen.HOME -> HomeScreen(
                category = category,
                subcategory = subcategory,
                onCategory = {
                    category = it
                    subcategory = SubCategory.ALL
                },
                onSubcategory = { subcategory = it },
                onSearch = { screen = Screen.SEARCH },
                onOpen = {
                    selected = it
                    screen = Screen.DETAIL
                },
                onMyList = { screen = Screen.MY_LIST },
                onDownloads = { screen = Screen.DOWNLOADS },
                onSettings = { screen = Screen.SETTINGS }
            )

            Screen.SEARCH -> SearchScreen(
                onBack = { screen = Screen.HOME },
                onOpen = {
                    selected = it
                    screen = Screen.DETAIL
                }
            )

            Screen.DETAIL -> selected?.let { item ->
                DetailScreen(
                    item = item,
                    inList = myList.containsSame(item),
                    onBack = { screen = Screen.HOME },
                    onToggleList = {
                        myList = if (myList.containsSame(item)) {
                            myList.filterNot { it.sameAs(item) }
                        } else {
                            myList + item
                        }
                    },
                    onDownload = {
                        if (!downloads.containsSame(item)) downloads = downloads + item
                    },
                    onOpenSimilar = {
                        selected = it
                    }
                )
            }

            Screen.MY_LIST -> CollectionScreen(
                title = "My List",
                items = myList,
                emptyText = "Your watchlist is empty.",
                onBack = { screen = Screen.HOME },
                onOpen = {
                    selected = it
                    screen = Screen.DETAIL
                }
            )

            Screen.DOWNLOADS -> CollectionScreen(
                title = "Downloads",
                items = downloads,
                emptyText = "No downloads yet.",
                onBack = { screen = Screen.HOME },
                onOpen = {
                    selected = it
                    screen = Screen.DETAIL
                }
            )

            Screen.SETTINGS -> SettingsScreen { screen = Screen.HOME }
        }
    }
}

private fun List<TmdbItem>.containsSame(item: TmdbItem): Boolean = any { it.sameAs(item) }

private fun TmdbItem.sameAs(other: TmdbItem): Boolean =
    id == other.id && media_type == other.media_type

private fun subcategories(category: HomeCategory): List<SubCategory> = when (category) {
    HomeCategory.TRENDING -> listOf(SubCategory.ALL)
    HomeCategory.MOVIES -> listOf(
        SubCategory.ALL, SubCategory.HOLLYWOOD, SubCategory.BOLLYWOOD,
        SubCategory.SOUTH, SubCategory.MULTI_AUDIO, SubCategory.HINDI_DUBBED
    )
    HomeCategory.TV -> listOf(
        SubCategory.ALL, SubCategory.WEB_SERIES,
        SubCategory.BOLLYWOOD_SERIES, SubCategory.TV_SHOWS
    )
    HomeCategory.ANIME -> listOf(
        SubCategory.ALL, SubCategory.ANIMATED, SubCategory.ANIME, SubCategory.CARTOON
    )
    HomeCategory.DRAMA -> listOf(
        SubCategory.ALL, SubCategory.KDRAMA,
        SubCategory.TURKISH, SubCategory.PAKISTANI
    )
}

@Composable
private fun HomeScreen(
    category: HomeCategory,
    subcategory: SubCategory,
    onCategory: (HomeCategory) -> Unit,
    onSubcategory: (SubCategory) -> Unit,
    onSearch: () -> Unit,
    onOpen: (TmdbItem) -> Unit,
    onMyList: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
    val repo = remember { TmdbRepository() }
    var content by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var heroIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(category, subcategory) {
        loading = true
        error = null
        content = emptyList()
        try {
            content = when (category) {
                HomeCategory.TRENDING -> repo.getTrending()

                HomeCategory.MOVIES -> when (subcategory) {
                    SubCategory.HOLLYWOOD -> repo.getHollywoodMovies()
                    SubCategory.BOLLYWOOD -> repo.getBollywoodMovies()
                    SubCategory.SOUTH -> repo.getSouthMovies()
                    SubCategory.MULTI_AUDIO, SubCategory.HINDI_DUBBED -> emptyList()
                    else -> repo.getMovies()
                }

                HomeCategory.TV -> when (subcategory) {
                    SubCategory.BOLLYWOOD_SERIES -> repo.getBollywoodSeries()
                    SubCategory.TV_SHOWS -> repo.getEnglishTvShows()
                    else -> repo.getTvShows()
                }

                HomeCategory.DRAMA -> when (subcategory) {
                    SubCategory.KDRAMA -> repo.getKDrama()
                    SubCategory.TURKISH -> repo.getTurkishDrama()
                    SubCategory.PAKISTANI -> repo.getPakistaniDrama()
                    else -> repo.getDrama()
                }

                HomeCategory.ANIME -> when (subcategory) {
                    SubCategory.ANIMATED -> repo.getAnimatedContent()
                    SubCategory.CARTOON -> repo.getCartoonShows()
                    else -> repo.getAnime()
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Unable to load TMDB content."
        }
        loading = false
        heroIndex = 0
    }

    LaunchedEffect(content) {
        while (content.size > 1) {
            delay(3000)
            heroIndex = (heroIndex + 1) % minOf(content.size, 5)
        }
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 94.dp)
        ) {
            item { TopBar(onSearch) }

            item {
                Spacer(Modifier.height(6.dp))
                when {
                    loading -> LoadingHero()
                    content.isNotEmpty() -> Hero(
                        item = content[heroIndex.coerceIn(0, content.lastIndex)],
                        dots = minOf(content.size, 5),
                        activeDot = heroIndex,
                        onOpen = onOpen
                    )
                    else -> EmptyHero(error)
                }
            }

            item {
                Spacer(Modifier.height(13.dp))
                MainCategoryPills(category, onCategory)
            }

            if (category != HomeCategory.TRENDING) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SubcategoryPills(
                        options = subcategories(category),
                        selected = subcategory,
                        onSelect = onSubcategory
                    )
                }
            }

            if (!loading && content.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(18.dp))
                    SectionTitle(
                        title = if (category == HomeCategory.TRENDING) "Trending" else category.label,
                        subtitle = if (subcategory == SubCategory.ALL) "TMDB catalogue" else subcategory.label
                    )
                    Spacer(Modifier.height(9.dp))
                    PosterRow(content.take(12), onOpen)
                }
            }

            if (!loading && content.isEmpty() && subcategory in listOf(SubCategory.MULTI_AUDIO, SubCategory.HINDI_DUBBED)) {
                item {
                    IntegrationNotice()
                }
            }
        }

        BottomBar(
            selected = Screen.HOME,
            onHome = {},
            onMyList = onMyList,
            onDownloads = onDownloads,
            onSettings = onSettings
        )
    }
}

@Composable
private fun TopBar(onSearch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(R.drawable.streamify_logo),
            contentDescription = "Streamify",
            modifier = Modifier.size(43.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(9.dp))

        Box(
            Modifier.weight(1f).height(43.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(Surface)
                .border(1.dp, Color.White.copy(alpha = .06f), RoundedCornerShape(23.dp))
                .clickable { onSearch() }
                .padding(horizontal = 13.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Search, null, tint = Grey, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Search movies, series, anime...",
                    color = Grey,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(9.dp))

        Box(
            Modifier.size(43.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Red, Orange))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, "Profile", tint = White, modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
private fun LoadingHero() {
    Box(
        Modifier.fillMaxWidth().height(218.dp)
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Red, strokeWidth = 3.dp)
    }
}

@Composable
private fun EmptyHero(message: String?) {
    Box(
        Modifier.fillMaxWidth().height(218.dp)
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface),
        contentAlignment = Alignment.Center
    ) {
        Text(message ?: "No content available.", color = Grey, fontSize = 13.sp)
    }
}

@Composable
private fun Hero(
    item: TmdbItem,
    dots: Int,
    activeDot: Int,
    onOpen: (TmdbItem) -> Unit
) {
    val title = item.title ?: item.name ?: "Untitled"

    Box(
        Modifier.fillMaxWidth().height(218.dp)
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Red, Orange, Color.Transparent)),
                RoundedCornerShape(20.dp)
            )
            .clickable { onOpen(item) }
    ) {
        AsyncImage(
            model = item.backdrop_path?.let { TMDB_IMAGE + "w780" + it },
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = .22f),
                        Black.copy(alpha = .97f)
                    )
                )
            )
        )

        Column(
            Modifier.align(Alignment.BottomStart).padding(15.dp)
        ) {
            Text(
                title,
                color = White,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "★ ${String.format("%.1f", item.vote_average ?: 0.0)}",
                    color = Gold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    if (item.media_type == "tv") "TV Series" else "Movie",
                    color = White.copy(alpha = .8f),
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(7.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .background(Red)
                        .clickable { onOpen(item) }
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PlayArrow, null, tint = White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Watch Now", color = White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.width(7.dp))

                Box(
                    Modifier.clip(RoundedCornerShape(7.dp))
                        .background(White.copy(alpha = .12f))
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    Text("HD", color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(dots) { index ->
                Box(
                    Modifier.size(if (index == activeDot) 17.dp else 5.dp, 5.dp)
                        .clip(CircleShape)
                        .background(if (index == activeDot) Red else White.copy(alpha = .48f))
                )
            }
        }
    }
}

@Composable
private fun MainCategoryPills(
    selected: HomeCategory,
    onSelect: (HomeCategory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 17.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items(HomeCategory.values().toList()) { item ->
            Pill(item.label, selected == item, { onSelect(item) }, true)
        }
    }
}

@Composable
private fun SubcategoryPills(
    options: List<SubCategory>,
    selected: SubCategory,
    onSelect: (SubCategory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 17.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(options) { item ->
            Pill(item.label, selected == item, { onSelect(item) }, false)
        }
    }
}

@Composable
private fun Pill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    primary: Boolean
) {
    Box(
        Modifier.clip(RoundedCornerShape(18.dp))
            .background(
                if (selected)
                    Brush.linearGradient(listOf(Red, Orange))
                else
                    Brush.linearGradient(listOf(Surface, Surface2))
            )
            .border(
                1.dp,
                if (selected) Color.Transparent else White.copy(alpha = .07f),
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(
                horizontal = if (primary) 14.dp else 12.dp,
                vertical = if (primary) 8.dp else 7.dp
            )
    ) {
        Text(
            label,
            color = if (selected) White else Grey,
            fontSize = if (primary) 11.sp else 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(4.dp))
                .background(Brush.verticalGradient(listOf(Red, Orange)))
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Grey, fontSize = 9.sp)
        }
        Text("See All", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PosterRow(items: List<TmdbItem>, onOpen: (TmdbItem) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(items) { PosterCard(it, onOpen) }
    }
}

@Composable
private fun PosterCard(item: TmdbItem, onOpen: (TmdbItem) -> Unit) {
    val title = item.title ?: item.name ?: "Untitled"

    Column(
        Modifier.width(106.dp).clickable { onOpen(item) }
    ) {
        Box(
            Modifier.fillMaxWidth().height(154.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Surface)
        ) {
            AsyncImage(
                model = item.poster_path?.let { TMDB_IMAGE + "w342" + it },
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                Modifier.align(Alignment.TopStart).padding(5.dp)
                    .clip(RoundedCornerShape(4.dp)).background(Red)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("HD", color = White, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold)
            }

            Box(
                Modifier.align(Alignment.BottomEnd).padding(5.dp)
                    .clip(RoundedCornerShape(4.dp)).background(Black.copy(alpha = .8f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    "★ ${String.format("%.1f", item.vote_average ?: 0.0)}",
                    color = Gold,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(5.dp))

        Text(
            title,
            color = White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            if (item.media_type == "tv") "TV Series" else "Movie",
            color = Grey,
            fontSize = 8.sp
        )
    }
}

@Composable
private fun IntegrationNotice() {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 24.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, White.copy(alpha = .06f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column {
            Text("Playback-source metadata required", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                "Hindi Dubbed and Multi-Audio labels are intentionally not guessed from TMDB. Connect your playback/source metadata here later.",
                color = Grey,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun SearchScreen(
    onBack: () -> Unit,
    onOpen: (TmdbItem) -> Unit
) {
    val repo = remember { TmdbRepository() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(Black).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = White)
            }

            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Search movies, series, anime...", color = Grey) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Red) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    cursorColor = Red,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(22.dp)
            )

            IconButton(
                onClick = {
                    if (query.isNotBlank()) {
                        loading = true
                        scope.launch {
                            results = try { repo.search(query) } catch (_: Exception) { emptyList() }
                            loading = false
                        }
                    }
                }
            ) {
                Icon(Icons.Outlined.Search, "Search", tint = White)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Red)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(results) { SearchResult(it, onOpen) }
            }
        }
    }
}

@Composable
private fun SearchResult(item: TmdbItem, onOpen: (TmdbItem) -> Unit) {
    val title = item.title ?: item.name ?: "Untitled"
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
            .background(Surface).clickable { onOpen(item) }.padding(8.dp)
    ) {
        AsyncImage(
            model = item.poster_path?.let { TMDB_IMAGE + "w185" + it },
            contentDescription = title,
            modifier = Modifier.size(62.dp, 90.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text(if (item.media_type == "tv") "TV Series" else "Movie", color = Orange, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                item.overview ?: "No description available.",
                color = Grey,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailScreen(
    item: TmdbItem,
    inList: Boolean,
    onBack: () -> Unit,
    onToggleList: () -> Unit,
    onDownload: () -> Unit,
    onOpenSimilar: (TmdbItem) -> Unit
) {
    val repo = remember { TmdbRepository() }
    var details by remember { mutableStateOf(item) }
    var similar by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }

    LaunchedEffect(item.id, item.media_type) {
        try {
            details = repo.getDetails(item)
            similar = repo.getSimilar(item).take(12)
        } catch (_: Exception) {}
    }

    val title = details.title ?: details.name ?: "Untitled"

    LazyColumn(
        Modifier.fillMaxSize().background(Black),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(310.dp)) {
                AsyncImage(
                    model = details.backdrop_path?.let { TMDB_IMAGE + "w780" + it },
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Black.copy(alpha = .2f), Black)
                        )
                    )
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(top = 8.dp, start = 8.dp)
                ) {
                    Icon(Icons.Outlined.ArrowBack, "Back", tint = White)
                }
                Box(
                    Modifier.align(Alignment.Center).size(58.dp).clip(CircleShape)
                        .background(Red),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.PlayArrow, "Play", tint = White, modifier = Modifier.size(33.dp))
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Text(title, color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★ ${String.format("%.1f", details.vote_average ?: 0.0)}", color = Gold, fontSize = 11.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(if (details.media_type == "tv") "TV Series" else "Movie", color = Grey, fontSize = 10.sp)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(5.dp)).background(Red)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("HD", color = White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(13.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    DetailButton(Icons.Outlined.PlayArrow, "Play", true) {
                        // PLAYBACK SOURCE INTEGRATION SLOT.
                        // Connect the user's authorized playback source here.
                    }
                    DetailButton(
                        if (inList) Icons.Outlined.Check else Icons.Outlined.StarBorder,
                        if (inList) "Added" else "My List",
                        false,
                        onToggleList
                    )
                    DetailButton(Icons.Outlined.Download, "Download", false, onDownload)
                    DetailButton(Icons.Outlined.Share, "Share", false) { }
                }

                Spacer(Modifier.height(22.dp))
                Text("About", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    details.overview ?: "No description available.",
                    color = Grey,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                if (!details.genres.isNullOrEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        details.genres!!.joinToString("  •  ") { it.name },
                        color = Orange,
                        fontSize = 10.sp
                    )
                }
            }
        }

        if (similar.isNotEmpty()) {
            item {
                Spacer(Modifier.height(25.dp))
                SectionTitle("Because You Watched This", "Similar TMDB titles")
                Spacer(Modifier.height(9.dp))
                PosterRow(similar, onOpenSimilar)

                Spacer(Modifier.height(23.dp))
                SectionTitle("You May Also Like", "More similar titles")
                Spacer(Modifier.height(9.dp))
                PosterRow(similar.shuffled().take(8), onOpenSimilar)
            }
        }
    }
}

@Composable
private fun DetailButton(
    icon: ImageVector,
    label: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (primary) Red else Surface)
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = White, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CollectionScreen(
    title: String,
    items: List<TmdbItem>,
    emptyText: String,
    onBack: () -> Unit,
    onOpen: (TmdbItem) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Black)) {
        Row(
            Modifier.fillMaxWidth().padding(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = White)
            }
            Text(title, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyText, color = Grey, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(items) { SearchResult(it, onOpen) }
            }
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Black)) {
        Row(
            Modifier.fillMaxWidth().padding(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = White)
            }
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
    selected: Screen,
    onHome: () -> Unit,
    onMyList: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(Surface.copy(alpha = .97f))
            .border(1.dp, White.copy(alpha = .07f), RoundedCornerShape(29.dp))
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(Icons.Outlined.Home, "Home", selected == Screen.HOME, onHome)
        NavItem(Icons.Outlined.StarBorder, "My List", selected == Screen.MY_LIST, onMyList)

        Box(
            Modifier.size(49.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Red, Orange))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.PlayArrow, "Play", tint = White, modifier = Modifier.size(28.dp))
        }

        NavItem(Icons.Outlined.Download, "Downloads", selected == Screen.DOWNLOADS, onDownloads)
        NavItem(Icons.Outlined.Settings, "Settings", selected == Screen.SETTINGS, onSettings)
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        Modifier.clickable { onClick() }.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            label,
            tint = if (selected) Red else Grey,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.height(1.dp))
        Text(
            label,
            color = if (selected) White else Grey,
            fontSize = 8.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
