package com.storagepulse.cleaner.notify

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.storagepulse.cleaner.ui.widgets.WidgetRefreshWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real workers once on the device (read-only: they scan and post a notification,
 * never move or delete files).
 */
@RunWith(AndroidJUnit4::class)
class WeeklyClutterWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun weeklyWorker_scansAndSucceeds() = runBlocking<Unit> {
        val worker = TestListenableWorkerBuilder<WeeklyClutterWorker>(context).build()
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }

    @Test
    fun widgetRefresh_succeeds() = runBlocking<Unit> {
        val worker = TestListenableWorkerBuilder<WidgetRefreshWorker>(context).build()
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }
}
