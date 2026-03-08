package com.loanshark.mobile.data;

import com.google.gson.annotations.SerializedName;

public class LoanApplicationRequest {
    @SerializedName("borrowerId") public String borrowerId;
    @SerializedName("loanAmount") public double loanAmount;

    public LoanApplicationRequest(String borrowerId, double loanAmount) {
        this.borrowerId = borrowerId;
        this.loanAmount = loanAmount;
    }
}
