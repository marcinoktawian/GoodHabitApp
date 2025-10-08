package com.devszatops.goodhabitapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HabitLogDao {
    @Insert
    void insertLog(HabitLog log);

    @Query("SELECT * FROM HabitLog WHERE habitId = :habitId AND date = :date LIMIT 1")
    HabitLog getLogByDate(int habitId, String date);

    @Query("SELECT * FROM HabitLog WHERE habitId = :habitId ORDER BY date DESC")
    List<HabitLog> getLogsForHabit(int habitId);

    @Query("DELETE FROM HabitLog WHERE habitId = :habitId")
    void deleteLogsForHabit(int habitId);

    // Pobiera wszystkie dni, które są oznaczone jako przerwa
    @Query("SELECT * FROM HabitLog WHERE habitId = :habitId AND isBreak = 1 ORDER BY date DESC")
    List<HabitLog> getBreaksForHabit(int habitId);

    // Aktualizuje wpis na dany dzień, ustawiając lub usuwając przerwę
    @Query("UPDATE HabitLog SET isBreak = :isBreak WHERE habitId = :habitId AND date = :date")
    void setBreakForDate(int habitId, String date, boolean isBreak);

    @Query("SELECT * FROM HabitLog WHERE habitId = :habitId AND isBreak = 0 ORDER BY date ASC LIMIT 1")
    HabitLog getEarliestNonBreakLog(int habitId);
}