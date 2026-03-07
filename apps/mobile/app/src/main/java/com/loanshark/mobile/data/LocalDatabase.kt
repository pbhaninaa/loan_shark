package com.loanshark.mobile.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Dao
interface SessionDao {
    @Query("SELECT * FROM session_cache LIMIT 1")
    suspend fun getSession(): SessionCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(sessionCache: SessionCache)

    @Query("DELETE FROM session_cache")
    suspend fun clear()
}

@Database(entities = [SessionCache::class], version = 2)
abstract class LoanSharkDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}

object DatabaseFactory {
    fun create(context: Context): LoanSharkDatabase {
        return Room.databaseBuilder(
            context,
            LoanSharkDatabase::class.java,
            "loan_shark_mobile.db"
        ).fallbackToDestructiveMigration().build()
    }
}
