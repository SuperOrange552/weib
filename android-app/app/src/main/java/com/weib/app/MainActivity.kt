package com.weib.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weib.app.ui.WeibApp
import com.weib.app.ui.theme.WeibTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeibTheme {
                val viewModel: AppViewModel = viewModel()
                WeibApp(viewModel)
            }
        }
    }
}
