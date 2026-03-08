package com.loanshark.mobile.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SessionCache.class}, version = 2)
public abstract class LoanSharkDatabase extends RoomDatabase {
    public abstract SessionDao sessionDao();

    private static volatile LoanSharkDatabase INSTANCE;

    public static LoanSharkDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (LoanSharkDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        LoanSharkDatabase.class,
                        "loan_shark_mobile.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
