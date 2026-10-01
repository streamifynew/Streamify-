package com.streamify.app

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val StreamBlack = Color(0xFF080808)
private val StreamDark = Color(0xFF101010)
private val StreamCard = Color(0xFF191919)
private val StreamRed = Color(0xFFFF3B22)
private val StreamOrange = Color(0xFFFF7A00)
private val StreamGold = Color(0xFFFFD58A)
private val StreamText = Color(0xFFF5F5F5)
private val StreamMuted = Color(0xFF999999)

private const val POSTER_URL =
    "https://image.tmdb.org/t/p/w500"

private const val BACKDROP_URL =
    "https://image.tmdb.org/t/p/w1280"

private enum class Screen {
    HOME,
    SEARCH,
    DETAIL,
    MY_LIST,
    DOWNLOADS,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StreamifyTheme {
                StreamifyApp()
            }
        }
    }
}

@Composable
fun StreamifyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme {
        content()
    }
}

@Composable
fun StreamifyApp() {

    var screen by remember {
        mutableStateOf(Screen.HOME)
    }

    var selectedItem by remember {
        mutableStateOf<TmdbItem?>(null)
    }

    var myList by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    var downloads by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    fun openDetails(item: TmdbItem) {
        selectedItem = item
        screen = Screen.DETAIL
    }

    BackHandler(
        enabled = screen != Screen.HOME
    ) {
        screen = Screen.HOME
        selectedItem = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamBlack)
    ) {

        when (screen) {

            Screen.HOME -> {
                HomeScreen(
                    onSearch = {
                        screen = Screen.SEARCH
                    },
                    onProfile = {
                        screen = Screen.SETTINGS
                    },
                    onItemClick = ::openDetails,
                    onMyList = {
                        screen = Screen.MY_LIST
                    },
                    onDownloads = {
                        screen = Screen.DOWNLOADS
                    },
                    onSettings = {
                        screen = Screen.SETTINGS
                    }
                )
            }

            Screen.SEARCH -> {
                SearchScreen(
                    onBack = {
                        screen = Screen.HOME
                    },
                    onItemClick = ::openDetails
                )
            }

            Screen.DETAIL -> {

                selectedItem?.let { item ->

                    DetailScreen(
                        item = item,
                        isInMyList = myList.any {
                            it.id == item.id &&
                            it.media_type == item.media_type
                        },
                        onBack = {
                            screen = Screen.HOME
                            selectedItem = null
                        },
                        onAddToList = {

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
                                downloads =
                                    downloads + item
                            }

                            screen = Screen.DOWNLOADS
                        },
                        onSimilarClick = ::openDetails
                    )
                }
            }

            Screen.MY_LIST -> {

                CollectionScreen(
                    title = "My List",
                    items = myList,
                    emptyText =
                        "Your saved movies and shows will appear here.",
                    onBack = {
                        screen = Screen.HOME
                    },
                    onItemClick = ::openDetails
                )
            }

            Screen.DOWNLOADS -> {

                CollectionScreen(
                    title = "Downloads",
                    items = downloads,
                    emptyText =
                        "Your downloaded content will appear here.",
                    onBack = {
                        screen = Screen.HOME
                    },
                    onItemClick = ::openDetails
                )
            }

            Screen.SETTINGS -> {

                SettingsScreen(
                    onBack = {
                        screen = Screen.HOME
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    onSearch: () -> Unit,
    onProfile: () -> Unit,
    onItemClick: (TmdbItem) -> Unit,
    onMyList: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {

    var trending by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {

        try {
            trending = TmdbRepository().getTrending()
        } catch (e: Exception) {
            error = e.message ?: "Unable to load TMDB."
        }

        loading = false
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                top = 14.dp,
                bottom = 18.dp
            )
        ) {

            item {

                TopBar(
                    onSearch = onSearch,
                    onProfile = onProfile
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
            }

            item {
                HeroSection(
                    items = trending,
                    onClick = onItemClick
                )
            }

            item {
                Spacer(Modifier.height(18.dp))
            }

            item {
                CategoryPills()
            }

            item {
                Spacer(Modifier.height(25.dp))
            }

            when {

                loading -> {

                    item {
                        LoadingView()
                    }
                }

                error != null -> {

                    item {
                        ErrorView(error!!)
                    }
                }

                else -> {

                    item {
                        SectionHeader(
                            title = "Trending",
                            subtitle = "What's popular right now"
                        )
                    }

                    item {
                        Spacer(Modifier.height(12.dp))
                    }

                    item {
                        PosterRow(
                            items = trending,
                            onItemClick = onItemClick
                        )
                    }

                    item {
                        Spacer(Modifier.height(28.dp))
                    }

                    item {
                        SectionHeader(
                            title = "Movies",
                            subtitle = "Movies from TMDB"
                        )
                    }

                    item {
                        Spacer(Modifier.height(12.dp))
                    }

                    item {
                        PosterRow(
                            items = trending.filter {
                                it.media_type == "movie"
                            },
                            onItemClick = onItemClick
                        )
                    }

                    item {
                        Spacer(Modifier.height(28.dp))
                    }

                    item {
                        SectionHeader(
                            title = "TV",
                            subtitle = "Series and shows"
                        )
                    }

                    item {
                        Spacer(Modifier.height(12.dp))
                    }

                    item {
                        PosterRow(
                            items = trending.filter {
                                it.media_type == "tv"
                            },
                            onItemClick = onItemClick
                        )
                    }

                    item {
                        Spacer(Modifier.height(28.dp))
                    }

                    item {
                        SectionHeader(
                            title = "Drama",
                            subtitle = "Drama discovery"
                        )
                    }

                    item {
                        Spacer(Modifier.height(12.dp))
                    }

                    item {
                        PosterRow(
                            items = trending,
                            onItemClick = onItemClick
                        )
                    }

                    item {
                        Spacer(Modifier.height(28.dp))
                    }

                    item {
                        SectionHeader(
                            title = "Anime",
                            subtitle = "Anime discovery"
                        )
                    }

                    item {
                        Spacer(Modifier.height(12.dp))
                    }

                    item {
                        PosterRow(
                            items = trending,
                            onItemClick = onItemClick
                        )
                    }
                }
            }
        }

        BottomNavigation(
            onHome = {},
            onMyList = onMyList,
            onDownloads = onDownloads,
            onSettings = onSettings
        )
    }
}

@Composable
fun TopBar(
    onSearch: () -> Unit,
    onProfile: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = "🍿 STREAMIFY",
                color = StreamRed,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )

            Text(
                text = "Entertainment, your way.",
                color = StreamMuted,
                fontSize = 11.sp
            )
        }

        IconButton(
            onClick = onSearch
        ) {

            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White
            )
        }

        IconButton(
            onClick = onProfile
        ) {

            Box(
                modifier = Modifier
                    .size(35.dp)
                    .background(
                        StreamCard,
                        RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = StreamGold,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
fun HeroSection(
    items: List<TmdbItem>,
    onClick: (TmdbItem) -> Unit
) {

    val hero = items.firstOrNull {
        !it.backdrop_path.isNullOrBlank()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(StreamDark)
            .clickable {
                hero?.let(onClick)
            }
    ) {

        hero?.backdrop_path?.let {

            AsyncImage(
                model = BACKDROP_URL + it,
                contentDescription =
                    hero.title ?: hero.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xF5080808)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {

            Text(
                text = "TRENDING",
                color = StreamGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = hero?.title
                    ?: hero?.name
                    ?: "Discover something new",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = "Powered by TMDB",
                color = StreamMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun CategoryPills() {

    val categories = listOf(
        "🔥 Trending",
        "🎬 Movies",
        "📺 TV",
        "🎭 Drama",
        "🍥 Anime"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            )
            .padding(horizontal = 18.dp),
        horizontalArrangement =
            Arrangement.spacedBy(9.dp)
    ) {

        categories.forEachIndexed { index, category ->

            Surface(
                shape = RoundedCornerShape(50),
                color =
                    if (index == 0)
                        StreamRed
                    else
                        StreamCard
            ) {

                Text(
                    text = category,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight =
                        if (index == 0)
                            FontWeight.Bold
                        else
                            FontWeight.Normal,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    )
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String
) {

    Column(
        modifier = Modifier.padding(
            horizontal = 18.dp
        )
    ) {

        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = subtitle,
            color = StreamMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
fun PosterRow(
    items: List<TmdbItem>,
    onItemClick: (TmdbItem) -> Unit
) {

    val filtered = items
        .filter {
            !it.poster_path.isNullOrBlank()
        }
        .take(15)

    LazyRow(
        contentPadding = PaddingValues(
            horizontal = 18.dp
        ),
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        items(filtered) { item ->

            PosterCard(
                item = item,
                onClick = {
                    onItemClick(item)
                }
            )
        }
    }
}

@Composable
fun PosterCard(
    item: TmdbItem,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .width(122.dp)
            .clickable(onClick = onClick)
    ) {

        Box(
            modifier = Modifier
                .width(122.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(StreamCard)
        ) {

            item.poster_path?.let {

                AsyncImage(
                    model = POSTER_URL + it,
                    contentDescription =
                        item.title ?: item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if ((item.vote_average ?: 0.0) > 0) {

                Text(
                    text = String.format(
                        "%.1f",
                        item.vote_average
                    ),
                    color = StreamGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .background(
                            Color(0xDD111111),
                            RoundedCornerShape(7.dp)
                        )
                        .padding(
                            horizontal = 7.dp,
                            vertical = 4.dp
                        )
                )
            }
        }

        Spacer(Modifier.height(7.dp))

        Text(
            text = item.title
                ?: item.name
                ?: "Untitled",
            color = StreamText,
            fontSize = 12.sp,
            maxLines = 2
        )
    }
}

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onItemClick: (TmdbItem) -> Unit
) {

    var query by remember {
        mutableStateOf("")
    }

    var results by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var searched by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Search",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            singleLine = true,
            placeholder = {
                Text(
                    "Search movies, series, anime..."
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    searched = true
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StreamRed,
                unfocusedBorderColor = StreamCard,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLeadingIconColor = StreamRed,
                unfocusedLeadingIconColor = StreamMuted
            )
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                searched = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StreamRed
            )
        ) {
            Text("Search")
        }

        if (searched) {

            LaunchedEffect(query) {

                if (query.isBlank()) {
                    results = emptyList()
                    return@LaunchedEffect
                }

                loading = true

                try {
                    results =
                        TmdbRepository().search(query)
                } finally {
                    loading = false
                }
            }
        }

        Spacer(Modifier.height(15.dp))

        when {

            loading -> {
                LoadingView()
            }

            searched && results.isEmpty() -> {

                Text(
                    text = "No results found.",
                    color = StreamMuted,
                    modifier = Modifier.padding(
                        horizontal = 18.dp
                    )
                )
            }

            else -> {

                LazyColumn(
                    contentPadding =
                        PaddingValues(
                            horizontal = 18.dp,
                            vertical = 8.dp
                        ),
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
}

@Composable
fun SearchResultCard(
    item: TmdbItem,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StreamCard)
            .clickable(onClick = onClick)
            .padding(9.dp)
    ) {

        AsyncImage(
            model = POSTER_URL +
                    (item.poster_path ?: ""),
            contentDescription =
                item.title ?: item.name,
            modifier = Modifier
                .width(75.dp)
                .height(105.dp)
                .clip(RoundedCornerShape(9.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 5.dp)
        ) {

            Text(
                text = item.title
                    ?: item.name
                    ?: "Untitled",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text =
                    if (item.media_type == "tv")
                        "TV Series"
                    else
                        "Movie",
                color = StreamRed,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = item.overview
                    ?: "No description available.",
                color = StreamMuted,
                fontSize = 11.sp,
                maxLines = 4
            )
        }
    }
}

@Composable
fun DetailScreen(
    item: TmdbItem,
    isInMyList: Boolean,
    onBack: () -> Unit,
    onAddToList: () -> Unit,
    onDownload: () -> Unit,
    onSimilarClick: (TmdbItem) -> Unit
) {

    var details by remember {
        mutableStateOf(item)
    }

    var similar by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    LaunchedEffect(item.id, item.media_type) {

        try {

            val repository = TmdbRepository()

            details =
                repository.getDetails(item)

            similar =
                repository.getSimilar(item)
                    .take(10)

        } catch (_: Exception) {
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 35.dp)
    ) {

        item {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
            ) {

                details.backdrop_path?.let {

                    AsyncImage(
                        model = BACKDROP_URL + it,
                        contentDescription =
                            details.title ?: details.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    StreamBlack
                                )
                            )
                        )
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                ) {

                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }

        item {

            Column(
                modifier = Modifier.padding(
                    horizontal = 18.dp
                )
            ) {

                Text(
                    text = details.title
                        ?: details.name
                        ?: "Untitled",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text =
                        if (details.media_type == "tv")
                            "TV Series"
                        else
                            "Movie",
                    color = StreamRed,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(15.dp))

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    Button(
                        onClick = {
                            // Playback source will be connected here.
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StreamRed
                        )
                    ) {

                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(5.dp))

                        Text("Play")
                    }

                    Button(
                        onClick = onAddToList,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StreamCard
                        )
                    ) {

                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint =
                                if (isInMyList)
                                    StreamGold
                                else
                                    Color.White
                        )

                        Spacer(Modifier.width(5.dp))

                        Text(
                            if (isInMyList)
                                "Saved"
                            else
                                "My List"
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StreamCard
                        )
                    ) {

                        Icon(
                            Icons.Default.Download,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(5.dp))

                        Text("Download")
                    }

                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StreamCard
                        )
                    ) {

                        Icon(
                            Icons.Default.Share,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(5.dp))

                        Text("Share")
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "About",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = details.overview
                        ?: "No description available.",
                    color = StreamMuted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(25.dp))
            }
        }

        if (similar.isNotEmpty()) {

            item {

                Text(
                    text = "Similar",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 18.dp
                    )
                )

                Spacer(Modifier.height(12.dp))
            }

            item {

                PosterRow(
                    items = similar,
                    onItemClick = onSimilarClick
                )
            }
        }
    }
}

@Composable
fun CollectionScreen(
    title: String,
    items: List<TmdbItem>,
    emptyText: String,
    onBack: () -> Unit,
    onItemClick: (TmdbItem) -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
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
                    color = StreamMuted,
                    fontSize = 13.sp
                )
            }

        } else {

            LazyColumn(
                contentPadding =
                    PaddingValues(18.dp),
                verticalArrangement =
                    Arrangement.spacedBy(15.dp)
            ) {

                items(items.chunked(2)) { rowItems ->

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(15.dp)
                    ) {

                        rowItems.forEach { item ->

                            PosterCard(
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
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        SettingRow(
            title = "Account",
            subtitle = "Login and subscription"
        )

        SettingRow(
            title = "Playback",
            subtitle = "Quality and player settings"
        )

        SettingRow(
            title = "Downloads",
            subtitle = "Manage downloaded content"
        )

        SettingRow(
            title = "Notifications",
            subtitle = "Streamify notifications"
        )

        SettingRow(
            title = "About Streamify",
            subtitle = "App information and TMDB attribution"
        )
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(
                horizontal = 20.dp,
                vertical = 18.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = subtitle,
                color = StreamMuted,
                fontSize = 11.sp
            )
        }

        Icon(
            Icons.Default.Settings,
            contentDescription = null,
            tint = StreamMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun BottomNavigation(
    onHome: () -> Unit,
    onMyList: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 18.dp,
                vertical = 10.dp
            ),
        shape = RoundedCornerShape(25.dp),
        color = Color(0xEE191919)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            horizontalArrangement =
                Arrangement.SpaceEvenly
        ) {

            NavItem(
                icon = Icons.Default.Home,
                label = "Home",
                onClick = onHome,
                selected = true
            )

            NavItem(
                icon = Icons.Default.Star,
                label = "My List",
                onClick = onMyList
            )

            NavItem(
                icon = Icons.Default.Download,
                label = "Downloads",
                onClick = onDownloads
            )

            NavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                onClick = onSettings
            )
        }
    }
}

@Composable
fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false
) {

    Column(
        modifier = Modifier.clickable(
            onClick = onClick
        ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint =
                if (selected)
                    StreamRed
                else
                    StreamMuted,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = label,
            color =
                if (selected)
                    Color.White
                else
                    StreamMuted,
            fontSize = 9.sp
        )
    }
}

@Composable
fun LoadingView() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {

        CircularProgressIndicator(
            color = StreamRed
        )
    }
}

@Composable
fun ErrorView(
    message: String
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = message,
            color = StreamMuted,
            fontSize = 12.sp
        )
    }
}
