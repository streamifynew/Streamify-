package com.streamify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun StreamifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}

@Composable
fun StreamifyHome() {

    var selectedCategory by remember {
        mutableIntStateOf(0)
    }

    val categories = listOf(
        "Trending",
        "Movies",
        "TV",
        "Drama",
        "Anime"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StreamBlack
    ) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StreamBlack)
                    .padding(bottom = 82.dp)
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                // TOP BAR
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

                Spacer(modifier = Modifier.height(18.dp))

                // HERO
                HeroPlaceholder()

                Spacer(modifier = Modifier.height(18.dp))

                // CATEGORY PILLS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {

                    categories.forEachIndexed { index, category ->

                        CategoryPill(
                            title = category,
                            selected = selectedCategory == index,
                            onClick = {
                                selectedCategory = index
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(25.dp))

                // CONTENT SECTIONS
                StreamSection(
                    title = "Trending",
                    subtitle = "What's popular right now"
                )

                Spacer(modifier = Modifier.height(12.dp))

                EmptyTmdbRow()

                Spacer(modifier = Modifier.height(26.dp))

                StreamSection(
                    title = "Movies",
                    subtitle = "Discover movies from TMDB"
                )

                Spacer(modifier = Modifier.height(12.dp))

                EmptyTmdbRow()

                Spacer(modifier = Modifier.height(26.dp))

                StreamSection(
                    title = "TV",
                    subtitle = "Series and shows"
                )

                Spacer(modifier = Modifier.height(12.dp))

                EmptyTmdbRow()
            }

            // BOTTOM NAV
            BottomNavigation()
        }
    }
}

@Composable
fun HeroPlaceholder() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(225.dp)
            .padding(horizontal = 16.dp)
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF242424),
                        Color(0xFF101010)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {

            Text(
                text = "STREAMIFY",
                color = StreamGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Discover something new.",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Real movie & TV metadata powered by TMDB.",
                color = StreamMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun CategoryPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) StreamRed else StreamCard
    ) {

        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(
                horizontal = 17.dp,
                vertical = 9.dp
            )
        )
    }
}

@Composable
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

@Composable
fun EmptyTmdbRow() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        repeat(5) {

            Box(
                modifier = Modifier
                    .size(
                        width = 120.dp,
                        height = 178.dp
                    )
                    .background(
                        StreamCard,
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "TMDB",
                    color = Color(0xFF555555),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
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

@Composable
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
            tint = if (selected) StreamRed else StreamMuted,
            modifier = Modifier.size(21.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            color = if (selected) Color.White else StreamMuted,
            fontSize = 9.sp
        )
    }
}
