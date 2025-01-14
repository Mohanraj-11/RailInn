package com.example.traincheckinapp

data class SignupRequest(
    val name: String,
    val email: String,
    val mobile: String,
    val password: String,

    val aadhaarNumber: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class OtpRequest(
    val mobile: String,
    val latitude: Double,
    val longitude: Double
)

data class JourneyDetailsRequest(
    val pnr: String
)