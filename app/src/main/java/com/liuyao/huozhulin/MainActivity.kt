package com.liuyao.huozhulin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.liuyao.huozhulin.ui.screens.CoinScreen
import com.liuyao.huozhulin.ui.screens.DateScreen
import com.liuyao.huozhulin.ui.screens.HistoryScreen
import com.liuyao.huozhulin.ui.screens.HomeScreen
import com.liuyao.huozhulin.ui.screens.HourMinuteScreen
import com.liuyao.huozhulin.ui.screens.LifetimeScreen
import com.liuyao.huozhulin.ui.screens.ManualScreen
import com.liuyao.huozhulin.ui.screens.NumberScreen
import com.liuyao.huozhulin.ui.screens.RandomScreen
import com.liuyao.huozhulin.ui.screens.ResultScreen
import com.liuyao.huozhulin.ui.screens.SettingsScreen
import com.liuyao.huozhulin.ui.screens.SpecifiedScreen
import com.liuyao.huozhulin.ui.theme.AppTheme
import com.liuyao.huozhulin.viewmodel.PaiPanViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val nav = rememberNavController()
                    val vm: PaiPanViewModel = viewModel()
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") { HomeScreen(nav) }
                        composable("random") { RandomScreen(nav, vm) }
                        composable("coin") { CoinScreen(nav, vm) }
                        composable("manual") { ManualScreen(nav, vm) }
                        composable("specified") { SpecifiedScreen(nav, vm) }
                        composable("number") { NumberScreen(nav, vm) }
                        composable("date") { DateScreen(nav, vm) }
                        composable("hourMinute") { HourMinuteScreen(nav, vm) }
                        composable("lifetime") { LifetimeScreen(nav, vm) }
                        composable("result") { ResultScreen(nav, vm) }
                        composable("history") { HistoryScreen(nav, vm) }
                        composable("settings") { SettingsScreen(nav, vm) }
                    }
                }
            }
        }
    }
}
