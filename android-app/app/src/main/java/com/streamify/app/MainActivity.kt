package com.streamify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch

private const val TMDB_IMAGE = "https://image.tmdb.org/t/p/"

private val StreamifyBlack = Color(0xFF080808)
private val StreamifyCard = Color(0xFF171717)
private val StreamifyRed = Color(0xFFFF3B30)
private val StreamifyOrange = Color(0xFFFF6A00)
private val StreamifyGold = Color(0xFFFFD166)
private val StreamifyWhite = Color(0xFFF7F7F7)
private val StreamifyGrey = Color(0xFF9A9A9A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StreamifyApp()
            }
        }
    }
}

private enum class Screen {
    HOME, SEARCH, DETAIL, MY_LIST, DOWNLOADS, SETTINGS
}

private enum class HomeCategory(val title: String) {
    TRENDING("Trending"),
    MOVIES("Movies"),
    TV("TV"),
    DRAMA("Drama"),
    ANIME("Anime")
}

private enum class SubCategory(val title: String) {
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
    var selectedItem by remember { mutableStateOf<TmdbItem?>(null) }
    var myList by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var downloads by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }

    BackHandler(enabled = screen != Screen.HOME) {
        screen = Screen.HOME
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamifyBlack)
    ) {
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
                onItemClick = {
                    selectedItem = it
                    screen = Screen.DETAIL
                },
                onMyList = { screen = Screen.MY_LIST },
                onDownloads = { screen = Screen.DOWNLOADS },
                onSettings = { screen = Screen.SETTINGS }
            )

            Screen.SEARCH -> SearchScreen(
                onBack = { screen = Screen.HOME },
                onItemClick = {
                    selectedItem = it
                    screen = Screen.DETAIL
                }
            )

            Screen.DETAIL -> {
                selectedItem?.let { item ->
                    DetailScreen(
                        item = item,
                        isInList = myList.any {
                            it.id == item.id && it.media_type == item.media_type
                        },
                        onBack = { screen = Screen.HOME },
                        onToggleList = {
                            val exists = myList.any {
                                it.id == item.id && it.media_type == item.media_type
                            }
                            myList = if (exists) {
                                myList.filterNot {
                                    it.id == item.id && it.media_type == item.media_type
                                }
                            } else {
                                myList + item
                            }
                        },
                        onDownload = {
                            if (downloads.none {
                                    it.id == item.id && it.media_type == item.media_type
                                }) {
                                downloads = downloads + item
                            }
                        },
                        onSimilarClick = {
                            selectedItem = it
                        }
                    )
                }
            }

            Screen.MY_LIST -> CollectionScreen(
                title = "My List",
                items = myList,
                emptyText = "Your watchlist is empty.",
                onBack = { screen = Screen.HOME },
                onClick = {
                    selectedItem = it
                    screen = Screen.DETAIL
                }
            )

            Screen.DOWNLOADS -> CollectionScreen(
                title = "Downloads",
                items = downloads,
                emptyText = "No downloads yet.",
                onBack = { screen = Screen.HOME },
                onClick = {
                    selectedItem = it
                    screen = Screen.DETAIL
                }
            )

            Screen.SETTINGS -> SettingsScreen {
                screen = Screen.HOME
            }
        }
    }
}

private fun subcategoriesFor(category: HomeCategory): List<SubCategory> {
    return when (category) {
        HomeCategory.TRENDING -> listOf(SubCategory.ALL)

        HomeCategory.MOVIES -> listOf(
            SubCategory.ALL,
            SubCategory.HOLLYWOOD,
            SubCategory.BOLLYWOOD,
            SubCategory.SOUTH,
            SubCategory.MULTI_AUDIO,
            SubCategory.HINDI_DUBBED
        )

        HomeCategory.TV -> listOf(
            SubCategory.ALL,
            SubCategory.WEB_SERIES,
            SubCategory.BOLLYWOOD_SERIES,
            SubCategory.TV_SHOWS
        )

        HomeCategory.ANIME -> listOf(
            SubCategory.ALL,
            SubCategory.ANIMATED,
            SubCategory.ANIME,
            SubCategory.CARTOON
        )

        HomeCategory.DRAMA -> listOf(
            SubCategory.ALL,
            SubCategory.KDRAMA,
            SubCategory.TURKISH,
            SubCategory.PAKISTANI
        )
    }
}

