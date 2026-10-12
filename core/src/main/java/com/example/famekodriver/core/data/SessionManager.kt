package com.example.famekodriver.core.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(val context: Context) {
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
        private const val KEY_PROFILE_PICTURE = "profilePicture"
        private const val KEY_IS_FIRST_LOGIN = "isLoginFirst"
        private const val KEY_ACCEPTED_TERMS_VERSION = "acceptedTermsVersion"

        // Notification Settings
        private const val KEY_NOTIF_TRIP_UPDATES = "notif_trip_updates"
        private const val KEY_NOTIF_MESSAGES = "notif_messages"
        private const val KEY_NOTIF_PROMOTIONS = "notif_promotions"
        private const val KEY_NOTIF_ACCOUNT = "notif_account"
    }

    fun setNotificationEnabled(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(key, enabled).apply()
    }

    fun isNotificationEnabled(key: String): Boolean {
        return prefs.getBoolean(key, true)
    }

    fun getTripUpdatesEnabled(): Boolean = isNotificationEnabled(KEY_NOTIF_TRIP_UPDATES)
    fun setTripUpdatesEnabled(enabled: Boolean) = setNotificationEnabled(KEY_NOTIF_TRIP_UPDATES, enabled)

    fun getMessagesEnabled(): Boolean = isNotificationEnabled(KEY_NOTIF_MESSAGES)
    fun setMessagesEnabled(enabled: Boolean) = setNotificationEnabled(KEY_NOTIF_MESSAGES, enabled)

    fun getPromotionsEnabled(): Boolean = isNotificationEnabled(KEY_NOTIF_PROMOTIONS)
    fun setPromotionsEnabled(enabled: Boolean) = setNotificationEnabled(KEY_NOTIF_PROMOTIONS, enabled)

    fun getAccountAlertsEnabled(): Boolean = isNotificationEnabled(KEY_NOTIF_ACCOUNT)
    fun setAccountAlertsEnabled(enabled: Boolean) = setNotificationEnabled(KEY_NOTIF_ACCOUNT, enabled)

    fun saveSession(
        driverId: String,
        driverName: String,
        status: String = "APPROVED",
        phone: String = "",
        role: String = "DRIVER",
        company: String? = null,
        vehicleType: String? = null,
        profilePicture: String? = null
    ) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_DRIVER_ID, driverId)
        editor.putString(KEY_DRIVER_NAME, driverName)
        editor.putString(KEY_DRIVER_PHONE, phone)
        editor.putString(KEY_DRIVER_STATUS, status)
        editor.putString(KEY_USER_ROLE, role)
        if (!company.isNullOrEmpty()) editor.putString(KEY_COMPANY_NAME, company)
        if (!vehicleType.isNullOrEmpty()) editor.putString(KEY_VEHICLE_TYPE, vehicleType)
        if (!profilePicture.isNullOrEmpty()) editor.putString(KEY_PROFILE_PICTURE, profilePicture)
        editor.apply()
    }

    fun saveDriverSession(
        driverId: String,
        driverName: String,
        status: String = "APPROVED",
        phone: String = "",
        role: String = "DRIVER"
    ) = saveSession(driverId, driverName, status, phone, role)

    fun getCustomerId(): String? = getDriverId()
    fun getCustomerIdInt(): Int? = getDriverId()?.removePrefix("DRIVER:")?.removePrefix("OWNER:")?.toIntOrNull()
    fun getCustomerIdStr(): String? = getDriverId()

    fun setUserRole(role: String) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply()
    }

    fun getUserRole(): String {
        return prefs.getString(KEY_USER_ROLE, "DRIVER") ?: "DRIVER"
    }

    fun getDriverRole(): String = getUserRole()

    fun getCompanyName(): String? {
        return prefs.getString(KEY_COMPANY_NAME, null)
    }

    fun getVehicleType(): String? {
        return prefs.getString(KEY_VEHICLE_TYPE, null)
    }

    fun getProfilePicture(): String? {
        return prefs.getString(KEY_PROFILE_PICTURE, null)
    }

    fun updateProfilePicture(url: String) {
        prefs.edit().putString(KEY_PROFILE_PICTURE, url).apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getDriverId(): String? {
        return prefs.getString(KEY_DRIVER_ID, null)
    }

    fun getDriverName(): String? {
        return prefs.getString(KEY_DRIVER_NAME, null)
    }

    fun getDriverPhone(): String? {
        return prefs.getString(KEY_DRIVER_PHONE, null)
    }

    fun getDriverStatus(): String {
        return prefs.getString(KEY_DRIVER_STATUS, "APPROVED") ?: "APPROVED"
    }

    fun updateStatus(status: String) {
        prefs.edit().putString(KEY_DRIVER_STATUS, status).apply()
    }

    fun isOnline(): Boolean {
        return prefs.getBoolean(KEY_IS_ONLINE, false)
    }

    fun setOnline(online: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ONLINE, online).apply()
    }

    fun setActiveOrderId(orderId: Int) {
        prefs.edit().putInt(KEY_ACTIVE_ORDER_ID, orderId).apply()
    }

    fun getActiveOrderId(): Int {
        return prefs.getInt(KEY_ACTIVE_ORDER_ID, -1)
    }

    fun clearActiveOrderId() {
        prefs.edit().remove(KEY_ACTIVE_ORDER_ID).apply()
    }

    fun getAcceptedTermsVersion(): String {
        return prefs.getString(KEY_ACCEPTED_TERMS_VERSION, "") ?: ""
    }

    fun setAcceptedTermsVersion(version: String) {
        prefs.edit().putString(KEY_ACCEPTED_TERMS_VERSION, version).apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun isFirstLogin(): Boolean {
        return prefs.getBoolean(KEY_IS_FIRST_LOGIN, true)
    }

    fun setFirstLoginDone() {
        prefs.edit().putBoolean(KEY_IS_FIRST_LOGIN, false).apply()
    }

    fun setFirstLogin(done: Boolean) {
        prefs.edit().putBoolean(KEY_IS_FIRST_LOGIN, !done).apply()
    }
}
