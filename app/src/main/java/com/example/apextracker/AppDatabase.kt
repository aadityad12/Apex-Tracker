package com.example.apextracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// MIGRATION POLICY (Issue #17): fallbackToDestructiveMigration() below DROPS EVERY
// TABLE on a version mismatch. Signed-in users get their data re-pulled from
// Firestore by the cold-start initial sync, but signed-out users lose everything.
// Any future version bump MUST ship a real Migration object. Schema JSONs are
// exported to app/schemas/ (checked in) so migrations can be written and tested.
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
    }
}

// Nullable REAL: existing categories have no cap, and null is the "no cap" encoding
// (see Category.monthlyLimit), so no DEFAULT is wanted here.
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN monthlyLimit REAL")
    }
}

// Issue #78: study_sessions gains a per-subject dimension, which changes the primary key
// from (date) to (date, subject). SQLite can't ALTER a primary key in place, so this is the
// standard create-new / copy / drop / rename dance. Every pre-existing daily total is copied
// into the empty-string ("No subject") bucket for its date, so no study data is lost — the old
// aggregate simply becomes that day's uncategorized row. No SQL DEFAULT on `subject`: the entity
// declares only a Kotlin default (= ""), which Room does not emit as a column default, so adding
// one here would make TableInfo mismatch at runtime. The INSERT supplies '' explicitly instead.
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `study_sessions_new` (`date` TEXT NOT NULL, `subject` TEXT NOT NULL, " +
                "`durationSeconds` INTEGER NOT NULL, PRIMARY KEY(`date`, `subject`))"
        )
        db.execSQL(
            "INSERT INTO `study_sessions_new` (`date`, `subject`, `durationSeconds`) " +
                "SELECT `date`, '', `durationSeconds` FROM `study_sessions`"
        )
        db.execSQL("DROP TABLE `study_sessions`")
        db.execSQL("ALTER TABLE `study_sessions_new` RENAME TO `study_sessions`")
    }
}

// Issue #45-follow-up (Dashboard): two purely additive tables — `goals` (habit-style daily goals
// feeding the contribution heatmap) and `goal_completions` (per-day manual check-offs). No data
// copy, so this mirrors MIGRATION_11_12/12_13 rather than the PK-change dance in MIGRATION_13_14.
// The exact DDL matches Room's exported app/schemas/…/15.json (create-me-from-there on any change).
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `type` TEXT NOT NULL, `metric` TEXT, `comparator` TEXT, " +
                "`threshold` REAL, `subject` TEXT, `startDate` TEXT NOT NULL, `archivedDate` TEXT, " +
                "`sortOrder` INTEGER NOT NULL, `cloudId` TEXT NOT NULL, `modifiedAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `goal_completions` (`goalCloudId` TEXT NOT NULL, " +
                "`date` TEXT NOT NULL, `done` INTEGER NOT NULL, `modifiedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`goalCloudId`, `date`))"
        )
    }
}

// Issue #126: reminders gain a priority. Purely additive; the NOT NULL DEFAULT matches the
// entity's Kotlin default so existing rows read back as NORMAL.
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reminders ADD COLUMN priority TEXT NOT NULL DEFAULT 'NORMAL'")
    }
}

// Issue #79: subscriptions can be paused. Additive; existing rows default to not paused.
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE subscriptions ADD COLUMN isPaused INTEGER NOT NULL DEFAULT 0")
    }
}

// Issue #124: per-app daily screen-time limits. New table, additive.
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `app_usage_limits` (`packageName` TEXT NOT NULL, " +
                "`dailyLimitMinutes` INTEGER NOT NULL, `lastNotifiedDate` TEXT, " +
                "PRIMARY KEY(`packageName`))"
        )
    }
}

// Issue #127: notes gain image attachments (a newline-separated filename list). Additive.
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN attachments TEXT NOT NULL DEFAULT ''")
    }
}

