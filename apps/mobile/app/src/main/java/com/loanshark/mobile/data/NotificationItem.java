package com.loanshark.mobile.data;

import com.google.gson.annotations.SerializedName;

public class NotificationItem {
    @SerializedName("id") public String id;
    @SerializedName("channel") public String channel;
    @SerializedName("message") public String message;
    @SerializedName("status") public String status;
    @SerializedName("createdAt") public String createdAt;
}
