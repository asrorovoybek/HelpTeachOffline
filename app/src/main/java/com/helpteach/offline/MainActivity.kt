package com.helpteach.offline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helpteach.offline.ui.MainNavigation
import com.helpteach.offline.ui.theme.HelpTeachOfflineTheme
import com.helpteach.offline.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HelpTeachOfflineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: AppViewModel = viewModel()
                    MainNavigation(viewModel)
                }
            }
        }
    }
}
