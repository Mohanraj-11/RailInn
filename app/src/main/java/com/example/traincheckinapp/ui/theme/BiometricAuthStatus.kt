package com.example.traincheckinapp.ui.theme

enum class BiometricAuthStatus(val id: Int, val message: String) {
    READY(1, "Biometric authentication is ready"),
    NOT_AVAILABLE(-1, "Biometric authentication is not available"),
    TEMPORARY_NOT_AVAILABLE(-2, "Biometric authentication is temporarily unavailable"),
    AVAILABLE_BUT_NOT_ENROLLED(-3, "Biometric hardware is available, but no biometrics are enrolled")
}
