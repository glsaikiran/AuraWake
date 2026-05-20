# AuraWake: The Smart Personal Alarm 🌅

**AuraWake** is a modern, feature-rich Android alarm clock application built with Jetpack Compose. It focuses on a gentle waking experience through volume crescendo features and a personalized smart briefing with a premium Glassmorphic UI.

## ✨ Key Features

### 🎙️ Smart Morning Briefing
Wake up to a personalized voice briefing the moment you dismiss your alarm.
*   **Personal Greeting:** The app greets you by name.
*   **Date & Time:** Real-time information to keep you on schedule.
*   **Weather Status:** Current weather briefing (Placeholder).
*   **Task Summary:** Automatically reads your "Today's Tasks" list aloud.

### ⏰ Advanced Alarm System
*   **Crescendo Mode:** Alarms start whisper-quiet and gradually increase to full volume over a custom duration.
*   **Wake Window:** A specialized pre-alarm sequence designed to pull you out of deep sleep gently before your main alarm sounds.
*   **Sequential Sounds:** Support for multiple sequential sounds and custom audio files.

### ✅ Integrated Task Manager
Stay on top of your day with a built-in to-do list. Your daily tasks are synced directly with the morning briefing voice.

### 🌙 Intelligent Sleep Schedule
*   **Wind Down:** Automatically silences distracting notifications before your set bedtime.
*   **Bedtime Voice Reminders:** Gentle voice prompts to remind you when it's time to sleep.

### 🎚️ Live Voice Customization
Fully customize the briefing voice directly in the app:
*   Adjust **Pitch** and **Speed** with live audio previews.
*   Supports regional accents including **English (India)**, UK, and US.

## 📸 Screenshots

| Home Screen | Sleep Schedule | Edit Alarm | Ringing UI |
| :---: | :---: | :---: | :---: |
| ![Home](https://via.placeholder.com/200x400?text=Home+Screen) | ![Schedule](https://via.placeholder.com/200x400?text=Sleep+Schedule) | ![Edit](https://via.placeholder.com/200x400?text=Edit+Alarm) | ![Ringing](https://via.placeholder.com/200x400?text=Ringing+UI) |

## 🚀 Tech Stack
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **Database:** Room Persistence Library
*   **Navigation:** Jetpack Navigation Compose
*   **Background Tasks:** Android Services & AlarmManager
*   **Asynchronous Flow:** Kotlin Coroutines & StateFlow

## 🛠️ Installation
1. Clone the repository.
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Ensure you have **JDK 17** configured in your Gradle settings.
4. Build and run the app on an emulator or physical device (Android 8.0+).

## 🔑 Permissions
To function correctly, AuraWake requires:
*   `SCHEDULE_EXACT_ALARM`: To trigger alarms precisely on time.
*   `POST_NOTIFICATIONS`: For bedtime reminders and active alarm alerts.
*   `FOREGROUND_SERVICE`: To maintain the crescendo audio playback while the screen is off.

## 🚀 New in v1.1.0 (AuraWake Update)
*   Added Personalized Morning Briefing.
*   Added Name Setup and Task Integration.
*   Added Live Voice Feedback sliders.
*   Updated database to v6.

## 📄 License
This software is developed by **glsaikiran**.
It is open for use and modification. Proper credit must be given to the original developer, glsaikiran, in all distributions or substantial portions of the software.

Copyright (c) 2026 glsaikiran

---
*Made with ❤️ for better mornings.*
**Developed by [sai](https://github.com/glsaikiran)**
