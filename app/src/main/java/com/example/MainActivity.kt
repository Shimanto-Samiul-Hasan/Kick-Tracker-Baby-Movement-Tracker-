package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppMode
import com.example.ui.KickTrackerUiState
import com.example.ui.KickTrackerViewModel
import com.example.ui.components.CountToTenView
import com.example.ui.components.DailyStatsCard
import com.example.ui.components.ExportDialog
import com.example.ui.components.HistoryView
import com.example.ui.components.OnboardingDialog
import com.example.ui.components.OneTapLoggerButton
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.KickTrackerTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private val viewModel: KickTrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            KickTrackerTheme(
                amoledDarkMode = uiState.isAmoledDark,
                useSystemDark = false
            ) {
                MainAppScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    uiState: KickTrackerUiState,
    viewModel: KickTrackerViewModel
) {
    val context = LocalContext.current
    var currentNavDestination by remember { mutableIntStateOf(0) } // 0 = Log Screen, 1 = History & Export
    var showExportDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentNavDestination == 0,
                    onClick = { currentNavDestination = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Log Kick"
                        )
                    },
                    label = { Text("Log Movement") },
                    modifier = Modifier.testTag("nav_item_log"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = currentNavDestination == 1,
                    onClick = { currentNavDestination = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("History & Export") },
                    modifier = Modifier.testTag("nav_item_history"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Top Header Row
                TopHeader(
                    isAmoled = uiState.isAmoledDark,
                    onToggleAmoled = { viewModel.toggleAmoledDark() },
                    onOpenSettings = { showSettingsDialog = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (currentNavDestination == 0) {
                    // Logging Screen: Mode Selector Tabs
                    TabRow(
                        selectedTabIndex = if (uiState.selectedMode == AppMode.QUICK_LOG) 0 else 1,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            val currentTab = if (uiState.selectedMode == AppMode.QUICK_LOG) 0 else 1
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 3.dp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Tab(
                            selected = uiState.selectedMode == AppMode.QUICK_LOG,
                            onClick = { viewModel.setMode(AppMode.QUICK_LOG) },
                            text = {
                                Text(
                                    text = "Quick Log",
                                    fontWeight = if (uiState.selectedMode == AppMode.QUICK_LOG) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("tab_quick_log")
                        )
                        Tab(
                            selected = uiState.selectedMode == AppMode.COUNT_TO_TEN,
                            onClick = { viewModel.setMode(AppMode.COUNT_TO_TEN) },
                            text = {
                                Text(
                                    text = "Count to 10",
                                    fontWeight = if (uiState.selectedMode == AppMode.COUNT_TO_TEN) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("tab_count_to_ten")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Main Logger Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (uiState.selectedMode == AppMode.QUICK_LOG) {
                            DailyStatsCard(
                                count = uiState.todayCount,
                                latestLog = uiState.latestLog,
                                onUndoClick = { viewModel.undoLastKick() },
                                selectedMovementType = uiState.selectedMovementType,
                                onSelectMovementType = { viewModel.setMovementType(it) }
                            )
                        } else {
                            CountToTenView(
                                activeSession = uiState.activeSession,
                                elapsedSeconds = uiState.sessionElapsedSeconds,
                                onStartSession = { viewModel.startCountToTen() },
                                onResetSession = { viewModel.cancelCountToTen() }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Massive Center Tap Logger Button
                        OneTapLoggerButton(
                            onClick = { viewModel.logMovement() },
                            hapticEnabled = uiState.hapticEnabled,
                            isCountToTenMode = uiState.selectedMode == AppMode.COUNT_TO_TEN,
                            sessionCount = uiState.activeSession?.completedCount ?: 0,
                            sessionTarget = uiState.activeSession?.targetCount ?: 10
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (uiState.selectedMode == AppMode.COUNT_TO_TEN) {
                                "Tap circle each time baby moves"
                            } else {
                                "Immediate, eyes-free logging"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                } else {
                    // History and Medical Export Screen
                    HistoryView(
                        allLogs = uiState.allLogs,
                        allSessions = uiState.allSessions,
                        onOpenExport = { showExportDialog = true },
                        onPartnerShare = {
                            val timeStr = uiState.latestLog?.let {
                                Instant.ofEpochMilli(it.timestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("h:mm a"))
                            } ?: "earlier"

                            val text = "Baby was active today! 🍼 ${uiState.todayCount} movements recorded, most recent at $timeStr. (Logged via Kick Tracker)"
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Baby's Kick Update")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share with Partner")
                            context.startActivity(shareIntent)
                        },
                        onDeleteLog = { id -> viewModel.deleteKickLog(id) }
                    )
                }
            }

            // Session Completion Celebration Banner
            AnimatedVisibility(
                visible = uiState.sessionCompletedBanner != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                uiState.sessionCompletedBanner?.let { session ->
                    val min = session.durationSeconds / 60
                    val sec = session.durationSeconds % 60
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Goal Reached! 10 Kicks Logged",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Completed in ${min}m ${sec}s (Normal fetal activity confirmed)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.dismissCompletionBanner() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (uiState.showOnboarding) {
        OnboardingDialog(
            onDismiss = { viewModel.dismissOnboarding() }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            generateMedicalReport = { name -> viewModel.generateMedicalReport(name) },
            generateCsvExport = { viewModel.generateCsvExport() }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            isAmoledDark = uiState.isAmoledDark,
            hapticEnabled = uiState.hapticEnabled,
            onToggleAmoled = { viewModel.toggleAmoledDark() },
            onToggleHaptic = { viewModel.toggleHaptic() },
            onShowTutorial = { viewModel.showOnboardingManual() },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun TopHeader(
    isAmoled: Boolean,
    onToggleAmoled: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Kick Tracker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Eyes-Free Movement Log",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleAmoled,
                modifier = Modifier.testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isAmoled) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                    contentDescription = "Toggle AMOLED Mode",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings & Info",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
