package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigrationTest {

    private val testDbName = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun createVersion2Database_fromExportedSchema_createsExpectedTablesAndIdentity() {
        val db = helper.createDatabase(testDbName, 2)
        try {
            // Verify database version
            assertEquals(2, db.version)

            // Verify expected tables are present
            val tableNames = mutableListOf<String>()
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
            cursor.use {
                while (it.moveToNext()) {
                    tableNames.add(it.getString(0))
                }
            }
            assertTrue("Expected 'orders' table to be created", tableNames.contains("orders"))
            assertTrue("Expected 'catalog_products' table to be created", tableNames.contains("catalog_products"))

            // Verify room_master_table and identity hash
            val hashCursor = db.query("SELECT identity_hash FROM room_master_table WHERE id = 42")
            hashCursor.use {
                assertTrue("Expected room_master_table record", it.moveToFirst())
                assertEquals("6c97199cf6eb0233bde9d666881202eb", it.getString(0))
            }
        } finally {
            helper.closeWhenFinished(db)
        }
    }

    @Test
    fun unhandledMigration_withoutDestructiveFallback_throwsIllegalStateExceptionAndPreservesData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val missingMigrationDbName = "test-missing-migration.db"

        // Ensure clean state before test
        context.deleteDatabase(missingMigrationDbName)

        try {
            // 1. Create a synthetic version 1 SQLite database using raw SQLite, with a sample table and record
            val rawDb = context.openOrCreateDatabase(missingMigrationDbName, Context.MODE_PRIVATE, null)
            rawDb.execSQL("CREATE TABLE sample_legacy_table (id INTEGER PRIMARY KEY, title TEXT NOT NULL)")
            rawDb.execSQL("INSERT INTO sample_legacy_table (id, title) VALUES (1, 'retained_record')")
            rawDb.version = 1
            // 2. Close the raw SQLite handle before opening it through Room
            rawDb.close()

            // 3. Attempt to open it using AppDatabase version 2 configuration, without destructive fallback and without adding a migration
            val testRoomDb = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                missingMigrationDbName
            ).build()

            var caughtException: Throwable? = null
            try {
                testRoomDb.openHelper.writableDatabase
            } catch (e: Throwable) {
                caughtException = e
            } finally {
                testRoomDb.close()
            }

            // 4. Assert that Room throws IllegalStateException because migration 1 to 2 is missing
            assertNotNull("Room must throw an exception when a required migration path is missing", caughtException)
            assertTrue(
                "Exception should be or be caused by IllegalStateException",
                caughtException is IllegalStateException || caughtException?.cause is IllegalStateException
            )

            // 5. Reopen the temporary database using raw SQLite
            val verifyDb = context.openOrCreateDatabase(missingMigrationDbName, Context.MODE_PRIVATE, null)
            try {
                // 6. Verify user_version remains 1 and original table and record remain intact
                assertEquals(1, verifyDb.version)
                val cursor = verifyDb.rawQuery("SELECT title FROM sample_legacy_table WHERE id = 1", null)
                cursor.use {
                    assertTrue("Sample legacy record must still exist after failed migration attempt", it.moveToFirst())
                    assertEquals("retained_record", it.getString(0))
                }
            } finally {
                verifyDb.close()
            }
        } finally {
            // 7. Delete only test-missing-migration.db in a finally block
            context.deleteDatabase(missingMigrationDbName)
        }
    }
}
