package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.Habit
import com.example.data.StreakMilestoneDao
import com.example.data.StreakMilestoneService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase
  private lateinit var dao: StreakMilestoneDao
  private lateinit var service: StreakMilestoneService

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    dao = db.streakMilestoneDao()
    service = StreakMilestoneService(dao)
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Habit Tracker", appName)
  }

  @Test
  fun `seed milestones and evaluate streaks`() = runBlocking {
    service.seedMilestonesIfEmpty()
    val milestones = dao.getAllMilestones().first()
    assertEquals(4, milestones.size)

    // Initially none achieved
    assertTrue(milestones.all { !it.isAchieved })

    // Build habit list with best streak 5
    val habits = listOf(
      Habit(name = "Drink Water", bestStreak = 5)
    )

    service.checkAndUpdateMilestones(habits)

    val updatedMilestones = dao.getAllMilestones().first()
    
    // starter_3 should be unlocked (since bestStreak 5 >= 3)
    val starter = updatedMilestones.first { it.milestoneId == "starter_3" }
    assertTrue(starter.isAchieved)

    // warrior_7 should still be locked
    val warrior = updatedMilestones.first { it.milestoneId == "warrior_7" }
    assertTrue(!warrior.isAchieved)
  }
}
