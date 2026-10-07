package com.eltavine.sereingram.adapters.room

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["dialog_id", "message_id"], unique = true)],
)
internal class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "dialog_id") val dialogId: Long,
    @ColumnInfo(name = "message_id") val messageId: Int,
    @ColumnInfo(name = "message_date") val messageDate: Int,
    @ColumnInfo(name = "sender_id") val senderId: Long,
    val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Dao
internal interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE dialog_id = :dialogId AND message_id = :messageId")
    fun delete(dialogId: Long, messageId: Int)

    @Query("SELECT * FROM bookmarks ORDER BY created_at DESC, id DESC")
    fun all(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE dialog_id = :dialogId ORDER BY message_id DESC")
    fun inChat(dialogId: Long): List<BookmarkEntity>
}

@Database(entities = [BookmarkEntity::class], version = 1, exportSchema = true)
internal abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarks(): BookmarkDao
}
