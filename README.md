📱 Smart Budgeting App

A simple yet powerful Android budgeting application designed to help users track expenses, categorize spending, and securely manage financial data using Room Database and Firebase Authentication.

---

## 📘 Description

Smart Budgeting is a Kotlin-based Android app that empowers users to manage their personal finances efficiently. It allows users to add custom categories (e.g., Food, Transport), log expenses under those categories, and visualize how much has been spent in each area. The app uses Room Database for offline data storage and Firebase Authentication for secure login. Optional Firebase Firestore support allows cloud syncing.

It’s an ideal beginner-to-intermediate Android project for understanding MVVM-inspired design, Room integration, and Firebase Authentication.

---

## 🛠 Getting Started

### 📋 Dependencies

Before installing or running the project, ensure you have the following:

- Android Studio Giraffe or newer
- Kotlin 1.8.x
- Gradle 8.x
- Android SDK 33 or higher
- Firebase project with Authentication enabled
- Internet connection (for Firebase login)

#### Main Libraries/Tools Used

- Room Database 2.5.2
- Kotlin Coroutines
- Firebase Authentication
- Firebase Firestore (optional)
- Android Jetpack (Lifecycle, ViewModel, ViewBinding)

---

## 🔽 Installing

1. Clone the repository:
git clone https://github.com/ST10254797/SmartBudgetting.git

2. Open the project in Android Studio.

3. Sync Gradle and let dependencies install.

4. Setup Firebase:
   - Go to Firebase Console → Create new project.
   - Enable Email/Password authentication.
   - Download google-services.json and place it inside the /app directory.
   - Make sure to add Firebase SDK to your Gradle files.

5. Add the following dependencies to your app-level build.gradle if missing:

groovy
// Room
implementation 'androidx.room:room-runtime:2.5.2'
kapt 'androidx.room:room-compiler:2.5.2'

// Kotlin Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'

// Firebase
implementation 'com.google.firebase:firebase-auth:22.1.0'
implementation 'com.google.firebase:firebase-firestore:24.7.1'


---

## ▶ Executing Program

1. Open Android Studio.
2. Build the project: Build → Make Project.
3. Run the app on emulator or physical Android device.
4. Register or log in using Firebase Email/Password Authentication.
5. Start managing your expenses:
   - Add categories.
   - Add expenses to categories.
   - View total spending by category.

---

## 🧠 Core Functionality

- 🔒 Secure user authentication via Firebase.
- 🧾 Add, delete, and list categories.
- 💰 Track expenses within each category.
- 📊 Calculate and display total spending.
- 🗂 Data isolation by Firebase UID.
- 💽 Room Database for offline persistence.

### 🧱 RoomDB Use Cases

- Persisting user-created expense categories.
- Storing individual expense entries linked to categories.
- Retrieving total spending per category.
- Ensuring offline access to budgeting data.

Example Code: Load categories and total expenses (CategoryActivity.kt)

kotlin
@SuppressLint("SetTextI18n")
private fun updateCategoryList() {
    val userId = auth.currentUser?.uid ?: return

    lifecycleScope.launch(Dispatchers.IO) {
        val categories = categoryDao.getCategoriesByUser(userId)
        val displayText = StringBuilder()

        for (category in categories) {
            val expenses = expenseDao.getExpensesByCategory(category.id, userId)
            val total = expenses.sumOf { it.amount }

            displayText.append("${category.name} (Total: R${"%.2f".format(total)})\n")
            expenses.forEach {
                displayText.append("  • R${"%.2f".format(it.amount)} - ${it.description}\n")
            }
            displayText.append("\n")
        }

        withContext(Dispatchers.Main) {
            binding.textViewCategories.text = displayText.toString()
        }
    }
}


---

## ❓ Help

Common Issues:

- Make sure Firebase is initialized correctly.
- Always verify that google-services.json is placed in the correct directory.
- If login fails, check Firebase Authentication settings in the console.

Useful command for logging Firebase debug:
adb logcat | grep FirebaseAuth


---

## 👨‍💻 Authors

- Cristiano — ST10254797
- Ryan — ST10377479
- Ethan — ST10279132

Project maintained at: https://github.com/ST10254797/SmartBudgetting.git

---

## 🔢 Version History

- v0.2
  - UI improvements
  - Firebase Authentication added
  - Code refactored for performance
  - Firestore sync (optional)
- v0.1
  - Initial Release
  - Basic RoomDB functionality
  - Expense and Category entities

---

## 📜 License

MIT License

Copyright (c) 2025

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED.

---

## 📹 Video Link

---

## 🙏 Acknowledgments

- Inspired by financial tracking apps like Monefy and Wallet.
- Thanks to Android Developers and Kotlin Slack community.

Tools and packages:
- PurpleBooth
- Firebase Docs
- Android Developer Docs
