package com.loanshark.mobile.data;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("token") public String token;
    @SerializedName("userId") public String userId;
    @SerializedName("role") public String role;
    @SerializedName("borrowerId") public String borrowerId;
}
