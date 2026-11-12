package com.lyldz.ab.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "intake", indices = [Index("atMs")])
data class Intake(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val atMs: Long,
    val ml: Int,
)

@Dao
interface IntakeDao {
    @Insert
    suspend fun insert(i: Intake): Long

    @Query("DELETE FROM intake WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM intake WHERE atMs >= :from AND atMs < :to ORDER BY atMs DESC")
    fun observeRange(from: Long, to: Long): Flow<List<Intake>>

    /** Geri al için: en son eklenen kayıt (geçmişe eklenen de olabilir, bu yüzden id sırası). */
    @Query("SELECT id FROM intake ORDER BY id DESC LIMIT 1")
    suspend fun lastId(): Long?
}

// shortcut: şema dışa aktarımı ve migration yok, uygulama yayınlanmadı; ilk sürümden önce açılır.
@Database(entities = [Intake::class], version = 1, exportSchema = false)
abstract class AbDb : RoomDatabase() {
    abstract fun intake(): IntakeDao
}
