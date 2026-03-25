package com.example.tasksbot

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tasksbot.ui.AuthViewModel
import com.example.tasksbot.ui.ChannelLinkScreen
import com.example.tasksbot.ui.CreateTaskScreen
import com.example.tasksbot.ui.HistoryScreen
import com.example.tasksbot.ui.LoginScreen
import com.example.tasksbot.ui.MainMenuScreen
import com.example.tasksbot.ui.RoomManagementScreen
import com.example.tasksbot.ui.SetupScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot()
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
    ChannelLink,
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

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (section) {
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
                    onChannelLink = { section = Section.ChannelLink },
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
                Section.ChannelLink -> ChannelLinkScreen(
                    onBackToMenu = { section = Section.MainMenu }
                )
            }
        }
    }
}

