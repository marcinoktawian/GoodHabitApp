package com.devszatops.goodhabitapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "habits")
public class Habit {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String name;

    public long createdAt;

    public int currentStreak;

    public int maxStreak;

    public int allowedSkipDays;

    public String reminderTime; // format "HH:mm", np. "08:30"

    public String unlockedTrophies; // np. "7,30,100"



    public Habit(@NonNull String name, long createdAt, int allowedSkipDays) {
        this.name = name;
        this.createdAt = createdAt;
        this.currentStreak = 0;
        this.maxStreak = 0;
        this.allowedSkipDays = allowedSkipDays;
        this.reminderTime = null;
        this.unlockedTrophies = "";
    }
}
