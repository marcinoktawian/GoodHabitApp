# GoodHabitApp

`GoodHabitApp` is a native Android application designed for tracking and building positive habits. It allows users to monitor their progress, visualize their consistency on a calendar, and manage their streaks effectively.<br>
<b><u><i>This app was fully created by AI. Whole project was a test if it's possible. There were many issues, code isn't clean, but AI passed test with a lot of user's help</i></u></b>

<img width="430" height="843" alt="image" src="https://github.com/user-attachments/assets/55983799-1d5d-4f89-9544-ef7ea6298a32" />


# 🚀 Features
- **Habit List**: View all your habits in a clean, organized list.
- **Detailed Habit View**: Dive into a detailed analysis of a specific habit's progress.
- **Progress Calendar**: Visualize your journey with an interactive calendar:
  - ✅ **Green**: A day the habit was successfully completed.
  - ❌ **Red**: A day that was missed.
  - ⏸️ **Blue**: A day marked as a planned break.
  - 🔘 **Grey**: The current day.
- **Streak Management**: The app automatically calculates your current and longest streaks to keep you motivated.
- **Planned Breaks**: Add scheduled breaks for a habit without interrupting your streak. This is perfect for rest days or vacations.
- **Local Data Persistence**: All user data is securely stored locally on the device using the Room persistence library.

  <img width="418" height="850" alt="image" src="https://github.com/user-attachments/assets/84a72297-0734-4784-b177-f64e8a49255c" />


#  🛠️ Tech Stack & Key Libraries

This project is built using modern Android development practices and libraries:
- Language: Java
- Architecture: Follows standard Android app component patterns.
- Core:
  - AndroidX Libraries: A suite of Jetpack libraries including `AppCompat` for backward compatibility and `Activity` for managing UI lifecycle.
  - Material Components for Android: Implements Material Design guidelines for a consistent and modern UI.
- Database:
  - Room Persistence Library: A robust SQL object mapping library for local data storage. All database operations are performed on background threads to keep the UI responsive.
- UI Components:
  - `RecyclerView`: For efficient display of the habit list.
  - `MaterialCalendarView`: An external library for rendering the interactive and highly customizable calendar.
- Build System: Gradle with Version Catalogs (`libs.versions.toml`) for streamlined dependency management.

# ⚙️ Project Structure
The project's codebase is organized into logical packages:
- `com.devszatops.goodhabitapp`: The root package for the application.
  - `data`: Contains all database-related classes, including Room `Entity`, `DAO` (Data Access Object), and the `Database` class definition.
  - `HabitAdapter.java`: The `RecyclerView.Adapter` responsible for binding habit data to the main list view.
  - `MainActivity.java`: The main entry point of the app, displaying the list of all user habits.
  - `HabitDetailActivity.java`: The detail screen, which features the `MaterialCalendarView`, streak calculation logic, and functionality for adding/managing habit logs and breaks.
 
# 🚀 Getting Started
To get a local copy up and running, follow these simple steps.

### Prerequisites
- Android Studio (latest stable version recommended)
- An Android Emulator or a physical Android device (minSdk 29 or higher)

### Installation
1. Clone the repository
2. Open the project in Android Studio.
3. Let Gradle sync the dependencies. This should happen automatically.
4. Build and run the application on your selected emulator or device.


