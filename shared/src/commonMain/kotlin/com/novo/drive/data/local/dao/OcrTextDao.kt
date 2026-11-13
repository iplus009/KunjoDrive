package com.novo.drive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novo.drive.data.local.entity.OcrTextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OcrTextDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: OcrTextEntity)

    @Query("SELECT * FROM ocr_texts WHERE fileId = :fileId")
    suspend fun byFileId(fileId: String): OcrTextEntity?

    @Query("SELECT * FROM ocr_texts WHERE fileId = :fileId")
    fun observeByFileId(fileId: String): Flow<OcrTextEntity?>

    @Query("SELECT COUNT(*) FROM ocr_texts WHERE LENGTH(TRIM(extractedText)) > 0")
    fun observeIndexedCount(): Flow<Int>

    @Query("DELETE FROM ocr_texts WHERE fileId IN (:fileIds)")
    suspend fun deleteByFileIds(fileIds: List<String>)
}

