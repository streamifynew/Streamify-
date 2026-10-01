package com.streamify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

private enum class StreamifyScreen {
    HOME,
    SEARCH,
    DETAIL,
    MY_LIST,
    DOWNLOADS,
    SETTINGS
}

private enum class HomeCategory(
    val title: String
) {
    TRENDING("Trending"),
    MOVIES("Movies"),
    TV("TV"),
    DRAMA("Drama"),
    ANIME("Anime")
}

@Composable
private fun StreamifyApp() {

    var screen by remember {
        mutableStateOf(StreamifyScreen.HOME)
    }

    var selectedCategory by remember {
        mutableStateOf(HomeCategory.TRENDING)
    }

    var selectedItem by remember {
        mutableStateOf<TmdbItem?>(null)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var myList by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    var downloads by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    BackHandler(
        enabled = screen != StreamifyScreen.HOME
    ) {
        screen = StreamifyScreen.HOME
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamifyBlack)
    ) {

        when (screen) {

            StreamifyScreen.HOME -> {

                HomeScreen(
                    selectedCategory = selectedCategory,
                    onCategorySelected = {
                        selectedCategory = it
                    },
                    onSearch = {
                        searchQuery = ""
                        screen = StreamifyScreen.SEARCH
                    },
                    onItemClick = {
                        selectedItem = it
                        screen = StreamifyScreen.DETAIL
                    },
                    onMyList = {
                        screen = StreamifyScreen.MY_LIST
                    },
                    onDownloads = {
                        screen = StreamifyScreen.DOWNLOADS
                    },
                    onSettings = {
                        screen = StreamifyScreen.SETTINGS
                    }
                )
            }

            StreamifyScreen.SEARCH -> {

                SearchScreen(
                    initialQuery = searchQuery,
                    onBack = {
                        screen = StreamifyScreen.HOME
                    },
                    onItemClick = {
                        selectedItem = it
                        screen = StreamifyScreen.DETAIL
                    }
                )
            }

            StreamifyScreen.DETAIL -> {

                selectedItem?.let { item ->

                    DetailScreen(
                        item = item,
                        isInList = myList.any {
                            it.id == item.id &&
                                    it.media_type == item.media_type
                        },
                        onBack = {
                            screen = StreamifyScreen.HOME
                        },
                        onToggleList = {

                            val exists = myList.any {
                                it.id == item.id &&
                                        it.media_type == item.media_type
                            }

                            myList =
                                if (exists) {
                                    myList.filterNot {
                                        it.id == item.id &&
                                                it.media_type == item.media_type
                                    }
                                } else {
                                    myList + item
                                }
                        },
                        onDownload = {

                            if (
                                downloads.none {
                                    it.id == item.id &&
                                            it.media_type == item.media_type
                                }
                            ) {
                                downloads = downloads + item
                            }
                        },
                        onSimilarClick = {
                            selectedItem = it
                        }
                    )
                }
            }

            StreamifyScreen.MY_LIST -> {

                SimpleCollectionScreen(
                    title = "My List",
                    items = myList,
                    emptyText = "Your watchlist is empty.",
                    onBack = {
                        screen = StreamifyScreen.HOME
                    },
                    onItemClick = {
                        selectedItem = it
                        screen = StreamifyScreen.DETAIL
                    }
                )
            }

            StreamifyScreen.DOWNLOADS -> {

                SimpleCollectionScreen(
                    title = "Downloads",
                    items = downloads,
                    emptyText = "No downloads yet.",
                    onBack = {
                        screen = StreamifyScreen.HOME
                    },
                    onItemClick = {
                        selectedItem = it
                        screen = StreamifyScreen.DETAIL
                    }
                )
            }

            StreamifyScreen.SETTINGS -> {

                SettingsScreen(
                    onBack = {
                        screen = StreamifyScreen.HOME
                    }
                )
            }
        }
    }
}


// ============================================================
// HOME
// ============================================================

