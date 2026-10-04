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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamifyApp()
        }
    }
}

private enum class Screen {
    HOME, SEARCH, DETAIL, MY_LIST, DOWNLOADS, SETTINGS
}

private enum class HomeCategory(
    val label: String,
    val icon: ImageVector
) {
    TRENDING("Trending", Icons.Outlined.LocalFireDepartment),
    MOVIES("Movies", Icons.Outlined.Movie),
    TV("TV", Icons.Outlined.Tv),
    DRAMA("Drama", Icons.Outlined.TheaterComedy),
    ANIME("Anime", Icons.Outlined.Face)
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

private data class ContentRow(
    val title: String,
    val items: List<TmdbItem>
)

private fun subcategories(category: HomeCategory): List<SubCategory> = when (category) {
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

    HomeCategory.DRAMA -> listOf(
        SubCategory.ALL,
        SubCategory.KDRAMA,
        SubCategory.TURKISH,
        SubCategory.PAKISTANI
    )

    HomeCategory.ANIME -> listOf(
        SubCategory.ALL,
        SubCategory.ANIMATED,
        SubCategory.ANIME,
        SubCategory.CARTOON
    )
}

private suspend fun loadContent(
    repo: TmdbRepository,
    category: HomeCategory,
    subcategory: SubCategory
): List<TmdbItem> {
    return when (category) {
        HomeCategory.TRENDING -> repo.getTrending()

        HomeCategory.MOVIES -> when (subcategory) {
            SubCategory.HOLLYWOOD -> repo.getHollywoodMovies()
            SubCategory.BOLLYWOOD -> repo.getBollywoodMovies()
            SubCategory.SOUTH -> repo.getSouthMovies()
            SubCategory.MULTI_AUDIO,
            SubCategory.HINDI_DUBBED -> emptyList()
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
}

private fun TmdbItem.sameAs(other: TmdbItem): Boolean {
    return id == other.id && media_type == other.media_type
}

private fun List<TmdbItem>.containsSame(item: TmdbItem): Boolean {
    return any { it.sameAs(item) }
}

private fun uniqueItems(items: List<TmdbItem>): List<TmdbItem> {
    return items.distinctBy { "${it.media_type ?: ""}-${it.id}" }
}

@Composable
private fun StreamifyApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var category by remember { mutableStateOf(HomeCategory.TRENDING) }
    var selected by remember { mutableStateOf<TmdbItem?>(null) }

    var myList by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    var downloads by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    BackHandler(enabled = screen != Screen.HOME) {
        screen = Screen.HOME
    }

    val open: (TmdbItem) -> Unit = {
        selected = it
        screen = Screen.DETAIL
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        when (screen) {
            Screen.HOME -> {
                HomeScreen(
                    category = category,
                    onCategory = { category = it },
                    onSearch = { screen = Screen.SEARCH },
                    onOpen = open,
                    onMyList = { screen = Screen.MY_LIST },
                    onDownloads = { screen = Screen.DOWNLOADS },
                    onSettings = { screen = Screen.SETTINGS }
                )
            }

            Screen.SEARCH -> {
                SearchScreen(
                    onBack = { screen = Screen.HOME },
                    onOpen = open
                )
            }

            Screen.DETAIL -> {
                selected?.let { item ->
                    DetailScreen(
                        item = item,
                        inList = myList.containsSame(item),
                        onBack = { screen = Screen.HOME },
                        onToggleList = {
                            myList =
                                if (myList.containsSame(item)) {
                                    myList.filterNot { it.sameAs(item) }
                                } else {
                                    myList + item
                                }
                        },
                        onDownload = {
                            if (!downloads.containsSame(item)) {
                                downloads = downloads + item
                            }
                        },
                        onOpenSimilar = {
                            selected = it
                        }
                    )
                }
            }

            Screen.MY_LIST -> {
                CollectionScreen(
                    title = "My List",
                    items = myList,
                    emptyText = "Your watchlist is empty.",
                    onBack = { screen = Screen.HOME },
                    onOpen = open
                )
            }

            Screen.DOWNLOADS -> {
                CollectionScreen(
                    title = "Downloads",
                    items = downloads,
                    emptyText = "No downloads yet.",
                    onBack = { screen = Screen.HOME },
                    onOpen = open
                )
            }

            Screen.SETTINGS -> {
                SettingsScreen {
                    screen = Screen.HOME
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    category: HomeCategory,
    onCategory: (HomeCategory) -> Unit,
    onSearch: () -> Unit,
    onOpen: (TmdbItem) -> Unit,
    onMyList: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
    val repo = remember { TmdbRepository() }

    var hero by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    var heroIndex by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(Unit) {
        hero = try {
            uniqueItems(repo.getTrending()).take(5)
        } catch (_: Exception) {
            emptyList()
        }
    }

    LaunchedEffect(hero, heroIndex) {
        if (hero.size > 1) {
            delay(3000)
            heroIndex = (heroIndex + 1) % hero.size
        }
    }

    val sections = if (category == HomeCategory.TRENDING) {
        listOf(
            HomeCategory.TRENDING,
            HomeCategory.MOVIES,
            HomeCategory.TV,
            HomeCategory.DRAMA,
            HomeCategory.ANIME
        )
    } else {
        listOf(category)
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            item {
                TopBar(onSearch)
            }

            item {
                if (hero.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Red,
                            strokeWidth = 3.dp
                        )
                    }
                } else {
                    Hero(
                        item = hero[heroIndex.coerceIn(0, hero.lastIndex)],
                        dots = hero.size,
                        activeDot = heroIndex,
                        onOpen = onOpen,
                        onPrev = {
                            heroIndex =
                                (heroIndex - 1 + hero.size) % hero.size
                        },
                        onNext = {
                            heroIndex =
                                (heroIndex + 1) % hero.size
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(14.dp))

                CategoryPills(
                    selected = category,
                    onSelect = onCategory
                )
            }

            sections.forEach { currentCategory ->
                item(key = "category-${currentCategory.name}") {
                    CategorySection(
                        category = currentCategory,
                        onOpen = onOpen
                    )
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
private fun CategorySection(
    category: HomeCategory,
    onOpen: (TmdbItem) -> Unit
) {
    val repo = remember { TmdbRepository() }

    var subcategory by remember(category) {
        mutableStateOf(SubCategory.ALL)
    }

    var rows by remember(category) {
        mutableStateOf<List<ContentRow>>(emptyList())
    }

    var loading by remember(category) {
        mutableStateOf(true)
    }

    LaunchedEffect(category, subcategory) {
        loading = true

        rows = try {
            buildContentRows(
                repo = repo,
                category = category,
                subcategory = subcategory
            )
        } catch (_: Exception) {
            emptyList()
        }

        loading = false
    }

    Column(
        Modifier.padding(top = 20.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                tint = Red,
                modifier = Modifier.size(25.dp)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                category.label,
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(Modifier.height(10.dp))

        SubPills(
            options = subcategories(category),
            selected = subcategory,
            onSelect = { subcategory = it }
        )

        Spacer(Modifier.height(8.dp))

        when {
            loading -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Red,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            subcategory == SubCategory.MULTI_AUDIO ||
                    subcategory == SubCategory.HINDI_DUBBED -> {
                Text(
                    "Connect your playback source metadata to show ${subcategory.label} titles.",
                    color = Grey,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 14.dp
                    )
                )
            }

            rows.isEmpty() -> {
                Text(
                    "No content available.",
                    color = Grey,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 14.dp
                    )
                )
            }

            else -> {
                rows.forEach { row ->
                    if (row.items.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))

                        SectionTitle(row.title)

                        Spacer(Modifier.height(8.dp))

                        PosterRow(
                            items = row.items,
                            onOpen = onOpen
                        )
                    }
                }
            }
        }
    }
}

private suspend fun buildContentRows(
    repo: TmdbRepository,
    category: HomeCategory,
    subcategory: SubCategory
): List<ContentRow> {

    suspend fun movieRow(
        title: String,
        genre: Int,
        language: String? = null
    ): ContentRow {
        return ContentRow(
            title,
            uniqueItems(
                repo.getMovieGenre(
                    genreId = genre,
                    language = language
                )
            ).take(12)
        )
    }

    suspend fun tvRow(
        title: String,
        genre: Int,
        language: String? = null
    ): ContentRow {
        return ContentRow(
            title,
            uniqueItems(
                repo.getTvGenre(
                    genreId = genre,
                    language = language
                )
            ).take(12)
        )
    }

    return when (category) {

        HomeCategory.TRENDING -> {
            listOf(
                ContentRow(
                    "Trending Now",
                    uniqueItems(repo.getTrending()).take(12)
                )
            )
        }

        HomeCategory.MOVIES -> {
            when (subcategory) {

                SubCategory.HOLLYWOOD -> {
                    listOf(
                        ContentRow(
                            "Hollywood Popular",
                            uniqueItems(repo.getHollywoodMovies()).take(12)
                        ),
                        movieRow("Action", 28, "en"),
                        movieRow("Comedy", 35, "en"),
                        movieRow("Horror", 27, "en"),
                        movieRow("Sci-Fi", 878, "en"),
                        movieRow("Thriller", 53, "en"),
                        movieRow("Romance", 10749, "en")
                    )
                }

                SubCategory.BOLLYWOOD -> {
                    listOf(
                        ContentRow(
                            "Bollywood Popular",
                            uniqueItems(repo.getBollywoodMovies()).take(12)
                        ),
                        movieRow("Action", 28, "hi"),
                        movieRow("Comedy", 35, "hi"),
                        movieRow("Drama", 18, "hi"),
                        movieRow("Romance", 10749, "hi"),
                        movieRow("Thriller", 53, "hi")
                    )
                }

                SubCategory.SOUTH -> {
                    listOf(
                        ContentRow(
                            "South Cinema Popular",
                            uniqueItems(repo.getSouthMovies()).take(12)
                        ),
                        movieRow("Action", 28, "ta"),
                        movieRow("Drama", 18, "ta"),
                        movieRow("Comedy", 35, "ta"),
                        movieRow("Romance", 10749, "ta"),
                        movieRow("Thriller", 53, "te")
                    )
                }

                SubCategory.ALL -> {
                    listOf(
                        ContentRow(
                            "Popular Movies",
                            uniqueItems(repo.getMovies()).take(12)
                        ),
                        movieRow("Action", 28),
                        movieRow("Comedy", 35),
                        movieRow("Drama", 18),
                        movieRow("Horror", 27),
                        movieRow("Romance", 10749),
                        movieRow("Sci-Fi", 878)
                    )
                }

                SubCategory.MULTI_AUDIO,
                SubCategory.HINDI_DUBBED -> emptyList()
            }
        }

        HomeCategory.TV -> {
            when (subcategory) {

                SubCategory.BOLLYWOOD_SERIES -> {
                    listOf(
                        ContentRow(
                            "Hindi Series Popular",
                            uniqueItems(repo.getBollywoodSeries()).take(12)
                        ),
                        tvRow("Drama", 18, "hi"),
                        tvRow("Comedy", 35, "hi"),
                        tvRow("Crime", 80, "hi"),
                        tvRow("Action & Adventure", 10759, "hi"),
                        tvRow("Mystery", 9648, "hi")
                    )
                }

                SubCategory.TV_SHOWS -> {
                    listOf(
                        ContentRow(
                            "Popular TV Shows",
                            uniqueItems(repo.getEnglishTvShows()).take(12)
                        ),
                        tvRow("Drama", 18, "en"),
                        tvRow("Comedy", 35, "en"),
                        tvRow("Crime", 80, "en"),
                        tvRow("Action & Adventure", 10759, "en"),
                        tvRow("Sci-Fi & Fantasy", 10765, "en")
                    )
                }

                SubCategory.WEB_SERIES -> {
                    listOf(
                        ContentRow(
                            "Popular Web Series",
                            uniqueItems(repo.getTvShows()).take(12)
                        ),
                        tvRow("Drama", 18),
                        tvRow("Comedy", 35),
                        tvRow("Crime", 80),
                        tvRow("Action & Adventure", 10759),
                        tvRow("Sci-Fi & Fantasy", 10765),
                        tvRow("Mystery", 9648)
                    )
                }

                SubCategory.ALL -> {
                    listOf(
                        ContentRow(
                            "Popular TV",
                            uniqueItems(repo.getTvShows()).take(12)
                        ),
                        tvRow("Drama", 18),
                        tvRow("Comedy", 35),
                        tvRow("Crime", 80),
                        tvRow("Action & Adventure", 10759),
                        tvRow("Sci-Fi & Fantasy", 10765)
                    )
                }

                else -> emptyList()
            }
        }

        HomeCategory.DRAMA -> {
            when (subcategory) {

                SubCategory.KDRAMA -> {
                    listOf(
                        ContentRow(
                            "K-Drama Popular",
                            uniqueItems(repo.getKDrama()).take(12)
                        ),
                        tvRow("Romance", 10749, "ko"),
                        tvRow("Crime", 80, "ko"),
                        tvRow("Mystery", 9648, "ko"),
                        tvRow("Comedy", 35, "ko")
                    )
                }

                SubCategory.TURKISH -> {
                    listOf(
                        ContentRow(
                            "Turkish Drama Popular",
                            uniqueItems(repo.getTurkishDrama()).take(12)
                        ),
                        tvRow("Romance", 10749, "tr"),
                        tvRow("Crime", 80, "tr"),
                        tvRow("Action & Adventure", 10759, "tr"),
                        tvRow("Comedy", 35, "tr")
                    )
                }

                SubCategory.PAKISTANI -> {
                    listOf(
                        ContentRow(
                            "Pakistani Drama Popular",
                            uniqueItems(repo.getPakistaniDrama()).take(12)
                        ),
                        tvRow("Drama", 18, "ur"),
                        tvRow("Romance", 10749, "ur"),
                        tvRow("Crime", 80, "ur")
                    )
                }

                SubCategory.ALL -> {
                    listOf(
                        ContentRow(
                            "Drama Popular",
                            uniqueItems(repo.getDrama()).take(12)
                        ),
                        tvRow("Drama", 18),
                        tvRow("Crime", 80),
                        tvRow("Romance", 10749),
                        tvRow("Mystery", 9648)
                    )
                }

                else -> emptyList()
            }
        }

        HomeCategory.ANIME -> {
            when (subcategory) {

                SubCategory.ANIME -> {
                    listOf(
                        ContentRow(
                            "Anime Popular",
                            uniqueItems(repo.getAnime()).take(12)
                        ),
                        tvRow("Action & Adventure", 10759, "ja"),
                        tvRow("Comedy", 35, "ja"),
                        tvRow("Sci-Fi & Fantasy", 10765, "ja"),
                        tvRow("Mystery", 9648, "ja")
                    )
                }

                SubCategory.ANIMATED -> {
                    listOf(
                        ContentRow(
                            "Animated Popular",
                            uniqueItems(repo.getAnimatedContent()).take(12)
                        ),
                        movieRow("Animation", 16),
                        movieRow("Family", 10751),
                        movieRow("Comedy", 35),
                        movieRow("Fantasy", 14)
                    )
                }

                SubCategory.CARTOON -> {
                    listOf(
                        ContentRow(
                            "Cartoon Popular",
                            uniqueItems(repo.getCartoonShows()).take(12)
                        ),
                        tvRow("Comedy", 35, "en"),
                        tvRow("Family", 10751, "en"),
                        tvRow("Animation", 16, "en"),
                        tvRow("Action & Adventure", 10759, "en")
                    )
                }

                SubCategory.ALL -> {
                    listOf(
                        ContentRow(
                            "Anime Popular",
                            uniqueItems(repo.getAnime()).take(12)
                        ),
                        tvRow("Animation", 16, "ja"),
                        tvRow("Action & Adventure", 10759, "ja"),
                        tvRow("Comedy", 35, "ja"),
                        tvRow("Sci-Fi & Fantasy", 10765, "ja")
                    )
                }

                else -> emptyList()
            }
        }
    }
}

@Composable
private fun TopBar(onSearch: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = painterResource(R.drawable.streamify_logo),
            contentDescription = "Streamify",
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.width(8.dp))

        Text(
            "Streamify",
            color = White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.width(10.dp))

        Row(
            Modifier
                .weight(1f)
                .height(42.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Surface)
                .border(
                    1.dp,
                    Red.copy(alpha = .55f),
                    RoundedCornerShape(24.dp)
                )
                .clickable { onSearch() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Search,
                null,
                tint = White,
                modifier = Modifier.size(19.dp)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                "Search movies, series, anime...",
                color = Grey,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Surface2)
                .border(2.dp, Red, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Person,
                "Profile",
                tint = White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun Hero(
    item: TmdbItem,
    dots: Int,
    activeDot: Int,
    onOpen: (TmdbItem) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val title = item.title ?: item.name ?: "Untitled"

    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.dp,
                Red.copy(alpha = .65f),
                RoundedCornerShape(18.dp)
            )
            .clickable { onOpen(item) }
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
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Black.copy(alpha = .92f),
                            Black.copy(alpha = .38f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(
                    start = 28.dp,
                    end = 58.dp
                )
        ) {

            Box(
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Red)
                    .padding(
                        horizontal = 8.dp,
                        vertical = 3.dp
                    )
            ) {
                Text(
                    "TRENDING",
                    color = White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                title,
                color = White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "${if (item.media_type == "tv") "TV Series" else "Movie"}  •  ★ ${
                    String.format("%.1f", item.vote_average ?: 0.0)
                }",
                color = White.copy(alpha = .85f),
                fontSize = 10.sp
            )

            Spacer(Modifier.height(9.dp))

            Row(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(RedBrush)
                    .clickable { onOpen(item) }
                    .padding(
                        horizontal = 13.dp,
                        vertical = 7.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    null,
                    tint = White,
                    modifier = Modifier.size(17.dp)
                )

                Spacer(Modifier.width(4.dp))

                Text(
                    "Watch Now",
                    color = White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Icon(
            Icons.Outlined.KeyboardArrowLeft,
            "Previous",
            tint = White,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(30.dp)
                .clickable { onPrev() }
        )

        Icon(
            Icons.Outlined.KeyboardArrowRight,
            "Next",
            tint = White,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(30.dp)
                .clickable { onNext() }
        )

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(dots) { index ->
                Box(
                    Modifier
                        .size(
                            if (index == activeDot) 16.dp else 6.dp,
                            6.dp
                        )
                        .clip(CircleShape)
                        .background(
                            if (index == activeDot) {
                                Red
                            } else {
                                White.copy(alpha = .45f)
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
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HomeCategory.values().toList()) { category ->

            val isSelected = selected == category

            Row(
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isSelected) {
                            Red.copy(alpha = .16f)
                        } else {
                            Surface
                        }
                    )
                    .border(
                        1.dp,
                        if (isSelected) {
                            Red
                        } else {
                            White.copy(alpha = .20f)
                        },
                        RoundedCornerShape(24.dp)
                    )
                    .clickable {
                        onSelect(category)
                    }
                    .padding(
                        horizontal = 14.dp,
                        vertical = 9.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    category.icon,
                    null,
                    tint = if (isSelected) Red else White,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    category.label,
                    color = White,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    }
                )
            }
        }
    }
}

@Composable
private fun SubPills(
    options: List<SubCategory>,
    selected: SubCategory,
    onSelect: (SubCategory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(options) { subcategory ->

            val isSelected = selected == subcategory

            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) {
                            RedBrush
                        } else {
                            Surface
                        }
                    )
                    .border(
                        1.dp,
                        if (isSelected) {
                            Color.Transparent
                        } else {
                            White.copy(alpha = .20f)
                        },
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        onSelect(subcategory)
                    }
                    .padding(
                        horizontal = 11.dp,
                        vertical = 6.dp
                    )
            ) {
                Text(
                    subcategory.label,
                    color = White,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    },
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun PosterRow(
    items: List<TmdbItem>,
    onOpen: (TmdbItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(
            uniqueItems(items),
            key = {
                "${it.media_type ?: ""}-${it.id}"
            }
        ) {
            PosterCard(
                item = it,
                onOpen = onOpen
            )
        }
    }
}

@Composable
private fun PosterCard(
    item: TmdbItem,
    onOpen: (TmdbItem) -> Unit
) {
    val title = item.title ?: item.name ?: "Untitled"

    Box(
        Modifier
            .width(108.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(
                1.dp,
                Red.copy(alpha = .32f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onOpen(item) }
    ) {

        AsyncImage(
            model = item.poster_path?.let {
                TMDB_IMAGE + "w342" + it
            },
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Black.copy(alpha = .95f)
                        )
                    )
                )
        )

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(7.dp)
        ) {

            Text(
                title,
                color = White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "${if (item.media_type == "tv") "TV" else "Movie"} • ★ ${
                    String.format("%.1f", item.vote_average ?: 0.0)
                }",
                color = Grey,
                fontSize = 8.sp
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

    var query by remember {
        mutableStateOf("")
    }

    var results by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 14.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    "Back",
                    tint = White
                )
            }

            TextField(
                value = query,
                onValueChange = {
                    query = it
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = {
                    Text(
                        "Search movies, series, anime...",
                        color = Grey
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        null,
                        tint = Red
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    cursorColor = Red,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )

            IconButton(
                onClick = {
                    if (query.isNotBlank()) {
                        loading = true

                        scope.launch {
                            results = try {
                                repo.search(query)
                            } catch (_: Exception) {
                                emptyList()
                            }

                            loading = false
                        }
                    }
                }
            ) {
                Icon(
                    Icons.Outlined.Search,
                    "Search",
                    tint = White
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (loading) {

            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Red)
            }

        } else {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(
                    uniqueItems(results),
                    key = {
                        "${it.media_type ?: ""}-${it.id}"
                    }
                ) {
                    ListRow(
                        item = it,
                        onOpen = onOpen
                    )
                }
            }
        }
    }
}

@Composable
private fun ListRow(
    item: TmdbItem,
    onOpen: (TmdbItem) -> Unit
) {
    val title = item.title ?: item.name ?: "Untitled"

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(
                1.dp,
                Red.copy(alpha = .25f),
                RoundedCornerShape(14.dp)
            )
            .clickable {
                onOpen(item)
            }
            .padding(8.dp)
    ) {

        AsyncImage(
            model = item.poster_path?.let {
                TMDB_IMAGE + "w185" + it
            },
            contentDescription = title,
            modifier = Modifier
                .size(62.dp, 90.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(11.dp))

        Column(
            Modifier.weight(1f)
        ) {

            Text(
                title,
                color = White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )

            Spacer(Modifier.height(4.dp))

            Text(
                if (item.media_type == "tv") {
                    "TV Series"
                } else {
                    "Movie"
                },
                color = Red,
                fontSize = 10.sp
            )

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

    var details by remember {
        mutableStateOf(item)
    }

    var similar by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    LaunchedEffect(item.id, item.media_type) {
        details = item

        try {
            details = repo.getDetails(item)
            similar = uniqueItems(
                repo.getSimilar(item)
            ).take(12)
        } catch (_: Exception) {
        }
    }

    val title = details.title ?: details.name ?: "Untitled"

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Black),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {

        item {

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
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
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Black.copy(alpha = .25f),
                                    Black
                                )
                            )
                        )
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(
                        top = 8.dp,
                        start = 8.dp
                    )
                ) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        "Back",
                        tint = White
                    )
                }

                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(
                            Black.copy(alpha = .5f)
                        )
                        .border(
                            2.dp,
                            White,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        "Play",
                        tint = White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        item {

            Column(
                Modifier.padding(horizontal = 18.dp)
            ) {

                Text(
                    title,
                    color = White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(7.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Tag("HD")

                    Spacer(Modifier.width(6.dp))

                    Tag("13+")

                    Spacer(Modifier.width(10.dp))

                    Text(
                        "★ ${
                            String.format(
                                "%.1f",
                                details.vote_average ?: 0.0
                            )
                        }",
                        color = Gold,
                        fontSize = 11.sp
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        if (details.media_type == "tv") {
                            "TV Series"
                        } else {
                            "Movie"
                        },
                        color = Grey,
                        fontSize = 11.sp
                    )
                }

                if (!details.genres.isNullOrEmpty()) {

                    Spacer(Modifier.height(6.dp))

                    Text(
                        details.genres!!.joinToString("  •  ") {
                            it.name
                        },
                        color = Grey,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    Row(
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(RedBrush)
                            .clickable {
                                // PLAYBACK SOURCE INTEGRATION SLOT
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {

                        Icon(
                            Icons.Outlined.PlayArrow,
                            null,
                            tint = White,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(Modifier.width(4.dp))

                        Text(
                            "Play",
                            color = White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconAction(
                        icon = if (inList) {
                            Icons.Outlined.Check
                        } else {
                            Icons.Outlined.Add
                        },
                        label = if (inList) "Added" else "My List",
                        onClick = onToggleList
                    )

                    IconAction(
                        icon = Icons.Outlined.Download,
                        label = "Download",
                        onClick = onDownload
                    )

                    IconAction(
                        icon = Icons.Outlined.Share,
                        label = "Share",
                        onClick = {}
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "About",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    details.overview
                        ?: "No description available.",
                    color = Grey,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        if (similar.isNotEmpty()) {

            item {

                Spacer(Modifier.height(22.dp))

                SectionTitle("Because You Watched This")

                Spacer(Modifier.height(9.dp))

                PosterRow(
                    items = similar,
                    onOpen = onOpenSimilar
                )

                Spacer(Modifier.height(20.dp))

                SectionTitle("Similar to $title")

                Spacer(Modifier.height(9.dp))

                PosterRow(
                    items = similar.reversed(),
                    onOpen = onOpenSimilar
                )
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(
                1.dp,
                White.copy(alpha = .6f),
                RoundedCornerShape(4.dp)
            )
            .padding(
                horizontal = 5.dp,
                vertical = 1.dp
            )
    ) {
        Text(
            text,
            color = White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        Modifier.clickable {
            onClick()
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            icon,
            label,
            tint = White,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.height(2.dp))

        Text(
            label,
            color = Grey,
            fontSize = 8.sp
        )
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
    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    "Back",
                    tint = White
                )
            }

            Text(
                title,
                color = White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (items.isEmpty()) {

            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    emptyText,
                    color = Grey,
                    fontSize = 13.sp
                )
            }

        } else {

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(
                    uniqueItems(items),
                    key = {
                        "${it.media_type ?: ""}-${it.id}"
                    }
                ) {
                    ListRow(
                        item = it,
                        onOpen = onOpen
                    )
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
        Modifier
            .fillMaxSize()
            .background(Black)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    "Back",
                    tint = White
                )
            }

            Text(
                "Settings",
                color = White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Setting(
            "Account",
            "Login and subscription"
        )

        Setting(
            "Playback",
            "Video quality and playback settings"
        )

        Setting(
            "Downloads",
            "Manage downloaded content"
        )

        Setting(
            "About Streamify",
            "App information"
        )
    }
}

@Composable
private fun Setting(
    title: String,
    subtitle: String
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 15.dp
            )
    ) {

        Text(
            title,
            color = White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(3.dp))

        Text(
            subtitle,
            color = Grey,
            fontSize = 11.sp
        )
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
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 14.dp,
                vertical = 8.dp
            )
            .clip(RoundedCornerShape(30.dp))
            .background(
                Black.copy(alpha = .96f)
            )
            .border(
                1.5.dp,
                Red.copy(alpha = .8f),
                RoundedCornerShape(30.dp)
            )
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
            icon = Icons.Outlined.List,
            label = "My List",
            selected = selected == Screen.MY_LIST,
            onClick = onMyList
        )

        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(RedBrush)
                .border(
                    2.dp,
                    White.copy(alpha = .25f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.PlayArrow,
                "Play",
                tint = White,
                modifier = Modifier.size(30.dp)
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
        Modifier
            .clickable {
                onClick()
            }
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            icon,
            label,
            tint = if (selected) Red else White,
            modifier = Modifier.size(23.dp)
        )

        Spacer(Modifier.height(1.dp))

        Text(
            label,
            color = if (selected) Red else White,
            fontSize = 9.sp,
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )
    }
}
