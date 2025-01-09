package com.lumos.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lumos.ui.components.AppLayout
import com.lumos.ui.components.TopBar

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    AppLayout(
        title = "Tarefas Recebidas",
        navigateBack = null
    ) { modifier ->
        Column(
            modifier = modifier
        ) {
            Text("Home Screen")
            Button(onClick = onLogout) {
                Text("Log Out")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HomeScreen(
        onLogout = {}
    )
}