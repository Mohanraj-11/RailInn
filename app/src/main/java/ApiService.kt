package com.example.traincheckinapp

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/signup")
    fun signup(@Body request: SignupRequest): Call<ResponseBody>

    @POST("/login")
    fun login(@Body request: LoginRequest): Call<ResponseBody>

    @POST("/send-otp")
    fun sendOtp(@Body request: OtpRequest): Call<ResponseBody>



    @POST("/journey-details")
    fun fetchJourneyDetails(@Body request: JourneyDetailsRequest): Call<ResponseBody>
}
