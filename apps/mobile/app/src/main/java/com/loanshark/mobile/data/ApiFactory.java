package com.loanshark.mobile.data;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * API client factory. Base URL points to backend on host machine:
 * - Emulator: 10.0.2.2:8080 (same as localhost:8080 on your PC)
 * - Physical device: use your PC's IP, e.g. http://192.168.1.x:8080/
 */
public final class ApiFactory {
    private static final String BASE_URL = "http://10.0.2.2:8080/";

    public static LoanSharkApi create(final TokenProvider tokenProvider) {
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                String token = tokenProvider.getToken();
                Request original = chain.request();
                Request.Builder builder = original.newBuilder();
                if (token != null && !token.isEmpty()) {
                    builder.addHeader("Authorization", "Bearer " + token);
                }
                return chain.proceed(builder.build());
            })
            .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build();

        return new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LoanSharkApi.class);
    }

    public interface TokenProvider {
        String getToken();
    }
}
