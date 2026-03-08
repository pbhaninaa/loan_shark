package com.loanshark.mobile.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "session_cache")
public class SessionCache {
    @PrimaryKey
    public int id = 1;
    public String token;
    public String role;
    public String userId;
    public String borrowerId;
    public String borrowerStatus;

    public SessionCache() {}
}
