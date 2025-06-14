# PennyWise – Budget Tracker App

## Developers

* Sagwadi Mashimbye (ST10168528)
* Tokollo Will Nonyane (ST10296818)
* Rinae Magadagela (ST10361117)

## Demo Video

YouTube: [https://youtu.be/1ZOmguUvEuI?si=q6jRVj7JfW0yemTw](https://youtu.be/1ZOmguUvEuI?si=q6jRVj7JfW0yemTw)

---

## Description

PennyWise is a powerful and user-centric budget tracker app designed to simplify financial planning and monitoring. Users can track expenses, define categories, set monthly goals, and view performance using graphs and gamification. The app is optimized for various device screens and initially supported offline capabilities using RoomDB, later migrated to Firebase Firestore.

---

## Core Features

* **Secure User Login**
  Sign in with registered email and password for a secure and personalized experience.

* **Sign Out Functionality**
  Log out from the session with a single tap to secure your data.

* **Create Expense Categories**
  Customize and organize spending into user-defined categories.

* **Add Expense Entries**
  Record expenses with:

    * Date
    * Start and end times
    * Description
    * Category
    * Optional photo attachment

* **Edit Transaction Entries**
  Modify previously added transactions with category-aware spinner support.

* **Set Monthly Goals**
  Define minimum and maximum spending limits to stay in control.

* **Expense History View**
  View a complete list of expenses filtered by user-selected time period. View and zoom into attached photos.

* **Category-wise Spending Summary**
  Breakdown of spending per category over time.

* **Offline Data Storage (Deprecated)**
  Previously used RoomDB for offline-first functionality.

* **Dark Mode and Light Mode**
  Toggle between themes for eye comfort.

* **User Interface**
  Clean, modern, and responsive design with form validation and intuitive interactions.

---

## Purpose

PennyWise is designed to help users:

* Set and manage monthly budgets
* Track daily expenses and categorize them
* Visualize spending trends with dynamic charts
* Stay motivated with badges and achievements (gamification)
* Learn healthy money habits

The goal is to **empower users to save more** and **spend wisely** with real-time financial insights.

---

## Design Considerations

We focused on:

* **Dark mode support** for eye comfort
* **Gamified experience**: badges for streaks, no-spend days, saving goals, etc.
* **Clean UI** using Material Design
* **Modular architecture** to simplify unit testing and CI/CD
* **Data visualization** via charts (Pie, Bar, Line, Radar using MPAndroidChart)

---

## Custom Features

1. **Gamification System: Badge Rewards**

    * Creating first category
    * Logging first transaction
    * Daily login streaks
    * Consistent expense tracking
    * Setting monthly budget goals
    * Completing a no-spend day

   Badges are shown in the `GamificationActivity`, motivating users through achievement recognition.

2. **Enhanced Transaction Detail Viewer**

    * Tap on a transaction to open a detailed view
    * Full data summary
    * Zoomable photo viewer
    * Slide-in animations for smooth transitions
    * Edit and revisit entries quickly

3. **Calendar-based Expense Overview (Bonus Feature)**

    * A calendar interface highlights:

        * Daily expense totals
        * Days with no spending
        * Click on a date to view/edit entries

4. **Budget Overview Dashboard (Bonus Feature)**

    * A visual dashboard showing:

        * Budget remaining
        * Goal tracking bar
        * Alerts when approaching limits
        * Adaptive color changes based on spending behavior

---

## Final POE Features

* **Graphical Spending Overview**
  View spending data per category via bar or pie charts. Goals appear as overlays.

* **Goal Tracker Visualization**
  Progress bars and alerts show how users are managing their goals throughout the month.

* **Gamification Integration**
  Badges and rewards drive user engagement and consistent usage.

* **App Icon & Final Assets**

    * Custom-designed app launcher icon
    * Final image assets for UI and category visuals
    * Fully responsive UI for all screen sizes and densities

---

## Technologies Used

* Kotlin – Android app development
* Firebase Firestore – Cloud-based data storage (replaces RoomDB)
* Android XML Layouts – UI components
* Glide – Efficient image loading
* GitHub Actions – CI/CD automation and testing workflows

---

## How to Run the App

1. **Clone the Repository:**

   ```bash
   git clone https://github.com/WhiteSkyTK/PennyWise
   ```

2. **Open in Android Studio**

3. **Sync Dependencies:**
   Use Gradle Sync to fetch all necessary libraries

4. **Build the App:**
   Navigate to `Build > Make Project`

5. **Run on Device/Emulator:**

    * Start emulator or connect physical device
    * Press the Run (green play) button

6. **Sign In:**
   Log in using your registered credentials to access features

---

## Testing

GitHub Actions ensures:

* Unit and instrumented tests are written to validate helper logic and UI components

**Examples:**

```kotlin
@Test
fun validateAmount_validInput_returnsDouble() {
    val result = TransactionHelper.validateAmount("R123.45")
    assertEquals(123.45, result!!, 0.001)
}
```

* The app builds successfully
* Key features are tested
* Errors are flagged during pull/merge


---

## Submission Checklist

* [x] Kotlin source code submitted on GitHub
* [x] No zip files used
* [x] Code comments and logs present
* [x] README with:

    * App purpose
    * Feature list
    * Custom features
    * GitHub usage
    * GitHub Actions usage
    * Demo video link
* [x] Built APK included in release
* [x] Research & design documents uploaded

---

## License

This project is developed for educational purposes and complies with the requirements set out by Rosebank College.

---

## Feedback & Support

For feedback or queries, please open an issue on the [GitHub repository](https://github.com/WhiteSkyTK/PennyWise/issues) or use the in-app support section.

---

Thank you for exploring PennyWise – your smart companion for personal finance management!
here is some pictures
![WhatsApp Image 2025-05-26 at 19 15 39](https://github.com/user-attachments/assets/3eb1fad6-0cb2-45d8-a9e0-b36675899b7a)
![WhatsApp Image 2025-05-26 at 19 15 40 (1)](https://github.com/user-attachments/assets/70e427cb-5572-441c-a726-1dd4835e8856)
![WhatsApp Image 2025-05-26 at 19 15 41](https://github.com/user-attachments/assets/a19aec53-f804-4964-89bb-d36485cd7497)
![WhatsApp Image 2025-05-26 at 19 15 41 (1)](https://github.com/user-attachments/assets/32360545-db55-4a05-b629-fbe18f3e53df)
![WhatsApp Image 2025-05-26 at 19 15 42](https://github.com/user-attachments/assets/d5178d21-f500-4056-9a91-48f8b6a3cd64)
![WhatsApp Image 2025-05-26 at 19 15 42 (1)](https://github.com/user-attachments/assets/ebaa8c33-066e-4919-8cee-8169dcbbe714)
![WhatsApp Image 2025-05-26 at 19 15 43](https://github.com/user-attachments/assets/e38bd186-8dc2-4d80-a743-0ab4eae30d1c)
![WhatsApp Image 2025-05-26 at 19 15 43 (1)](https://github.com/user-attachments/assets/122fbf0d-cd29-4389-8ca0-982d2fe70fae)
![WhatsApp Image 2025-05-26 at 19 15 44](https://github.com/user-attachments/assets/c3d311c9-800d-45a7-bfb7-fe7313df6863)
![WhatsApp Image 2025-05-26 at 19 15 44 (1)](https://github.com/user-attachments/assets/279c0413-9881-4df9-b4d6-38a40ba9c1d9)
![WhatsApp Image 2025-05-26 at 19 15 44 (2)](https://github.com/user-attachments/assets/0598b361-a859-4f4e-bb31-cf8f586c60c2)
![WhatsApp Image 2025-05-26 at 19 15 45](https://github.com/user-attachments/assets/7636ac18-ff77-429c-947c-c63884937cef)
![WhatsApp Image 2025-05-26 at 19 15 45 (1)](https://github.com/user-attachments/assets/1c10804e-4601-4d6d-8f2f-d4fbd376bf75)
![WhatsApp Image 2025-05-26 at 19 15 46](https://github.com/user-attachments/assets/6bc46d3d-30ca-4dfe-a074-f7ba0756a128)
![WhatsApp Image 2025-05-26 at 19 15 46 (1)](https://github.com/user-attachments/assets/a168f945-7f09-45cb-8ce7-b4bc783921a6)
![WhatsApp Image 2025-05-26 at 19 15 47](https://github.com/user-attachments/assets/a5b071c1-6837-4865-aaba-3d7b178f8418)
![WhatsApp Image 2025-05-26 at 19 15 27](https://github.com/user-attachments/assets/9c8954e5-672a-44e6-861e-746c51628d87)
![WhatsApp Image 2025-05-26 at 19 15 40](https://github.com/user-attachments/assets/2d230f72-f80e-47d3-9e2c-72bf1fdc4120)