// Papers feature (Plan.md Phase 1): the reading log. New table, additive — mirrors
// MIGRATION_14_15. DDL diffed against Room's exported app/schemas/…/20.json.
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `papers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`s2Id` TEXT NOT NULL, `title` TEXT NOT NULL, `authors` TEXT NOT NULL, " +
                "`year` INTEGER, `venue` TEXT NOT NULL, `abstractText` TEXT NOT NULL, " +
                "`tldr` TEXT NOT NULL, `url` TEXT NOT NULL, `pdfUrl` TEXT NOT NULL, " +
                "`source` TEXT NOT NULL, `status` TEXT NOT NULL, `addedDate` TEXT NOT NULL, " +
                "`readDate` TEXT, `memo` TEXT NOT NULL, `signal` INTEGER, " +
                "`cloudId` TEXT NOT NULL, `modifiedAt` INTEGER NOT NULL)"
        )
    }
}

// Issue #166: goals can be evaluated daily or weekly. Existing rows keep the exact historical
// behavior through the DAILY default; new weekly goals opt in explicitly.
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN cadence TEXT NOT NULL DEFAULT 'DAILY'")
    }
}

// Papers discovery redesign: keyword-scoped topics replace the bare-field rotation. New table,
// additive — mirrors MIGRATION_19_20. topicCloudId is nullable-free ('' sentinel, matching every
// other cross-device reference column in this schema) so old papers just read as "not
// topic-sourced" with no backfill needed. DDL diffed against app/schemas/…/22.json.
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `paper_topics` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`field` TEXT NOT NULL, `keyword` TEXT NOT NULL, `pausedAt` TEXT, " +
                "`createdDate` TEXT NOT NULL, `lastCheckedDate` TEXT, `readCount` INTEGER NOT NULL, " +
                "`abandonedCount` INTEGER NOT NULL, `ratingSum` INTEGER NOT NULL, " +
                "`ratingCount` INTEGER NOT NULL, `consecutiveAbandons` INTEGER NOT NULL, " +
                "`cloudId` TEXT NOT NULL, `modifiedAt` INTEGER NOT NULL)"
        )
        db.execSQL("ALTER TABLE papers ADD COLUMN topicCloudId TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * Issue #197: indices on the sync join keys. There were none anywhere in the schema.
 *
 * `cloudId` is what every `applyXDoc` looks a row up by, once per document — from
 * `performInitialSync` and again from each of SyncCoordinator's live listeners for the life of
 * the session. Unindexed, each of those was a full table scan, so the cost of syncing grew with
 * the square of the user's history. `papers.s2Id` and `papers.url` are the same story for the
 * daily discovery fetch, which probes them once per candidate.
 *
 * **Not unique**, deliberately. `""` is the not-yet-assigned sentinel for `cloudId` throughout
 * this schema (rows get a UUID on first push), and `papers.s2Id` is `""` for every offline seed —
 * so multiple rows legitimately share those values and a unique index would reject them. That
 * leaves the check-then-insert dedup in the ViewModels advisory rather than enforced; making it
 * enforced means moving the sentinel to NULL across every entity and every `.ifEmpty { UUID… }`
 * call site, which is a separate change.
 *
 * Index names and DDL must match Room's own generated form exactly or TableInfo validation fails
 * at runtime — diffed against the exported app/schemas/…/23.json.
 */
val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_items_cloudId` ON `budget_items` (`cloudId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_cloudId` ON `categories` (`cloudId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_cloudId` ON `subscriptions` (`cloudId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_cloudId` ON `notes` (`cloudId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_cloudId` ON `reminders` (`cloudId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_cloudId` ON `goals` (`cloudId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_papers_cloudId` ON `papers` (`cloudId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_papers_s2Id` ON `papers` (`s2Id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_papers_url` ON `papers` (`url`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_paper_topics_cloudId` ON `paper_topics` (`cloudId`)")
    }
}

// Issue #218: Budget items can be income or expense. Existing rows keep their exact historical
// meaning through the EXPENSE default — every total computed pre-#218 stays unchanged.
val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE budget_items ADD COLUMN type TEXT NOT NULL DEFAULT 'EXPENSE'")
    }
}

@Database(entities = [BudgetItem::class, Category::class, Subscription::class, StudySession::class, ScreenTimeSession::class, ExcludedApp::class, Reminder::class, Note::class, Goal::class, GoalCompletion::class, AppUsageLimit::class, Paper::class, PaperTopic::class], version = 24, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun screenTimeSessionDao(): ScreenTimeSessionDao
    abstract fun excludedAppDao(): ExcludedAppDao
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao
    abstract fun goalDao(): GoalDao
    abstract fun goalCompletionDao(): GoalCompletionDao
    abstract fun appUsageLimitDao(): AppUsageLimitDao
    abstract fun paperDao(): PaperDao
    abstract fun paperTopicDao(): PaperTopicDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        const val DB_NAME = "budget_database"

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val builder = Room.databaseBuilder(appContext, AppDatabase::class.java, DB_NAME)
                    .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24)
                    .fallbackToDestructiveMigration()
                // SQLCipher (Issue #117). Returns null only when encryption genuinely can't be
                // used, in which case Room's stock helper opens the file exactly as it did before
                // — never a path that discards readable data. Note this runs before Room opens the
                // file, which is the only point at which an existing plaintext database can be
                // converted. See DatabaseEncryption.kt.
                databaseOpenHelperFactory(appContext, DB_NAME)?.let { builder.openHelperFactory(it) }
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}
