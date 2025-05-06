package com.example.traincheckinapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.traincheckinapp.ui.theme.BiometricAuthStatus
import com.example.traincheckinapp.ui.theme.biometricAuthenticator
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit


const val BASE_URL = "http://192.168.29.14:4000"





class MainActivity : FragmentActivity() {
    private val locationPermissionRequestCode = 1
    private val cameraPermissionRequestCode = 2

    @RequiresApi(Build.VERSION_CODES.Q)
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
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraPermissionRequestCode)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            locationPermissionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission granted
                } else {
                    Toast.makeText(this, "Location permission is required for this app to function.", Toast.LENGTH_SHORT).show()
                }
            }
            cameraPermissionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Camera permission granted
                } else {
                    Toast.makeText(this, "Camera permission is required for face recognition.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("In linedApi")
    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION), locationPermissionRequestCode)
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
    var isGeofenceEntered by remember { mutableStateOf(false) }
    var showLocationError by remember { mutableStateOf(false) }
    var isOtpSent by remember { mutableStateOf(false) } // Track if OTP has been sent
    var isLoading by remember { mutableStateOf(false) } // Track loading state
    var showSuccessDialog by remember { mutableStateOf(false) } // Track success dialog visibility

    // New state variables for error dialog
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val userPhoneNumber = "+916380524885" // Replace with actual user phone number
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Create OkHttpClient
    val client = OkHttpClient()

    // Function to send OTP
    fun sendOTP(latitude: Double, longitude: Double) {
        isLoading = true // Start loading
        val requestBody = """{"mobile": "$userPhoneNumber", "latitude": $latitude, "longitude": $longitude}"""
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$BASE_URL/send-otp") // Adjust the URL as needed
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                isLoading = false // Stop loading
                otpStatus = "Failed to send OTP: ${e.message}"
                Log.e("OTPCheckIn", "Error sending OTP: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                isLoading = false // Stop loading
                if (response.isSuccessful) {
                    otpStatus = "OTP sent successfully!"
                    isOtpSent = true // Set OTP sent status to true
                } else {
                    otpStatus = "Failed to send OTP: ${response.message}"
                    Log.e("OTPCheckIn", "Error sending OTP: ${response.message}")
                }
            }
        })
    }

    // Function to verify OTP
    fun verifyOTP() {
        isLoading = true // Start loading
        val requestBody = """{"mobile": "$userPhoneNumber", "otp": "$otp"}"""
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$BASE_URL/verify-otp")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure on the main thread
                Handler(Looper.getMainLooper()).post {
                    isLoading = false // Stop loading
                    errorMessage = "Failed to verify OTP: ${e.message}" // Set error message
                    showErrorDialog = true // Show error dialog
                    Log.e("OTP Verification", "Error: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Handler(Looper.getMainLooper()).post {
                    isLoading = false // Stop loading
                    if (response.isSuccessful) {
                        try {
                            if (responseBody.isNullOrEmpty()) {
                                otpStatus = "Unexpected empty response from server"
                            } else {
                                try {
                                    // Try parsing as JSON
                                    val responseJson = JSONObject(responseBody)
                                    val message = responseJson.optString("message", "")
                                    if (message == "OTP verified successfully") {
                                        otpStatus = "OTP Verified Successfully!"
                                        showSuccessDialog = true // Show success dialog
                                        navController.navigate("journey_details") // Navigate on success
                                    } else {
                                        otpStatus = "Invalid OTP"
                                        errorMessage = "Invalid OTP" // Set error message
                                        showErrorDialog = true // Show error dialog
                                    }
                                } catch (e: JSONException) {
                                    // Handle plain string response
                                    if (responseBody == "OTP verified successfully") {
                                        otpStatus = "OTP Verified Successfully!"
                                        showSuccessDialog = true // Show success dialog
                                        navController.navigate("journey_details")
                                    } else {
                                        errorMessage = "Error parsing response: ${e.message}" // Set error message
                                        showErrorDialog = true // Show error dialog
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            otpStatus = "Unexpected error: ${e.message}"
                            Log.e("OTP Verification", "Error: ${e.message}")
                        }
                    } else {
                        errorMessage = "Invalid OTP: ${response.message}" // Set error message
                        showErrorDialog = true // Show error dialog
                        Log.e("OTP Verification", "Error: ${response.message}")
                    }
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
                    isGeofenceEntered = true // Simulate geofence check
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
                // Show loading indicator while sending OTP
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Sending OTP...")
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
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Verify OTP", color = Color.White)
                    }
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

        // Back to Home Button
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("pnr_list") }, // Navigate back to PNR List page
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Home", color = Color.White)}
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Success", color = Color.White) },
            text = {
                Surface(
                    modifier = Modifier.padding(16.dp), // Add padding around the content
                    color = Color(0xFF808080), // Set background color
                    shape = MaterialTheme.shapes.medium, // Set shape
                ) {
                    Text(
                        "Check-in successfully ✅ !",
                        color = Color.Black,
                        modifier = Modifier.padding(16.dp) // Add padding inside the Surface
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false // Dismiss the dialog first
                    Handler(Looper.getMainLooper()).post {
                        navController.navigate("journey_details") // Then navigate
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }

// Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error", color = Color.White) },
            text = {
                Surface(
                    modifier = Modifier.padding(16.dp), // Add padding around the content
                    color = Color.Gray, // Set background color to grey
                    shape = MaterialTheme.shapes.medium, // Set shape
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Error, // Use an error icon
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp) // Adjust size as needed
                        )
                        Text(
                            " Check-in Unsuccessful", // Display the error message
                            color = Color.Black,
                            modifier = Modifier.padding(start = 8.dp, end = 16.dp) // Add padding inside the Surface
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showErrorDialog = false // Dismiss the dialog
                }) {
                    Text("OK")
                }
            }
        )
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
            composable("otp_check_in") { OTPCheckInPage(navController) }
            composable("journey_details") { JourneyDetailsPage(navController) }
            composable("biometric_check_in") { BiometricCheckInPage(navController) }
            composable("passenger_care") { PassengerCarePage(navController) }
            composable("reset_password/{token}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token")
                Reset_password(navController, token ?: "")
            }
        }
    }
}


