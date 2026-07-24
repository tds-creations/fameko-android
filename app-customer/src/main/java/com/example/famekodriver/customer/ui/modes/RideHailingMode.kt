package com.example.famekodriver.customer.ui.modes

import androidx.compose.runtime.Composable
import com.example.famekodriver.customer.CustomerMapViewModel
import com.example.famekodriver.customer.CustomerSheetState
import com.example.famekodriver.customer.ui.components.*

@Composable
fun RideHailingSheetContent(
    state: CustomerSheetState,
    viewModel: CustomerMapViewModel,
    onNavigateToChat: (Int, String) -> Unit,
    onScheduleClick: () -> Unit,
    onShareTrip: () -> Unit,
    onCloseScheduled: () -> Unit,
    onRetryTimeout: () -> Unit,
    onCloseTimeout: () -> Unit
) {
    when (state) {
        CustomerSheetState.SELECTING_SERVICE -> {
            ServiceSelectionSheet(
                estimates = viewModel.rideEstimates,
                activeServiceMode = viewModel.activeServiceMode,
                discountRate = viewModel.discountRate,
                peakMultiplier = viewModel.pricingConfig?.peakMultiplier ?: 1.0,
                selectedType = viewModel.selectedVehicleType,
                onTypeSelected = { type, _ -> viewModel.setVehicleType(type) },
                onConfirm = { viewModel.confirmOrder() }, 
                onScheduleClick = onScheduleClick,
                isPlacing = viewModel.isOrderPlacing,
                isLoading = viewModel.isLoading
            )
        }
        CustomerSheetState.SEARCHING_FOR_DRIVER -> {
            SearchingSheetContent(viewModel = viewModel, onCancel = { viewModel.showCancelConfirmation = true })
        }
        CustomerSheetState.ON_TRIP -> {
            viewModel.orderStatusData?.let { data ->
                viewModel.currentOrderId?.let { id ->
                    DriverInfoSheetContent(
                        data = data,
                        orderId = id,
                        onNavigateToChat = onNavigateToChat,
                        onCancel = { viewModel.showCancelConfirmation = true },
                        onInitiateCall = { viewModel.initiateCall() },
                        onShareTrip = onShareTrip
                    )
                }
            }
        }
        CustomerSheetState.RIDE_SCHEDULED -> {
            ScheduledRideSheetContent(
                onCancel = { viewModel.showCancelConfirmation = true },
                onClose = onCloseScheduled
            )
        }
        CustomerSheetState.TIMED_OUT -> {
            TimedOutSheetContent(
                onRetry = onRetryTimeout,
                onClose = onCloseTimeout
            )
        }
        else -> {}
    }
}
