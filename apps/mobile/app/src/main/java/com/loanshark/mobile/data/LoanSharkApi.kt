package com.loanshark.mobile.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Part

interface LoanSharkApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @Multipart
    @POST("auth/register/borrower")
    suspend fun registerBorrower(
        @Part("username") username: RequestBody,
        @Part("password") password: RequestBody,
        @Part("firstName") firstName: RequestBody,
        @Part("lastName") lastName: RequestBody,
        @Part("idNumber") idNumber: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part("email") email: RequestBody,
        @Part("address") address: RequestBody,
        @Part("employmentStatus") employmentStatus: RequestBody,
        @Part("monthlyIncome") monthlyIncome: RequestBody,
        @Part("employerName") employerName: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("locationName") locationName: RequestBody,
        @Part idDocument: MultipartBody.Part,
        @Part selfieImage: MultipartBody.Part
    ): AuthResponse

    @GET("borrowers/me")
    suspend fun getMyBorrower(): BorrowerProfileResponse

    @GET("verifications/me")
    suspend fun getMyVerification(): VerificationResponse

    @POST("loans/apply")
    suspend fun applyForLoan(@Body request: LoanApplicationRequest): LoanResponse

    @GET("loans/{loanId}")
    suspend fun getLoan(@Path("loanId") loanId: Long): LoanResponse

    @GET("loans/{loanId}/schedule")
    suspend fun getSchedule(@Path("loanId") loanId: Long): List<ScheduleResponse>

    @POST("borrowers/{borrowerId}/documents")
    suspend fun uploadDocument(
        @Path("borrowerId") borrowerId: Long,
        @Body request: BorrowerDocumentRequest
    )

    @GET("notifications/me")
    suspend fun getMyNotifications(): List<NotificationItem>
}

object ApiFactory {
    fun create(tokenProvider: () -> String?): LoanSharkApi {
        val authInterceptor = Interceptor { chain ->
            val token = tokenProvider()
            val request = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) {
                    addHeader("Authorization", "Bearer $token")
                }
            }.build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LoanSharkApi::class.java)
    }
}
