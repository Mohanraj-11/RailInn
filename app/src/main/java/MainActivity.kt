package com.example.traincheckinapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar


const val BASE_URL = "http://192.168.29.14:4000"





class MainActivity : ComponentActivity() {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val CAMERA_PERMISSION_REQUEST_CODE = 2 // Define a unique request code

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocationPermissions()
        requestCameraPermission()
        setContent {
            TrainCheckInApp() // Ensure this is a Composable function
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission granted
                } else {
                    Toast.makeText(this, "Location permission is required for this app to function.", Toast.LENGTH_SHORT).show()
                }
            }
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Camera permission granted
                } else {
                    Toast.makeText(this, "Camera permission is required for face recognition.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }
}



class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null && !geofencingEvent.hasError()) {
            val geofenceTransition = geofencingEvent.geofenceTransition
            val isEntering = geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER

            val sharedPreferences = context.getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putBoolean("isGeofenceEntered", isEntering)
                apply()
            }

            Log.d("GeofenceReceiver", if (isEntering) "Entered geofence area" else "Exited geofence area")
        } else {
            Log.e("GeofenceReceiver", "Error in geofence event: ${geofencingEvent?.errorCode}")
        }
    }
}

@Composable
fun OTPCheckInPage(navController: NavHostController) {
    var otp by remember { mutableStateOf("") }
    var otpStatus by remember { mutableStateOf("OTP Pending...") }
    var generatedOTP by remember { mutableStateOf("") }
    var isGeofenceEntered by remember { mutableStateOf(false) }
    var showLocationError by remember { mutableStateOf(false) }
    var isOtpSent by remember { mutableStateOf(false) } // Track if OTP has been sent

    val userPhoneNumber = "+916380524885" // Replace with actual user phone number
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Create a CookieJar to manage cookies
    val cookieJar = object : CookieJar {
        private val cookieStore = HashMap<HttpUrl, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url] = cookies.toMutableList()
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url] ?: emptyList()
        }
    }

    // Create OkHttpClient with CookieJar
    val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .build()

    fun sendOTP(latitude: Double, longitude: Double) {
        val requestBody = """{"mobile": "$userPhoneNumber", "latitude": $latitude, "longitude": $longitude}"""
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$BASE_URL/send-otp") // Adjust the URL as needed
            .post(requestBody)
            .build()
        Log.d("OTPCheckIn", "Sending OTP to $userPhoneNumber with latitude: $latitude and longitude: $longitude") // Log the request details

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                otpStatus = "Failed to send OTP."
                Log.e("OTPCheckIn", "Error sending OTP: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    Log.d("OTPCheckIn", "Response: $responseBody") // Log the response
                    generatedOTP = responseBody // Adjust this based on your server response
                    otpStatus = "OTP sent successfully!"
                    isOtpSent = true // Set OTP sent status to true
                } else {
                    otpStatus = "Failed to send OTP."
                    Log.e("OTPCheckIn", "Error sending OTP: ${response.message}")
                }
            }
        })
    }
    fun verifyOTP() {
        // Ensure the request body is correctly formatted without a trailing comma
        val requestBody = """{"otp": "$otp"}"""

        // Log the OTP being verified for debugging purposes
        Log.d("OTP Verification", "Verifying OTP: $otp")

        // Create the request to verify the OTP
        val request = Request.Builder()
            .url("$BASE_URL/verify-otp") // Ensure this is the correct endpoint
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())) // Set the request body
            .build()

        // Execute the request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                otpStatus = "Failed to verify OTP: ${e.message}"
                Log.e("OTP Verification", "Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                // Get the response body
                val responseBody = response.body?.string() ?: ""
                Log.d("OTP Verification", "Response: $responseBody") // Log the full response

                if (response.isSuccessful) {
                    // Parse the JSON response
                    val responseJson = JSONObject(responseBody)
                    val message = responseJson.optString("message", "")

                    // Check if the OTP was verified successfully
                    if (message == "OTP verified successfully") {
                        otpStatus = "OTP Verified Successfully!"
                        navController.navigate("journey_details") // Navigate on success
                    } else {
                        otpStatus = "Invalid OTP" // Handle invalid OTP case
                    }
                } else {
                    // Handle unsuccessful response
                    otpStatus = "Invalid OTP: ${response.message}"
                    Log.e("OTP Verification", "Error: ${response.message}, Body: $responseBody") // Log error details
                }
            }
        })
    }
    // Request Location
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    // Simulate geofence check
                    isGeofenceEntered = true // Replace with actual geofence logic
                    sendOTP(latitude, longitude) // Send OTP with fetched location
                } else {
                    showLocationError = true
                }
            }
        } else {
            showLocationError = true
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("OTP Check-In", style = MaterialTheme.typography.headlineLarge, color = Color(0xFFDC143C))
        Spacer(modifier = Modifier.height(32.dp))

        if (showLocationError) {
            Text("Location permission is required or unable to fetch location.", color = Color.Red)
        } else if (isGeofenceEntered) {
            if (!isOtpSent) {
                // Show the button to send OTP
                Button(
                    onClick = { /* This button is no longer needed since OTP is sent automatically */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sending OTP...", color = Color.White)
                }
            } else {
                // Show OTP input field and verify button
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it },
                    label = { Text("Enter OTP") },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { verifyOTP() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { verifyOTP() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verify OTP", color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Resend OTP Button
                Button(
                    onClick = {
                        // Resend OTP with the last known location
                        if (isGeofenceEntered) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                                if (location != null) {
                                    sendOTP(location.latitude, location.longitude)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resend OTP", color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(otpStatus, color = if (otpStatus.contains("Successfully")) Color.Green else Color.Red)
            }
        } else {
            Text("Please go to the nearest railway station and try again.")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainCheckInApp() {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_train),
                            contentDescription = "Train Logo",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rail-Inn", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color(0xFFDC143C))
            )
        },
        bottomBar = { Footer(navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("login") { LoginPage(navController) }
            composable("signup") { SignupPage(navController) }
            composable("forgot_password") { ForgotPasswordPage(navController) }
            composable("pnr_list") { PNRListPage(navController) }
            composable("biometric_check_in") { BiometricCheckInPage(navController) }
            composable("otp_check_in") { OTPCheckInPage(navController) }
            composable("journey_details") { JourneyDetailsPage() }
            composable("passenger_care") { PassengerCarePage(navController) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(title: String, navController: NavHostController) {
    CenterAlignedTopAppBar(
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
        },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(0xFFDC143C)
        )
    )
}

@SuppressLint("QueryPermissionsNeeded")
@Composable
fun PassengerCarePage(navController: NavHostController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CustomTopAppBar(title = "Customer Support", navController = navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Contact Us: +1234567890",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFDC143C)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Email: passenger.care@example.com",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFDC143C)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val contactNumber = "+1234567890"
                    val whatsappUrl = "https://wa.me/${contactNumber.removePrefix("+")}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "WhatsApp is not installed.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_whatsapp),
                    contentDescription = "WhatsApp Icon",
                    tint = Color.Green,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contact on WhatsApp", color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ForgotPasswordPage(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val client = OkHttpClient()

    fun sendPasswordResetEmail() {
        if (email.isEmpty()) {
            emailError = "Please enter your email address."
            return
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Please enter a valid email address."
            return
        } else {
            emailError = ""
        }

        isLoading = true // Start loading

        // Create the request body
        val requestBody = """{"email":"$email"}"""
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$BASE_URL/send-reset-email") // Adjust the URL as needed
            .post(requestBody)
            .build()

        // Make the network call
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                isLoading = false // Stop loading
                message = "Failed to send email: ${e.message}"
                Log.e("ForgotPassword", "Error: ${e.message}") // Log the error
            }

            override fun onResponse(call: Call, response: Response) {
                isLoading = false // Stop loading
                if (response.isSuccessful) {
                    // Assuming the response contains a token for the reset
                    val token = response.body?.string() // Adjust based on your API response
                    navController.navigate("reset_password/$token") // Navigate to Reset Password page
                    message = "Password reset link has been sent to your email."
                } else {
                    message = "Error: ${response.message}"
                }
            }
        })
    }
    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Forgot Password",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFDC143C)
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Enter your Email Address") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color.White),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (emailError.isNotEmpty()) {
            Text(text = emailError, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { sendPasswordResetEmail() },
            colors = ButtonDefaults.buttonColors (containerColor = Color(0xFFDC143C)),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Send Reset Link", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Text(
                message,
                color = if (message.contains("sent")) Color.Green else Color.Red,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
@Composable
fun Reset_password(navController: NavHostController, token: String) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    // Function to reset the password
    fun resetPassword() {
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            passwordError = "Please fill in both fields."
            return
        } else if (newPassword != confirmPassword) {
            passwordError = "Passwords do not match."
            return
        } else {
            passwordError = ""
        }

        // Create the request body
        val requestBody = """
            {
                "token": "$token",
                "newPassword": "$newPassword"
            }
        """.trimIndent().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$BASE_URL/reset-password") // Adjust the URL as needed
            .post(requestBody)
            .build()

        // Make the network call
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                message = "Failed to reset password: ${e.message}"
                Log.e("ResetPassword", "Error: ${e.message}") // Log the error
            }

            override fun onResponse(call: Call, response: Response) {
                message = if (response.isSuccessful) {
                    "Password has been reset successfully!"
                } else {
                    "Error: ${response.message}"
                }
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Reset Password",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFDC143C)
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color.White),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color.White),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (passwordError.isNotEmpty()) {
            Text(text = passwordError, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { resetPassword() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Password", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Text(
                message,
                color = if (message.contains("successfully")) Color.Green else Color.Red,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun Footer(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Rover Assistant",
            color = Color(0xFFDC143C),
            modifier = Modifier.clickable {
                navController.navigate("passenger_care")
            }
        )
    }
}

@Composable
fun PasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        modifier = modifier,
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val icon = if (isPasswordVisible) {
                Icons.Filled.Visibility // Use an icon for visible password
            } else {
                Icons.Filled.VisibilityOff // Use an icon for hidden password
            }
            IconButton(onClick = {
                isPasswordVisible = !isPasswordVisible
                if (isPasswordVisible) {
                    // Start a coroutine to hide the password after 10 seconds
                    coroutineScope.launch {
                        delay(10000) // Delay for 10 seconds
                        isPasswordVisible = false // Hide the password
                    }
                }
            }) {
                Icon(imageVector = icon, contentDescription = if (isPasswordVisible) "Hide password" else "Show password")
            }
        }
    )
}

