package com.loanshark.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class LoginRequest(
    val username: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val userId: Long,
    val role: String,
    val borrowerId: Long?
)

data class BorrowerProfileResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val idNumber: String,
    val phone: String,
    val email: String?,
    val address: String,
    val employmentStatus: String,
    val monthlyIncome: Double,
    val employerName: String?,
    val status: String,
    val riskScore: Int
)

data class VerificationResponse(
    val id: Long,
    val borrowerId: Long,
    val status: String,
    val saIdValid: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val locationCapturedAt: String?,
    val locationName: String?,
    val extractedFirstName: String?,
    val extractedLastName: String?,
    val extractedIdNumber: String?,
    val ocrConfidence: Double?,
    val detailsMatched: Boolean,
    val faceMatchScore: Double?,
    val faceMatched: Boolean,
    val reviewNotes: String?,
    val reviewedBy: String?,
    val reviewedAt: String?,
    val createdAt: String,
    val updatedAt: String
)

data class BorrowerRegistrationRequest(
    val username: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val idNumber: String,
    val phone: String,
    val email: String?,
    val address: String,
    val employmentStatus: String,
    val monthlyIncome: Double,
    val employerName: String?
)

data class LoanApplicationRequest(
    val borrowerId: Long,
    val loanAmount: Double,
    val loanTermDays: Int? = null
)

data class LoanResponse(
    val id: Long,
    val borrowerId: Long,
    val loanAmount: Double,
    val interestRate: Double,
    val totalAmount: Double,
    val loanTermDays: Int,
    val issueDate: String?,
    val dueDate: String?,
    val status: String,
    val riskScore: Int,
    val riskBand: String
)

data class ScheduleResponse(
    val installmentNumber: Int,
    val dueDate: String,
    val amountDue: Double,
    val status: String
)

data class BorrowerDocumentRequest(
    val documentType: String,
    val fileUrl: String
)

data class NotificationItem(
    val id: Long,
    val channel: String,
    val message: String,
    val status: String,
    val createdAt: String
)

@Entity(tableName = "session_cache")
data class SessionCache(
    @PrimaryKey val id: Int = 1,
    val token: String,
    val role: String,
    val userId: Long,
    val borrowerId: Long?,
    val borrowerStatus: String?
)