@Composable
private fun HomeScreen(
    selectedCategory: HomeCategory,
    onCategorySelected: (HomeCategory) -> Unit,
    onSearch: () -> Unit,
    onItemClick: (TmdbItem) -> Unit,
    onMyList: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {

    val repository = remember {
        TmdbRepository()
    }

    var items by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var heroIndex by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(selectedCategory) {

        isLoading = true
        error = null

        try {

            items = when (selectedCategory) {

                HomeCategory.TRENDING ->
                    repository.getTrending()

                HomeCategory.MOVIES ->
                    repository.getMovies()

                HomeCategory.TV ->
                    repository.getTvShows()

                HomeCategory.DRAMA ->
                    repository.getDrama()

                HomeCategory.ANIME ->
                    repository.getAnime()
            }

        } catch (e: Exception) {

            error = e.message ?: "Unable to load content."

        } finally {

            isLoading = false
            heroIndex = 0
        }
    }

    LaunchedEffect(items) {

        while (items.size > 1) {

            delay(3000)

            heroIndex =
                (heroIndex + 1) % minOf(
                    items.size,
                    5
                )
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
            contentPadding = PaddingValues(
                bottom = 110.dp
            )
        ) {

            item {

                HomeTopBar(
                    onSearch = onSearch
                )
            }

            item {

                Spacer(
                    modifier = Modifier.height(18.dp)
                )
            }

            item {

                if (isLoading) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(330.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = StreamifyRed
                        )
                    }

                } else if (
                    error != null ||
                    items.isEmpty()
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(330.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = error
                                ?: "No content available.",
                            color = StreamifyGrey,
                            fontSize = 15.sp
                        )
                    }

                } else {

                    HeroCarousel(
                        items = items.take(5),
                        selectedIndex = heroIndex,
                        onItemClick = onItemClick
                    )
                }
            }

            item {

                Spacer(
                    modifier = Modifier.height(22.dp)
                )
            }

            item {

                CategoryPills(
                    selectedCategory = selectedCategory,
                    onCategorySelected = onCategorySelected
                )
            }

            if (!isLoading && items.isNotEmpty()) {

                item {

                    Spacer(
                        modifier = Modifier.height(28.dp)
                    )

                    SectionHeader(
                        title = selectedCategory.title,
                        subtitle = when (selectedCategory) {

                            HomeCategory.TRENDING ->
                                "What's popular right now"

                            HomeCategory.MOVIES ->
                                "Popular movies"

                            HomeCategory.TV ->
                                "Popular TV shows"

                            HomeCategory.DRAMA ->
                                "Popular drama"

                            HomeCategory.ANIME ->
                                "Animation & anime"
                        }
                    )
                }

                item {

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    ContentRow(
                        items = items,
                        onItemClick = onItemClick
                    )
                }

                item {

                    Spacer(
                        modifier = Modifier.height(30.dp)
                    )
                }

                item {

                    SectionHeader(
                        title = "More ${selectedCategory.title}",
                        subtitle = "Discover more titles"
                    )
                }

                item {

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    ContentRow(
                        items = items.drop(5),
                        onItemClick = onItemClick
                    )
                }
            }
        }

        BottomNavigationBar(
            selected = StreamifyScreen.HOME,
            onHome = {},
            onMyList = onMyList,
            onDownloads = onDownloads,
            onSettings = onSettings
        )
    }
}


// ============================================================
// TOP BAR
// ============================================================

