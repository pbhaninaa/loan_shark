package com.loanshark.mobile.data;

import com.google.gson.annotations.SerializedName;

public class VerificationResponse {
    @SerializedName("id") public String id;
    @SerializedName("borrowerId") public String borrowerId;
    @SerializedName("status") public String status;
    @SerializedName("saIdValid") public boolean saIdValid;
    @SerializedName("latitude") public Double latitude;
    @SerializedName("longitude") public Double longitude;
    @SerializedName("locationName") public String locationName;
    @SerializedName("detailsMatched") public boolean detailsMatched;
    @SerializedName("faceMatchScore") public Double faceMatchScore;
    @SerializedName("faceMatched") public boolean faceMatched;
    @SerializedName("reviewNotes") public String reviewNotes;
    @SerializedName("createdAt") public String createdAt;
    @SerializedName("updatedAt") public String updatedAt;
}
