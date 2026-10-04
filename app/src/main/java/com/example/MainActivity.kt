package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.QuickTemplate
import com.example.ui.MainViewModel
import com.example.ui.UiEvent
import com.example.ui.components.AiAdvisorBottomSheet
import com.example.ui.components.QuickAddBottomSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val shortcutTitle = it.getStringExtra("shortcut_title")
            val shortcutAmount = it.getStringExtra("shortcut_amount")
            val shortcutCategory = it.getStringExtra("shortcut_category")
            if (!shortcutTitle.isNullOrBlank()) {
                viewModel.handleShortcutIntent(shortcutTitle, shortcutAmount, shortcutCategory)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiAnalysisResult by viewModel.aiAnalysisResult.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showQuickAddSheet by remember { mutableStateOf(false) }
    var quickAddInitialCategory by remember { mutableStateOf("Makanan") }
    var quickAddInitialTitle by remember { mutableStateOf("") }
    var quickAddInitialAmount by remember { mutableStateOf(0.0) }

    var showAiAdvisorSheet by remember { mutableStateOf(false) }

    val quickAddSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val aiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Handle back button when on sub-screens
    BackHandler(enabled = selectedTabIndex != 0) {
        selectedTabIndex = 0
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.onAction?.invoke()
                    }
                }
                is UiEvent.QuickTemplateTriggered -> {
                    quickAddInitialTitle = event.template.name
                    quickAddInitialCategory = event.template.category
                    quickAddInitialAmount = event.template.defaultAmount
                    showQuickAddSheet = true
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                val navItems = listOf(
                    Triple(0, "Beranda", Icons.Default.Home),
                    Triple(1, "Statistik", Icons.Default.BarChart),
                    Triple(2, "Shortcut", Icons.Default.Bolt),
                    Triple(3, "Pengaturan", Icons.Default.Settings)
                )

                navItems.forEach { (index, label, icon) ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_item_$index")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTabIndex) {
            0 -> HomeScreen(
                uiState = uiState,
                currencyFormat = viewModel.currencyFormat,
                onQuickAddClick = {
                    quickAddInitialTitle = ""
                    quickAddInitialAmount = 0.0
                    quickAddInitialCategory = "Makanan"
                    showQuickAddSheet = true
                },
                onTemplateClick = { template ->
                    viewModel.quickLogTemplate(template)
                },
                onManageTemplatesClick = { selectedTabIndex = 2 },
                onEditBudgetClick = { selectedTabIndex = 3 },
                onAiAdvisorClick = {
                    viewModel.requestAiInsight()
                    showAiAdvisorSheet = true
                },
                onDeleteExpense = { viewModel.deleteExpense(it) },
                onSyncClick = { viewModel.performManualSync() },
                modifier = Modifier.padding(innerPadding)
            )
            1 -> AnalyticsScreen(
                uiState = uiState,
                currencyFormat = viewModel.currencyFormat,
                onAiAdvisorClick = {
                    viewModel.requestAiInsight()
                    showAiAdvisorSheet = true
                },
                modifier = Modifier.padding(innerPadding)
            )
            2 -> TemplatesScreen(
                templates = uiState.quickTemplates,
                currencyFormat = viewModel.currencyFormat,
                onSaveTemplate = { viewModel.addOrUpdateTemplate(it) },
                onDeleteTemplate = { viewModel.deleteTemplate(it) },
                modifier = Modifier.padding(innerPadding)
            )
            3 -> SettingsScreen(
                uiState = uiState,
                currencyFormat = viewModel.currencyFormat,
                onUpdateBudget = { budget, hour, minute ->
                    viewModel.updateBudget(budget, hour, minute)
                },
                onManualSync = { viewModel.performManualSync() },
                modifier = Modifier.padding(innerPadding)
            )
        }

        // Quick Add Bottom Sheet
        if (showQuickAddSheet) {
            QuickAddBottomSheet(
                sheetState = quickAddSheetState,
                initialCategory = quickAddInitialCategory,
                initialTitle = quickAddInitialTitle,
                initialAmount = quickAddInitialAmount,
                onDismiss = { showQuickAddSheet = false },
                onSaveExpense = { title, amount, category, note ->
                    viewModel.addExpense(title, amount, category, note)
                    showQuickAddSheet = false
                }
            )
        }

        // AI Advisor Bottom Sheet
        if (showAiAdvisorSheet) {
            AiAdvisorBottomSheet(
                sheetState = aiSheetState,
                analysisText = aiAnalysisResult,
                isLoading = isAiLoading,
                onRefreshAnalysis = { viewModel.requestAiInsight() },
                onDismiss = { showAiAdvisorSheet = false }
            )
        }
    }
}