@Composable
private fun HomeTopBar(
    onSearch: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 18.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {

            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    id = com.streamify.app.R.drawable.streamify_logo
                ),
                contentDescription = "Streamify",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Text(
            text = "STREAMIFY",
            color = StreamifyRed,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )

        Spacer(
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onSearch
        ) {

            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = StreamifyWhite,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(
            modifier = Modifier.width(4.dp)
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(StreamifyCard),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Profile",
                tint = StreamifyGold,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}


// ============================================================
// HERO
// ============================================================

@Composable
private fun HeroCarousel(
    items: List<TmdbItem>,
    selectedIndex: Int,
    onItemClick: (TmdbItem) -> Unit
) {

    if (items.isEmpty()) {
        return
    }

    val safeIndex =
        selectedIndex.coerceIn(
            0,
            items.lastIndex
        )

    val item = items[safeIndex]

    val title =
        item.title
            ?: item.name
            ?: "Untitled"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                onItemClick(item)
            }
    ) {

        AsyncImage(
            model =
                if (item.backdrop_path != null) {
                    TMDB_IMAGE +
                            "w780" +
                            item.backdrop_path
                } else {
                    null
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
                .padding(22.dp)
        ) {

            Text(
                text = title,
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "⭐ ${
                        String.format(
                            "%.1f",
                            item.vote_average ?: 0.0
                        )
                    }",
                color = StreamifyGold,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = item.overview ?: "",
                color = StreamifyWhite,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// ============================================================
// CATEGORY PILLS
// ============================================================

@Composable
private fun CategoryPills(
    selectedCategory: HomeCategory,
    onCategorySelected: (HomeCategory) -> Unit
) {

    LazyRow(
        contentPadding = PaddingValues(
            horizontal = 20.dp
        ),
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        items(HomeCategory.values().toList()) { category ->

            val selected =
                category == selectedCategory

            Text(
                text = category.title,
                color =
                    if (selected)
                        Color.White
                    else
                        StreamifyGrey,
                fontSize = 14.sp,
                fontWeight =
                    if (selected)
                        FontWeight.Bold
                    else
                        FontWeight.Normal,
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(50.dp)
                    )
                    .background(
                        if (selected)
                            StreamifyRed
                        else
                            StreamifyCard
                    )
                    .clickable {
                        onCategorySelected(category)
                    }
                    .padding(
                        horizontal = 18.dp,
                        vertical = 10.dp
                    )
            )
        }
    }
}


// ============================================================
// SECTION HEADER
// ============================================================

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {

    Column(
        modifier = Modifier.padding(
            horizontal = 20.dp
        )
    ) {

        Text(
            text = title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(3.dp)
        )

        Text(
            text = subtitle,
            color = StreamifyGrey,
            fontSize = 12.sp
        )
    }
}


// ============================================================
// CONTENT ROW
// ============================================================

@Composable
private fun ContentRow(
    items: List<TmdbItem>,
    onItemClick: (TmdbItem) -> Unit
) {

    LazyRow(
        contentPadding = PaddingValues(
            horizontal = 20.dp
        ),
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        items(items) { item ->

            ContentCard(
                item = item,
                onClick = {
                    onItemClick(item)
                }
            )
        }
    }
}


// ============================================================
// CONTENT CARD
// ============================================================

@Composable
private fun ContentCard(
    item: TmdbItem,
    onClick: () -> Unit
) {

    val title =
        item.title
            ?: item.name
            ?: "Untitled"

    Column(
        modifier = Modifier
            .width(145.dp)
            .clickable {
                onClick()
            }
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(StreamifyCard)
        ) {

            AsyncImage(
                model =
                    if (item.poster_path != null) {
                        TMDB_IMAGE +
                                "w342" +
                                item.poster_path
                    } else {
                        null
                    },
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(
                        RoundedCornerShape(8.dp)
                    )
                    .background(
                        Color.Black.copy(
                            alpha = 0.75f
                        )
                    )
                    .padding(
                        horizontal = 7.dp,
                        vertical = 4.dp
                    )
            ) {

                Text(
                    text =
                        String.format(
                            "%.1f",
                            item.vote_average ?: 0.0
                        ),
                    color = StreamifyGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}


// ============================================================
// SEARCH
// ============================================================

@Composable
private fun SearchScreen(
    initialQuery: String,
    onBack: () -> Unit,
    onItemClick: (TmdbItem) -> Unit
) {

    val repository = remember {
        TmdbRepository()
    }

    var query by remember {
        mutableStateOf(initialQuery)
    }

    var results by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamifyBlack)
            .padding(
                horizontal = 16.dp
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 16.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            TextField(
                value = query,
                onValueChange = {
                    query = it
                },
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                placeholder = {
                    Text(
                        text = "Search movies & shows",
                        color = StreamifyGrey
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor =
                        StreamifyCard,
                    unfocusedContainerColor =
                        StreamifyCard,
                    focusedTextColor =
                        Color.White,
                    unfocusedTextColor =
                        Color.White,
                    focusedIndicatorColor =
                        Color.Transparent,
                    unfocusedIndicatorColor =
                        Color.Transparent
                ),
                shape =
                    RoundedCornerShape(18.dp)
            )

            IconButton(
                onClick = {

                    if (query.isBlank()) {
                        return@IconButton
                    }

                    loading = true

                    kotlinx.coroutines.CoroutineScope(
                        kotlinx.coroutines.Dispatchers.Main
                    ).launch {

                        results =
                            try {
                                repository.search(
                                    query.trim()
                                )
                            } catch (_: Exception) {
                                emptyList()
                            }

                        loading = false
                    }
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = StreamifyRed
                )
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        if (loading) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator(
                    color = StreamifyRed
                )
            }

        } else if (results.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Search for a movie or TV show.",
                    color = StreamifyGrey
                )
            }

        } else {

            LazyColumn(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                items(results) { item ->

                    SearchResultCard(
                        item = item,
                        onClick = {
                            onItemClick(item)
                        }
                    )
                }
            }
        }
    }
}


// ============================================================
// SEARCH RESULT
// ============================================================

@Composable
private fun SearchResultCard(
    item: TmdbItem,
    onClick: () -> Unit
) {

    val title =
        item.title
            ?: item.name
            ?: "Untitled"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StreamifyCard)
            .clickable {
                onClick()
            }
            .padding(10.dp)
    ) {

        AsyncImage(
            model =
                if (item.poster_path != null) {
                    TMDB_IMAGE +
                            "w185" +
                            item.poster_path
                } else {
                    null
                },
            contentDescription = title,
            modifier = Modifier
                .size(
                    width = 70.dp,
                    height = 100.dp
                )
                .clip(
                    RoundedCornerShape(12.dp)
                ),
            contentScale = ContentScale.Crop
        )

        Spacer(
            modifier = Modifier.width(14.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    if (item.media_type == "tv")
                        "TV"
                    else
                        "Movie",
                color = StreamifyRed,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    item.overview
                        ?: "No description available.",
                color = StreamifyGrey,
                fontSize = 13.sp,
                maxLines = 3,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}


// ============================================================
// DETAIL
// ============================================================

@Composable
private fun DetailScreen(
    item: TmdbItem,
    isInList: Boolean,
    onBack: () -> Unit,
    onToggleList: () -> Unit,
    onDownload: () -> Unit,
    onSimilarClick: (TmdbItem) -> Unit
) {

    val repository = remember {
        TmdbRepository()
    }

    var details by remember {
        mutableStateOf(item)
    }

    var similar by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    LaunchedEffect(
        item.id,
        item.media_type
    ) {

        try {

            details =
                repository.getDetails(item)

            similar =
                repository.getSimilar(item)

        } catch (_: Exception) {
            // Keep original TMDB item.
        }
    }

    val title =
        details.title
            ?: details.name
            ?: "Untitled"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamifyBlack),
        contentPadding = PaddingValues(
            bottom = 40.dp
        )
    ) {

        item {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {

                AsyncImage(
                    model =
                        if (details.backdrop_path != null) {
                            TMDB_IMAGE +
                                    "w780" +
                                    details.backdrop_path
                        } else {
                            null
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
                                    Color.Black.copy(
                                        alpha = 0.10f
                                    ),
                                    StreamifyBlack
                                )
                            )
                        )
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(
                        10.dp
                    )
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }

        item {

            Column(
                modifier = Modifier.padding(
                    horizontal = 20.dp
                )
            ) {

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text =
                        "⭐ ${
                            String.format(
                                "%.1f",
                                details.vote_average ?: 0.0
                            )
                        }",
                    color = StreamifyGold,
                    fontSize = 15.sp
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    DetailButton(
                        icon =
                            Icons.Outlined.PlayArrow,
                        text = "Play",
                        primary = true,
                        onClick = {
                            // Playback source slot.
                            // Will be connected later.
                        }
                    )

                    DetailButton(
                        icon =
                            if (isInList)
                                Icons.Outlined.Check
                            else
                                Icons.Outlined.StarBorder,
                        text =
                            if (isInList)
                                "Added"
                            else
                                "My List",
                        onClick =
                            onToggleList
                    )

                    DetailButton(
                        icon =
                            Icons.Outlined.Download,
                        text = "Download",
                        onClick =
                            onDownload
                    )
                }

                Spacer(
                    modifier = Modifier.height(26.dp)
                )

                Text(
                    text = "About",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text =
                        details.overview
                            ?: "No description available.",
                    color = StreamifyGrey,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }

        if (similar.isNotEmpty()) {

            item {

                Spacer(
                    modifier = Modifier.height(30.dp)
                )

                SectionHeader(
                    title = "You May Also Like",
                    subtitle = "Similar titles"
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                ContentRow(
                    items = similar,
                    onItemClick =
                        onSimilarClick
                )
            }
        }
    }
}


// ============================================================
// DETAIL BUTTON
// ============================================================

@Composable
private fun DetailButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false
) {

    Row(
        modifier = Modifier
            .clip(
                RoundedCornerShape(14.dp)
            )
            .background(
                if (primary)
                    StreamifyRed
                else
                    StreamifyCard
            )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 12.dp,
                vertical = 11.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )

        Spacer(
            modifier = Modifier.width(5.dp)
        )

        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


// ============================================================
// COLLECTION
// ============================================================

@Composable
private fun SimpleCollectionScreen(
    title: String,
    items: List<TmdbItem>,
    emptyText: String,
    onBack: () -> Unit,
    onItemClick: (TmdbItem) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamifyBlack)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (items.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = emptyText,
                    color = StreamifyGrey
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
    start = 12.dp,
    top = 0.dp,
    end = 12.dp,
    bottom = 30.dp
),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(items) { item ->

                    SearchResultCard(
                        item = item,
                        onClick = {
                            onItemClick(item)
                        }
                    )
                }
            }
        }
    }
}


