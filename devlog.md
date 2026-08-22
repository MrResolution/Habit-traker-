# Habit Tracker - Development Log

This document tracks the progress, recent updates, and future roadmap for the Habit Tracker application.

## Recent Progress & Updates (August 2026)

### 🚀 New Features & Enhancements
- **In-App Updates & Hosting:** Implemented a self-hosted in-app update feature. Set up Firebase Hosting to serve the live GitHub Release APK and `version.json`. Bumped version to `v1.1.0`.
- **Onboarding Experience:** 
  - Added a post-login onboarding flow tailored with profession and curated habit suggestions.
  - Synchronized onboarding completion status with Firestore to ensure persistence across reinstalls.
  - Removed the gender selection step for a more streamlined experience.
- **Login Experience Redesign:** 
  - Redesigned Auth UI utilizing a bottom sheet.
  - Replaced 3D illustrations with custom flat vector, hand-drawn avatars matching the app's art style.
  - Forced Light Mode exclusively for the login screen.
- **Frequent Reminders:** Added the capability to receive frequent reminders for habits throughout the day.
- **Notification Improvements:** 
  - Improved custom notification layout and typography.
  - Replaced the generic alarm icon with the app's launcher icon for better brand recognition.

### 🐛 Bug Fixes & UI Polish
- **Selected Cards UI Glitches:** Fixed grey shadow and double-border glitches on selected cards by replacing Material Card with a flat Row, removing elevation, and clipping ripple effects.
- **Chart Rendering:** Fixed a chart overlap issue by making the Canvas padding density-aware.
- **Notifications:** Resolved an issue with custom notification remote views that caused the app icon to appear squished in a collapsed state. Fixed unresolved reference to `custom_notification` in FCMService.
- **Compose State:** Fixed a composable invocation error by hoisting `animateFloatAsState`.
- **Input Validation:** Fixed an hour input validation glitch in the habit creation dialog.
- **Text Fields:** Corrected indentation so `CustomTextField` is properly nested inside the Box scope.

---

## 🗺️ Roadmap & Planned Features

* **Consistency Map Interactivity:** Add a feature to long-press on the consistency map to reveal the specific date.
* **Home Screen Widgets:** Implement Android widgets (checklist, consistency graph) for quick access and tracking.
* **Smart Reward System (AI Automation):** 
  - Animation for 100% completion (clickable to reveal a random reward).
  - Use AI to identify and suggest the right type of rewards based on mapped habits.
* **Intelligent Notifications:** Enhance the notification system to remind users of incomplete habit tasks during their peak screen time (utilizing screen time data).
* **Extreme Commitment Feature:** Introduce a "commit" function where users sign up for a task with high stakes to feel the weight of not completing it.
* **Leaderboard Expansion:** Allow users to tap on a peer's ID in the leaderboard to view their complete public stats.
* **Punctuality Metric:** Build a system to measure and reward punctuality in habit completion.
* **Score Math Upgrade:** Incorporate screen time metrics into the overall scoring algorithm.
