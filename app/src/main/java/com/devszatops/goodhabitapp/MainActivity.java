package com.devszatops.goodhabitapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.util.TypedValue;

public class MainActivity extends AppCompatActivity {

    private LinearLayout habitList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        habitList = findViewById(R.id.habitList);

        addAddButtonCard(); // tylko przycisk „Dodaj zwyczaj”
    }

    private void addHabitCard(String habitText) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                200
        );
        cardParams.setMargins(0, 16, 0, 0);
        card.setLayoutParams(cardParams);
        card.setRadius(24f);
        card.setCardElevation(4f);
        card.setBackgroundResource(R.drawable.habit_gradient_background); // gradient

        TextView text = new TextView(this);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
        );
        text.setLayoutParams(textParams);
        String capitalized = habitText.substring(0, 1).toUpperCase() + habitText.substring(1).toLowerCase();
        text.setText(capitalized);
        text.setTextColor(Color.WHITE);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        text.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        text.setPadding(48, 0, 0, 0);

        card.addView(text);
        habitList.addView(card, habitList.getChildCount() - 1);
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
                addHabitCard(text);  // uproszczona wersja funkcji
            }
        });

        builder.setNegativeButton("Anuluj", (dialog, which) -> dialog.cancel());

        builder.show();
    }

}
