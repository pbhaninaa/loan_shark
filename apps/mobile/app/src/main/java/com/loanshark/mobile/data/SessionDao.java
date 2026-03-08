package com.loanshark.mobile.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface SessionDao {
    @Query("SELECT * FROM session_cache LIMIT 1")
    SessionCache getSession();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(SessionCache sessionCache);

    @Query("DELETE FROM session_cache")
    void clear();
}
