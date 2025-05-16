package com.example.pennywise

import androidx.room.*

@Dao
interface EarnedBadgeDao {

    @Query("SELECT * FROM earned_badges WHERE userEmail = :email")
    suspend fun getEarnedBadges(email: String): List<EarnedBadge>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadge(badge: EarnedBadge)

    @Query("SELECT COUNT(*) FROM earned_badges WHERE userEmail = :email AND badgeTitle = :badgeTitle")
    suspend fun isBadgeEarned(email: String, badgeTitle: String): Int

    @Query("UPDATE earned_badges SET metadata = :metadata WHERE userEmail = :email AND badgeTitle = :badgeTitle")
    suspend fun updateBadgeMetadata(email: String, badgeTitle: String, metadata: Int)
}