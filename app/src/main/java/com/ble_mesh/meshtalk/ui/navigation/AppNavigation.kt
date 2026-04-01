package com.ble_mesh.meshtalk.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ble_mesh.meshtalk.ble.BLEService
import com.ble_mesh.meshtalk.ui.screens.ChatScreen
import com.ble_mesh.meshtalk.ui.screens.DebugScreen
import com.ble_mesh.meshtalk.ui.screens.DeviceListScreen
import com.ble_mesh.meshtalk.viewmodel.ChatViewModel
import com.ble_mesh.meshtalk.viewmodel.DebugViewModel
import com.ble_mesh.meshtalk.viewmodel.MainViewModel

/** Navigation route constants */
object Routes {
    const val DEVICE_LIST = "device_list"
    const val CHAT = "chat/{peerId}/{peerName}"
    const val DEBUG = "debug"

    fun chatRoute(peerId: String, peerName: String) =
        "chat/${peerId.replace("/", "_")}/${peerName.replace("/", "_")}"
}

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    chatViewModel: ChatViewModel,
    debugViewModel: DebugViewModel,
    bleService: BLEService?
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.DEVICE_LIST) {

        composable(Routes.DEVICE_LIST) {
            DeviceListScreen(
                viewModel = mainViewModel,
                onOpenChat = { peerId, peerName ->
                    navController.navigate(
                        Routes.chatRoute(peerId, peerName)
                    )
                },
                onOpenDebug = { navController.navigate(Routes.DEBUG) }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("peerId") { type = NavType.StringType },
                navArgument("peerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
            val peerName = backStackEntry.arguments?.getString("peerName") ?: "Unknown"

            chatViewModel.setConversation(peerId)
            
            // Get fresh instance from MainViewModel instead of standard lambda param to avoid null state capture
            val activeService = mainViewModel.getBleService()
            activeService?.let { chatViewModel.attachBleService(it) }

            ChatScreen(
                peerId = peerId,
                peerName = peerName,
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DEBUG) {
            bleService?.let { debugViewModel.updateFromService(it) }

            DebugScreen(
                viewModel = debugViewModel,
                bleService = bleService,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
