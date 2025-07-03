package com.tk.pennywise

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object PreloadedCategories {

    val defaultCategories = listOf(
        // Expense Categories
        "Accessories" to "expense",
        "Alcohol" to "expense",
        "Baby Supplies" to "expense",
        "Bills" to "expense",
        "Books" to "expense",
        "Car" to "expense",
        "Charity" to "expense",
        "Clothes" to "expense",
        "Coffee" to "expense",
        "Delivery" to "expense",
        "Dining Out" to "expense",
        "Electricity" to "expense",
        "Fine" to "expense",
        "Fuel" to "expense",
        "Gaming" to "expense",
        "Gifts" to "expense",
        "Groceries" to "expense",
        "Gym" to "expense",
        "Haircut" to "expense",
        "Hardware" to "expense",
        "Healthcare" to "expense",
        "Insurance" to "expense",
        "Internet" to "expense",
        "Laundry" to "expense",
        "Lottery" to "expense",
        "Miscellaneous" to "expense",
        "Movies" to "expense",
        "Music" to "expense",
        "Parking" to "expense",
        "Pet Care" to "expense",
        "Phone" to "expense",
        "Public Transport" to "expense",
        "Rent" to "expense",
        "Repairs" to "expense",
        "Snacks…" to "expense",
        "Software" to "expense",
        "Spa" to "expense",
        "Stationery" to "expense",
        "Streaming" to "expense",
        "Subscription" to "expense",
        "Taxi" to "expense",
        "Tools" to "expense",
        "Toys" to "expense",
        "Travel" to "expense",
        "Tuition" to "expense",
        "Water" to "expense",
        "WiFi" to "expense",
        "Other" to "expense",

        // Income Categories
        "Affiliate" to "income",
        "Allowance" to "income",
        "Blog" to "income",
        "Bonus" to "income",
        "Cashback" to "income",
        "Coding" to "income",
        "Commission" to "income",
        "Consulting" to "income",
        "Digital Art" to "income",
        "Dividends" to "income",
        "Dog Walking" to "income",
        "Donations" to "income",
        "Dropshipping" to "income",
        "eBook" to "income",
        "Event Hosting" to "income",
        "Freelance" to "income",
        "Gift" to "income",
        "Grant" to "income",
        "Hair Braiding" to "income",
        "Income" to "income",
        "Interest" to "income",
        "Investment" to "income",
        "Lottery" to "income",
        "Online Courses" to "income",
        "Other" to "income",
        "Part-time Job" to "income",
        "Passive Income" to "income",
        "Pension" to "income",
        "Prize" to "income",
        "Profit" to "income",
        "Refund" to "income",
        "Reimbursement" to "income",
        "Rent Income" to "income",
        "Royalties" to "income",
        "Salary" to "income",
        "Scholarship" to "income",
        "Selling Items" to "income",
        "Side Hustle" to "income",
        "Social Media" to "income",
        "Stipend" to "income",
        "Surveys" to "income",
        "Tax Refund" to "income",
        "Tips" to "income",
        "Translation" to "income",
        "Trust Fund" to "income",
        "Tutoring" to "income",
        "Vouchers" to "income",
        "Winnings" to "income",
        "YouTube" to "income"
    ).mapIndexed { index, (name, type) ->
        Category(name = name, type = type, categoryIndex = index)
    }

    suspend fun preloadUserCategories(userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val userCategoryCollection = firestore.collection("users").document(userId).collection("categories")

        try {
            val existingSnapshot = userCategoryCollection.get().await()
            val existingCategoryKeys = existingSnapshot.documents.mapNotNull {
                val name = it.getString("name")
                val type = it.getString("type")
                if (name != null && type != null) "$name-$type" else null
            }.toSet()

            val categoriesToAdd = defaultCategories.filter {
                "${it.name}-${it.type}" !in existingCategoryKeys
            }

            Log.d("PreloadCategories", "Preloading ${categoriesToAdd.size} categories for $userId")

            categoriesToAdd.forEach { category ->
                val newDoc = userCategoryCollection.document()
                val categoryMap = mapOf(
                    "id" to newDoc.id,
                    "name" to category.name,
                    "type" to category.type,
                    "categoryIndex" to category.categoryIndex
                )
                newDoc.set(categoryMap)
            }

            Log.d("PreloadCategories", "Successfully preloaded categories for $userId")
        } catch (e: Exception) {
            Log.e("PreloadCategories", "Failed to preload categories for $userId", e)
        }
    }
}
