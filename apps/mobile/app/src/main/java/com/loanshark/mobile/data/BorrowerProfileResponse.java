package com.loanshark.mobile.data;

import com.google.gson.annotations.SerializedName;

public class BorrowerProfileResponse {
    @SerializedName("id") public String id;
    @SerializedName("firstName") public String firstName;
    @SerializedName("lastName") public String lastName;
    @SerializedName("idNumber") public String idNumber;
    @SerializedName("phone") public String phone;
    @SerializedName("email") public String email;
    @SerializedName("address") public String address;
    @SerializedName("employmentStatus") public String employmentStatus;
    @SerializedName("monthlyIncome") public double monthlyIncome;
    @SerializedName("employerName") public String employerName;
    @SerializedName("status") public String status;
    @SerializedName("riskScore") public int riskScore;
}