@Composable
private fun HomeScreen(
    category: HomeCategory,
    subcategory: SubCategory,
    onCategory: (HomeCategory) -> Unit,
    onSubcategory: (SubCategory) -> Unit,
    onSearch: () -> Unit,
    onItemClick: (TmdbItem) -> Unit,
    onMyList: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
    val repository = remember { TmdbRepository() }

    var items by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var heroIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(category, subcategory) {
        loading = true
        error = null

        try {
            items = when (category) {
                HomeCategory.TRENDING -> repository.getTrending()

                HomeCategory.MOVIES -> when (subcategory) {
                    SubCategory.HOLLYWOOD -> repository.getHollywoodMovies()
                    SubCategory.BOLLYWOOD -> repository.getBollywoodMovies()
                    SubCategory.SOUTH -> repository.getSouthMovies()
                    else -> repository.getMovies()
                }

                HomeCategory.TV -> when (subcategory) {
                    SubCategory.BOLLYWOOD_SERIES -> repository.getBollywoodSeries()
                    SubCategory.TV_SHOWS -> repository.getEnglishTvShows()
                    else -> repository.getTvShows()
                }

                HomeCategory.DRAMA -> when (subcategory) {
                    SubCategory.KDRAMA -> repository.getKDrama()
                    SubCategory.TURKISH -> repository.getTurkishDrama()
                    SubCategory.PAKISTANI -> repository.getPakistaniDrama()
                    else -> repository.getDrama()
                }

                HomeCategory.ANIME -> when (subcategory) {
                    SubCategory.ANIMATED -> repository.getAnimatedContent()
                    SubCategory.CARTOON -> repository.getCartoonShows()
                    else -> repository.getAnime()
                }
            }
        } catch (e: Exception) {
            items = emptyList()
            error = e.message ?: "Unable to load TMDB content."
        } finally {
            loading = false
            heroIndex = 0
        }
    }

    LaunchedEffect(items) {
        while (items.size > 1) {
            delay(3000)
            heroIndex = (heroIndex + 1) % minOf(items.size, 5)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                HomeTopBar(onSearch)
            }

            item {
                Spacer(Modifier.height(12.dp))

                when {
                    loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = StreamifyRed)
                        }
                    }

                    items.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = error ?: "No content available.",
                                color = StreamifyGrey
                            )
                        }
                    }

                    else -> {
                        HeroCarousel(
                            items = items.take(5),
                            selectedIndex = heroIndex,
                            onClick = onItemClick
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                CategoryPills(
                    selected = category,
                    onSelect = onCategory
                )
            }

            if (category != HomeCategory.TRENDING) {
                item {
                    Spacer(Modifier.height(10.dp))
                    SubcategoryPills(
                        list = subcategoriesFor(category),
                        selected = subcategory,
                        onSelect = onSubcategory
                    )
                }
            }

            if (!loading && items.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))

                    SectionHeader(
                        title = if (subcategory == SubCategory.ALL) {
                            category.title
                        } else {
                            subcategory.title
                        },
                        subtitle = "TMDB catalogue"
                    )

                    Spacer(Modifier.height(10.dp))

                    ContentRow(
                        items = items,
                        onClick = onItemClick
                    )
                }

                if (items.size > 5) {
                    item {
                        Spacer(Modifier.height(24.dp))

                        SectionHeader(
                            title = "More ${category.title}",
                            subtitle = "Discover more titles"
                        )

                        Spacer(Modifier.height(10.dp))

                        ContentRow(
                            items = items.drop(5),
                            onClick = onItemClick
                        )
                    }
                }
            }
        }

        BottomNavigationBar(
            selected = Screen.HOME,
            onHome = {},
            onMyList = onMyList,
            onDownloads = onDownloads,
            onSettings = onSettings
        )
    }
}