@Composable
fun LoginPage(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFEFEF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Your Journey Awaits",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFDC143C)
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    PasswordField(
                        password = password,
                        onPasswordChange = { password = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isLoading) return@Button // Prevent multiple clicks
                            isLoading = true
                            errorMessage = ""
                            coroutineScope.launch {
                                loginUser (navController, email, password) { message ->
                                    isLoading = false
                                    errorMessage = message
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading // Disable button while loading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Login", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = Color.Red)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Create Account",
                        color = Color(0xFFDC143C),
                        modifier = Modifier
                            .clickable { navController.navigate("signup") }
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Forgot Password?",
                        color = Color(0xFFDC143C),
                        modifier = Modifier
                            .clickable { navController.navigate("forgot_password") }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

suspend fun loginUser(
    navController: NavHostController,
    email: String,
    password: String,
    onErrorMessage: (String) -> Unit
) {
    if (email.isNotEmpty() && password.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val requestBody = """
                    {
                        "email": "$email",
                        "password": "$password"
                    }
                """.trimIndent()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("$BASE_URL/login")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute() // Make network call

                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    if (!responseData.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            navController.navigate("pnr_list") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onErrorMessage("Unexpected response from server.")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onErrorMessage("Login failed: ${response.message}")
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onErrorMessage("Network error: ${e.localizedMessage}")
            }
        }
    } else {
        onErrorMessage("Please enter a valid email and password.")
    }
}

@Composable
fun SignupPage(navController: NavHostController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var aadhaarNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    fun validateAadhaar(aadhaar: String): Boolean {
        return aadhaar.matches(Regex("^[0-9]{12}$"))
    }

    fun validateEmail(email: String): Boolean {
        return email.contains("@") && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validateName(name: String): Boolean {
        return name.all { it.isLetter() } // Check if all characters are letters
    }

    fun validatePassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isDigit() } &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() }
    }

    fun validateSignup(): Boolean {
        return name.isNotEmpty() && validateName(name) &&
                validateEmail(email) && mobile.isNotEmpty() &&
                aadhaarNumber.isNotEmpty() && validatePassword(password) &&
                confirmPassword.isNotEmpty() && validateAadhaar(aadhaarNumber) &&
                password == confirmPassword
    }

    fun signupUser () {
        // Log the current values of the fields for debugging
        Log.d("Signup", "Name: $name, Email: $email, Mobile: $mobile, Aadhaar: $aadhaarNumber, Password: $password, Confirm Password: $confirmPassword")

        if (validateSignup()) {
            val client = OkHttpClient()
            val requestBody = """
        {
            "name": "$name",
            "email": "$email",
            "mobile": "$mobile",
            "password": "$password",
            "aadhaarNumber": "$aadhaarNumber"
        }
        """.trimIndent().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("$BASE_URL/signup") // Adjust the URL as needed
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Update error message on the main thread
                    (navController.context as? ComponentActivity)?.runOnUiThread {
                        errorMessage = "Error: ${e.message}"
                        Log.e("Signup", "Error: ${e.message}") // Log the error
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    // Ensure UI updates are done on the main thread
                    (navController.context as? ComponentActivity)?.runOnUiThread {
                        if (response.isSuccessful) {
                            errorMessage = "You are registered successfully!"
                            navController.navigate("login") // Navigate to login after successful signup
                        } else {
                            // Log the response body for debugging
                            val responseBody = response.body?.string() ?: "No response body"
                            errorMessage = "Error: ${response.message} - $responseBody"
                            Log.e("Signup", "Error: ${response.message}, Body: $responseBody") // Log the error
                        }
                    }
                }
            })
        } else {
            errorMessage = when {
                name.isEmpty() || email.isEmpty() || mobile.isEmpty() || aadhaarNumber.isEmpty() -> "Please fill all fields."
                !validateName(name) -> "Name must contain only characters."
                !validateEmail(email) -> "Please enter a valid email address."
                !validatePassword(password) -> "Password must be at least 8 characters long and contain at least one digit, one uppercase letter, and one lowercase letter."
                !validateAadhaar(aadhaarNumber) -> "Please enter a valid Aadhaar number."
                password != confirmPassword -> "Passwords do not match."
                else -> "Please fill all fields correctly."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
            .verticalScroll(rememberScrollState()), // Enable vertical scrolling
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Create Account",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFDC143C)
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp) // Rounded corners
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp) // Rounded corners
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mobile,
            onValueChange = { mobile = it },
            label = { Text("Mobile") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp) // Rounded corners
        )
        Spacer(modifier = Modifier.height(16.dp))

        PasswordField(
            password = password,
            onPasswordChange = { password = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        PasswordField(
            password = confirmPassword,
            onPasswordChange = { confirmPassword = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = aadhaarNumber,
            onValueChange = { aadhaarNumber = it },
            label = { Text("Aadhaar Number") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp) // Rounded corners
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { signupUser  () },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp) // Rounded corners
        ) {
            Text("Sign Up", color = Color.White)
        }

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PNRListPage(navController: NavHostController) {

    remember {
        val calendar = Calendar.getInstance()
        String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
    }

    var selectedDate by remember { mutableStateOf("2024-12-19") }
    var showDatePicker by remember { mutableStateOf(false) }
    var pnrList by remember { mutableStateOf(listOf<String>()) } // Declare pnrList here
    var showCheckInDialog by remember { mutableStateOf(false) }
    var selectedPNR by remember { mutableStateOf("") }
    LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PNR List") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFDC143C), // Background color
                    titleContentColor = Color.White // Title color
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Your future Pnr Select Date: $selectedDate", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showDatePicker = true }) {
                Text("Pick Date")
            }
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(pnrList) { pnr ->
                    PNRItem(pnr) {
                        selectedPNR = pnr
                        showCheckInDialog = true // Show the dialog when a PNR is clicked
                    }
                }
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                onDateSelected = { date ->
                    selectedDate = date
                    showDatePicker = false
                    generatePNRForDate { newPNR ->
                        pnrList = pnrList + newPNR // Update the PNR list with the new PNR
                    }
                }
            )
        }

        // Check-In Options Dialog
        if (showCheckInDialog) {
            CheckInOptionsDialog(
                onDismiss = { showCheckInDialog = false },
                onOtpCheckInSuccess = {
                    navController.navigate("otp_check_in") // Navigate to OTP Check-In page
                    showCheckInDialog = false // Dismiss the dialog
                },
                onBiometricCheckInSuccess = {
                    navController.navigate("biometric_check_in") // Navigate to Biometric Check-In page
                    showCheckInDialog = false // Dismiss the dialog
                }
            )
        }
    }
}

