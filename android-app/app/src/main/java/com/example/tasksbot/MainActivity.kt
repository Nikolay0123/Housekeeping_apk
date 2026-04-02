package com.example.tasksbot

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tasksbot.ui.AuthViewModel
import com.example.tasksbot.ui.BroomSplashScreen
import com.example.tasksbot.ui.SettingsScreen
import com.example.tasksbot.ui.CreateTaskScreen
import com.example.tasksbot.ui.HistoryScreen
import com.example.tasksbot.ui.LoginScreen
import com.example.tasksbot.ui.MainMenuScreen
import com.example.tasksbot.ui.RoomManagementScreen
import com.example.tasksbot.ui.SetupScreen
import com.example.tasksbot.ui.theme.TasksBotTheme
import kotlinx.coroutines.delay

private const val LaunchSplashMillis = 1_900L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            var launchReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(LaunchSplashMillis)
                launchReady = true
            }
            TasksBotTheme {
                AnimatedContent(
                    targetState = launchReady,
                    transitionSpec = {
                        fadeIn(
                            tween(480, easing = FastOutSlowInEasing),
                        ) togetherWith fadeOut(tween(360, easing = FastOutSlowInEasing))
                    },
                    label = "launchSplash",
                ) { ready ->
                    if (!ready) {
                        BroomSplashScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.systemBars),
                        )
                    } else {
                        AppRoot()
                    }
                }
            }
        }
    }
}

private enum class Section {
    Setup,
    Login,
    MainMenu,
    CreateTask,
    History,
    RoomManagement,
    Settings,
}

@Composable
private fun AppRoot() {
    val authVm: AuthViewModel = viewModel()
    val isSetupComplete by authVm.isSetupComplete
    val isUnlocked by authVm.isUnlocked

    var section by remember(isSetupComplete, isUnlocked) {
        mutableStateOf(
            when {
                !isSetupComplete -> Section.Setup
                !isUnlocked -> Section.Login
                else -> Section.MainMenu
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        color = MaterialTheme.colorScheme.background,
    ) {
        AnimatedContent(
            targetState = section,
            transitionSpec = {
                fadeIn(animationSpec = tween(240, delayMillis = 40)) togetherWith
                    fadeOut(animationSpec = tween(180))
            },
            label = "section",
        ) { sec ->
            when (sec) {
                Section.Setup -> SetupScreen(
                    onDone = { section = Section.Login }
                )
                Section.Login -> LoginScreen(
                    onLoggedIn = { section = Section.MainMenu },
                    onGoToSetup = { section = Section.Setup }
                )
                Section.MainMenu -> MainMenuScreen(
                    onCreateTask = { section = Section.CreateTask },
                    onHistory = { section = Section.History },
                    onRooms = { section = Section.RoomManagement },
                    onOpenSettings = { section = Section.Settings },
                )
                Section.CreateTask -> CreateTaskScreen(
                    onBackToMenu = { section = Section.MainMenu },
                    onGoToHistory = { section = Section.History },
                )
                Section.History -> HistoryScreen(
                    onBackToMenu = { section = Section.MainMenu }
                )
                Section.RoomManagement -> RoomManagementScreen(
                    onBackToMenu = { section = Section.MainMenu }
                )
                Section.Settings -> SettingsScreen(
                    onBackToMenu = { section = Section.MainMenu }
                )
            }
        }
    }
}