@Composable
fun BiometricCheckInPage(navController: NavHostController) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var isSuccessDialogVisible by remember { mutableStateOf(false) }
    var isErrorDialogVisible by remember { mutableStateOf(false) }
    var backgroundColor by remember { mutableStateOf(Color.White) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(backgroundColor),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Biometric Check-In",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFDC143C)
        )
        Spacer(modifier = Modifier.height(32.dp))




        TextButton(onClick = {
            coroutineScope.launch {
                Log.d("BiometricCheckInPage", "Context is: ${context::class.java.simpleName}")

                val activity = context as? FragmentActivity
                if (activity != null) {
                    val biometricAuth = biometricAuthenticator(context)
                    val status = biometricAuth.isBiometricAuthAvailable()

                    if (status == BiometricAuthStatus.READY) {
                        biometricAuth.promptBiometricAuth(
                            title = "Biometric Authentication",
                            subTitle = "Authenticate using your fingerprint",
                            negativeButtonText = "Cancel",
                            fragmentActivity = activity,
                            onSuccess = {
                                message = "Check-in Successful!"
                                backgroundColor = Color.Gray
                                isSuccessDialogVisible = true
                            },
                            onFailed = {
                                message = "Authentication Failed. Please try again."
                                isErrorDialogVisible = true
                            },
                            onError = { errorCode, errorString ->
                                message = "Error: $errorString (Code: $errorCode)"
                                isErrorDialogVisible = true
                            }
                        )
                    } else {
                        message = "Biometric authentication not available: ${status.message}"
                    }
                } else {
                    message = "Error: Biometric authentication requires a FragmentActivity context."
                }
            }
        }) {
            Text(text = "Authenticate with Biometrics")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message)

        // Success Dialog
        if (isSuccessDialogVisible) {
            AlertDialog(
                onDismissRequest = { isSuccessDialogVisible = false },
                title = { Text("Success") },
                text = { Text("Check-in successfully ✅ !") },
                confirmButton = {
                    TextButton(onClick = {
                        isSuccessDialogVisible = false
                        navController.navigate("journey_details")
                    }) {
                        Text("OK")
                    }
                }
            )
        }
        //Error Dialog
            if (isErrorDialogVisible) {
                AlertDialog(
                    onDismissRequest = { isErrorDialogVisible = false },
                    title = { Text("Error") },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Close, // Use a close or error icon
                                contentDescription = "Error",
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp) // Adjust size as needed
                            )
                            Text(
                                text = " Check-in Unsuccessful",
                                color = Color.Red,
                                fontSize = 20.sp // Adjust font size as needed
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            isErrorDialogVisible = false
                            // No navigation here, just close the dialog
                        }) {
                            Text("OK")
                        }
                    }
                )
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

