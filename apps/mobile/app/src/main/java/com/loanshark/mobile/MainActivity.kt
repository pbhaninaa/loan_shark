package com.loanshark.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoanSharkApp()
                }
            }
        }
    }
}

@Composable
fun LoanSharkApp(viewModel: LoanViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf("login") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Loan Shark Borrower App", style = MaterialTheme.typography.headlineSmall)
        Text(text = viewModel.message)
        if (viewModel.borrowerStatus != null) {
            Text(text = "Borrower status: ${viewModel.borrowerStatus}")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { currentScreen = "login" }) { Text("Login") }
            Button(onClick = { currentScreen = "register" }) { Text("Register") }
            Button(onClick = {
                currentScreen = "verification"
                viewModel.loadBorrowerVerification()
            }) { Text("Verification") }
            Button(onClick = { currentScreen = "loan" }) { Text("Loan") }
            Button(onClick = { currentScreen = "schedule" }) { Text("Schedule") }
            Button(onClick = {
                currentScreen = "notifications"
                viewModel.loadNotifications()
            }) { Text("Notifications") }
        }

        when (currentScreen) {
            "login" -> LoginScreen(viewModel)
            "register" -> RegisterScreen(viewModel)
            "verification" -> VerificationScreen(viewModel)
            "loan" -> LoanScreen(viewModel)
            "schedule" -> ScheduleScreen(viewModel)
            "notifications" -> NotificationScreen(viewModel)
        }
    }
}

@Composable
private fun LoginScreen(viewModel: LoanViewModel) {
    var username by remember { mutableStateOf("borrower.demo") }
    var password by remember { mutableStateOf("Password1!") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
        Button(onClick = { viewModel.login(username, password) }) { Text("Login") }
    }
}

@Composable
private fun RegisterScreen(viewModel: LoanViewModel) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var registrationStep by remember { mutableStateOf(1) }
    var registrationSubmitted by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("Password1!") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var employmentStatus by remember { mutableStateOf("EMPLOYED") }
    var monthlyIncome by remember { mutableStateOf("5000") }
    var employerName by remember { mutableStateOf("") }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var selfieBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationName by remember { mutableStateOf<String?>(null) }
    var locationPermissionDenied by remember { mutableStateOf(false) }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pdfUri = uri
    }
    val selfieCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        selfieBitmap = bitmap
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            selfieCapture.launch(null)
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.any { it }) {
            fetchLocation(
                fusedLocationClient = fusedLocationClient,
                onLocation = { lat, lng, resolvedName ->
                    latitude = lat
                    longitude = lng
                    locationName = resolvedName
                    locationPermissionDenied = false
                },
                onError = { viewModel.publishMessage(it) },
                geocoder = Geocoder(context, Locale.ENGLISH)
            )
        } else {
            locationPermissionDenied = true
            locationName = null
            latitude = null
            longitude = null
        }
    }

    LaunchedEffect(registrationStep) {
        if (registrationStep == 2) {
            val fineGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (fineGranted || coarseGranted) {
                fetchLocation(
                    fusedLocationClient = fusedLocationClient,
                    onLocation = { lat, lng, resolvedName ->
                        latitude = lat
                        longitude = lng
                        locationName = resolvedName
                        locationPermissionDenied = false
                    },
                    onError = { viewModel.publishMessage(it) },
                    geocoder = Geocoder(context, Locale.ENGLISH)
                )
            } else {
                locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
    }

    if (registrationSubmitted) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Profile created. You must wait for owner review before full access. Check your verification status.")
            Button(onClick = { registrationSubmitted = false; registrationStep = 1 }) { Text("Start new registration") }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Step $registrationStep of 5: " + when (registrationStep) {
            1 -> "Personal details"
            2 -> "Location"
            3 -> "ID PDF"
            4 -> "Selfie"
            else -> "Review & submit"
        })

        when (registrationStep) {
            1 -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First name") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last name") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = idNumber, onValueChange = { idNumber = it }, label = { Text("SA ID number") }, modifier = Modifier.fillMaxWidth()) }
                    item { Text(if (isValidSouthAfricanId(idNumber)) "ID format valid" else "Enter a valid 13-digit SA ID") }
                    item { OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = employmentStatus, onValueChange = { employmentStatus = it }, label = { Text("Employment") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = monthlyIncome, onValueChange = { monthlyIncome = it }, label = { Text("Income") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = employerName, onValueChange = { employerName = it }, label = { Text("Employer") }, modifier = Modifier.fillMaxWidth()) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { registrationStep = 2 }) { Text("Next: Location") }
                }
            }
            2 -> {
                Text(
                    text = if (locationPermissionDenied) {
                        "Location permission was declined. We will not proceed without it."
                    } else if (latitude == null || longitude == null || locationName == null) {
                        "Capturing your current location automatically..."
                    } else {
                        "Location: $locationName (${String.format(Locale.US, "%.6f", latitude)} / ${String.format(Locale.US, "%.6f", longitude)})"
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { registrationStep = 1 }) { Text("Back") }
                    Button(onClick = { locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Retry location") }
                    Button(onClick = { registrationStep = 3 }) { Text("Next: ID PDF") }
                }
            }
            3 -> {
                Button(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (pdfUri == null) "Select ID PDF" else "PDF selected")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { registrationStep = 2 }) { Text("Back") }
                    Button(onClick = { registrationStep = 4 }, enabled = pdfUri != null) { Text("Next: Selfie") }
                }
            }
            4 -> {
                Button(
                    onClick = {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            selfieCapture.launch(null)
                        } else {
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selfieBitmap == null) "Capture Selfie" else "Selfie captured")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { registrationStep = 3 }) { Text("Back") }
                    Button(onClick = { registrationStep = 5 }, enabled = selfieBitmap != null) { Text("Next: Review") }
                }
            }
            else -> {
                Text("$firstName $lastName · $idNumber")
                Text("$phone ${if (email.isNotBlank()) "· $email" else ""}")
                Text("$address")
                Text("Location: ${locationName ?: "—"}")
                Text("ID PDF and selfie attached.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { registrationStep = 4 }) { Text("Back") }
                    Button(
                        onClick = {
                            if (!isValidSouthAfricanId(idNumber)) {
                                viewModel.publishMessage("Please provide a valid South African ID number")
                                return@Button
                            }
                            if (pdfUri == null || selfieBitmap == null || latitude == null || longitude == null || locationName == null || locationPermissionDenied) {
                                viewModel.publishMessage("We will not proceed without location permission and full location details")
                                return@Button
                            }
                            viewModel.registerKyc(
                                username = username,
                                password = password,
                                firstName = firstName,
                                lastName = lastName,
                                idNumber = idNumber,
                                phone = phone,
                                email = email.ifBlank { null },
                                address = address,
                                employmentStatus = employmentStatus,
                                monthlyIncome = monthlyIncome.toDoubleOrNull() ?: 0.0,
                                employerName = employerName.ifBlank { null },
                                latitude = latitude ?: 0.0,
                                longitude = longitude ?: 0.0,
                                locationName = locationName ?: return@Button,
                                idDocumentUri = pdfUri ?: return@Button,
                                selfieBitmap = selfieBitmap ?: return@Button,
                                onSuccess = { registrationSubmitted = true }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Submit KYC Registration") }
                }
            }
        }
    }
}

