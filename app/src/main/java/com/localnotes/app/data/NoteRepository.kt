package com.localnotes.app.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {

    fun observeNotes(ownerId: String, query: String): Flow<List<NoteEntity>> =
        dao.observeNotes(ownerId, query.trim())

    suspend fun find(id: Long): NoteEntity? = dao.findById(id)

    /**
     * 新建或更新一条笔记。[id] 为 null 表示新建，返回实际写入的笔记 id。
     */
    suspend fun upsert(
        id: Long?,
        ownerId: String,
        title: String,
        content: String,
        pinned: Boolean
    ): Long {
        val now = System.currentTimeMillis()
        val cleanTitle = title.trim()
        if (id == null) {
            return dao.insert(
                NoteEntity(
                    ownerId = ownerId,
                    title = cleanTitle,
                    content = content,
                    pinned = pinned,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        val existing = dao.findById(id)
        if (existing == null) {
            // 原记录被清理掉了，退化成新建
            return dao.insert(
                NoteEntity(
                    ownerId = ownerId,
                    title = cleanTitle,
                    content = content,
                    pinned = pinned,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        dao.update(
            existing.copy(
                title = cleanTitle,
                content = content,
                pinned = pinned,
                updatedAt = now
            )
        )
        return id
    }

    suspend fun softDelete(id: Long) = dao.softDelete(id, System.currentTimeMillis())

    suspend fun restore(id: Long) = dao.restore(id)

    suspend fun setPinned(id: Long, pinned: Boolean) =
        dao.setPinned(id, pinned, System.currentTimeMillis())

    /** 回收站保留 30 天 */
    suspend fun purgeOldDeleted() =
        dao.purgeDeletedBefore(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
}
