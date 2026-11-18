package com.playspace

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.launch

private val Context.store by preferencesDataStore(name = "playspace")
private val SELECTED = stringSetPreferencesKey("selected")
private val FAVORITES = stringSetPreferencesKey("favorites")

private val Background = Color(0xFF101114)
private val SurfaceDark = Color(0xFF1C1D22)
private val Elevated = Color(0xFF292B33)
private val Muted = Color(0xFF999BA5)

data class Game(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PlaySpaceApp() }
    }
}

@Composable
private fun PlaySpaceApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allGames = remember { discoverApps(context) }
    val prefs by context.store.data.collectAsState(initial = null)

    val selected = prefs?.get(SELECTED).orEmpty()
    val favorites = prefs?.get(FAVORITES).orEmpty()

    var page by remember { mutableStateOf("Home") }
    var query by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    fun toggle(key: androidx.datastore.preferences.core.Preferences.Key<Set<String>>, pkg: String) {
        scope.launch {
            context.store.edit { data ->
                val updated = data[key].orEmpty().toMutableSet()
                if (!updated.add(pkg)) updated.remove(pkg)
                data[key] = updated
            }
        }
    }

    val myGames = allGames.filter { it.packageName in selected }
    val favoriteGames = allGames.filter { it.packageName in favorites }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            background = Background,
            surface = SurfaceDark,
            onSurface = Color.White
        )
    ) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF18191D)) {
                    val tabs = listOf(
                        "Home" to Icons.Default.Home,
                        "Library" to Icons.Default.GridView,
                        "Favorites" to Icons.Default.Favorite,
                        "Settings" to Icons.Default.Settings
                    )

                    tabs.forEach { (label, icon) ->
                        NavigationBarItem(
                            selected = page == label,
                            onClick = { page = label },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted,
                                indicatorColor = Elevated
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(22.dp))
                Text(
                    "PLAYSPACE",
                    color = Muted,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    page,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(18.dp))

                when (page) {
                    "Home" -> HomeScreen(
                        games = myGames,
                        onAdd = { showPicker = true },
                        onLaunch = { launchGame(context, it) }
                    )

                    "Library" -> LibraryScreen(
                        games = allGames.filter { it.name.contains(query, true) },
                        query = query,
                        onQuery = { query = it },
                        selected = selected,
                        favorites = favorites,
                        onSelect = { toggle(SELECTED, it.packageName) },
                        onFavorite = { toggle(FAVORITES, it.packageName) },
                        onLaunch = { launchGame(context, it) }
                    )

                    "Favorites" -> {
                        if (favoriteGames.isEmpty()) {
                            EmptyState(
                                "No favorites yet",
                                "Tap the heart beside a game in your library."
                            )
                        } else {
                            GameList(favoriteGames) { launchGame(context, it) }
                        }
                    }

                    "Settings" -> SettingsScreen(allGames.size, myGames.size)
                }
            }
        }

        if (showPicker) {
            GamePicker(
                games = allGames,
                selected = selected,
                onToggle = { toggle(SELECTED, it.packageName) },
                onDismiss = { showPicker = false }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    games: List<Game>,
    onAdd: () -> Unit,
    onLaunch: (Game) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Elevated)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(
                    "YOUR GAMING SPACE",
                    color = Muted,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    "All your games.",
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "One place. Every adventure.",
                    color = Muted,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add games")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "MY GAMES",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text("${games.size} games", color = Muted, fontSize = 12.sp)
        }

        Spacer(Modifier.height(10.dp))

        if (games.isEmpty()) {
            EmptyState(
                "Your library is empty",
                "Add games to build your personal collection."
            )
        } else {
            GameList(games, onLaunch)
        }
    }
}

@Composable
private fun LibraryScreen(
    games: List<Game>,
    query: String,
    onQuery: (String) -> Unit,
    selected: Set<String>,
    favorites: Set<String>,
    onSelect: (Game) -> Unit,
    onFavorite: (Game) -> Unit,
    onLaunch: (Game) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search installed apps…") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "Choose which apps appear in My Games.",
            color = Muted,
            fontSize = 12.sp
        )

        LazyColumn(contentPadding = PaddingValues(bottom = 18.dp)) {
            items(games, key = { it.packageName }) { game ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GameIcon(game)

                    Column(
                        Modifier.weight(1f)
                            .padding(start = 12.dp)
                            .clickable { onLaunch(game) }
                    ) {
                        Text(
                            game.name,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            game.packageName,
                            color = Muted,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }

                    IconButton(onClick = { onFavorite(game) }) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Toggle favorite",
                            tint = if (game.packageName in favorites)
                                Color(0xFFFF6470) else Muted
                        )
                    }

                    Checkbox(
                        checked = game.packageName in selected,
                        onCheckedChange = { onSelect(game) }
                    )
                }
                HorizontalDivider(color = Color(0xFF292A30))
            }
        }
    }
}

@Composable
private fun GameList(games: List<Game>, onLaunch: (Game) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 18.dp)) {
        items(games, key = { it.packageName }) { game ->
            Row(
                Modifier.fillMaxWidth()
                    .clickable { onLaunch(game) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameIcon(game)
                Text(
                    game.name,
                    color = Color.White,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Launch game",
                    tint = Muted
                )
            }
        }
    }
}

@Composable
private fun GameIcon(game: Game) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        },
        update = { it.setImageDrawable(game.icon) },
        modifier = Modifier.size(48.dp)
            .background(SurfaceDark, RoundedCornerShape(12.dp))
    )
}

@Composable
private fun GamePicker(
    games: List<Game>,
    selected: Set<String>,
    onToggle: (Game) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add games") },
        text = {
            if (games.isEmpty()) {
                Text("No launchable apps were found.")
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(games, key = { it.packageName }) { game ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { onToggle(game) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GameIcon(game)
                            Text(
                                game.name,
                                Modifier.weight(1f).padding(start = 10.dp)
                            )
                            Checkbox(
                                checked = game.packageName in selected,
                                onCheckedChange = { onToggle(game) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        containerColor = SurfaceDark,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.GridView,
            contentDescription = null,
            tint = Muted,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsScreen(totalApps: Int, selectedApps: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Application", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text("PLAYSPACE • Version 1.0", color = Color.White)
            Spacer(Modifier.height(18.dp))
            Text("Launchable apps found: $totalApps", color = Color.White)
            Text("Games in your library: $selectedApps", color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text(
                "Your library and favorites are stored on this device.",
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}

private fun discoverApps(context: Context): List<Game> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)

    val results = if (Build.VERSION.SDK_INT >= 33) {
        pm.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(0)
        )
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
    }

    return results
        .filter { it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map {
            Game(
                name = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                icon = it.loadIcon(pm)
            )
        }
        .sortedBy { it.name.lowercase() }
}

private fun launchGame(context: Context, game: Game) {
    val intent = context.packageManager
        .getLaunchIntentForPackage(game.packageName)

    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
