package com.example.famekodriver.core.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.famekodriver.core.data.local.entity.NavigationRouteEntity
import com.example.famekodriver.core.data.local.entity.SavedPlaceEntity
import com.example.famekodriver.core.data.local.entity.SyncOperationEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationRouteMigrationTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testMigration1To2CreatesNavigationRoutesTableAndPreservesData() = runBlocking {
        // Insert representative record into saved_places
        val place = SavedPlaceEntity(
            id = "place_home_1",
            customerId = "cust_99",
            label = "Home",
            address = "123 East Legon St, Accra",
            latitude = 5.6358,
            longitude = -0.1601,
            updatedAt = System.currentTimeMillis()
        )
        db.savedPlaceDao().insertPlace(place)

        // Insert representative record into sync_queue
        val syncOp = SyncOperationEntity(
            queueId = 1,
            operation = "CREATE",
            placeId = "place_home_1",
            data = "{ \"label\": \"Home\" }",
            retries = 0,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        db.syncQueueDao().enqueue(syncOp)

        // Insert new NavigationRoute record
        val route = NavigationRouteEntity(
            orderId = 205,
            status = "IN_TRANSIT",
            pickupLat = 5.6037,
            pickupLng = -0.1870,
            dropOffLat = 5.6358,
            dropOffLng = -0.1601,
            originalPathJson = "[[-0.187,5.6037],[-0.1601,5.6358]]",
            instructionsJson = "[{\"instruction\":\"Turn left\",\"distance\":150.0}]",
            currentSegmentIndex = 0,
            remainingDistanceKm = 4.8,
            remainingDurationMin = 11.0,
            timestamp = System.currentTimeMillis()
        )
        db.navigationRouteDao().saveRoute(route)

        // Verify saved_places record remains intact
        val fetchedPlace = db.savedPlaceDao().getPlaceById("place_home_1")
        assertNotNull(fetchedPlace)
        assertEquals("Home", fetchedPlace?.label)
        assertEquals("123 East Legon St, Accra", fetchedPlace?.address)

        // Verify sync_queue record remains intact
        val pendingOps = db.syncQueueDao().getPendingOperations()
        assertEquals(1, pendingOps.size)
        assertEquals("place_home_1", pendingOps[0].placeId)

        // Verify navigation_routes record is saved and read correctly
        val fetchedRoute = db.navigationRouteDao().getRouteForOrder(205)
        assertNotNull(fetchedRoute)
        assertEquals(205, fetchedRoute?.orderId)
        assertEquals("IN_TRANSIT", fetchedRoute?.status)
        assertEquals(4.8, fetchedRoute?.remainingDistanceKm ?: 0.0, 0.001)

        // Verify deletion of navigation_routes record
        db.navigationRouteDao().deleteRouteForOrder(205)
        val deletedRoute = db.navigationRouteDao().getRouteForOrder(205)
        assertNull(deletedRoute)
    }
}