// ============================================================
// SETTINGS
// ============================================================

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
                .padding(12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        SettingsRow(
            title = "Account",
            subtitle = "Login and subscription"
        )

        SettingsRow(
            title = "Playback",
            subtitle = "Video quality and playback settings"
        )

        SettingsRow(
            title = "Downloads",
            subtitle = "Manage downloaded content"
        )

        SettingsRow(
            title = "About Streamify",
            subtitle = "App information"
        )
    }
}


@Composable
private fun SettingsRow(
    title: String,
    subtitle: String
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {}
            .padding(
                horizontal = 22.dp,
                vertical = 18.dp
            )
    ) {

        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = subtitle,
            color = StreamifyGrey,
            fontSize = 13.sp
        )
    }
}


// ============================================================
// BOTTOM NAVIGATION
// ============================================================

@Composable
private fun BottomNavigationBar(
    selected: StreamifyScreen,
    onHome: () -> Unit,
    onMyList: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 10.dp
            )
            .clip(
                RoundedCornerShape(32.dp)
            )
            .background(
                StreamifyCard.copy(
                    alpha = 0.96f
                )
            )
            .padding(
                vertical = 12.dp
            ),
        horizontalArrangement =
            Arrangement.SpaceEvenly,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        BottomNavItem(
            icon = Icons.Outlined.Home,
            label = "Home",
            selected =
                selected == StreamifyScreen.HOME,
            onClick = onHome
        )

        BottomNavItem(
            icon = Icons.Outlined.StarBorder,
            label = "My List",
            selected =
                selected == StreamifyScreen.MY_LIST,
            onClick = onMyList
        )

        BottomNavItem(
            icon = Icons.Outlined.Download,
            label = "Downloads",
            selected =
                selected == StreamifyScreen.DOWNLOADS,
            onClick = onDownloads
        )

        BottomNavItem(
            icon = Icons.Outlined.Settings,
            label = "Settings",
            selected =
                selected == StreamifyScreen.SETTINGS,
            onClick = onSettings
        )
    }
}


@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 9.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint =
                if (selected)
                    StreamifyRed
                else
                    StreamifyGrey,
            modifier = Modifier.size(25.dp)
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = label,
            color =
                if (selected)
                    Color.White
                else
                    StreamifyGrey,
            fontSize = 11.sp
        )
    }
}
