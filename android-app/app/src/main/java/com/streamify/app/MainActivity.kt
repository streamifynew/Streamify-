package com.streamify.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.viewmodel.compose.viewModel
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

@androidx.compose.runtime.Composable
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

    val myList = remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    val downloads = remember {
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
                        isInList = myList.value.any {
                            it.id == item.id &&
                                    it.media_type == item.media_type
                        },
                        onBack = {
                            screen = StreamifyScreen.HOME
                        },
                        onToggleList = {

                            val exists = myList.value.any {
                                it.id == item.id &&
                                        it.media_type == item.media_type
                            }

                            myList.value =
                                if (exists) {
                                    myList.value.filterNot {
                                        it.id == item.id &&
                                                it.media_type == item.media_type
                                    }
                                } else {
                                    myList.value + item
                                }
                        },
                        onDownload = {

                            if (
                                downloads.value.none {
                                    it.id == item.id &&
                                            it.media_type == item.media_type
                                }
                            ) {
                                downloads.value =
                                    downloads.value + item
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
                    items = myList.value,
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
                    items = downloads.value,
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

@androidx.compose.runtime.Composable
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

    // --------------------------------------------------------
    // LOAD CATEGORY
    // --------------------------------------------------------

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

    // --------------------------------------------------------
    // HERO AUTO ROTATION
    // --------------------------------------------------------

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

        Column(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
        ) {

            // This outer horizontal scroll is intentionally
            // replaced below by a vertical content container.
        }

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
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
                            .height(360.dp),
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
                            .height(360.dp),
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

                    val heroItems =
                        items.take(5)

                    HeroCarousel(
                        items = heroItems,
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

            item {

                Spacer(
                    modifier = Modifier.height(26.dp)
                )
            }

            if (!isLoading && items.isNotEmpty()) {

                item {

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
                                "Drama collection"

                            HomeCategory.ANIME ->
                                "Animation & anime"
                        }
                    )
                }

                item {

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )
                }

                item {

                    ContentRow(
                        items = items,
                        onItemClick = onItemClick
                    )
                }

                item {

                    Spacer(
                        modifier = Modifier.height(28.dp)
                    )
                }

                if (selectedCategory == HomeCategory.MOVIES) {

                    item {

                        SectionHeader(
                            title = "More Movies",
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

                if (selectedCategory == HomeCategory.TV) {

                    item {

                        SectionHeader(
                            title = "More TV",
                            subtitle = "Discover more shows"
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

                if (selectedCategory == HomeCategory.DRAMA) {

                    item {

                        SectionHeader(
                            title = "Drama Picks",
                            subtitle = "Popular drama titles"
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

                if (selectedCategory == HomeCategory.ANIME) {

                    item {

                        SectionHeader(
                            title = "Animation Picks",
                            subtitle = "Popular animated titles"
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

@androidx.compose.runtime.Composable
private fun HomeTopBar(
    onSearch: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 22.dp,
                end = 22.dp,
                top = 18.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Streamify popcorn-style brand mark
        Box(
    modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(Color.Transparent),
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

@androidx.compose.runtime.Composable
private fun HeroCarousel(
    items: List<TmdbItem>,
    selectedIndex: Int,
    onItemClick: (TmdbItem) -> Unit
) {

    if (items.isEmpty()) return

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
            .height(365.dp)
            .padding(horizontal = 22.dp)
            .clip(RoundedCornerShape(30.dp))
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
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(26.dp)
        ) {

            Text(
                text = "TRENDING",
                color = StreamifyGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = title,
                color = Color.White,
                fontSize = 31.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Powered by TMDB",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row {

                repeat(
                    minOf(items.size, 5)
                ) { index ->

                    Box(
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .size(
                                width =
                                    if (index == safeIndex)
                                        22.dp
                                    else
                                        7.dp,
                                height = 7.dp
                            )
                            .clip(CircleShape)
                            .background(
                                if (index == safeIndex)
                                    StreamifyRed
                                else
                                    Color.White.copy(
                                        alpha = 0.45f
                                    )
                            )
                    )
                }
            }
        }
    }
}


// ============================================================
// CATEGORY PILLS
// ============================================================

@androidx.compose.runtime.Composable
private fun CategoryPills(
    selectedCategory: HomeCategory,
    onCategorySelected: (HomeCategory) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            )
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        HomeCategory.values().forEach { category ->

            val selected =
                selectedCategory == category

            Box(
                modifier = Modifier
                    .clip(CircleShape)
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
                        horizontal = 20.dp,
                        vertical = 13.dp
                    )
            ) {

                Text(
                    text = when (category) {

                        HomeCategory.TRENDING ->
                            "🔥 Trending"

                        HomeCategory.MOVIES ->
                            "🎬 Movies"

                        HomeCategory.TV ->
                            "📺 TV"

                        HomeCategory.DRAMA ->
                            "🎭 Drama"

                        HomeCategory.ANIME ->
                            "🎨 Anime"
                    },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


// ============================================================
// SECTION HEADER
// ============================================================

@androidx.compose.runtime.Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {

    Column(
        modifier = Modifier.padding(horizontal = 22.dp)
    ) {

        Text(
            text = title,
            color = Color.White,
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = subtitle,
            color = StreamifyGrey,
            fontSize = 14.sp
        )
    }
}


// ============================================================
// CONTENT ROW
// ============================================================

@androidx.compose.runtime.Composable
private fun ContentRow(
    items: List<TmdbItem>,
    onItemClick: (TmdbItem) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            )
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        items.take(15).forEach { item ->

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

@androidx.compose.runtime.Composable
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
            .width(160.dp)
            .clickable {
                onClick()
            }
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(235.dp)
                .clip(RoundedCornerShape(20.dp))
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
                    .padding(9.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Color.Black.copy(alpha = 0.75f)
                    )
                    .padding(
                        horizontal = 9.dp,
                        vertical = 7.dp
                    )
            ) {

                Text(
                    text =
                        String.format(
                            "%.1f",
                            item.vote_average ?: 0.0
                        ),
                    color = StreamifyGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


// ============================================================
// SEARCH
// ============================================================

@androidx.compose.runtime.Composable
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

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamifyBlack)
            .padding(horizontal = 20.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
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
                        "Search movies & shows",
                        color = StreamifyGrey
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = StreamifyCard,
                    unfocusedContainerColor = StreamifyCard,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(18.dp)
            )

            IconButton(
                onClick = {

                    scope.launch {

                        if (query.isNotBlank()) {

                            loading = true

                            results =
                                try {
                                    repository.search(query)
                                } catch (
                                    _: Exception
                                ) {
                                    emptyList()
                                }

                            loading = false
                        }
                    }
                }
            ) {

                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = StreamifyRed
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
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

        } else {

            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                items(
                    count = results.size
                ) { index ->

                    SearchResultCard(
                        item = results[index],
                        onClick = {
                            onItemClick(results[index])
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

@androidx.compose.runtime.Composable
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
                .clip(RoundedCornerShape(12.dp)),
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
                modifier = Modifier.height(7.dp)
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
                modifier = Modifier.height(7.dp)
            )

            Text(
                text =
                    item.overview
                        ?: "No description available.",
                color = StreamifyGrey,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// ============================================================
// DETAIL
// ============================================================

@androidx.compose.runtime.Composable
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

    LaunchedEffect(item.id, item.media_type) {

        try {

            details =
                repository.getDetails(item)

            similar =
                repository.getSimilar(item)

        } catch (_: Exception) {
            // Keep original item if detail request fails.
        }
    }

    val title =
        details.title
            ?: details.name
            ?: "Untitled"

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamifyBlack),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
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
                                    Color.Black.copy(alpha = 0.2f),
                                    StreamifyBlack
                                )
                            )
                        )
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(12.dp)
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
                modifier = Modifier
                    .padding(horizontal = 22.dp)
            ) {

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
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
                        Arrangement.spacedBy(10.dp)
                ) {

                    DetailButton(
                        icon = Icons.Outlined.PlayArrow,
                        text = "Play",
                        onClick = {
                            // Playback source intentionally
                            // remains unconnected.
                        },
                        primary = true
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
                        onClick = onToggleList
                    )

                    DetailButton(
                        icon = Icons.Outlined.Download,
                        text = "Download",
                        onClick = onDownload
                    )
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
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
                    modifier = Modifier.height(28.dp)
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
                    onItemClick = onSimilarClick
                )
            }
        }
    }
}


// ============================================================
// DETAIL BUTTON
// ============================================================

@androidx.compose.runtime.Composable
private fun DetailButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false
) {

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
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
                horizontal = 13.dp,
                vertical = 11.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )

        Spacer(
            modifier = Modifier.width(6.dp)
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

@androidx.compose.runtime.Composable
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
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
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
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = emptyText,
                    color = StreamifyGrey
                )
            }

        } else {

            androidx.compose.foundation.lazy.LazyColumn {

                items(items.size) { index ->

                    SearchResultCard(
                        item = items[index],
                        onClick = {
                            onItemClick(items[index])
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                }
            }
        }
    }
}


// ============================================================
// SETTINGS
// ============================================================

@androidx.compose.runtime.Composable
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
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
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


@androidx.compose.runtime.Composable
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
// BOTTOM NAV
// ============================================================

@androidx.compose.runtime.Composable
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
                horizontal = 22.dp,
                vertical = 12.dp
            )
            .clip(RoundedCornerShape(32.dp))
            .background(StreamifyCard)
            .padding(
                vertical = 13.dp
            ),
        horizontalArrangement =
            Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
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


@androidx.compose.runtime.Composable
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
                horizontal = 10.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint =
                if (selected)
                    StreamifyRed
                else
                    StreamifyGrey,
            modifier = Modifier.size(26.dp)
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
