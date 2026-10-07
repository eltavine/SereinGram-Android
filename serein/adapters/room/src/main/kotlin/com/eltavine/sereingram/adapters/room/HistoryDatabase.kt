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
    tableName = "records",
    indices = [Index(value = ["kind", "dialog_id", "message_id", "revision"], unique = true)],
)
internal class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val kind: Int,
    @ColumnInfo(name = "dialog_id") val dialogId: Long,
    @ColumnInfo(name = "message_id") val messageId: Int,
    val revision: Int,
    @ColumnInfo(name = "topic_id") val topicId: Long,
    val date: Int,
    @ColumnInfo(name = "recorded_at") val recordedAt: Long,
    @ColumnInfo(name = "from_id") val fromId: Long,
    val text: String,
    @ColumnInfo(name = "tl_message", typeAffinity = ColumnInfo.BLOB) val tlMessage: ByteArray,
    @ColumnInfo(name = "api_layer") val apiLayer: Int,
)

@Dao
internal interface RecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(records: List<RecordEntity>)

    @Query(
        "SELECT * FROM records WHERE kind = :kind AND dialog_id = :dialogId AND message_id < :before " +
            "ORDER BY message_id DESC LIMIT :limit",
    )
    fun page(kind: Int, dialogId: Long, before: Int, limit: Int): List<RecordEntity>

    @Query(
        "SELECT * FROM records WHERE kind = :kind AND dialog_id = :dialogId AND message_id = :messageId " +
            "ORDER BY revision, recorded_at",
    )
    fun versions(kind: Int, dialogId: Long, messageId: Int): List<RecordEntity>

    @Query("SELECT DISTINCT message_id FROM records WHERE kind = :kind AND dialog_id = :dialogId AND message_id IN (:messageIds)")
    fun present(kind: Int, dialogId: Long, messageIds: List<Int>): List<Int>

    @Query("DELETE FROM records WHERE dialog_id = :dialogId AND message_id IN (:messageIds)")
    fun forget(dialogId: Long, messageIds: List<Int>)

    @Query("SELECT COUNT(*) FROM records WHERE kind = :kind AND dialog_id = :dialogId")
    fun count(kind: Int, dialogId: Long): Int

    @Query("DELETE FROM records WHERE dialog_id = :dialogId")
    fun clear(dialogId: Long)
}

@Database(entities = [RecordEntity::class], version = 1, exportSchema = true)
internal abstract class HistoryDatabase : RoomDatabase() {
    abstract fun records(): RecordDao
}
