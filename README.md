# AuraWake (Crescendo Alarm) 🌅

A modern, feature-rich Android alarm clock application built with Jetpack Compose. AuraWake focuses on a gentle waking experience through volume crescendo features and integrated sleep scheduling with a premium Glassmorphic UI.

## ✨ Features

-   **Crescendo Mode**: Gently wake up with volume that ramps up gradually over a custom duration (15-60 minutes).
-   **Sleep Schedule**: Manage your entire sleep cycle with dedicated "Bedtime" and "Wake up" windows.
-   **Evening Wind Down**: Automatically silence notifications and prepare for sleep before your scheduled bedtime.
-   **Bedtime Voice Reminders**: Custom AI-voice or original recording reminders to help you stick to your sleep routine.
-   **Modern UI**: High-fidelity Glassmorphic design with deep blue gradients and vibrant accents.
-   **Smart Scheduling**: Easy repeat options for weekdays/weekends and intuitive time pickers.
-   **Vibration Control**: Optional haptic feedback during the wake-up window.

## 📸 Screenshots

| Home Screen | Sleep Schedule | Edit Alarm | Ringing UI |
| :---: | :---: | :---: | :---: |
| ![Home](https://via.placeholder.com/200x400?text=Home+Screen) | ![Schedule](https://via.placeholder.com/200x400?text=Sleep+Schedule) | ![Edit](https://via.placeholder.com/200x400?text=Edit+Alarm) | ![Ringing](https://via.placeholder.com/200x400?text=Ringing+UI) |

## 🚀 Tech Stack

-   **Language**: Kotlin
-   **UI Framework**: Jetpack Compose (Material 3)
-   **Architecture**: MVVM (Model-View-ViewModel)
-   **Database**: Room Persistence Library
-   **Navigation**: Jetpack Navigation Compose
-   **Background Tasks**: Android Services & AlarmManager
-   **Asynchronous Flow**: Kotlin Coroutines & StateFlow

## 🛠️ Installation
1. Clone the repository:
-   **Background Tasks**: Android Services & AlarmManager
-   **Asynchronous Flow**: Kotlin Coroutines & StateFlow
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Ensure you have **JDK 17** configured in your Gradle settings.
4. Build and run the app on an emulator or physical device (Android 8.0+).

## 🔑 Permissions

To function correctly, AuraWake requires:
-   `SCHEDULE_EXACT_ALARM`: To trigger alarms precisely on time.
-   `POST_NOTIFICATIONS`: For bedtime reminders and active alarm alerts.
-   `FOREGROUND_SERVICE`: To maintain the crescendo audio playback while the screen is off.

## 🤝 Contributing

Contributions are welcome! If you'd like to improve the UI or add new features:
1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the Branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---
**Developed by [sai](https://github.com/glsaikiran)**

1. Clone the repository:
   
