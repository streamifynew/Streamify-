package com.streamify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StreamifyTheme {
                StreamifyHome()
            }
        }
    }
}

private val StreamBlack = Color(0xFF080808)
private val StreamDark = Color(0xFF111111)
private val StreamCard = Color(0xFF191919)
private val StreamRed = Color(0xFFFF4D2E)
private val StreamOrange = Color(0xFFFF7A00)
private val StreamGold = Color(0xFFFFD58A)
private val StreamText = Color(0xFFF5F5F5)
private val StreamMuted = Color(0xFF9A9A9A)

private const val TMDB_IMAGE_BASE_URL =
    "https://image.tmdb.org/t/p/w500"

@androidx.compose.runtime.Composable
fun StreamifyTheme(
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}

@androidx.compose.runtime.Composable
fun StreamifyHome() {

    var trending by remember {
        mutableStateOf<List<TmdbItem>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {

        try {
            val repository = TmdbRepository()
            trending = repository.getTrending()
        } catch (exception: Exception) {
            errorMessage = exception.message ?: "Unable to load TMDB data"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamBlack)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 82.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            TopBar()

            Spacer(modifier = Modifier.height(18.dp))

            HeroSection(
                trending = trending
            )

            Spacer(modifier = Modifier.height(18.dp))

            CategoryRow()

            Spacer(modifier = Modifier.height(25.dp))

            if (isLoading) {

                LoadingSection()

            } else if (errorMessage != null) {

                ErrorSection(
                    message = errorMessage!!
                )

            } else {

                if (trending.isNotEmpty()) {

                    StreamSection(
                        title = "Trending",
                        subtitle = "What's popular right now"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TmdbPosterRow(
                        items = trending
                    )

                    Spacer(modifier = Modifier.height(26.dp))

                    StreamSection(
                        title = "Movies",
                        subtitle = "Discover movies"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TmdbPosterRow(
                        items = trending.filter {
                            it.media_type == "movie"
                        }
                    )

                    Spacer(modifier = Modifier.height(26.dp))

                    StreamSection(
                        title = "TV",
                        subtitle = "Series and shows"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TmdbPosterRow(
                        items = trending.filter {
                            it.media_type == "tv"
                        }
                    )

                } else {

                    ErrorSection(
                        message = "No TMDB results available."
                    )
                }
            }
        }

        BottomNavigation()
    }
}

@androidx.compose.runtime.Composable
fun TopBar() {

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
                text = "STREAMIFY",
                color = StreamRed,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )

            Text(
                text = "Entertainment, your way.",
                color = StreamMuted,
                fontSize = 11.sp
            )
        }

        IconButton(
            onClick = { }
        ) {

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White
            )
        }

        IconButton(
            onClick = { }
        ) {

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        StreamCard,
                        RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "S",
                    color = StreamGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun HeroSection(
    trending: List<TmdbItem>
) {

    val hero = trending.firstOrNull {
        !it.backdrop_path.isNullOrBlank()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(225.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(StreamDark)
    ) {

        if (hero?.backdrop_path != null) {

            AsyncImage(
                model = TMDB_IMAGE_BASE_URL.replace(
                    "w500",
                    "w1280"
                ) + hero.backdrop_path,
                contentDescription = hero.title ?: hero.name,
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
                            Color(0xEE080808)
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
                text = "STREAMIFY",
                color = StreamGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = hero?.title
                    ?: hero?.name
                    ?: "Discover something new",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Powered by TMDB",
                color = StreamMuted,
                fontSize = 11.sp
            )
        }
    }
}

@androidx.compose.runtime.Composable
fun CategoryRow() {

    val categories = listOf(
        "Trending",
        "Movies",
        "TV",
        "Drama",
        "Anime"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {

        categories.forEachIndexed { index, category ->

            Surface(
                onClick = { },
                shape = RoundedCornerShape(50),
                color = if (index == 0) {
                    StreamRed
                } else {
                    StreamCard
                }
            ) {

                Text(
                    text = category,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (index == 0) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    modifier = Modifier.padding(
                        horizontal = 17.dp,
                        vertical = 9.dp
                    )
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun StreamSection(
    title: String,
    subtitle: String
) {

    Column(
        modifier = Modifier.padding(horizontal = 18.dp)
    ) {

        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = subtitle,
            color = StreamMuted,
            fontSize = 11.sp
        )
    }
}

@androidx.compose.runtime.Composable
fun TmdbPosterRow(
    items: List<TmdbItem>
) {

    if (items.isEmpty()) {

        Text(
            text = "No titles available.",
            color = StreamMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        return
    }

    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 18.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        items(
            items = items
                .filter { !it.poster_path.isNullOrBlank() }
                .take(15)
        ) { item ->

            TmdbPosterCard(item)
        }
    }
}

@androidx.compose.runtime.Composable
fun TmdbPosterCard(
    item: TmdbItem
) {

    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { }
    ) {

        Box(
            modifier = Modifier
                .width(120.dp)
                .height(178.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(StreamCard)
        ) {

            if (!item.poster_path.isNullOrBlank()) {

                AsyncImage(
                    model = TMDB_IMAGE_BASE_URL + item.poster_path,
                    contentDescription = item.title ?: item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (item.vote_average != null && item.vote_average > 0) {

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .background(
                            Color(0xCC080808),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(
                            horizontal = 7.dp,
                            vertical = 4.dp
                        )
                ) {

                    Text(
                        text = String.format(
                            "%.1f",
                            item.vote_average
                        ),
                        color = StreamGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = item.title ?: item.name ?: "Untitled",
            color = StreamText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2
        )
    }
}

@androidx.compose.runtime.Composable
fun LoadingSection() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            CircularProgressIndicator(
                color = StreamRed
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Loading TMDB...",
                color = StreamMuted,
                fontSize = 13.sp
            )
        }
    }
}

@androidx.compose.runtime.Composable
fun ErrorSection(
    message: String
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 18.dp)
            .background(
                StreamCard,
                RoundedCornerShape(18.dp)
            ),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "TMDB connection issue",
                color = StreamRed,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = StreamMuted,
                fontSize = 12.sp
            )
        }
    }
}

@androidx.compose.runtime.Composable
fun BottomNavigation() {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 18.dp,
                vertical = 12.dp
            )
            .align(Alignment.BottomCenter),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xEE191919)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            BottomItem(
                icon = Icons.Default.Home,
                label = "Home",
                selected = true
            )

            BottomItem(
                icon = Icons.Default.Star,
                label = "My List"
            )

            BottomItem(
                icon = Icons.Default.Star,
                label = "Downloads"
            )

            BottomItem(
                icon = Icons.Default.Settings,
                label = "Settings"
            )
        }
    }
}

@androidx.compose.runtime.Composable
fun BottomItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean = false
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) {
                StreamRed
            } else {
                StreamMuted
            },
            modifier = Modifier.size(21.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            color = if (selected) {
                Color.White
            } else {
                StreamMuted
            },
            fontSize = 9.sp
        )
    }
}