fun generatePNRForDate(onPNRGenerated: (String) -> Unit) {
    // Generate a random PNR number starting with "46"
    val randomSuffix = (100000..999999).random() // Generate a random number between 100000 and 999999
    val pnr = "46$randomSuffix" // Concatenate "46" with the random number

    // Call the callback to update the PNR list on the main thread
    Handler(Looper.getMainLooper()).post {
        onPNRGenerated(pnr) // Call the callback to update the PNR list
    }
}

@Composable
fun CheckInOptionsDialog(
    onDismiss: () -> Unit,
    onOtpCheckInSuccess: () -> Unit,
    onBiometricCheckInSuccess: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Check-In Method") },
        text = {
            Column {
                Button(
                    onClick = {
                        onOtpCheckInSuccess() // Call the success callback for OTP Check-In
                        onDismiss() // Dismiss the dialog
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("OTP Check-In", color = Color.White)
                }
                Button(
                    onClick = {
                        onBiometricCheckInSuccess() // Call the success callback for Biometric Check-In
                        onDismiss() // Dismiss the dialog
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Biometric Check-In", color = Color.White)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@SuppressLint("DefaultLocale")
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            onDateSelected(selectedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    datePickerDialog.setOnDismissListener { onDismissRequest() }
    datePickerDialog.show()
}

@Composable
fun PNRItem(pnr: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C))
    ) {
        Text(pnr, color = Color.White)
    }
}

@Composable
fun BiometricCheckInPage(navController: NavHostController) {
    var authenticationState by remember { mutableStateOf<AuthenticationState>(AuthenticationState.Pending) }

    val context = LocalContext.current

    // Check if biometric authentication is supported
    if (!isBiometricSupported(context)) {
        Text("Biometric authentication is not supported on this device.", color = Color.Red)
        return
    }

    // Verify context is a FragmentActivity
    val activity = context as? FragmentActivity
    if (activity==null) {
        Text("Biometric authentication is not supported in this context.", color = Color.Red)
        return
    }

    val executor = remember { ContextCompat.getMainExecutor(context) }
    val biometricPrompt = remember {
        BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    authenticationState = AuthenticationState.Success
                    navController.navigate("journey_details") // Navigate on success
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    authenticationState = AuthenticationState.Failed
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    authenticationState = AuthenticationState.Error(errString.toString())
                }
            }
        )
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Log in using your fingerprint or face")
            .setNegativeButtonText("Cancel")
            .setConfirmationRequired(true) // Require confirmation for biometric authentication
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG) // Allow strong biometric authentication
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Biometric Check-In", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        when (authenticationState) {
            is AuthenticationState.Pending -> {
                Text("Biometric Authentication Pending", color = Color.Gray)
                Button(
                    onClick = { biometricPrompt.authenticate(promptInfo) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C))
                ) {
                    Text("Start Authentication", color = Color.White)
                }
            }
            is AuthenticationState.Success -> {
                Text("Authentication Succeeded!", color = Color.Green)
            }
            is AuthenticationState.Failed -> {
                Text("Authentication Failed. Please try again.", color = Color.Red)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { biometricPrompt.authenticate(promptInfo) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C))
                ) {
                    Text("Retry Authentication", color = Color.White)
                }
            }
            is AuthenticationState.Error -> {
                val errorMessage = (authenticationState as AuthenticationState.Error).message
                Text("Error: $errorMessage", color = Color.Red)
            }

            else -> {}
        }
    }
}