@SuppressLint("QueryPermissionsNeeded", "UseKtx")
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
                "Contact Us: 8667744339",
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
                    val contactNumber = "918667744339" // Ensure the correct country code without "+"
                    val whatsappUrl = "https://wa.me/$contactNumber"

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = whatsappUrl.toUri()
                        setPackage("com.whatsapp") // This ensures WhatsApp opens directly
                    }

                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "WhatsApp is not installed.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_whatsapp),
                    contentDescription = "WhatsApp Icon",
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

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

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

        // Make the network call using Coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                isLoading = false // Stop loading
                if (response.isSuccessful) {
                    val token = response.body?.string() // Adjust based on your API response
                    if (token != null) {
                        withContext(Dispatchers.Main) {
                            navController.navigate("reset_password/$token") // Ensure this route exists in your navigation graph
                            message = "Password reset link has been sent to your email."
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            message = "Error: No token received."
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        message = "Error: ${response.message}"
                    }
                }
            } catch (e: IOException) {
                isLoading = false // Stop loading
                withContext(Dispatchers.Main) {
                    message = "Failed to send email: ${e.message}"
                    Log.e("ForgotPassword", "Error: ${e.message}") // Log the error
                }
            }
        }
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
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
                if (response.isSuccessful) {
                    message = "Password has been reset successfully!"
                    // Navigate to the login page after a successful password reset
                    navController.navigate("login") { // Adjust the route as needed
                        popUpTo("reset_password") { inclusive = true } // Clear the back stack
                    }
                } else {
                    message = "Error: ${response.message}"
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
    var selectedLanguage by remember { mutableStateOf("English") }
    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFEFEF))
    ) {
        // Enable vertical scrolling
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Add vertical scroll
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Language Dropdown Menu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Row(
                    modifier = Modifier
                        .clickable { expanded = true }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedLanguage, fontSize = 16.sp, color = Color.Black)
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown Arrow",
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    val languages = listOf("English", "தமிழ்", "हिन्दी")
                    languages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(text = language) },
                            onClick = {
                                selectedLanguage = language
                                expanded = false
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
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
                            if (isLoading) return@Button
                            isLoading = true
                            errorMessage = ""

                            coroutineScope.launch {
                                loginUser(navController, email, password) { message ->
                                    isLoading = false
                                    errorMessage = message
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Login", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Biometric Authentication Button
                    TextButton(
                        onClick = {
                            val biometricAuth = biometricAuthenticator(context)
                            val status = biometricAuth.isBiometricAuthAvailable()

                            if (status == BiometricAuthStatus.READY) {
                                biometricAuth.promptBiometricAuth(
                                    title = "Biometric Authentication",
                                    subTitle = "Authenticate using your fingerprint",
                                    negativeButtonText = "Cancel",
                                    fragmentActivity = context as FragmentActivity,
                                    onSuccess = {
                                        navController.navigate("pnr_list") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    },
                                    onFailed = {
                                        errorMessage = "Authentication Failed. Try again."
                                    },
                                    onError = { errorCode, errorString ->
                                        errorMessage = "Error: $errorString (Code: $errorCode)"
                                    }
                                )
                            } else {
                                errorMessage = "Biometric authentication not available: ${status.message}"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_fingerprint),
                            contentDescription = "Fingerprint Icon",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Create Account and Forgot Password Links
                    Text(
                        text = "Create Account",
                        color = Color(0xFFDC143C),
                        modifier = Modifier
                            .clickable { navController.navigate("signup") }
                            .padding(8.dp)
                    )

                    Text(
                        text = "Forgot Password?",
                        color = Color(0xFFDC143C),
                        modifier = Modifier
                            .clickable { navController.navigate("forgot_password") }
                            .padding(8.dp)
                    )

                    // Display error message if any
                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(errorMessage, color = Color.Red)
                    }
                }
            }
        }
    }
}


