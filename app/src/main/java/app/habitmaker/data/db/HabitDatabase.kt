package app.habitmaker.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.DeleteColumn
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities = [RewardEntity::class, HabitEntity::class, HabitRecordEntity::class, RewardClaimEntity::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // 2: reward.color
        AutoMigration(from = 2, to = 3, spec = HabitDatabase.DropExceptions::class), // 3: no exceptions
    ],
    exportSchema = true,
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun recordDao(): RecordDao
    abstract fun rewardDao(): RewardDao

    @DeleteColumn.Entries(
        DeleteColumn(tableName = "habit", columnName = "except_dow"),
        DeleteColumn(tableName = "habit", columnName = "except_dom"),
        DeleteColumn(tableName = "habit", columnName = "except_dates"),
    )
    class DropExceptions : AutoMigrationSpec

    companion object {
        const val FILE_NAME = "habitmaker.sqlite"

        fun build(context: Context): HabitDatabase =
            Room.databaseBuilder(context, HabitDatabase::class.java, FILE_NAME)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}
