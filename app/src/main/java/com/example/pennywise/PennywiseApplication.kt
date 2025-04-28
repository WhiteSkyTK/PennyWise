package com.example.pennywise

import android.app.Application
import android.util.Log
import com.example.pennywise.data.AppDatabase
import com.example.pennywise.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PennywiseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("PennywiseApplication", "Application started")

        val database = AppDatabase.getDatabase(this)
        database.openHelper.writableDatabase

        CoroutineScope(Dispatchers.IO).launch {
            val dao = database.categoryDao()

            val existingCategories = dao.getAllCategories()
            if (existingCategories.isEmpty()) {
                dao.insertAll(
                    listOf(
                        Category(name = "Accessories", type = "expense"),
                        Category(name = "Alcohol", type = "expense"),
                        Category(name = "Baby Supplies", type = "expense"),
                        Category(name = "Bills", type = "expense"),
                        Category(name = "Books", type = "expense"),
                        Category(name = "Car", type = "expense"),
                        Category(name = "Charity", type = "expense"),
                        Category(name = "Clothes", type = "expense"),
                        Category(name = "Coffee", type = "expense"),
                        Category(name = "Delivery", type = "expense"),
                        Category(name = "Dining Out", type = "expense"),
                        Category(name = "Electricity", type = "expense"),
                        Category(name = "Fine", type = "expense"),
                        Category(name = "Fuel", type = "expense"),
                        Category(name = "Gaming", type = "expense"),
                        Category(name = "Gifts", type = "expense"),
                        Category(name = "Groceries", type = "expense"),
                        Category(name = "Gym", type = "expense"),
                        Category(name = "Haircut", type = "expense"),
                        Category(name = "Hardware", type = "expense"),
                        Category(name = "Healthcare", type = "expense"),
                        Category(name = "Insurance", type = "expense"),
                        Category(name = "Internet", type = "expense"),
                        Category(name = "Laundry", type = "expense"),
                        Category(name = "Lottery", type = "expense"),
                        Category(name = "Miscellaneous", type = "expense"),
                        Category(name = "Movies", type = "expense"),
                        Category(name = "Music", type = "expense"),
                        Category(name = "Parking", type = "expense"),
                        Category(name = "Pet Care", type = "expense"),
                        Category(name = "Phone", type = "expense"),
                        Category(name = "Public Transport", type = "expense"),
                        Category(name = "Rent", type = "expense"),
                        Category(name = "Repairs", type = "expense"),
                        Category(name = "Snacks…", type = "expense"),
                        Category(name = "Software", type = "expense"),
                        Category(name = "Spa", type = "expense"),
                        Category(name = "Stationery", type = "expense"),
                        Category(name = "Streaming", type = "expense"),
                        Category(name = "Subscription", type = "expense"),
                        Category(name = "Taxi", type = "expense"),
                        Category(name = "Tools", type = "expense"),
                        Category(name = "Toys", type = "expense"),
                        Category(name = "Travel", type = "expense"),
                        Category(name = "Tuition", type = "expense"),
                        Category(name = "Water", type = "expense"),
                        Category(name = "WiFi", type = "expense"),
                        Category(name = "Other", type = "expense"),
                        Category(name = "Affiliate", type = "income"),
                        Category(name = "Allowance", type = "income"),
                        Category(name = "Blog", type = "income"),
                        Category(name = "Bonus", type = "income"),
                        Category(name = "Cashback", type = "income"),
                        Category(name = "Coding", type = "income"),
                        Category(name = "Commission", type = "income"),
                        Category(name = "Consulting", type = "income"),
                        Category(name = "Digital Art", type = "income"),
                        Category(name = "Dividends", type = "income"),
                        Category(name = "Dog Walking", type = "income"),
                        Category(name = "Donations", type = "income"),
                        Category(name = "Dropshipping", type = "income"),
                        Category(name = "eBook", type = "income"),
                        Category(name = "Event Hosting", type = "income"),
                        Category(name = "Freelance", type = "income"),
                        Category(name = "Gift", type = "income"),
                        Category(name = "Grant", type = "income"),
                        Category(name = "Hair Braiding", type = "income"),
                        Category(name = "Income", type = "income"),
                        Category(name = "Interest", type = "income"),
                        Category(name = "Investment", type = "income"),
                        Category(name = "Lottery", type = "income"),
                        Category(name = "Online Courses", type = "income"),
                        Category(name = "Other", type = "income"),
                        Category(name = "Part-time Job", type = "income"),
                        Category(name = "Passive Income", type = "income"),
                        Category(name = "Pension", type = "income"),
                        Category(name = "Prize", type = "income"),
                        Category(name = "Profit", type = "income"),
                        Category(name = "Refund", type = "income"),
                        Category(name = "Reimbursement", type = "income"),
                        Category(name = "Rent Income", type = "income"),
                        Category(name = "Royalties", type = "income"),
                        Category(name = "Salary", type = "income"),
                        Category(name = "Scholarship", type = "income"),
                        Category(name = "Selling Items", type = "income"),
                        Category(name = "Side Hustle", type = "income"),
                        Category(name = "Social Media", type = "income"),
                        Category(name = "Stipend", type = "income"),
                        Category(name = "Surveys", type = "income"),
                        Category(name = "Tax Refund", type = "income"),
                        Category(name = "Tips", type = "income"),
                        Category(name = "Translation", type = "income"),
                        Category(name = "Trust Fund", type = "income"),
                        Category(name = "Tutoring", type = "income"),
                        Category(name = "Vouchers", type = "income"),
                        Category(name = "Winnings", type = "income"),
                        Category(name = "YouTube", type = "income")
                    )
                )
                Log.d("PennywiseApplication", "Default categories inserted.")
            } else {
                Log.d("PennywiseApplication", "Categories already exist. Skipping insert.")
            }
        }

        Log.d("PennywiseApplication", "Database initialized")
    }
}
