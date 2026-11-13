package com.novo.drive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novo.drive.data.local.entity.IplusMessageContactEntity
import com.novo.drive.data.local.entity.IplusMessageEntity
import com.novo.drive.data.local.entity.IplusMessageGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IplusMessageDao {

    // --- Contacts ---
    @Query("SELECT * FROM iplus_message_contacts WHERE isBlocked = 0 ORDER BY lastSeenTimestamp DESC")
    fun observeContacts(): Flow<List<IplusMessageContactEntity>>

    @Query("SELECT * FROM iplus_message_contacts WHERE userId = :userId")
    suspend fun getContact(userId: Long): IplusMessageContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(contact: IplusMessageContactEntity)

    @Query("UPDATE iplus_message_contacts SET isBlocked = :blocked WHERE userId = :userId")
    suspend fun setBlocked(userId: Long, blocked: Boolean)

    @Query("SELECT * FROM iplus_message_contacts WHERE isBlocked = 1")
    fun observeBlockedContacts(): Flow<List<IplusMessageContactEntity>>

    // --- Messages ---
    @Query("SELECT * FROM iplus_message_messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun observeMessages(chatId: Long): Flow<List<IplusMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: IplusMessageEntity)

    @Query("UPDATE iplus_message_messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: Long, status: String)

    @Query("UPDATE iplus_message_messages SET status = 'READ' WHERE chatId = :chatId AND isOutgoing = 0")
    suspend fun markMessagesAsRead(chatId: Long)

    @Query("UPDATE iplus_message_messages SET isViewed = 1 WHERE messageId = :messageId")
    suspend fun markViewOnceViewed(messageId: Long)

    @Query("DELETE FROM iplus_message_messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: Long)

    @Query("DELETE FROM iplus_message_messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: Long)

    // --- Groups ---
    @Query("SELECT * FROM iplus_message_groups ORDER BY groupId DESC")
    fun observeGroups(): Flow<List<IplusMessageGroupEntity>>

    @Query("SELECT * FROM iplus_message_groups WHERE groupId = :groupId")
    suspend fun getGroup(groupId: Long): IplusMessageGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: IplusMessageGroupEntity)

    @Query("UPDATE iplus_message_groups SET isReadOnly = :isReadOnly WHERE groupId = :groupId")
    suspend fun updateGroupReadOnly(groupId: Long, isReadOnly: Boolean)

    @Query("UPDATE iplus_message_groups SET adminIds = :adminIds WHERE groupId = :groupId")
    suspend fun updateGroupAdmins(groupId: Long, adminIds: String)

    @Query("UPDATE iplus_message_groups SET allowedSenderIds = :allowedSenderIds WHERE groupId = :groupId")
    suspend fun updateGroupAllowedSenders(groupId: Long, allowedSenderIds: String)

    @Query("UPDATE iplus_message_groups SET groupUsername = :username WHERE groupId = :groupId")
    suspend fun updateGroupUsername(groupId: Long, username: String?)

    @Query("UPDATE iplus_message_groups SET title = :title WHERE groupId = :groupId")
    suspend fun updateGroupTitle(groupId: Long, title: String)

    @Query("DELETE FROM iplus_message_groups WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: Long)
}

