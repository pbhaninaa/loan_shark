package com.loanshark.mobile.data;

import com.google.gson.annotations.SerializedName;

public class BorrowerDocumentRequest {
    @SerializedName("documentType") public String documentType;
    @SerializedName("fileUrl") public String fileUrl;

    public BorrowerDocumentRequest(String documentType, String fileUrl) {
        this.documentType = documentType;
        this.fileUrl = fileUrl;
    }
}
