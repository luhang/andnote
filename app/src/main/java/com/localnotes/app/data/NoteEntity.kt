package com.localnotes.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一条笔记。全部字段只存在本机 Room 数据库里，不上传任何服务器。
 *
 * ownerId 记录「这条笔记属于哪个 Google 账户」，这样同一台设备上换账号登录时
 * 各自只看到自己的笔记（数据仍然都在本机，没有云端同步）。
 */
@Entity(
    tableName = "notes",
    indices = [Index("ownerId"), Index("updatedAt")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ownerId: String,
    val title: String,
    val content: String,
    val pinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    /** 软删除时间戳，用于「删除后撤销」；null 表示未删除 */
    val deletedAt: Long? = null
)
