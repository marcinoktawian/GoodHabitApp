package com.devszatops.goodhabitapp;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.room.Room;

import com.devszatops.goodhabitapp.R;
import com.devszatops.goodhabitapp.data.AppDatabase;
import com.devszatops.goodhabitapp.data.Habit;
import com.devszatops.goodhabitapp.data.HabitDao;
import com.devszatops.goodhabitapp.data.HabitLog;
import com.devszatops.goodhabitapp.data.HabitLogDao;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class HabitDetailActivity extends AppCompatActivity {

    private String habitName;
    private TextView title;
    private MaterialCalendarView calendarView;
    private LinearLayout contentLayout;
    private ProgressBar progressBar;
    private int habitId;
    private AppDatabase db;
    private HabitLogDao habitLogDao;
    private HabitDao habitDao;
    private int currentStreak = 0;
    private int longestStreak = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_habit_detail);

        habitName = getIntent().getStringExtra("habitName");
        habitId = getIntent().getIntExtra("habitId", -1);

        title = findViewById(R.id.habitTitle);
        calendarView = findViewById(R.id.calendarView);
        contentLayout = findViewById(R.id.contentLayout);
        progressBar = findViewById(R.id.progressBar);

        // Inicjalizacja bazy danych
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "habit-db")
                .fallbackToDestructiveMigration()
                .build();

        habitLogDao = db.habitLogDao(); // Inicjalizacja DAO
        habitDao = db.habitDao(); // Inicjalizacja DAO dla Habit


        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton menuButton = findViewById(R.id.menuButton);

        backButton.setOnClickListener(v -> finish());

        menuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.habit_detail_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_delete) {
                    Toast.makeText(this, "Tu będzie usuwanie...", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });

            popup.show();
        });

        simulateLoadingData();
    }

    private void simulateLoadingData() {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // symulacja opóźnienia
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                title.setText(habitName);
                contentLayout.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                setupCalendar();
                highlightSampleDates();
                calculateStreaks();
            });
        }).start();
    }

    private void setupCalendar() {
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            // Pobierz dzisiejszą datę
            Calendar today = Calendar.getInstance();
            today.setTimeInMillis(System.currentTimeMillis());

            // Sprawdź, czy kliknięta data jest późniejsza niż dzisiejsza
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(date.getYear(), date.getMonth() - 1, date.getDay());

            if (selectedDate.after(today)) {
                // Jeśli kliknięta data jest w przyszłości, zablokuj możliwość jej zaznaczenia
                Toast.makeText(this, "Nie możesz zaznaczyć dat w przyszłości", Toast.LENGTH_SHORT).show();
            } else {
                // Jeśli data jest w przeszłości lub dzisiaj, pokaż dialog
                showMarkDialog(date.getYear(), date.getMonth(), date.getDay());
            }
        });
    }

    private void showMarkDialog(int year, int month, int day) {
        String dateStr = String.format("%04d-%02d-%02d", year, month, day);

        // Sprawdzamy, czy wpis już istnieje
        new Thread(() -> {
            HabitLog existingLog = habitLogDao.getLogByDate(habitId, dateStr); // Sprawdzamy, czy istnieje log dla tej daty

            runOnUiThread(() -> {
                if (existingLog != null) {
                    // Jeśli log istnieje, pokazujemy komunikat, że dzień już został zapisany
                    Toast.makeText(this, "Dzień został już zapisany!", Toast.LENGTH_SHORT).show();
                } else {
                    // Jeśli log nie istnieje, pokazujemy dialog i zapisujemy wpis
                    new AlertDialog.Builder(this)
                            .setTitle("Wykonanie zwyczaju")
                            .setMessage("Czy wykonałeś \"" + habitName + "\" dnia " + dateStr + "?")
                            .setPositiveButton("Tak", (dialog, which) -> {
                                HabitLog habitLog = new HabitLog();
                                habitLog.habitId = habitId;  // Ustawiamy habitId
                                habitLog.date = dateStr;     // Ustawiamy datę

                                // Dodajemy log do bazy
                                new Thread(() -> {
                                    habitLogDao.insertLog(habitLog); // Zapisujemy do bazy danych

                                    // Zaktualizuj UI po zapisaniu
                                    runOnUiThread(() -> {
                                        highlightSampleDates(); // Zaktualizuj kalendarz
                                        calculateStreaks();
                                        Toast.makeText(this, "Zwyczaj zapisany!", Toast.LENGTH_SHORT).show();
                                    });
                                }).start();
                            })
                            .setNegativeButton("Nie", null)
                            .show();
                }
            });
        }).start();
    }


    private void highlightSampleDates() {
        List<CalendarDay> completedDays = new ArrayList<>();
        List<CalendarDay> missedDays = new ArrayList<>();

        // Operacje na bazie danych muszą odbywać się w osobnym wątku
        new Thread(() -> {
            List<HabitLog> logs = habitLogDao.getLogsForHabit(habitId); // Pobierz dane z bazy
            Log.d("HabitDetailActivity", "Pobrano logi: " + logs.size()); // Logowanie liczby logów

            if (logs.isEmpty()) {
                // Jeśli brak logów, zakończ
                Log.d("HabitDetailActivity", "Brak logów w bazie");
                return;
            }

            // Znajdź datę pierwszego wpisu
            String firstDateStr = logs.get(logs.size() - 1).date; // Pierwszy wpis to ostatni w bazie (posortowane malejąco)
            Log.d("HabitDetailActivity", "Pierwszy wpis data: " + firstDateStr); // Logowanie daty pierwszego wpisu

            Calendar firstDate = stringToCalendar(firstDateStr);

            // Pobierz dzisiejszą datę
            Calendar today = Calendar.getInstance();

            // Dodaj dni wykonane do completedDays
            for (HabitLog log : logs) {
                Calendar logDate = stringToCalendar(log.date);
                completedDays.add(CalendarDay.from(logDate.get(Calendar.YEAR), logDate.get(Calendar.MONTH) + 1, logDate.get(Calendar.DAY_OF_MONTH)));
                Log.d("HabitDetailActivity", "Dodano completedDay: " + log.date); // Logowanie dodanych dni
            }

            // Sprawdzamy wszystkie dni pomiędzy pierwszym wpisem a dzisiaj
            for (Calendar day = (Calendar) firstDate.clone(); !day.after(today); day.add(Calendar.DATE, 1)) {
                CalendarDay calendarDay = CalendarDay.from(day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH));

                // Jeśli brak wpisu na dany dzień, oznacz jako missedDay
                if (!completedDays.contains(calendarDay)) {
                    missedDays.add(calendarDay);
                    Log.d("HabitDetailActivity", "Dodano missedDay: " + calendarDay); // Logowanie dodanych missedDays
                }
            }

            // Logowanie przed wyświetleniem
            Log.d("HabitDetailActivity", "Completed days count: " + completedDays.size());
            Log.d("HabitDetailActivity", "Missed days count: " + missedDays.size());

            // Zaktualizuj UI po zakończeniu operacji na bazie
            runOnUiThread(() -> {
                calendarView.removeDecorators(); // Usuwamy stare dekoratory
                // Zielony, stonowany kolor dla dni wykonanych
                int stonowanyZielony = Color.parseColor("#388E3C");
                calendarView.addDecorator(new DayColorDecorator(completedDays, stonowanyZielony));
                // Czerwony kolor dla dni, które nie zostały wykonane
                int stonowanyCzerwony = Color.parseColor("#D32F2F");
                calendarView.addDecorator(new DayColorDecorator(missedDays, stonowanyCzerwony));
                updateUI();
            });
        }).start(); // Uruchamiamy wątek roboczy
    }


    private Calendar stringToCalendar(String dateStr) {
        String[] dateParts = dateStr.split("-");
        int year = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]) - 1;  // Miesiące są 0-indexed, więc odejmujemy 1
        int day = Integer.parseInt(dateParts[2]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        Log.d("HabitDetailActivity", "Date parsed: " + calendar.getTime()); // Debugging log for the parsed date

        return calendar;
    }


    private List<CalendarDay> getCompletedDaysFromDatabase() {
        List<CalendarDay> completedDays = new ArrayList<>();
        new Thread(() -> {
            List<HabitLog> logs = habitLogDao.getLogsForHabit(habitId);
            for (HabitLog log : logs) {
                completedDays.add(CalendarDay.from(Integer.parseInt(log.date.split("-")[0]),
                        Integer.parseInt(log.date.split("-")[1]) - 1,
                        Integer.parseInt(log.date.split("-")[2])));
            }
        }).start();
        return completedDays;
    }

    private List<CalendarDay> getMissedDaysFromDatabase() {
        List<CalendarDay> missedDays = new ArrayList<>();
        missedDays.add(CalendarDay.from(2025, 5, 21));  // Przykład
        return missedDays;
    }

    public class DayColorDecorator implements DayViewDecorator {
        private final HashSet<CalendarDay> dates;
        private final int color;

        public DayColorDecorator(Collection<CalendarDay> dates, int color) {
            this.dates = new HashSet<>(dates);  // Zapewniamy unikalność dni
            this.color = color;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day); // Sprawdzamy, czy dany dzień jest na liście
        }

        @Override
        public void decorate(DayViewFacade view) {
            // Tworzymy kółko z tłem w wybranym kolorze
            ShapeDrawable circle = new ShapeDrawable(new OvalShape());
            circle.getPaint().setColor(color); // Ustawiamy kolor tła
            circle.setIntrinsicHeight(15); // Ustawiamy wysokość kółka
            circle.setIntrinsicWidth(15); // Ustawiamy szerokość kółka

            // Dodajemy kółko jako tło
            view.setBackgroundDrawable(circle);
        }
    }

    private void calculateStreaks() {
        new Thread(() -> {
            List<HabitLog> logs = habitLogDao.getLogsForHabit(habitId); // Pobierz dane z bazy
            Log.d("HabitDetailActivity", "Pobrano logi: " + logs.size()); // Logowanie liczby logów

            int allowedSkipDays = getHabitAllowedSkipDays();  // Pobierz liczbę dozwolonych dni przerwy
            List<Calendar> completeDays = new ArrayList<>();

            // Zamiana logów na listę dat
            for (HabitLog log : logs) {
                completeDays.add(stringToCalendar(log.date));
            }

            // Sortujemy daty w porządku rosnącym (od najstarszego do najnowszego)
            completeDays.sort(Calendar::compareTo);

            // Zmienna na przechowanie obecnego streaku
            int tempCurrentStreak = 0;
            int tempLongestStreak = 0;
            int tempPotentialLongestStreak = 1;
            boolean currentStreak = true;  // Flaga, która mówi, czy streak trwa

            Calendar today = Calendar.getInstance();

            if (completeDays.isEmpty()) {
                Log.d("HabitDetailActivity", "Brak logów w bazie. Streak ustawiony na 0.");
                tempPotentialLongestStreak =0;
            }else {
                // Sprawdzamy ostatni wpis (czy dzisiejszy dzień jest częścią streaku)
                if (!compareDates(today, completeDays.get(completeDays.size() - 1), allowedSkipDays-1)) {
                    currentStreak = false;  // Jeśli dzisiejszy dzień nie jest częścią streaku
                }else{
                    tempCurrentStreak++;
                }

                // Iterujemy przez dni od najnowszego do najstarszego
                for (int i = completeDays.size() - 1; i > 0; i--) {
                    Calendar currentDay = completeDays.get(i);
                    Calendar previousDay = completeDays.get(i - 1);

                    // Sprawdzamy, czy różnica między datami nie przekroczyła dozwolonej przerwy
                    if (compareDates(currentDay, previousDay, allowedSkipDays)) {
                        tempPotentialLongestStreak++;  // Zwiększamy potencjalny streak

                        if (currentStreak) {
                            tempCurrentStreak++;  // Zwiększamy aktualny streak
                        }
                    } else {
                        // Jeśli przerwa jest za długa, resetujemy streak
                        currentStreak = false;

                        // Aktualizujemy najdłuższy streak, jeśli to konieczne
                        if (tempPotentialLongestStreak > tempLongestStreak) {
                            tempLongestStreak = tempPotentialLongestStreak;
                        }
                        // Resetujemy potencjalny streak
                        tempPotentialLongestStreak = 1;
                    }
                }
            }

            // Po pętli sprawdzamy, czy ostatni streak był najdłuższy
            if (tempPotentialLongestStreak > tempLongestStreak) {
                tempLongestStreak = tempPotentialLongestStreak;
            }

            // Używamy zmiennych finalnych do zaktualizowania UI w głównym wątku
            final int finalTempCurrentStreak = tempCurrentStreak;
            final int finalTempLongestStreak = tempLongestStreak;

            // Zaktualizuj UI po zakończeniu wątku
            runOnUiThread(() -> {
                updateStreakInUI(finalTempCurrentStreak, finalTempLongestStreak);
            });
        }).start(); // Uruchamiamy wątek roboczy
    }

    // Funkcja do porównania dat
    private boolean compareDates(Calendar newerDate, Calendar olderDate, int breakTimeInDays) {
        // Ustawiamy godziny, minuty, sekundy i milisekundy na 00:00:00 dla obu dat
        newerDate.set(Calendar.HOUR_OF_DAY, 0);
        newerDate.set(Calendar.MINUTE, 0);
        newerDate.set(Calendar.SECOND, 0);
        newerDate.set(Calendar.MILLISECOND, 0);

        olderDate.set(Calendar.HOUR_OF_DAY, 0);
        olderDate.set(Calendar.MINUTE, 0);
        olderDate.set(Calendar.SECOND, 0);
        olderDate.set(Calendar.MILLISECOND, 0);
        breakTimeInDays++;

        // Obliczamy różnicę w milisekundach
        long diffInMillis = newerDate.getTimeInMillis() - olderDate.getTimeInMillis();

        // Konwertujemy różnicę na dni
        long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);  // Przekształcamy na dni

        return diffInDays <= breakTimeInDays;  // Jeśli różnica nie przekracza dozwolonej przerwy
    }




    // Nowa metoda aktualizacji UI
    private void updateStreakInUI(int currentStreakTemp, int longestStreakTemp) {
        currentStreak = currentStreakTemp;  // Przypisujemy aktualny streak
        longestStreak = longestStreakTemp;  // Przypisujemy najdłuższy streak
        updateUI();  // Aktualizacja UI z nowymi wartościami streaku
    }


    private int getHabitAllowedSkipDays() {
        // W tym przypadku zakładamy, że odczytujemy ustawienie przerwy dla tego zwyczaju
        Habit habit = habitDao.getHabitById(habitId);
        return habit.allowedSkipDays;
    }

    private void updateStreakInDatabase(int currentStreak, int longestStreak) {
        // Zaktualizuj odpowiednie wartości w bazie danych
        Habit habit = habitDao.getHabitById(habitId);
        habit.currentStreak = currentStreak;
        habit.maxStreak = longestStreak;

        new Thread(() -> {
            habitDao.updateHabit(habit);  // Zaktualizuj rekord w bazie
        }).start();
    }

    private void updateUI() {
        TextView currentStreakTextView = findViewById(R.id.currentStreak);
        TextView longestStreakTextView = findViewById(R.id.longestStreak);

        currentStreakTextView.setText("Obecny streak: " + currentStreak);
        longestStreakTextView.setText("Najdłuższy streak: " + longestStreak);
    }


}