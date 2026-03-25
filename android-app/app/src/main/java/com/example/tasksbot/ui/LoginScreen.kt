package com.example.tasksbot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onGoToSetup: () -> Unit,
) {
    val authVm: AuthViewModel = viewModel()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Введите PIN начальника")

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (error != null) {
            Text(error ?: "", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                error = null
                val ok = authVm.login(pin)
                if (!ok) {
                    error = "Неверный PIN."
                    return@Button
                }
                onLoggedIn()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Войти")
        }

        Button(
            onClick = onGoToSetup,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сбросить настройки")
        }
    }
}