@Composable
private fun VerificationScreen(viewModel: LoanViewModel) {
    val verification = viewModel.verification
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { viewModel.loadBorrowerVerification() }) {
            Text("Refresh Verification Status")
        }
        if (verification == null) {
            Text("No verification record loaded yet")
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Verification status: ${verification.status}")
                    Text("Location: ${verification.locationName ?: "-"}")
                    Text("SA ID valid: ${verification.saIdValid}")
                    Text("Details matched: ${verification.detailsMatched}")
                    Text("Face matched: ${verification.faceMatched}")
                    Text("Face match score: ${verification.faceMatchScore ?: "-"}")
                    Text("Review notes: ${verification.reviewNotes ?: "Awaiting review"}")
                }
            }
        }
    }
}

@Composable
private fun LoanScreen(viewModel: LoanViewModel) {
    var amount by remember { mutableStateOf("1000") }
    var lookupId by remember { mutableStateOf("1") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Interest and terms are set by the business. You can pay any amount at any time; each payment reduces what you owe and interest continues until the loan is paid off.")
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Loan amount") })
        Button(onClick = {
            viewModel.applyForLoan(amount = amount.toDoubleOrNull() ?: 0.0)
        }) { Text("Apply For Loan") }

        viewModel.currentLoan?.let { loan ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Loan #${loan.id}")
                    Text("Status: ${loan.status}")
                    Text("Risk: ${loan.riskBand} / ${loan.riskScore}")
                    Text("Total repayable: R %.2f".format(Locale.US, loan.totalAmount))
                }
            }
        }

        OutlinedTextField(value = lookupId, onValueChange = { lookupId = it }, label = { Text("Loan ID") })
        Button(onClick = { viewModel.loadLoanDetails(lookupId.toLongOrNull() ?: 1L) }) { Text("Load Loan Status") }
    }
}

@Composable
private fun ScheduleScreen(viewModel: LoanViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(viewModel.schedule) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Installment ${item.installmentNumber}")
                    Text("Due date: ${item.dueDate}")
                    Text("Amount: R %.2f".format(Locale.US, item.amountDue))
                    Text("Status: ${item.status}")
                }
            }
        }
    }
}

@Composable
private fun NotificationScreen(viewModel: LoanViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(viewModel.notifications) { notification ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(notification.channel)
                    Text(notification.message)
                    Text(notification.status)
                }
            }
        }
    }
}

private fun isValidSouthAfricanId(idNumber: String): Boolean {
    if (!Regex("^\\d{13}$").matches(idNumber)) {
        return false
    }
    val yearPart = idNumber.substring(0, 2).toInt()
    val month = idNumber.substring(2, 4).toInt()
    val day = idNumber.substring(4, 6).toInt()
    if (month !in 1..12 || day !in 1..31) {
        return false
    }
    var sumOdd = 0
    for (index in 0 until 12 step 2) {
        sumOdd += idNumber[index].digitToInt()
    }
    val evenDigits = buildString {
        for (index in 1 until 12 step 2) {
            append(idNumber[index])
        }
    }
    val doubled = (evenDigits.toInt() * 2).toString()
    val sumEven = doubled.sumOf { it.digitToInt() }
    val total = sumOdd + sumEven
    val checkDigit = (10 - (total % 10)) % 10
    return checkDigit == idNumber.last().digitToInt() && yearPart >= 0
}

@Suppress("MissingPermission")
private fun fetchLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocation: (Double, Double, String) -> Unit,
    onError: (String) -> Unit,
    geocoder: Geocoder
) {
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            if (location == null) {
                onError("Could not determine current location")
            } else {
                val addresses = try {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
                val resolvedName = addresses.firstOrNull()?.getAddressLine(0)
                if (resolvedName.isNullOrBlank()) {
                    onError("Could not resolve your full location name")
                } else {
                    onLocation(location.latitude, location.longitude, resolvedName)
                }
            }
        }
        .addOnFailureListener {
            onError("Could not determine current location")
        }
}
