package com.loanshark.mobile.data;

import com.google.gson.annotations.SerializedName;

public class LoanResponse {
    @SerializedName("id") public String id;
    @SerializedName("borrowerId") public String borrowerId;
    @SerializedName("loanAmount") public double loanAmount;
    @SerializedName("interestRate") public double interestRate;
    @SerializedName("totalAmount") public double totalAmount;
    @SerializedName("loanTermDays") public int loanTermDays;
    @SerializedName("status") public String status;
    @SerializedName("riskScore") public int riskScore;
    @SerializedName("riskBand") public String riskBand;
}
