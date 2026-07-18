package com.example.famekodriver.core.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "FamekoDriverPrefs"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_DRIVER_ID = "driverId"
        private const val KEY_DRIVER_NAME = "driverName"
        private const val KEY_DRIVER_PHONE = "driverPhone"
        private const val KEY_DRIVER_STATUS = "driverStatus"
        private const val KEY_IS_ONLINE = "isOnline"
        private const val KEY_ACTIVE_ORDER_ID = "activeOrderId"
        private const val KEY_USER_ROLE = "userRole"
        private const val KEY_COMPANY_NAME = "companyName"
        private const val KEY_VEHICLE_TYPE = "vehicleType"
        private const val KEY_IS_FIRST_LOGIN = "isFirstLogin"

        // Notification Settings
        private const val KEY_NOTIF_TRIP_UPDATES = "notif_trip_updates"
        private const val KEY_NOTIF_MESSAGES = "notif_messages"
        private const val KEY_NOTIF_PROMOTIONS = "notif_promotions"
        private const val KEY_NOTIF_ACCOUNT = "notif_account"
    }

    fun setNotificationPreference(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(key, enabled).apply()
    }

    fun getNotificationPreference(key: String, default: Boolean = true): Boolean {
        return prefs.getBoolean(key, default)
    }

    fun getTripUpdatesEnabled(): Boolean = getNotificationPreference(KEY_NOTIF_TRIP_UPDATES)
    fun setTripUpdatesEnabled(enabled: Boolean) = setNotificationPreference(KEY_NOTIF_TRIP_UPDATES, enabled)

    fun getMessagesEnabled(): Boolean = getNotificationPreference(KEY_NOTIF_MESSAGES)
    fun setMessagesEnabled(enabled: Boolean) = setNotificationPreference(KEY_NOTIF_MESSAGES, enabled)

    fun getPromotionsEnabled(): Boolean = getNotificationPreference(KEY_NOTIF_PROMOTIONS)
    fun setPromotionsEnabled(enabled: Boolean) = setNotificationPreference(KEY_NOTIF_PROMOTIONS, enabled)

    fun getAccountAlertsEnabled(): Boolean = getNotificationPreference(KEY_NOTIF_ACCOUNT)
    fun setAccountAlertsEnabled(enabled: Boolean) = setNotificationPreference(KEY_NOTIF_ACCOUNT, enabled)

    fun saveSession(driverId: String, driverName: String, status: String = "PENDING", phone: String = "", role: String = "DRIVER", company: String? = null, vehicleType: String? = null) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_DRIVER_ID, driverId)
            putString(KEY_DRIVER_NAME, driverName)
            putString(KEY_DRIVER_PHONE, phone)
            putString(KEY_DRIVER_STATUS, status)
            putString(KEY_USER_ROLE, role)
            putString(KEY_COMPANY_NAME, company)
            putString(KEY_VEHICLE_TYPE, vehicleType)
            apply()
        }
    }

    fun saveDriverSession(driverId: String, driverName: String, role: String) {
        saveSession(driverId = driverId, driverName = driverName, role = role)
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getDriverId(): String? = prefs.getString(KEY_DRIVER_ID, null)
    fun getCustomerId(): String? = prefs.getString(KEY_DRIVER_ID, null)

    fun getDriverName(): String? = prefs.getString(KEY_DRIVER_NAME, "Driver")
    fun getDriverPhone(): String? = prefs.getString(KEY_DRIVER_PHONE, null)
    fun getDriverStatus(): String = prefs.getString(KEY_DRIVER_STATUS, "PENDING_DOCS") ?: "PENDING_DOCS"

    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "DRIVER") ?: "DRIVER"
    fun getDriverRole(): String = getUserRole()
    fun getCompanyName(): String? = prefs.getString(KEY_COMPANY_NAME, null)
    fun getVehicleType(): String? = prefs.getString(KEY_VEHICLE_TYPE, null)
    
    fun setUserRole(role: String) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply()
    }

    fun isOnline(): Boolean = prefs.getBoolean(KEY_IS_ONLINE, false)

    fun setOnline(online: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ONLINE, online).apply()
    }

    fun updateStatus(status: String) {
        prefs.edit().putString(KEY_DRIVER_STATUS, status).apply()
    }

    fun updateVehicleType(type: String?) {
        prefs.edit().putString(KEY_VEHICLE_TYPE, type).apply()
    }

    fun setActiveOrderId(orderId: Int?) {
        if (orderId == null) {
            prefs.edit().remove(KEY_ACTIVE_ORDER_ID).apply()
        } else {
            prefs.edit().putInt(KEY_ACTIVE_ORDER_ID, orderId).apply()
        }
    }

    fun getActiveOrderId(): Int? {
        val id = prefs.getInt(KEY_ACTIVE_ORDER_ID, -1)
        return if (id == -1) null else id
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun isFirstLogin(): Boolean = prefs.getBoolean(KEY_IS_FIRST_LOGIN, true)

    fun setFirstLogin(isFirst: Boolean) {
        prefs.edit().putBoolean(KEY_IS_FIRST_LOGIN, isFirst).apply()
    }
}
