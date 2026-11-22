package com.example.famekodriver.core.data.local.dao

import androidx.room.*
import com.example.famekodriver.core.data.local.entity.NavigationRouteEntity

@Dao
interface NavigationRouteDao {
    @Query("SELECT * FROM navigation_routes WHERE orderId = :orderId LIMIT 1")
    suspend fun getRouteForOrder(orderId: Int): NavigationRouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRoute(route: NavigationRouteEntity)

    @Query("DELETE FROM navigation_routes WHERE orderId = :orderId")
    suspend fun deleteRouteForOrder(orderId: Int)

    @Query("DELETE FROM navigation_routes")
    suspend fun clearAllRoutes()
}
