package uz.carpet.washer.pos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uz.carpet.washer.pos.ui.screens.dashboard.DashboardScreen
import uz.carpet.washer.pos.ui.screens.neworder.NewOrderScreen
import uz.carpet.washer.pos.ui.screens.orderdetail.OrderDetailScreen
import uz.carpet.washer.pos.ui.screens.printer.PrinterSettingsScreen
import uz.carpet.washer.pos.ui.screens.statistics.StatisticsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val NEW_ORDER = "new_order"
    const val ORDER_DETAIL = "order_detail/{orderId}"
    const val EDIT_ORDER = "edit_order/{editOrderId}"   // Tahrirlash route
    const val PRINTER_SETTINGS = "printer_settings"
    const val STATISTICS = "statistics"

    fun orderDetail(orderId: Long) = "order_detail/$orderId"
    fun editOrder(orderId: Long) = "edit_order/$orderId"  // Tahrirlash uchun
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNewOrder = { navController.navigate(Routes.NEW_ORDER) },
                onOrderClick = { id -> navController.navigate(Routes.orderDetail(id)) },
                onEditOrder = { id -> navController.navigate(Routes.editOrder(id)) },  // Tahrirlash
                onPrinterSettings = { navController.navigate(Routes.PRINTER_SETTINGS) },
                onStatistics = { navController.navigate(Routes.STATISTICS) }
            )
        }

        // Yangi buyurtma
        composable(Routes.NEW_ORDER) {
            NewOrderScreen(
                onBack = { navController.popBackStack() },
                onOrderSaved = { navController.popBackStack() }
            )
        }

        // Buyurtmani TAHRIRLASH (ma'lumotlar yuklangan holda)
        composable(
            Routes.EDIT_ORDER,
            arguments = listOf(navArgument("editOrderId") { type = NavType.LongType })
        ) {
            NewOrderScreen(
                onBack = { navController.popBackStack() },
                onOrderSaved = { navController.popBackStack() }
            )
        }

        composable(
            Routes.ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStack ->
            val orderId = backStack.arguments?.getLong("orderId") ?: 0L
            OrderDetailScreen(
                orderId = orderId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PRINTER_SETTINGS) {
            PrinterSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STATISTICS) {
            StatisticsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
