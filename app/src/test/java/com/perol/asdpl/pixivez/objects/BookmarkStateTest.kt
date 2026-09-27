package com.perol.asdpl.pixivez.objects

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.MutableLiveData
import com.perol.asdpl.pixivez.data.model.*
import com.perol.asdpl.pixivez.objects.IllustCacheRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkStateTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val id = -128
    @Before fun setup() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread() = true
        })
        Dispatchers.setMain(dispatcher); IllustCacheRepo.clear()
    }
    @After fun cleanup() { IllustCacheRepo.clear(); Dispatchers.resetMain(); ArchTaskExecutor.getInstance().setDelegate(null) }
    private fun confirm(item: IllustX, value: Boolean) {
        // Same entry point as the successful InteractionUtil callback.
        IllustCacheRepo.confirmBookmark(item, value, IllustCacheRepo.beginBookmarkMutation())
    }
    private fun fixture() = IllustX(
        id = id, title = "probe", type = "illust", caption = "", restrict = 0,
        user = UserX(-128, "probe", "probe", ProfileImageUrls("")), tags = emptyList(),
        tools = emptyList(), create_date = "", page_count = 1, width = 1, height = 1,
        sanity_level = 2, x_restrict = 0, series = null, meta = emptyList(),
        total_view = 0, total_bookmarks = 0, visible = true, is_muted = false,
        illust_ai_type = 0, illust_book_style = 0
    )
    private fun response(item: IllustX): String = """{
      "id":-128,"title":"probe","type":"illust","caption":"","restrict":0,
      "user":{"id":-128,"name":"probe","account":"probe","profile_image_urls":{"medium":""}},
      "tags":[],"tools":[],"create_date":"","page_count":1,"width":1,"height":1,
      "sanity_level":2,"x_restrict":0,"series":null,"total_view":0,"total_bookmarks":0,
      "visible":true,"is_muted":false,"illust_ai_type":0,"illust_book_style":0,
      "is_bookmarked":${item.is_bookmarked},"meta_pages":[],
      "meta_single_page":{"original_image_url":""},
      "image_urls":{"large":"","medium":"","square_medium":""}
    }"""
    @Test fun sharedInstanceNotifiesBothScreens() {
        val current = IllustCacheRepo.update(id, fixture())
        val a = MutableLiveData(false); val b = MutableLiveData(false)
        current.addBinder("A", a); current.addBinder("B", b)
        confirm(current, true)
        assertEquals(true, a.value); assertEquals(true, b.value)
    }
    @Test fun staleObjectCannotOverwriteSuccessfulBookmarkAndBothScreens() {
        val current = IllustCacheRepo.update(id, fixture())
        val stale = fixture()
        val a = MutableLiveData(false); val b = MutableLiveData(false)
        current.addBinder("A", a); current.addBinder("B", b)
        confirm(current, true)
        assertEquals(true, a.value)
        val resolved = IllustCacheRepo.update(id, stale)
        assertSame(current, resolved)
        assertTrue(current.is_bookmarked)
        assertEquals(true, a.value); assertEquals(true, b.value)
    }
    @Test fun delayedSerializedSnapshotCannotOverwriteSuccessfulBookmark() {
        val current = IllustCacheRepo.update(id, fixture())
        val staleResponse = response(fixture())
        confirm(current, true)
        val resolved = Json.decodeFromString(MergeMetaIllustSerializer, staleResponse)
        assertSame(current, resolved)
        assertTrue(current.is_bookmarked)
    }
    @Test fun staleTrueSnapshotCannotResurrectSuccessfulUnbookmark() {
        val current = IllustCacheRepo.update(id, fixture())
        confirm(current, true)
        val staleResponse = response(current)
        confirm(current, false)
        val resolved = Json.decodeFromString(MergeMetaIllustSerializer, staleResponse)
        assertSame(current, resolved)
        assertFalse(current.is_bookmarked)
    }
    @Test fun mutationOnDetachedInstanceUpdatesCanonicalAndBinder() {
        val canonical = IllustCacheRepo.update(id, fixture())
        val binder = MutableLiveData(false)
        canonical.addBinder("screen", binder)
        val detached = fixture()
        confirm(detached, true)
        assertTrue(canonical.is_bookmarked)
        assertTrue(detached.is_bookmarked)
        assertEquals(true, binder.value)
    }

    @Test fun staleSuccessCannotOverrideNewerSuccessfulAction() {
        val item = IllustCacheRepo.update(id, fixture())
        val first = IllustCacheRepo.beginBookmarkMutation()
        val second = IllustCacheRepo.beginBookmarkMutation()
        assertTrue(IllustCacheRepo.confirmBookmark(item, false, second))
        assertFalse(IllustCacheRepo.confirmBookmark(item, true, first))
        assertFalse(item.is_bookmarked)
    }

    @Test fun failedActionDoesNotProtectUnconfirmedState() {
        val item = IllustCacheRepo.update(id, fixture())
        IllustCacheRepo.beginBookmarkMutation() // no successful callback
        val remote = fixture().apply { is_bookmarked = true }
        IllustCacheRepo.update(id, remote)
        assertTrue(item.is_bookmarked)
    }

    @Test fun accountSwitchClearsStateAndRejectsLateCallback() {
        IllustCacheRepo.activateAccount(1)
        val item = IllustCacheRepo.update(id, fixture())
        confirm(item, true)
        val pending = IllustCacheRepo.beginBookmarkMutation()
        IllustCacheRepo.activateAccount(2)
        val otherAccount = IllustCacheRepo.update(id, fixture())
        assertNotSame(item, otherAccount)
        assertEquals(listOf(otherAccount), IllustCacheRepo.getAll())
        assertFalse(otherAccount.is_bookmarked)
        assertFalse(IllustCacheRepo.confirmBookmark(item, true, pending))
        assertFalse(otherAccount.is_bookmarked)
    }

    @Test fun sameAccountTokenRefreshDoesNotLoseConfirmedState() {
        IllustCacheRepo.activateAccount(1)
        val item = IllustCacheRepo.update(id, fixture())
        confirm(item, true)
        IllustCacheRepo.activateAccount(1)
        assertTrue(IllustCacheRepo.update(id, fixture()).is_bookmarked)
    }

    @Test fun queuedNotificationsDeliverLatestStateInsteadOfOldCapturedValues() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        val item = IllustCacheRepo.update(id, fixture())
        val events = mutableListOf<Boolean>()
        val binder = MutableLiveData(false)
        val observer = androidx.lifecycle.Observer<Boolean> { events.add(it) }
        binder.observeForever(observer)
        item.addBinder("screen", binder)
        item.is_bookmarked = true
        item.is_bookmarked = false
        scheduler.runCurrent()
        assertFalse(events.contains(true))
        binder.removeObserver(observer)
    }

}
