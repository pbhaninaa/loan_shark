package com.loanshark.mobile.data;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface LoanSharkApi {
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @Multipart
    @POST("auth/register/borrower")
    Call<AuthResponse> registerBorrower(
        @Part("username") RequestBody username,
        @Part("password") RequestBody password,
        @Part("firstName") RequestBody firstName,
        @Part("lastName") RequestBody lastName,
        @Part("idNumber") RequestBody idNumber,
        @Part("phone") RequestBody phone,
        @Part("email") RequestBody email,
        @Part("address") RequestBody address,
        @Part("employmentStatus") RequestBody employmentStatus,
        @Part("monthlyIncome") RequestBody monthlyIncome,
        @Part("employerName") RequestBody employerName,
        @Part("latitude") RequestBody latitude,
        @Part("longitude") RequestBody longitude,
        @Part("locationName") RequestBody locationName,
        @Part MultipartBody.Part idDocument,
        @Part MultipartBody.Part selfieImage
    );

    @GET("borrowers/me")
    Call<BorrowerProfileResponse> getMyBorrower();

    @GET("verifications/me")
    Call<VerificationResponse> getMyVerification();

    @POST("loans/apply")
    Call<LoanResponse> applyForLoan(@Body LoanApplicationRequest request);

    @GET("loans/{loanId}")
    Call<LoanResponse> getLoan(@Path("loanId") String loanId);

    @GET("loans/{loanId}/schedule")
    Call<java.util.List<ScheduleResponse>> getSchedule(@Path("loanId") String loanId);

    @GET("notifications/me")
    Call<java.util.List<NotificationItem>> getMyNotifications();
}
