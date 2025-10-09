package com.devszatops.goodhabitapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.room.Room;

import com.devszatops.goodhabitapp.data.AppDatabase;
import com.devszatops.goodhabitapp.data.Habit;
import com.devszatops.goodhabitapp.data.HabitDao;
import com.devszatops.goodhabitapp.data.HabitLog;
import com.devszatops.goodhabitapp.data.HabitLogDao;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.ArrayList;
import java.util.Arrays;
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

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // kolor status bara i tło okna
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary_dark_green));
        getWindow().setBackgroundDrawableResource(R.color.primary_dark_green);

        // padding dla status bar + navigation bar
        View rootView = findViewById(R.id.rootHabitLayout);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });


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
                int id = item.getItemId();

                if (id == R.id.menu_edit_name) {
                    showEditNameDialog();
                    return true;
                } else if (id == R.id.menu_edit_skip_days) {
                    showEditSkipDaysDialog();
                    return true;
                } else if (id == R.id.menu_set_reminder) {
                    checkNotificationPermission();
                    showTimePickerDialog();
                    return true;
                } else if (id == R.id.menu_delete) {
                    showDeleteHabitDialog();
                    return true;
                }
                return false;
            });

            popup.show();
        });

        simulateLoadingData();

    }

    private void showEditNameDialog() {
        EditText input = new EditText(this);
        input.setHint("Nowa nazwa");

        new AlertDialog.Builder(this)
                .setTitle("Edytuj nazwę zwyczaju")
                .setView(input)
                .setPositiveButton("Zapisz", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    String formatted = newName.substring(0, 1).toUpperCase() + newName.substring(1).toLowerCase();
                    if (!newName.isEmpty()) {
                        new Thread(() -> {
                            habitDao.updateName(habitId, formatted);
                            runOnUiThread(this::refreshHabitData);
                        }).start();
                        Toast.makeText(this, "Nazwa zmieniona", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void showEditSkipDaysDialog() {
        new Thread(() -> {
            // pobierz dane w tle
            int currentSkipDays = getHabitAllowedSkipDays();

            runOnUiThread(() -> {
                // Dane do spinnera
                Integer[] values = new Integer[31];
                for (int i = 0; i <= 30; i++) values[i] = i;

                Spinner spinner = new Spinner(this);
                ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item, values);
                spinner.setAdapter(adapter);
                spinner.setSelection(currentSkipDays);

                // Kontener do centrowania
                FrameLayout container = new FrameLayout(this);

                // LayoutParams z wrap_content
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );
                params.gravity = Gravity.CENTER;
                spinner.setLayoutParams(params);

                // Dodajemy trochę paddingu poziomego – ok. szerokości zawartości
                int padding = (int) (64 * getResources().getDisplayMetrics().density); // ~32dp po bokach
                spinner.setPadding(padding, spinner.getPaddingTop(), padding, spinner.getPaddingBottom());

                container.addView(spinner);
                int outerPadding = (int) (32 * getResources().getDisplayMetrics().density);
                container.setPadding(outerPadding, outerPadding, outerPadding, outerPadding);

                new AlertDialog.Builder(this)
                        .setTitle("Zmień dozwoloną przerwę")
                        .setView(container)
                        .setPositiveButton("Zapisz", (dialog, which) -> {
                            int skipDays = (int) spinner.getSelectedItem();
                            new Thread(() -> {
                                habitDao.updateAllowedSkipDays(habitId, skipDays);
                                runOnUiThread(this::refreshHabitData);
                            }).start();
                            Toast.makeText(this, "Dni przerwy zmienione na " + skipDays, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Anuluj", null)
                        .show();
            });
        }).start();
    }


    private void showDeleteHabitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Usuń zwyczaj")
                .setMessage("Na pewno chcesz usunąć ten zwyczaj?")
                .setPositiveButton("Usuń", (dialog, which) -> {
                    new Thread(() -> {
                        habitDao.deleteHabitById(habitId); // musisz mieć metodę w DAO
                    }).start();
                    Toast.makeText(this, "Zwyczaj usunięty", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Anuluj", null)
                .show();
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

    private void refreshHabitData() {
        new Thread(() -> {
            Habit habit = habitDao.getHabitById(habitId);
            runOnUiThread(() -> {
                title.setText(habit.name);
                calculateStreaks(); // aktualizacja streaków
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
                                habitLog.isBreak = Boolean.FALSE;     // Ustawiamy datę

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
                            .setNeutralButton("Zacznij przerwę", (dialog, which) -> showBreakPicker(year, month, day))
                            .show();
                }
            });
        }).start();
    }

    private void showBreakPicker(int year, int month, int day) {
        new Thread(() -> {
            HabitLog earliestLog = habitLogDao.getEarliestNonBreakLog(habitId);

            runOnUiThread(() -> {
                if (earliestLog == null) {
                    Toast.makeText(this, "Nie możesz rozpocząć przerwy przed pierwszym wykonaniem nawyku!", Toast.LENGTH_LONG).show();
                    return;
                }

                // Zamieniamy datę pierwszego wpisu na Calendar
                Calendar firstLogDate = stringToCalendar(earliestLog.date);

                // Kliknięta data (np. z kalendarza)
                Calendar clickedDate = Calendar.getInstance();
                clickedDate.set(year, month - 1, day);

                // 🧠 Sprawdzamy, czy kliknięty dzień nie jest wcześniejszy niż pierwszy log
                if (clickedDate.before(firstLogDate)) {
                    Toast.makeText(this,
                            "Nie możesz rozpocząć przerwy przed pierwszym wykonaniem nawyku (" +
                                    new java.text.SimpleDateFormat("dd.MM.yyyy").format(firstLogDate.getTime()) + ")!",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                // 🔹 Jeśli wszystko OK – pokaż DatePicker startowy
                DatePickerDialog startPicker = new DatePickerDialog(
                        this,
                        (view, startYear, startMonth, startDayOfMonth) -> {
                            Calendar startDate = Calendar.getInstance();
                            startDate.set(startYear, startMonth, startDayOfMonth);

                            // 🧠 Zabezpieczenie: start przerwy nie może być przed pierwszym logiem
                            if (startDate.before(firstLogDate)) {
                                Toast.makeText(this,
                                        "Początek przerwy nie może być przed pierwszym wykonaniem nawyku!",
                                        Toast.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            // Po wybraniu startu – wybór daty zakończenia
                            DatePickerDialog endPicker = new DatePickerDialog(
                                    this,
                                    (view2, endYear, endMonth, endDay) -> {
                                        Calendar endDate = Calendar.getInstance();
                                        endDate.set(endYear, endMonth, endDay);

                                        if (endDate.before(startDate)) {
                                            Toast.makeText(this, "Data zakończenia nie może być przed rozpoczęciem", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        saveBreakDays(startDate, endDate);
                                    },
                                    startYear,
                                    startMonth,
                                    startDayOfMonth
                            );

                            // 🔹 kalendarz od poniedziałku, minimalna data = start przerwy
                            endPicker.getDatePicker().setFirstDayOfWeek(Calendar.MONDAY);
                            endPicker.getDatePicker().setMinDate(startDate.getTimeInMillis());
                            endPicker.setTitle("Wybierz datę zakończenia przerwy");
                            endPicker.show();
                        },
                        clickedDate.get(Calendar.YEAR),
                        clickedDate.get(Calendar.MONTH),
                        clickedDate.get(Calendar.DAY_OF_MONTH)
                );

                startPicker.getDatePicker().setFirstDayOfWeek(Calendar.MONDAY);
                startPicker.getDatePicker().setMinDate(firstLogDate.getTimeInMillis()); // 🔒 nie można wybrać wcześniejszej daty
                startPicker.setTitle("Wybierz datę rozpoczęcia przerwy");
                startPicker.show();
            });
        }).start();
    }


    private void saveBreakDays(Calendar startDate, Calendar endDate) {
        new Thread(() -> {
            Calendar day = (Calendar) startDate.clone();

            while (!day.after(endDate)) {
                String dateStr = String.format("%04d-%02d-%02d",
                        day.get(Calendar.YEAR),
                        day.get(Calendar.MONTH) + 1,
                        day.get(Calendar.DAY_OF_MONTH));

                HabitLog breakLog = new HabitLog();
                breakLog.habitId = habitId;
                breakLog.date = dateStr;
                breakLog.isBreak = true;

                habitLogDao.insertLog(breakLog);

                day.add(Calendar.DATE, 1); // przechodzimy do następnego dnia
            }

            runOnUiThread(() -> {
                highlightSampleDates();
                calculateStreaks();
                Toast.makeText(this, "Przerwa zapisana!", Toast.LENGTH_SHORT).show();
            });
        }).start();

    }


    private void highlightSampleDates() {
        List<CalendarDay> completedDays = new ArrayList<>();
        List<CalendarDay> missedDays = new ArrayList<>();
        List<CalendarDay> breakDays = new ArrayList<>();

        List<CalendarDay> todayDays = new ArrayList<>();
        todayDays.add(CalendarDay.today());
        int greyColor = Color.parseColor("#808080");  // dzisiaj


        new Thread(() -> {
            List<HabitLog> logs = habitLogDao.getLogsForHabit(habitId);

            if (logs.isEmpty()) {
                runOnUiThread(() -> {
                    calendarView.removeDecorators();
                    calendarView.addDecorator(new DayColorDecorator(todayDays, greyColor));
                });
                return;
            }

            // Sortujemy logi po dacie (rosnąco)
            logs.sort((a, b) -> a.date.compareTo(b.date));

            Calendar firstDate = stringToCalendar(logs.get(0).date);
            Calendar today = Calendar.getInstance();

            HashSet<String> logDatesSet = new HashSet<>();

            for (HabitLog log : logs) {
                Calendar logDate = stringToCalendar(log.date);

                // UWAGA: Calendar.MONTH jest 0-indexowane, więc dodajemy +1 tylko TUTAJ
                CalendarDay day = CalendarDay.from(
                        logDate.get(Calendar.YEAR),
                        logDate.get(Calendar.MONTH) + 1, // <- +1 bo MaterialCalendarView używa 1-12
                        logDate.get(Calendar.DAY_OF_MONTH)
                );

                logDatesSet.add(log.date);

                if (log.isBreak) {
                    breakDays.add(day);
                } else {
                    completedDays.add(day);
                }
            }

            // Szukamy brakujących dni między pierwszym logiem a dzisiaj (dni pominięte)
            for (Calendar day = (Calendar) firstDate.clone(); !day.after(today); day.add(Calendar.DATE, 1)) {
                String dayStr = String.format("%04d-%02d-%02d",
                        day.get(Calendar.YEAR),
                        day.get(Calendar.MONTH) + 1, // miesiąc 1-12
                        day.get(Calendar.DAY_OF_MONTH));

                if (!logDatesSet.contains(dayStr)) {
                    CalendarDay missed = CalendarDay.from(
                            day.get(Calendar.YEAR),
                            day.get(Calendar.MONTH) + 1,
                            day.get(Calendar.DAY_OF_MONTH)
                    );
                    missedDays.add(missed);
                }
            }

            // Aktualizacja UI na głównym wątku
            runOnUiThread(() -> {
                calendarView.removeDecorators();

                int greenColor = Color.parseColor("#388E3C"); // wykonane
                int redColor = Color.parseColor("#D32F2F");   // pominięte
                int blueColor = Color.parseColor("#1976D2");  // przerwa

                calendarView.addDecorator(new DayColorDecorator(missedDays, redColor));
                calendarView.addDecorator(new DayColorDecorator(todayDays, greyColor));
                calendarView.addDecorator(new DayColorDecorator(completedDays, greenColor));
                calendarView.addDecorator(new DayColorDecorator(breakDays, blueColor));


                updateUI();
            });

        }).start();
    }


    private Calendar stringToCalendar(String dateString) {
        String[] parts = dateString.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1; // <- Calendar miesiące liczy od 0
        int day = Integer.parseInt(parts[2]);

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
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
            List<Calendar> breakDays = new ArrayList<>();

            // Zamiana logów na listę dat
            for (HabitLog log : logs) {
                if (!log.isBreak) {
                    completeDays.add(stringToCalendar(log.date));
                } else {
                    breakDays.add(stringToCalendar(log.date));
                }
            }

            // Sortujemy daty w porządku rosnącym (od najstarszego do najnowszego)
            completeDays.sort(Calendar::compareTo);
            breakDays.sort(Calendar::compareTo);

            // Zmienna na przechowanie obecnego streaku
            int tempCurrentStreak = 0;
            int tempLongestStreak = 0;
            int tempPotentialLongestStreak = 1;
            boolean currentStreak = true;  // Flaga, która mówi, czy streak trwa

            Calendar today = Calendar.getInstance();

            if (completeDays.isEmpty()) {
                Log.d("HabitDetailActivity", "Brak logów w bazie. Streak ustawiony na 0.");
                tempPotentialLongestStreak = 0;
            } else {
                // Sprawdzamy ostatni wpis (czy dzisiejszy dzień jest częścią streaku)
                if (!compareDates(today, completeDays.get(completeDays.size() - 1), allowedSkipDays - 1)) {
                    currentStreak = false;  // Jeśli dzisiejszy dzień nie jest częścią streaku
                } else {
                    tempCurrentStreak++;
                }

                // Iterujemy przez dni od najnowszego do najstarszego
                for (int i = completeDays.size() - 1; i > 0; i--) {
                    Calendar currentDay = completeDays.get(i);
                    Calendar previousDay = completeDays.get(i - 1);

                    // Normalny przypadek — różnica w granicach allowSkipDays
                    if (compareDates(currentDay, previousDay, allowedSkipDays)) {
                        tempPotentialLongestStreak++;
                        if (currentStreak) tempCurrentStreak++;
                        continue;
                    }

                    // Sprawdzamy, czy w luce była przerwa
                    boolean wasBreakInGap = false;
                    Calendar breakEnd = null;
                    Calendar breakStart = null;

                    for (Calendar breakDay : breakDays) {
                        if (isBetween(previousDay, currentDay, breakDay)) {
                            if (breakStart == null){
                                breakStart = (Calendar) breakDay.clone();
                                wasBreakInGap = true;
                            }
                            if (breakEnd == null || breakDay.after(breakEnd)) {
                                breakEnd = (Calendar) breakDay.clone();
                            }
                        }
                    }

                    if (wasBreakInGap ) {
                        // 🔹 Obliczamy różnicę między końcem przerwy a następnym wykonanym dniem
                        long diffDays = daysBetween(breakStart, previousDay) + daysBetween(currentDay, breakEnd) - 2;

                        if (diffDays <= allowedSkipDays) {
                            // ✅ Zalicza się do streaka, bo wrócono w czasie dozwolonej przerwy
                            tempPotentialLongestStreak++;
                            if (currentStreak) tempCurrentStreak++;
                            continue;
                        }
                    }

                    // 🔴 W przeciwnym razie — streak się kończy
                    currentStreak = false;

                    if (tempPotentialLongestStreak > tempLongestStreak) {
                        tempLongestStreak = tempPotentialLongestStreak;
                    }
                    tempPotentialLongestStreak = 1;
                }
            }

            // Po pętli sprawdzamy, czy ostatni streak był najdłuższy
            if (tempPotentialLongestStreak > tempLongestStreak) {
                tempLongestStreak = tempPotentialLongestStreak;
            }

            // Używamy zmiennych finalnych do zaktualizowania UI w głównym wątku
            final int finalTempCurrentStreak = tempCurrentStreak;
            final int finalTempLongestStreak = tempLongestStreak;
            checkForNewTrophies(finalTempCurrentStreak);
            LinearLayout currentTrophyContainer = findViewById(R.id.currentTrophyContainer);
            LinearLayout longestTrophyContainer = findViewById(R.id.longestTrophyContainer);

            // Zaktualizuj UI po zakończeniu wątku
            runOnUiThread(() -> {
                updateStreakInUI(finalTempCurrentStreak, finalTempLongestStreak);
                displayTrophies(finalTempCurrentStreak, currentTrophyContainer);
                displayTrophies(finalTempLongestStreak, longestTrophyContainer);
            });
        }).start(); // Uruchamiamy wątek roboczy
    }

    private boolean isBetween(Calendar start, Calendar end, Calendar target) {
        return !target.before(start) && !target.after(end);
    }

    private long daysBetween(Calendar start, Calendar end) {
        // Ustawiamy godziny, minuty, sekundy i milisekundy na 00:00:00 dla obu dat
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        end.set(Calendar.HOUR_OF_DAY, 0);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        // Obliczamy różnicę w milisekundach
        long diffInMillis = start.getTimeInMillis() - end.getTimeInMillis();
        return diffInMillis / (1000 * 60 * 60 * 24);
    }

    // Funkcja do porównania dat
    private boolean compareDates(Calendar newerDate, Calendar olderDate, int breakTimeInDays) {
        long diffInDays = daysBetween(newerDate, olderDate);
        breakTimeInDays++;
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


    private void updateUI() {
        TextView currentStreakTextView = findViewById(R.id.currentStreak);
        TextView longestStreakTextView = findViewById(R.id.longestStreak);

        currentStreakTextView.setText("Obecny streak: " + currentStreak);
        longestStreakTextView.setText("Najdłuższy streak: " + longestStreak);
    }

    private void showTimePickerDialog() {
        // Sprawdź uprawnienia do powiadomień
        checkNotificationPermission();

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    String reminderTime = String.format("%02d:%02d", selectedHour, selectedMinute);

                    // Zapisz w bazie w tle
                    new Thread(() -> {
                        habitDao.updateReminderTime(habitId, reminderTime);

                        // Ustaw alarm i pokaż Toast w głównym wątku
                        runOnUiThread(() -> {
                            setHabitReminder(habitId, habitName, reminderTime);
                            Toast.makeText(this, "Przypomnienie ustawione dla zwyczaju: " + habitName, Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                },
                hour,
                minute,
                true
        );

        timePicker.show();
    }

    @SuppressLint("ScheduleExactAlarm")
    private void setHabitReminder(int habitId, String habitName, String reminderTime) {
        // Sprawdzenie dokładnych alarmów (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Włącz dokładne alarmy w ustawieniach systemu", Toast.LENGTH_LONG).show();
                return;
            }
        }

        String[] parts = reminderTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Jeśli wybrana godzina już minęła dzisiaj, ustaw na jutro
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("habitId", habitId);
        intent.putExtra("habitName", habitName);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(HabitDetailActivity.class); // dodaje MainActivity jako parent
        stackBuilder.addNextIntent(intent);


        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                habitId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001); // dowolny requestCode
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Zezwolenie na powiadomienia przyznane", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Brak zezwolenia na powiadomienia", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void displayTrophies(int streakValue, LinearLayout container) {
        container.removeAllViews();

        int[] thresholds = {7, 30, 100, 200, 365, 730, 1000};
        int[] icons = {
                R.drawable.trophy_bronze,
                R.drawable.trophy_silver,
                R.drawable.trophy_gold,
                R.drawable.trophy_platinum,
                R.drawable.trophy_diamond,
                R.drawable.trophy_master,
                R.drawable.trophy_legend
        };

        for (int i = 0; i < thresholds.length; i++) {
            if (streakValue >= thresholds[i]) {
                ImageView trophy = new ImageView(this);
                trophy.setImageResource(icons[i]);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
                params.setMargins(8, 0, 8, 0);
                trophy.setLayoutParams(params);
                container.addView(trophy);
            }
        }

        if (container.getChildCount() == 0) {
            TextView noTrophyText = new TextView(this);
            noTrophyText.setText("Brak trofeów — pracuj dalej!");
            noTrophyText.setTextColor(Color.GRAY);
            noTrophyText.setTextSize(14);
            container.addView(noTrophyText);
        }
    }

    private void checkForNewTrophies(int streak) {
        // Przykładowe progi
        int[] trophyThresholds = {7, 30, 100, 200, 365, 500, 1000};
        int[] trophyIcons = {
                R.drawable.trophy_bronze,
                R.drawable.trophy_silver,
                R.drawable.trophy_gold,
                R.drawable.trophy_platinum,
                R.drawable.trophy_diamond,
                R.drawable.trophy_master,
                R.drawable.trophy_legend
        };

        new Thread(() -> {
            Habit habit = habitDao.getHabitById(habitId);
            if (habit == null) return;

            String unlocked = habit.unlockedTrophies == null ? "" : habit.unlockedTrophies;
            List<String> unlockedList = new ArrayList<>(Arrays.asList(unlocked.split(",")));

            for (int i = 0; i < trophyThresholds.length; i++) {
                int threshold = trophyThresholds[i];
                if (streak >= threshold && !unlockedList.contains(String.valueOf(threshold))) {
                    unlockedList.add(String.valueOf(threshold));

                    String updated = TextUtils.join(",", unlockedList);
                    habitDao.updateUnlockedTrophies(habitId, updated);

                    int trophyRes = trophyIcons[i];

                    runOnUiThread(() -> showTrophyUnlockedDialog(threshold, trophyRes));
                    break; // pokazujemy tylko jedno nowe trofeum na raz
                }
            }
        }).start();
    }

    private void showTrophyUnlockedDialog(int streak, int trophyResId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_trophy_unlocked, null);
        builder.setView(dialogView);

        ImageView trophyImage = dialogView.findViewById(R.id.trophyImage);
        TextView title = dialogView.findViewById(R.id.trophyTitle);
        TextView message = dialogView.findViewById(R.id.trophyMessage);

        trophyImage.setImageResource(trophyResId);
        title.setText("🎉 Gratulacje!");
        message.setText("Wykonujesz ten zwyczaj od " + streak + " dni!\nNowe trofeum zdobyte 🏆");

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        // automatycznie zamknij po 3 sekundach
        new Handler(Looper.getMainLooper()).postDelayed(dialog::dismiss, 5000);
    }


}