// Function to check if biometric authentication is supported
fun isBiometricSupported(context: Context): Boolean {
    val biometricManager = BiometricManager.from(context)
    val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    return result == BiometricManager.BIOMETRIC_SUCCESS
}

// Authentication State for Tracking Biometric Status
sealed class AuthenticationState {
    data object Pending : AuthenticationState()
    data object Success : AuthenticationState()
    data object Failed : AuthenticationState()
    data class Error(val message: String) : AuthenticationState()
}

data class Journey(
    val pnr: String,
    val trainNumber: String,
    val departure: String,
    val arrival: String,
    val date: String,
    val status: String
)

@Composable
fun JourneyDetailsPage() {
    val journeys by remember { mutableStateOf(listOf<Journey>()) }
    val loading by remember { mutableStateOf(true) }
    val errorMessage by remember { mutableStateOf("") }

    val userId=""

    LaunchedEffect(userId) {
        fetchJourneyDetails(userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Journey Details", style = MaterialTheme.typography.titleLarge, color = Color(0xFFDC143C))
        Spacer(modifier = Modifier.height(32.dp))

        if (loading) {
            CircularProgressIndicator()
        } else if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = Color.Red)
        } else if (journeys.isEmpty()) {
            Text("Your journey details will be displayed here.")
        } else {
            LazyColumn {
                items (journeys) { journey ->
                    JourneyItem(journey)
                }
            }
        }
    }
}

fun fetchJourneyDetails(userId: String) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("$BASE_URL/fetch-journeys/$userId")
        .get()
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("JourneyDetails", "Error: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                response.body?.string()
                // Parse the response and update the state
            } else {
                Log.e("JourneyDetails", "Error: ${response.message}")
            }
        }
    })
}

@Composable
fun JourneyItem(journey: Journey) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("PNR: ${journey.pnr}", style = MaterialTheme.typography.titleMedium)
            Text("Train Number: ${journey.trainNumber}")
            Text("Departure: ${journey.departure}")
            Text("Arrival: ${journey.arrival}")
            Text("Date: ${journey.date}")
            Text("Status: ${journey.status}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TrainCheckInApp()
}