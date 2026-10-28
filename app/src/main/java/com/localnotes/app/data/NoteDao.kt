package com.localnotes.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    /**
     * query 为空串时返回全部；否则按标题/正文模糊匹配。
     * 置顶优先，其次按更新时间倒序。
     */
    @Query(
        """
        SELECT * FROM notes
        WHERE ownerId = :ownerId
          AND deletedAt IS NULL
          AND (:query = '' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY pinned DESC, updatedAt DESC
        """
    )
    fun observeNotes(ownerId: String, query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun findById(id: Long): NoteEntity?

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("UPDATE notes SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long)

    @Query("UPDATE notes SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("UPDATE notes SET pinned = :pinned, updatedAt = :timestamp WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, timestamp: Long)

    /** 清理放太久的回收站数据，避免数据库无限增长 */
    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeletedBefore(before: Long)
}
