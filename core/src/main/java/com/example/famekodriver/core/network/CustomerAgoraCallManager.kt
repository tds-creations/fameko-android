package com.example.famekodriver.core.network

import android.content.Context
import com.example.famekodriver.core.data.repository.DriverRepository

class CustomerAgoraCallManager(
    context: Context,
    repository: DriverRepository
) : AgoraCallManager(context, repository)
