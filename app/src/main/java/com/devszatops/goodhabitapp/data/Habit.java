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

    public int allowedSkips;

    public Habit(@NonNull String name, long createdAt, int allowedSkips) {
        this.name = name;
        this.createdAt = createdAt;
        this.currentStreak = 0;
        this.maxStreak = 0;
        this.allowedSkips = allowedSkips;
    }
}
