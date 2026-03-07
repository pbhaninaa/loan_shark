package com.loanshark.mobile

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loanshark.mobile.data.ApiFactory
import com.loanshark.mobile.data.BorrowerDocumentRequest
import com.loanshark.mobile.data.DatabaseFactory
import com.loanshark.mobile.data.LoanApplicationRequest
import com.loanshark.mobile.data.LoanResponse
import com.loanshark.mobile.data.NotificationItem
import com.loanshark.mobile.data.ScheduleResponse
import com.loanshark.mobile.data.SessionCache
import com.loanshark.mobile.data.VerificationResponse
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.launch

class LoanViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseFactory.create(application)
    private val sessionDao = database.sessionDao()

    var token by mutableStateOf<String?>(null)
        private set

    var borrowerId by mutableStateOf<Long?>(1L)
        private set

    var borrowerStatus by mutableStateOf<String?>(null)
        private set

    var verification by mutableStateOf<VerificationResponse?>(null)
        private set

    var currentLoan by mutableStateOf<LoanResponse?>(null)
        private set

    var schedule by mutableStateOf<List<ScheduleResponse>>(emptyList())
        private set

    var notifications by mutableStateOf<List<NotificationItem>>(emptyList())
        private set

    var message by mutableStateOf("Ready")
        private set

    private val api by lazy {
        ApiFactory.create { token }
    }

    init {
        viewModelScope.launch {
            val session = sessionDao.getSession()
            token = session?.token
            borrowerId = if (session?.role == "BORROWER") session.borrowerId else borrowerId
            borrowerStatus = session?.borrowerStatus
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            runCatching {
                api.login(com.loanshark.mobile.data.LoginRequest(username, password))
            }.onSuccess { auth ->
                token = auth.token
                borrowerId = auth.borrowerId
                sessionDao.save(
                    SessionCache(
                        token = auth.token,
                        role = auth.role,
                        userId = auth.userId,
                        borrowerId = auth.borrowerId,
                        borrowerStatus = borrowerStatus
                    )
                )
                if (auth.role == "BORROWER") {
                    refreshBorrowerAccessState(auth.userId)
                }
                message = "Logged in"
            }.onFailure {
                message = "Login failed: ${it.message}"
            }
        }
    }

    fun registerKyc(
        username: String,
        password: String,
        firstName: String,
        lastName: String,
        idNumber: String,
        phone: String,
        email: String?,
        address: String,
        employmentStatus: String,
        monthlyIncome: Double,
        employerName: String?,
        latitude: Double,
        longitude: Double,
        locationName: String,
        idDocumentUri: Uri,
        selfieBitmap: Bitmap,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            runCatching {
                val idDocumentFile = copyUriToTempFile(idDocumentUri, "id-copy.pdf")
                val selfieFile = saveBitmapToTempFile(selfieBitmap, "selfie.jpg")
                api.registerBorrower(
                    username = username.toPart(),
                    password = password.toPart(),
                    firstName = firstName.toPart(),
                    lastName = lastName.toPart(),
                    idNumber = idNumber.toPart(),
                    phone = phone.toPart(),
                    email = (email ?: "").toPart(),
                    address = address.toPart(),
                    employmentStatus = employmentStatus.toPart(),
                    monthlyIncome = monthlyIncome.toString().toPart(),
                    employerName = (employerName ?: "").toPart(),
                    latitude = latitude.toString().toPart(),
                    longitude = longitude.toString().toPart(),
                    locationName = locationName.toPart(),
                    idDocument = MultipartBody.Part.createFormData(
                        "idDocument",
                        idDocumentFile.name,
                        idDocumentFile.asRequestBody("application/pdf".toMediaType())
                    ),
                    selfieImage = MultipartBody.Part.createFormData(
                        "selfieImage",
                        selfieFile.name,
                        selfieFile.asRequestBody("image/jpeg".toMediaType())
                    )
                )
            }
                .onSuccess { auth ->
                    token = auth.token
                    borrowerId = auth.borrowerId
                    refreshBorrowerAccessState(auth.userId)
                    sessionDao.save(
                        SessionCache(
                            token = auth.token,
                            role = auth.role,
                            userId = auth.userId,
                            borrowerId = auth.borrowerId,
                            borrowerStatus = borrowerStatus
                        )
                    )
                    message = "Verification submitted"
                    onSuccess?.invoke()
                }
                .onFailure {
                    message = "Registration failed: ${it.message}"
                }
        }
    }

    fun applyForLoan(amount: Double) {
        val id = borrowerId ?: return
        if (borrowerStatus != "ACTIVE") {
            message = "Complete verification before applying for a loan"
            return
        }
        viewModelScope.launch {
            runCatching {
                api.applyForLoan(
                    LoanApplicationRequest(
                        borrowerId = id,
                        loanAmount = amount
                    )
                )
            }.onSuccess {
                currentLoan = it
                message = "Loan application submitted"
            }.onFailure {
                message = "Loan application failed: ${it.message}"
            }
        }
    }

    fun loadLoanDetails(loanId: Long) {
        viewModelScope.launch {
            runCatching { api.getLoan(loanId) }
                .onSuccess { currentLoan = it }
                .onFailure { message = "Could not load loan: ${it.message}" }

            runCatching { api.getSchedule(loanId) }
                .onSuccess { schedule = it }
                .onFailure { message = "Could not load schedule: ${it.message}" }
        }
    }

    fun uploadDocument(documentType: String, fileUrl: String) {
        val id = borrowerId ?: return
        viewModelScope.launch {
            runCatching { api.uploadDocument(id, BorrowerDocumentRequest(documentType, fileUrl)) }
                .onSuccess { message = "Document captured" }
                .onFailure { message = "Document upload failed: ${it.message}" }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            runCatching { api.getMyNotifications() }
                .onSuccess { notifications = it }
                .onFailure { message = "Notifications failed: ${it.message}" }
        }
    }

    fun publishMessage(value: String) {
        message = value
    }

    fun loadBorrowerVerification() {
        viewModelScope.launch {
            runCatching {
                refreshBorrowerAccessState(sessionDao.getSession()?.userId ?: 0L)
            }.onFailure {
                message = "Could not load verification: ${it.message}"
            }
        }
    }

    private suspend fun refreshBorrowerAccessState(userId: Long) {
        val borrower = api.getMyBorrower()
        borrowerStatus = borrower.status
        verification = if (borrower.status == "ACTIVE") {
            null
        } else {
            runCatching { api.getMyVerification() }.getOrNull()
        }
        sessionDao.save(
            SessionCache(
                token = token ?: "",
                role = "BORROWER",
                userId = userId,
                borrowerId = borrowerId,
                borrowerStatus = borrower.status
            )
        )
    }

    private fun String.toPart() = toRequestBody("text/plain".toMediaType())

    private fun copyUriToTempFile(uri: Uri, fileName: String): File {
        val file = File(getApplication<Application>().cacheDir, fileName)
        getApplication<Application>().contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(file).use { output ->
                input?.copyTo(output)
            }
        }
        return file
    }

    private fun saveBitmapToTempFile(bitmap: Bitmap, fileName: String): File {
        val file = File(getApplication<Application>().cacheDir, fileName)
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        }
        return file
    }
}
