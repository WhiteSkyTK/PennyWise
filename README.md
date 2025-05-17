PennyWise – Budget Tracker App

Developers

Sagwadi Mashimbye (ST102968528)

Tokollo Will Nonyane (ST10296818)

Rinae Magadagela (ST10361117)

Demo Video

YouTube: https://youtu.be/1ZOmguUvEuI?si=q6jRVj7JfW0yemTw

Description

PennyWise is a powerful and user-centric Budget Tracker App designed
to simplify financial planning and monitoring. 
Users can track expenses, define categories, set monthly goals, 
and view performance using graphs and gamification. 
The app is fully functional offline with RoomDB and optimized for various device screens.

Core Features

Secure User LoginLog in with a username and password for a personalized experience.

Create Expense CategoriesCustomize and organize spending into user-defined categories.

Add Expense EntriesLog expenses with:

Date

Start and end times

Description

Category

Optional photo attachment

Set Monthly GoalsDefine minimum and maximum monthly spending limits.

Expense History ViewAccess a complete list of expenses filtered by user-selected time period. 
View and open attached photos.

Category-wise Spending SummaryBreakdown of total spending per category for a specific time range.

Offline Data StorageAll data is stored locally with RoomDB.

User InterfaceClean, modern, and responsive design with form validation and intuitive interactions.

Purpose

PennyWise is designed to help users:
- Set and manage monthly budgets
- Track daily expenses and categorize them
- Visualize spending trends with dynamic charts
- Stay motivated with badges and achievements (gamification)
- Learn healthy money habits

The goal is to **empower users to save more** and **spend wisely** with real-time 
financial insights.

---

##  Design Considerations

We focused on:
- **Dark mode support** for eye comfort
- **Gamified experience**: badges for streaks, no-spend days, saving goals, etc.
- **Clean UI** using Material Design
- **Modular architecture** to simplify unit testing and CI/CD
- **Data visualization** via charts (Pie, Bar, Line, Radar using MPAndroidChart)


Custom Features

1. Gamification System: Badge Rewards

Unlock badges for key activities:

Creating first category

Logging first transaction

Daily login streaks

Consistent expense tracking

Setting monthly budget goals

Completing a no-spend day

Badges are shown in the GamificationActivity, motivating users through achievement recognition.

2.  Enhanced Transaction Detail Viewer

Tap on a transaction to open a detailed view.

Features include:

Full data summary

Zoomable photo viewer

Slide-in animations for smooth transitions

Edit and revisit entries quickly

3.  Calendar-based Expense Overview (Bonus Feature)

A calendar interface highlights:

Daily expense totals

Days with no spending

Click on a date to view/edit entries

Enhances financial habit visualization

4.  Budget Overview Dashboard (Bonus Feature)

A visual dashboard showing:

Budget remaining

Goal tracking bar

Alerts when approaching limits

Adaptive color changes based on spending behavior

Final POE Features

Graphical Spending OverviewView spending data per category via bar or pie charts.
Minimum and maximum goals appear as overlays.

Goal Tracker VisualizationProgress bars and alerts show
how users are managing their goals throughout the month.

Gamification IntegrationBadges and rewards drive user engagement and consistent usage.

App Icon & Final Assets

Custom-designed app launcher icon

Final image assets for UI and category visuals

Fully responsive UI for all screen sizes and densities

 Technologies Used

Kotlin – Android app development

Room Database – Local offline data storage

Android XML Layouts – UI components

Glide – Efficient image loading

GitHub Actions – CI/CD automation and testing workflows

How to Run the App

Clone the Repository:

git clone https://github.com/WhiteSkyTK/PennyWise

Open in Android Studio

Sync Dependencies:

Use Gradle Sync to fetch all necessary libraries

Build the App:

Navigate to Build > Make Project

Run on Device/Emulator:

Start emulator or connect physical device

Press the Run (green play) button

Sign In:

Log in using your registered credentials to access features

Testing

GitHub Actions ensures:
Unit and instrumented tests are written to validate helper logic and UI components.

**Examples:**
```kotlin
@Test
fun validateAmount_validInput_returnsDouble() {
    val result = TransactionHelper.validateAmount("R123.45")
    assertEquals(123.45, result!!, 0.001)
}

The app builds successfully

Key features are tested

Errors are flagged during pull/merge