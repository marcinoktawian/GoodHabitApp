package com.devszatops.goodhabitapp.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Habit.class, HabitLog.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract HabitDao habitDao();
    public abstract HabitLogDao habitLogDao();
}
