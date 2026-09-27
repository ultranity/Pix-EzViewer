package com.perol.asdpl.pixivez.networks

import android.content.Intent
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.services.PixivApiService
import com.perol.asdpl.pixivez.ui.novel.NovelActivity
import com.perol.asdpl.pixivez.ui.novel.chunkNovel
import com.perol.asdpl.pixivez.ui.novel.parseWebNovel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt in with -e liveNetwork true on a signed-in device. Never logs tokens or content. */
@RunWith(AndroidJUnit4::class)
class PixivConnectivityInstrumentedTest {
    @Test fun readsHomepageAndNovelThroughProductionClients(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveNetwork") == "true")
        assertNotNull("Sign in before running live checks", AppDataRepo.getUser())
        assertEquals(SniMode.ECH, SniMode.current())
        RefreshToken.getInstance().refreshToken(AppDataRepo.currentUser.Refresh_token)
        // Uses the real auth-refresh interceptor, selected transport, Retrofit and parsers.
        val api = RestClient.retrofitAppApi.create(PixivApiService::class.java)
        val first = api.getIllustRecommend()
        assertTrue(first.illusts.isNotEmpty())
        val refresh = api.getIllustRecommend()
        assertTrue(refresh.illusts.isNotEmpty())
        val novels = api.getNovelRecommend().novels
        assertTrue(novels.isNotEmpty())
        val id = novels.first().id
        assertEquals(id, api.getNovelDetail(id).novel.id)
        val parsed = api.getNovelText(id).use { parseWebNovel(it.string()) }
        assertNotNull("Real novel HTML must parse on Android ICU", parsed)
        assertTrue(parsed!!.text.isNotBlank())
        assertTrue(chunkNovel(parsed.text).isNotEmpty())
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(instrumentation.targetContext, NovelActivity::class.java)
            .putExtra("novel_id", id)
        ActivityScenario.launch<NovelActivity>(intent).use { scenario ->
            var rows = 0
            val deadline = System.nanoTime() + 15_000_000_000L
            while (rows <= 1 && System.nanoTime() < deadline) {
                scenario.onActivity { activity ->
                    rows = activity.findViewById<RecyclerView>(R.id.novel_recycler).adapter?.itemCount ?: 0
                }
                if (rows <= 1) Thread.sleep(100)
            }
            assertTrue("Reader must render a header and real body chunks without crashing", rows > 1)
            instrumentation.waitForIdleSync()
            if (InstrumentationRegistry.getArguments().getString("captureReader") == "true") {
                instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                    java.io.File(instrumentation.targetContext.cacheDir, "novel-validation.png").outputStream().use {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                }
            }
        }
        Log.i("PixEzConnectivity", "ECH production: home=${first.illusts.size}, refresh=${refresh.illusts.size}, novels=${novels.size}; novel detail/text parsed")
    }
}
