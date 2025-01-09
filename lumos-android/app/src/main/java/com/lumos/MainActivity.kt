package com.lumos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.lumos.navigation.AppNavigation
import com.lumos.ui.theme.LumosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LumosTheme {
//                Scaffold(
//                    modifier = Modifier.fillMaxSize(),
//                    topBar = {
//                        CenterAlignedTopAppBar(
//                            title = {
//                                Text(
//                                    "Navigation example",
//                                )
//                            },
//                            navigationIcon = {
//                                IconButton(onClick = navigateBack) {
//                                    Icon(
//                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                                        contentDescription = "Localized description"
//                                    )
//                                }
//                            },
//                        )
//                    },
//                ) { innerPadding ->
//
//                }
                AppNavigation()
            }
        }
    }
}
