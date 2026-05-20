package com.crescendo.alarm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.crescendo.alarm.ui.screens.*
import com.crescendo.alarm.viewmodel.AlarmViewModel

sealed class Screen(val route: String) {
    object Home          : Screen("home")
    object EditAlarm     : Screen("edit_alarm/{alarmId}") {
        fun createRoute(id: Int) = "edit_alarm/$id"
    }
    object SleepSchedule : Screen("sleep_schedule")
}

@Composable
fun AppNavigation(vm: AlarmViewModel = viewModel()) {
    val nav = rememberNavController()

    NavHost(nav, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(vm,
                onAddAlarm   = { nav.navigate(Screen.EditAlarm.createRoute(0)) },
                onEditAlarm  = { nav.navigate(Screen.EditAlarm.createRoute(it.id)) },
                onOpenSleep  = { nav.navigate(Screen.SleepSchedule.route) })
        }
        composable(Screen.EditAlarm.route) { back ->
            val id = back.arguments?.getString("alarmId")?.toInt() ?: 0
            EditAlarmScreen(id, vm) { nav.popBackStack() }
        }
        composable(Screen.SleepSchedule.route) {
            SleepScheduleScreen(vm) { nav.popBackStack() }
        }
    }
}
