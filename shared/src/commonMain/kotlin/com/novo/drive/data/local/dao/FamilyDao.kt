package com.novo.drive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novo.drive.data.local.entity.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {

    @Query("SELECT * FROM family_members ORDER BY slotIndex ASC")
    fun observeAll(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members ORDER BY slotIndex ASC")
    suspend fun getAll(): List<FamilyMemberEntity>

    @Query("SELECT * FROM family_members WHERE slotIndex = :slotIndex")
    suspend fun bySlot(slotIndex: Int): FamilyMemberEntity?

    @Query("SELECT COUNT(*) FROM family_members WHERE isRevoked = 0")
    suspend fun activeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: FamilyMemberEntity)

    @Query(
        """UPDATE family_members
           SET isRevoked = :revoked, lastHeartbeatAt = :updatedAt
           WHERE slotIndex = :slotIndex"""
    )
    suspend fun setRevoked(slotIndex: Int, revoked: Boolean, updatedAt: Long)

    @Query("DELETE FROM family_members WHERE slotIndex = :slotIndex")
    suspend fun deleteSlot(slotIndex: Int)
}

