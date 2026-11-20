package com.proclipping.master

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proclipping.master.ui.navigation.ProClippingNavHost
import com.proclipping.master.ui.theme.ProClippingMasterTheme
import com.proclipping.master.ui.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        val container = (application as ProClippingApp).container

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val studioViewModel: StudioViewModel = viewModel(
                factory = StudioViewModel.Factory(
                    repository = container.clippingRepository,
                    downloadManager = container.clipDownloadManager
                )
            )

            ProClippingMasterTheme {
                ProClippingNavHost(
                    viewModel = studioViewModel,
                    widthSizeClass = windowSizeClass.widthSizeClass
                )
            }
        }
    }
}
