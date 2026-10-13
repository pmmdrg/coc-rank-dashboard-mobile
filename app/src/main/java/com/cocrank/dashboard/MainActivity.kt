package com.cocrank.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cocrank.dashboard.ui.navigation.AppNavigation
import com.cocrank.dashboard.ui.screens.dashboard.DashboardViewModel
import com.cocrank.dashboard.ui.theme.CoCRankTheme
import com.cocrank.dashboard.ui.theme.DarkBackground

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoCRankTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}
