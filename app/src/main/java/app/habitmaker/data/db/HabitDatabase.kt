package app.habitmaker.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RewardEntity::class, HabitEntity::class, HabitRecordEntity::class, RewardClaimEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)], // 2: reward.color
    exportSchema = true,
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun recordDao(): RecordDao
    abstract fun rewardDao(): RewardDao

    companion object {
        const val FILE_NAME = "habitmaker.sqlite"

        fun build(context: Context): HabitDatabase =
            Room.databaseBuilder(context, HabitDatabase::class.java, FILE_NAME)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}