suspend fun loginUser (
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
                        onErrorMessage("Login failed. Please try again.")
                    }
                } else {
                    onErrorMessage("Error: ${response.message}")
                }
            }
        } catch (e: Exception) {
            onErrorMessage("Exception: ${e.message}")
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

    LocalContext.current

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

    // Get the current date
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

    // Create the DatePickerDialog
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            onDateSelected(selectedDate)
        },
        currentYear,
        currentMonth,
        currentDay
    )

    // Set the minimum date to today
    datePickerDialog.datePicker.minDate = calendar.timeInMillis

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
        Text("PNR NO. $pnr", color = Color.White)
    }
}

data class Journey(
    val trainName: String,
    val trainNumber: String,
    val departure: String,
    val arrival: String,
    val date: String,
    val status: String,
    val platformNumber: String // Added platform number
)

@Composable
fun JourneyDetailsPage(navController: NavHostController) {
    val journeys = remember { mutableStateOf(listOf<Journey>()) }
    val loading = remember { mutableStateOf(true) }
    val errorMessage = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // Simulate fetching dummy data
        fetchDummyJourneyDetails(journeys, loading, errorMessage)
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

        if (loading.value) {
            CircularProgressIndicator()
        } else if (errorMessage.value.isNotEmpty()) {
            Text(text = errorMessage.value, color = Color.Red)
        } else if (journeys.value.isEmpty()) {
            Text("Your journey details will be displayed here.")
        } else {
            LazyColumn {
                items(journeys.value) { journey ->
                    JourneyItem(journey)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { navController.navigate("pnr_List") }) {
            Text("Back to Home")
        }
    }
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
            // Display each detail in a row-like format
            DetailRow(label = "Train Name:", value = journey.trainName)
            DetailRow(label = "Train Number:", value = journey.trainNumber)
            DetailRow(label = "Departure:", value = journey.departure)
            DetailRow(label = "Arrival:", value = journey.arrival)
            DetailRow(label = "Date:", value = journey.date)
            DetailRow(label = "Status:", value = journey.status)
            DetailRow(label = "Platform Number:", value = journey.platformNumber)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun fetchDummyJourneyDetails(
    journeys: MutableState<List<Journey>>,
    loading: MutableState<Boolean>,
    errorMessage: MutableState<String>
) {
    // Simulate a delay to mimic network call
    kotlinx.coroutines.GlobalScope.launch {
        delay(2000) // 2-second delay
        try {
            // Add dummy data with platform number
            journeys.value = listOf(
                Journey(
                    trainName = "Maharani Express",
                    trainNumber = "12433",
                    departure = "New Delhi",
                    arrival = "Mumbai Central",
                    date = "2025-01-25",
                    status = "On-time",
                    platformNumber = "5" // Added platform number
                )
            )
            loading.value = false
        } catch (e: Exception) {
            loading.value = false
            errorMessage.value = "Failed to load journey details: ${e.message}"
        }
    }
}



@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TrainCheckInApp()
}