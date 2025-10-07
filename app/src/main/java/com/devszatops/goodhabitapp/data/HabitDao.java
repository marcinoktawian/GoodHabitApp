package com.devszatops.goodhabitapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.room.Update;

import java.util.List;

@Dao
public interface HabitDao {

    @Query("SELECT * FROM habits")
    List<Habit> getAllHabits();

    @Insert
    long insertHabit(Habit habit);

    @Delete
    void deleteHabit(Habit habit);

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    Habit getHabitById(int habitId);

    @Query("SELECT COUNT(*) FROM habits WHERE LOWER(name) = LOWER(:name)")
    int countByName(String name);

    // --- Update nazwy zwyczaju ---
    @Query("UPDATE habits SET name = :newName WHERE id = :habitId")
    void updateName(int habitId, String newName);

    // --- Update ilości dni przerwy ---
    @Query("UPDATE habits SET allowedSkipDays = :skipDays WHERE id = :habitId")
    void updateAllowedSkipDays(int habitId, int skipDays);

    // --- Usunięcie zwyczaju po ID ---
    @Query("DELETE FROM habits WHERE id = :habitId")
    void deleteHabitById(int habitId);

    @Query("UPDATE habits SET reminderTime = :reminderTime WHERE id = :id")
    void updateReminderTime(int id, String reminderTime);

    @Query("UPDATE habits SET unlockedTrophies = :trophies WHERE id = :habitId")
    void updateUnlockedTrophies(int habitId, String trophies);

}
