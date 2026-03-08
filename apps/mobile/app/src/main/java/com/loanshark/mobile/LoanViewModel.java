package com.loanshark.mobile;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.loanshark.mobile.data.ApiFactory;
import com.loanshark.mobile.data.LoanApplicationRequest;
import com.loanshark.mobile.data.LoanResponse;
import com.loanshark.mobile.data.LoanSharkApi;
import com.loanshark.mobile.data.LoanSharkDatabase;
import com.loanshark.mobile.data.NotificationItem;
import com.loanshark.mobile.data.ScheduleResponse;
import com.loanshark.mobile.data.SessionCache;
import com.loanshark.mobile.data.SessionDao;
import com.loanshark.mobile.data.VerificationResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class LoanViewModel extends AndroidViewModel {
    private final LoanSharkDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private LoanSharkApi api;
    private String token;
    private String borrowerId;
    private String userId;

    private final MutableLiveData<String> tokenLive = new MutableLiveData<>();
    private final MutableLiveData<String> borrowerStatusLive = new MutableLiveData<>();
    private final MutableLiveData<VerificationResponse> verificationLive = new MutableLiveData<>();
    private final MutableLiveData<LoanResponse> currentLoanLive = new MutableLiveData<>();
    private final MutableLiveData<List<ScheduleResponse>> scheduleLive = new MutableLiveData<>();
    private final MutableLiveData<List<NotificationItem>> notificationsLive = new MutableLiveData<>();
    private final MutableLiveData<String> messageLive = new MutableLiveData<>();

    public LoanViewModel(@NonNull Application application) {
        super(application);
        database = LoanSharkDatabase.getInstance(application);
        SessionDao sessionDao = database.sessionDao();
        refreshApi();
        messageLive.setValue("Ready");
        executor.execute(() -> {
            SessionCache s = sessionDao.getSession();
            if (s != null) {
                token = s.token;
                borrowerId = s.borrowerId;
                userId = s.userId;
                mainHandler.post(() -> {
                    tokenLive.setValue(token);
                    borrowerStatusLive.setValue(s.borrowerStatus);
                });
            }
        });
    }

    private void refreshApi() {
        api = ApiFactory.create(() -> token);
    }

    public LiveData<String> getMessage() { return messageLive; }
    public LiveData<String> getBorrowerStatus() { return borrowerStatusLive; }
    public LiveData<VerificationResponse> getVerification() { return verificationLive; }
    public LiveData<LoanResponse> getCurrentLoan() { return currentLoanLive; }
    public LiveData<List<ScheduleResponse>> getSchedule() { return scheduleLive; }
    public LiveData<List<NotificationItem>> getNotifications() { return notificationsLive; }

    public void login(String username, String password) {
        executor.execute(() -> {
            api.login(new com.loanshark.mobile.data.LoginRequest(username, password)).enqueue(
                new retrofit2.Callback<com.loanshark.mobile.data.AuthResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.loanshark.mobile.data.AuthResponse> call,
                                           retrofit2.Response<com.loanshark.mobile.data.AuthResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.loanshark.mobile.data.AuthResponse auth = response.body();
                            token = auth.token;
                            borrowerId = auth.borrowerId;
                            userId = auth.userId;
                            refreshApi();
                            SessionCache c = new SessionCache();
                            c.token = auth.token;
                            c.role = auth.role;
                            c.userId = auth.userId;
                            c.borrowerId = auth.borrowerId;
                            c.borrowerStatus = null;
                            executor.execute(() -> {
                                database.sessionDao().save(c);
                                refreshBorrowerAccessState(auth.userId);
                            });
                            mainHandler.post(() -> messageLive.setValue("Logged in"));
                        } else {
                            mainHandler.post(() -> messageLive.setValue("Login failed"));
                        }
                    }
                    @Override
                    public void onFailure(retrofit2.Call<com.loanshark.mobile.data.AuthResponse> call, Throwable t) {
                        mainHandler.post(() -> messageLive.setValue("Login failed: " + (t != null ? t.getMessage() : "")));
                    }
                });
        });
    }

    public void registerKyc(String username, String password, String firstName, String lastName,
                            String idNumber, String phone, String email, String address,
                            String employmentStatus, double monthlyIncome, String employerName,
                            double latitude, double longitude, String locationName,
                            Uri idDocumentUri, Bitmap selfieBitmap, Runnable onSuccess) {
        executor.execute(() -> {
            try {
                File idFile = copyUriToTempFile(idDocumentUri, "id-copy.pdf");
                File selfieFile = saveBitmapToTempFile(selfieBitmap, "selfie.jpg");
                RequestBody un = RequestBody.create(username, MediaType.parse("text/plain"));
                RequestBody pw = RequestBody.create(password, MediaType.parse("text/plain"));
                RequestBody fn = RequestBody.create(firstName, MediaType.parse("text/plain"));
                RequestBody ln = RequestBody.create(lastName, MediaType.parse("text/plain"));
                RequestBody idNum = RequestBody.create(idNumber, MediaType.parse("text/plain"));
                RequestBody ph = RequestBody.create(phone, MediaType.parse("text/plain"));
                RequestBody em = RequestBody.create(email != null ? email : "", MediaType.parse("text/plain"));
                RequestBody addr = RequestBody.create(address, MediaType.parse("text/plain"));
                RequestBody emp = RequestBody.create(employmentStatus, MediaType.parse("text/plain"));
                RequestBody income = RequestBody.create(String.valueOf(monthlyIncome), MediaType.parse("text/plain"));
                RequestBody empName = RequestBody.create(employerName != null ? employerName : "", MediaType.parse("text/plain"));
                RequestBody lat = RequestBody.create(String.valueOf(latitude), MediaType.parse("text/plain"));
                RequestBody lng = RequestBody.create(String.valueOf(longitude), MediaType.parse("text/plain"));
                RequestBody locName = RequestBody.create(locationName, MediaType.parse("text/plain"));
                MultipartBody.Part idPart = MultipartBody.Part.createFormData("idDocument", idFile.getName(),
                    RequestBody.create(idFile, MediaType.parse("application/pdf")));
                MultipartBody.Part selfiePart = MultipartBody.Part.createFormData("selfieImage", selfieFile.getName(),
                    RequestBody.create(selfieFile, MediaType.parse("image/jpeg")));

                api.registerBorrower(un, pw, fn, ln, idNum, ph, em, addr, emp, income, empName, lat, lng, locName, idPart, selfiePart)
                    .enqueue(new retrofit2.Callback<com.loanshark.mobile.data.AuthResponse>() {
                        @Override
                        public void onResponse(retrofit2.Call<com.loanshark.mobile.data.AuthResponse> call,
                                               retrofit2.Response<com.loanshark.mobile.data.AuthResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                com.loanshark.mobile.data.AuthResponse auth = response.body();
                                token = auth.token;
                                borrowerId = auth.borrowerId;
                                userId = auth.userId;
                                refreshApi();
                                SessionCache c = new SessionCache();
                                c.token = auth.token;
                                c.role = auth.role;
                                c.userId = auth.userId;
                                c.borrowerId = auth.borrowerId;
                                c.borrowerStatus = null;
                                executor.execute(() -> database.sessionDao().save(c));
                                mainHandler.post(() -> {
                                    messageLive.setValue("Verification submitted");
                                    if (onSuccess != null) onSuccess.run();
                                });
                            } else {
                                mainHandler.post(() -> messageLive.setValue("Registration failed"));
                            }
                        }
                        @Override
                        public void onFailure(retrofit2.Call<com.loanshark.mobile.data.AuthResponse> call, Throwable t) {
                            mainHandler.post(() -> messageLive.setValue("Registration failed: " + (t != null ? t.getMessage() : "")));
                        }
                    });
            } catch (Exception e) {
                mainHandler.post(() -> messageLive.setValue("Registration failed: " + e.getMessage()));
            }
        });
    }

    public void applyForLoan(double amount) {
        if (borrowerId == null) return;
        if (borrowerStatusLive.getValue() != null && !"ACTIVE".equals(borrowerStatusLive.getValue())) {
            messageLive.setValue("Complete verification before applying for a loan");
            return;
        }
        api.applyForLoan(new LoanApplicationRequest(borrowerId, amount)).enqueue(
            new retrofit2.Callback<LoanResponse>() {
                @Override
                public void onResponse(retrofit2.Call<LoanResponse> call, retrofit2.Response<LoanResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        mainHandler.post(() -> {
                            currentLoanLive.setValue(response.body());
                            messageLive.setValue("Loan application submitted");
                        });
                    } else {
                        mainHandler.post(() -> messageLive.setValue("Loan application failed"));
                    }
                }
                @Override
                public void onFailure(retrofit2.Call<LoanResponse> call, Throwable t) {
                    mainHandler.post(() -> messageLive.setValue("Loan application failed: " + (t != null ? t.getMessage() : "")));
                }
            });
    }

    public void loadLoanDetails(String loanId) {
        api.getLoan(loanId).enqueue(new retrofit2.Callback<LoanResponse>() {
            @Override
            public void onResponse(retrofit2.Call<LoanResponse> call, retrofit2.Response<LoanResponse> response) {
                if (response.isSuccessful() && response.body() != null)
                    mainHandler.post(() -> currentLoanLive.setValue(response.body()));
            }
            @Override
            public void onFailure(retrofit2.Call<LoanResponse> call, Throwable t) {
                mainHandler.post(() -> messageLive.setValue("Could not load loan"));
            }
        });
        api.getSchedule(loanId).enqueue(new retrofit2.Callback<List<ScheduleResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<List<ScheduleResponse>> call, retrofit2.Response<List<ScheduleResponse>> response) {
                if (response.isSuccessful() && response.body() != null)
                    mainHandler.post(() -> scheduleLive.setValue(response.body()));
            }
            @Override
            public void onFailure(retrofit2.Call<List<ScheduleResponse>> call, Throwable t) {
                mainHandler.post(() -> messageLive.setValue("Could not load schedule"));
            }
        });
    }

    public void loadNotifications() {
        api.getMyNotifications().enqueue(new retrofit2.Callback<List<NotificationItem>>() {
            @Override
            public void onResponse(retrofit2.Call<List<NotificationItem>> call, retrofit2.Response<List<NotificationItem>> response) {
                if (response.isSuccessful() && response.body() != null)
                    mainHandler.post(() -> notificationsLive.setValue(response.body()));
            }
            @Override
            public void onFailure(retrofit2.Call<List<NotificationItem>> call, Throwable t) {}
        });
    }

    public void publishMessage(String msg) { messageLive.postValue(msg); }

    public void loadBorrowerVerification() {
        if (userId == null) userId = "";
        refreshBorrowerAccessState(userId);
    }

    private void refreshBorrowerAccessState(String uid) {
        api.getMyBorrower().enqueue(new retrofit2.Callback<com.loanshark.mobile.data.BorrowerProfileResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.loanshark.mobile.data.BorrowerProfileResponse> call,
                                   retrofit2.Response<com.loanshark.mobile.data.BorrowerProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String status = response.body().status;
                    mainHandler.post(() -> borrowerStatusLive.setValue(status));
                    SessionCache c = new SessionCache();
                    c.token = token != null ? token : "";
                    c.role = "BORROWER";
                    c.userId = uid;
                    c.borrowerId = borrowerId;
                    c.borrowerStatus = status;
                    executor.execute(() -> database.sessionDao().save(c));
                    if (!"ACTIVE".equals(status)) {
                        api.getMyVerification().enqueue(new retrofit2.Callback<VerificationResponse>() {
                            @Override
                            public void onResponse(retrofit2.Call<VerificationResponse> call, retrofit2.Response<VerificationResponse> r) {
                                if (r.isSuccessful() && r.body() != null)
                                    mainHandler.post(() -> verificationLive.setValue(r.body()));
                            }
                            @Override
                            public void onFailure(retrofit2.Call<VerificationResponse> call, Throwable t) {}
                        });
                    }
                }
            }
            @Override
            public void onFailure(retrofit2.Call<com.loanshark.mobile.data.BorrowerProfileResponse> call, Throwable t) {
                mainHandler.post(() -> messageLive.setValue("Could not load verification"));
            }
        });
    }

    private File copyUriToTempFile(Uri uri, String fileName) throws Exception {
        File file = new File(getApplication().getCacheDir(), fileName);
        InputStream in = getApplication().getContentResolver().openInputStream(uri);
        if (in != null) {
            FileOutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.close();
            in.close();
        }
        return file;
    }

    private File saveBitmapToTempFile(Bitmap bitmap, String fileName) throws Exception {
        File file = new File(getApplication().getCacheDir(), fileName);
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out);
        out.close();
        return file;
    }
}
