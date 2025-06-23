package com.devszatops.goodhabitapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.devszatops.goodhabitapp.data.AppDatabase;
import com.devszatops.goodhabitapp.data.Habit;
import com.google.android.material.card.MaterialCardView;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.util.TypedValue;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout habitList;
    private AppDatabase db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        habitList = findViewById(R.id.habitList);

        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "habit-db")
                .fallbackToDestructiveMigration()
                .build();
        loadHabitsFromDatabase();
        addAddButtonCard();
    }


    private void addHabitCard(Habit habit) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                200
        );
        cardParams.setMargins(0, 16, 0, 0);
        card.setLayoutParams(cardParams);
        card.setRadius(24f);
        card.setCardElevation(4f);
        card.setStrokeColor(Color.TRANSPARENT);
        card.setStrokeWidth(0);
        card.setBackgroundResource(R.drawable.habit_gradient_background);

        TextView text = new TextView(this);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
        );
        text.setLayoutParams(textParams);
        text.setText(habit.name);
        text.setTextColor(Color.WHITE);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        text.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        text.setPadding(48, 0, 0, 0);

        card.addView(text);
        habitList.addView(card, habitList.getChildCount() - 1);

        // DŁUGIE PRZYTRZYMANIE = USUWANIE
        card.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Usuń zwyczaj")
                    .setMessage("Czy na pewno chcesz usunąć \"" + habit.name + "\"?")
                    .setPositiveButton("Tak", (dialog, which) -> {
                        new Thread(() -> {
                            db.habitDao().deleteHabit(habit);

                            runOnUiThread(() -> {
                                habitList.removeView(card);
                                Toast.makeText(this, "Usunięto", Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    })
                    .setNegativeButton("Nie", null)
                    .show();
            return true;
        });
    }




    private void addAddButtonCard() {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 16, 0, 0);
        card.setLayoutParams(cardParams);
        card.setRadius(24f);
        card.setCardElevation(4f);
        card.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
        card.setStrokeColor(Color.TRANSPARENT); // brak ramki
        card.setStrokeWidth(0);

        TextView text = new TextView(this);
        text.setText("+ Dodaj zwyczaj");
        text.setTextSize(18f);
        text.setPadding(32, 32, 32, 32);
        text.setTextColor(Color.parseColor("#4CAF50"));
        text.setGravity(Gravity.CENTER);

        card.addView(text);

        card.setOnClickListener(v -> showAddHabitDialog());

        habitList.addView(card);
    }

    private void showAddHabitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nowy zwyczaj");

        final EditText input = new EditText(this);
        input.setHint("Nazwa zwyczaju");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setPadding(50, 40, 50, 40);

        builder.setView(input);

        builder.setPositiveButton("Dodaj", (dialog, which) -> {
            String text = input.getText().toString().trim();

            if (!text.isEmpty()) {
                final String formatted = text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();

                new Thread(() -> {
                    int count = db.habitDao().countByName(formatted);
                    if (count > 0) {
                        runOnUiThread(() ->
                                Toast.makeText(this, "Taki zwyczaj już istnieje!", Toast.LENGTH_SHORT).show()
                        );
                    } else {
                        Habit habit = new Habit(formatted, System.currentTimeMillis(), 3);
                        long newId = db.habitDao().insertHabit(habit);
                        habit.id = (int) newId;

                        runOnUiThread(() -> {
                            addHabitCard(habit);
                        });
                    }
                }).start();
            }
        });

        builder.setNegativeButton("Anuluj", (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void loadHabitsFromDatabase() {
        new Thread(() -> {
            if (db == null) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Baza danych nie została zainicjalizowana", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            List<Habit> habits = db.habitDao().getAllHabits();
            runOnUiThread(() -> {
                for (Habit h : habits) {
                    addHabitCard(h);
                }
            });
        }).start();
    }


}
