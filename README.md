# Habit Tracker

A modern, feature-rich Android Habit Tracker application built with Kotlin and Jetpack Compose. This app helps users build and maintain positive habits through consistency tracking, smart reminders, and a beautiful UI.

## 🌟 Features

* **Beautiful UI/UX:** Built entirely with Jetpack Compose, featuring a clean, responsive design, custom flat vector illustrations, and a dedicated Light Mode login experience.
* **Smart Habit Tracking:** Track your daily habits, view your consistency map, and stay on top of your goals.
* **Personalized Onboarding:** Tailored post-login onboarding flow with profession selection and curated habit suggestions.
* **Frequent Reminders:** Receive timely, custom notifications throughout the day to ensure you never miss a habit.
* **Firebase Integration:** 
  * **Authentication:** Secure login and user management.
  * **Firestore:** Sync your onboarding status and habit data seamlessly across devices (persists across reinstalls).
  * **Hosting:** Self-hosted in-app updates with an automated release pipeline.

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose
* **Backend:** Firebase (Auth, Firestore, Hosting)
* **Architecture:** Modern Android Architecture (MVVM/MVI)

## 🚀 Getting Started

### Prerequisites

* [Android Studio](https://developer.android.com/studio) (Latest version recommended)
* A Firebase Project configured for Android.

### Installation

1. **Clone the repository:**
   ```bash
   git clone <your-repository-url>
   ```
2. **Open the project in Android Studio:**
   Select **File > Open** and choose the cloned directory.
3. **Configure Firebase:**
   Make sure you have your `google-services.json` file placed in the `app/` directory (if not already included).
4. **Environment Variables:**
   Create a file named `.env` in the project root and add your API keys (see `.env.example` for reference).
5. **Run the App:**
   Select your emulator or physical device and click **Run**.

## 🗺️ Roadmap

Check out the [devlog.md](devlog.md) for a detailed history of our recent updates and planned features, which include:
* Interactive Consistency Map
* Home Screen Widgets
* Smart Reward System (AI Automation)
* Intelligent Notifications based on Screen Time

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
