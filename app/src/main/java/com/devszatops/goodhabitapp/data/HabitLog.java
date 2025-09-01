package com.devszatops.goodhabitapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class HabitLog {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int habitId;
    public String date; // format: "2025-06-21"

    public Boolean isBreak;
}
