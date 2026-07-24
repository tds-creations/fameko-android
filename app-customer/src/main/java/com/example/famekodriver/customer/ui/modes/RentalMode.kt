package com.example.famekodriver.customer.ui.modes

import androidx.compose.runtime.Composable
import com.example.famekodriver.customer.CustomerMapViewModel
import com.example.famekodriver.customer.CustomerSheetState
import com.example.famekodriver.customer.ui.components.*

@Composable
fun RentalSheetContent(
    state: CustomerSheetState,
    viewModel: CustomerMapViewModel,
    onDetailsClick: (Map<String, Any>) -> Unit,
    onStartNavigation: () -> Unit
) {
    if (state == CustomerSheetState.ACTIVE_RENTAL) {
        viewModel.activeRental?.let { rental ->
            ActiveRentalSheetContent(
                rental = rental,
                viewModel = viewModel,
                onDetailsClick = { onDetailsClick(rental) },
                onStartNavigation = onStartNavigation
            )
        }
    }
}
