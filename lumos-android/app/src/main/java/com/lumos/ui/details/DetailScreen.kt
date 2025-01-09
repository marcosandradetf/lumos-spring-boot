package com.lumos.ui.details

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DetailScreen(itemId: String?) {
    Column {
        Text("Detail Screen for item: $itemId")
    }
}