@Composable
private fun HomeTopBar(onSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.streamify_logo),
            contentDescription = "Streamify",
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(9.dp))

        Text(
            text = "STREAMIFY",
            color = StreamifyRed,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp
        )

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(StreamifyCard)
                .clickable { onSearch() }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = StreamifyGrey,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "Search movies, series, anime...",
                    color = StreamifyGrey,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(StreamifyCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Profile",
                tint = StreamifyGold,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun HeroCarousel(
    items: List<TmdbItem>,
    selectedIndex: Int,
    onClick: (TmdbItem) -> Unit
) {
    if (items.isEmpty()) return

    val item = items[selectedIndex.coerceIn(0, items.lastIndex)]
    val title = item.title ?: item.name ?: "Untitled"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(255.dp)
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick(item) }
    ) {
        AsyncImage(
            model = item.backdrop_path?.let {
                TMDB_IMAGE + "w780" + it
            },
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.25f),
                            StreamifyBlack.copy(alpha = 0.98f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Text(
                text = title,
                color = StreamifyWhite,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = "⭐ ${String.format("%.1f", item.vote_average ?: 0.0)}",
                color = StreamifyGold,
                fontSize = 13.sp
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(
                            width = if (index == selectedIndex) 18.dp else 6.dp,
                            height = 6.dp
                        )
                        .clip(CircleShape)
                        .background(
                            if (index == selectedIndex) {
                                StreamifyRed
                            } else {
                                StreamifyWhite.copy(alpha = 0.5f)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun CategoryPills(
    selected: HomeCategory,
    onSelect: (HomeCategory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HomeCategory.values().toList()) { category ->
            Pill(
                text = category.title,
                selected = selected == category,
                onClick = { onSelect(category) }
            )
        }
    }
}

@Composable
private fun SubcategoryPills(
    list: List<SubCategory>,
    selected: SubCategory,
    onSelect: (SubCategory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(list) { subcategory ->
            Pill(
                text = subcategory.title,
                selected = selected == subcategory,
                onClick = { onSelect(subcategory) }
            )
        }
    }
}

@Composable
private fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) StreamifyRed else StreamifyCard
            )
            .clickable { onClick() }
            .padding(
                horizontal = 14.dp,
                vertical = 8.dp
            )
    ) {
        Text(
            text = text,
            color = if (selected) StreamifyWhite else StreamifyGrey,
            fontSize = 12.sp,
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Medium
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = StreamifyWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = subtitle,
                color = StreamifyGrey,
                fontSize = 12.sp
            )
        }

        Text(
            text = "See All",
            color = StreamifyOrange,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ContentRow(
    items: List<TmdbItem>,
    onClick: (TmdbItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items) { item ->
            ContentCard(
                item = item,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun ContentCard(
    item: TmdbItem,
    onClick: (TmdbItem) -> Unit
) {
    val title = item.title ?: item.name ?: "Untitled"

    Column(
        modifier = Modifier
            .width(104.dp)
            .clickable { onClick(item) }
    ) {
        AsyncImage(
            model = item.poster_path?.let {
                TMDB_IMAGE + "w342" + it
            },
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = title,
            color = StreamifyWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = if (item.media_type == "tv") "TV" else "Movie",
            color = StreamifyGrey,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SearchScreen(
    onBack: () -> Unit,
    onItemClick: (TmdbItem) -> Unit
) {
    val repository = remember { TmdbRepository() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamifyBlack)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = StreamifyWhite
                )
            }

            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = {
                    Text(
                        "Search movies, series, anime...",
                        color = StreamifyGrey
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = StreamifyRed
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = StreamifyCard,
                    unfocusedContainerColor = StreamifyCard,
                    focusedTextColor = StreamifyWhite,
                    unfocusedTextColor = StreamifyWhite,
                    cursorColor = StreamifyRed,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(22.dp)
            )

            IconButton(
                onClick = {
                    loading = true
                    scope.launch {
                        results = try {
                            repository.search(query)
                        } catch (_: Exception) {
                            emptyList()
                        }
                        loading = false
                    }
                }
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = StreamifyWhite
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StreamifyRed)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results) { item ->
                    SearchResultCard(item) {
                        onItemClick(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: TmdbItem,
    onClick: () -> Unit
) {
    val title = item.title ?: item.name ?: "Untitled"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StreamifyCard)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        AsyncImage(
            model = item.poster_path?.let {
                TMDB_IMAGE + "w185" + it
            },
            contentDescription = title,
            modifier = Modifier
                .size(62.dp, 92.dp)
                .clip(RoundedCornerShape(9.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = StreamifyWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = if (item.media_type == "tv") "TV" else "Movie",
                color = StreamifyRed,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = item.overview ?: "No description available.",
                color = StreamifyGrey,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailScreen(
    item: TmdbItem,
    isInList: Boolean,
    onBack: () -> Unit,
    onToggleList: () -> Unit,
    onDownload: () -> Unit,
    onSimilarClick: (TmdbItem) -> Unit
) {
    val repository = remember { TmdbRepository() }

    var details by remember { mutableStateOf(item) }
    var similar by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }

    LaunchedEffect(item.id, item.media_type) {
        try {
            details = repository.getDetails(item)
            similar = repository.getSimilar(item)
        } catch (_: Exception) {
        }
    }

    val title = details.title ?: details.name ?: "Untitled"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamifyBlack),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
            ) {
                AsyncImage(
                    model = details.backdrop_path?.let {
                        TMDB_IMAGE + "w780" + it
                    },
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    StreamifyBlack
                                )
                            )
                        )
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = StreamifyWhite
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp)
            ) {
                Text(
                    text = title,
                    color = StreamifyWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "⭐ ${String.format("%.1f", details.vote_average ?: 0.0)}",
                    color = StreamifyGold,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton(
                        icon = Icons.Outlined.PlayArrow,
                        text = "Play",
                        primary = true
                    ) {
                        // PLAYBACK INTEGRATION SLOT.
                        // Connect the user's authorized playback source here later.
                    }

                    ActionButton(
                        icon = if (isInList) {
                            Icons.Outlined.Check
                        } else {
                            Icons.Outlined.StarBorder
                        },
                        text = if (isInList) "Added" else "My List",
                        primary = false,
                        onClick = onToggleList
                    )

                    ActionButton(
                        icon = Icons.Outlined.Download,
                        text = "Download",
                        primary = false,
                        onClick = onDownload
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = "About",
                    color = StreamifyWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = details.overview ?: "No description available.",
                    color = StreamifyGrey,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }

        if (similar.isNotEmpty()) {
            item {
                Spacer(Modifier.height(28.dp))

                SectionHeader(
                    title = "You May Also Like",
                    subtitle = "Similar titles"
                )

                Spacer(Modifier.height(10.dp))

                ContentRow(
                    items = similar,
                    onClick = onSimilarClick
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    text: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (primary) StreamifyRed else StreamifyCard
            )
            .clickable { onClick() }
            .padding(
                horizontal = 11.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = StreamifyWhite,
            modifier = Modifier.size(19.dp)
        )

        Spacer(Modifier.width(5.dp))

        Text(
            text = text,
            color = StreamifyWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CollectionScreen(
    title: String,
    items: List<TmdbItem>,
    emptyText: String,
    onBack: () -> Unit,
    onClick: (TmdbItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamifyBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = StreamifyWhite
                )
            }

            Text(
                text = title,
                color = StreamifyWhite,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyText,
                    color = StreamifyGrey
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { item ->
                    SearchResultCard(item) {
                        onClick(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamifyBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = StreamifyWhite
                )
            }

            Text(
                text = "Settings",
                color = StreamifyWhite,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
        }

        SettingRow(
            title = "Account",
            subtitle = "Login and subscription"
        )

        SettingRow(
            title = "Playback",
            subtitle = "Video quality and playback settings"
        )

        SettingRow(
            title = "Downloads",
            subtitle = "Manage downloaded content"
        )

        SettingRow(
            title = "About Streamify",
            subtitle = "App information"
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp
            )
    ) {
        Text(
            text = title,
            color = StreamifyWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = subtitle,
            color = StreamifyGrey,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun BottomNavigationBar(
    selected: Screen,
    onHome: () -> Unit,
    onMyList: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 18.dp,
                vertical = 8.dp
            )
            .clip(RoundedCornerShape(30.dp))
            .background(StreamifyCard.copy(alpha = 0.97f))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(
            icon = Icons.Outlined.Home,
            label = "Home",
            selected = selected == Screen.HOME,
            onClick = onHome
        )

        NavItem(
            icon = Icons.Outlined.StarBorder,
            label = "My List",
            selected = selected == Screen.MY_LIST,
            onClick = onMyList
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(StreamifyRed)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.PlayArrow,
                contentDescription = "Play",
                tint = StreamifyWhite,
                modifier = Modifier.size(27.dp)
            )
        }

        NavItem(
            icon = Icons.Outlined.Download,
            label = "Downloads",
            selected = selected == Screen.DOWNLOADS,
            onClick = onDownloads
        )

        NavItem(
            icon = Icons.Outlined.Settings,
            label = "Settings",
            selected = selected == Screen.SETTINGS,
            onClick = onSettings
        )
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
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) StreamifyRed else StreamifyGrey,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = label,
            color = if (selected) StreamifyWhite else StreamifyGrey,
            fontSize = 9.sp
        )
    }
}
