package com.example.famekodriver.core.domain.model

data class CustomerRegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val password: String,
    val region: String? = null,
    val profilePicture: String? = null,
    val firebaseUid: String? = null
)

data class LoginRequest(
    val phone: String? = null,
    val email: String? = null,
    val password: String? = null,
    val googleToken: String? = null
)

data class OtpRequest(
    val phone: String,
    val method: String = "WHATSAPP"
)

data class OtpVerifyRequest(
    val phone: String,
    val otp: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String?,
    val user_id: String?,
    val name: String?,
    val status: String? = null,
    val profile_picture: String? = null,
    val user_role: String? = "DRIVER",
    val company_name: String? = null
)
