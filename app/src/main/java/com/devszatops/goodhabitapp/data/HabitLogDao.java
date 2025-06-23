package com.devszatops.goodhabitapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HabitLogDao {

    @Insert
    void insertLog(HabitLog log);

    @Query("SELECT * FROM HabitLog WHERE habitId = :habitId ORDER BY date DESC")
    List<HabitLog> getLogsForHabit(int habitId);

    @Query("DELETE FROM HabitLog WHERE habitId = :habitId")
    void deleteLogsForHabit(int habitId);
}
