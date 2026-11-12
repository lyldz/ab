package com.lyldz.ab.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "intake", indices = [Index("atMs")])
data class Intake(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val atMs: Long,
    val ml: Int,
    @ColumnInfo(defaultValue = "0") val source: Int = SOURCE_APP,
)

const val SOURCE_APP = 0
const val SOURCE_NOTIFICATION = 1
const val SOURCE_WIDGET = 2

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE intake ADD COLUMN source INTEGER NOT NULL DEFAULT 0")
    }
}

@Dao
interface IntakeDao {
    @Insert
    suspend fun insert(i: Intake): Long

    @Query("UPDATE intake SET ml = :ml, atMs = :atMs WHERE id = :id")
    suspend fun update(id: Long, ml: Int, atMs: Long)

    @Query("DELETE FROM intake WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM intake WHERE atMs >= :from AND atMs < :to ORDER BY atMs DESC")
    fun observeRange(from: Long, to: Long): Flow<List<Intake>>

    /** Geri al için: en son eklenen kayıt (geçmişe eklenen de olabilir, bu yüzden id sırası). */
    @Query("SELECT id FROM intake ORDER BY id DESC LIMIT 1")
    suspend fun lastId(): Long?
}

// shortcut: şema dışa aktarımı yok (exportSchema=false), uygulama yayınlanmadı; ilk sürümden önce açılır.
@Database(entities = [Intake::class], version = 2, exportSchema = false)
abstract class AbDb : RoomDatabase() {
    abstract fun intake(): IntakeDao
}
