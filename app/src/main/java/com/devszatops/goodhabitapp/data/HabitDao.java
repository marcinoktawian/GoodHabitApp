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

    @Update
    void updateHabit(Habit habit);

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    Habit getHabitById(int habitId);

    @Query("SELECT COUNT(*) FROM habits WHERE LOWER(name) = LOWER(:name)")
    int countByName(String name);


}
