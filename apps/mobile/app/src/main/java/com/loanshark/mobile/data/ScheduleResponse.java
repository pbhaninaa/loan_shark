package com.loanshark.mobile.data;

import com.google.gson.annotations.SerializedName;

public class ScheduleResponse {
    @SerializedName("installmentNumber") public int installmentNumber;
    @SerializedName("dueDate") public String dueDate;
    @SerializedName("amountDue") public double amountDue;
    @SerializedName("status") public String status;
}
