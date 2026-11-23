# PEAK Mobile — Native Android

A native Kotlin Android mountain-climbing game. It uses Android Canvas and has no third-party libraries.

## Build
1. Install Android Studio.
2. Extract this ZIP.
3. In Android Studio choose **Open** and select the `peak_mobile_android` folder.
4. Let Gradle sync. Use JDK 17 if prompted.
5. Connect an Android device or start an emulator.
6. Press **Run ▶**.
7. To export an APK, choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

## Requirements
- Android Studio
- Android SDK Platform 35
- JDK 17
- Minimum Android 6.0 (API 23)

## Controls
- ◀ / ▶: move
- ▲ / ▼: climb when near a ledge
- Hold GRAB: cling to nearby rock
- JUMP: jump
- Green rest ledges restore stamina; higher ledges activate checkpoints.

## Source
- `MainActivity.kt`: fullscreen landscape activity.
- `PeakGameView.kt`: rendering, game loop, physics, touch controls, stamina, hazards, checkpoints and win state.
- Gradle files: Android project configuration.

This ZIP contains the Android source project, not a precompiled APK.