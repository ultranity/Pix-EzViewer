package com.perol.asdpl.pixivez

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
 @Test
 @Throws(Exception::class)
 fun useAppContext() {
 // Context of the app under test.
 val appContext = InstrumentationRegistry.getTargetContext()
 runBlocking {
 val result = AppDataRepository.getAllUser()
 println("x:${result.size}")
 }

 }

 @Test
 fun testRetrofit() {
 val appContext = InstrumentationRegistry.getTargetContext()
 Glide.with(appContext)
 .load("https://i.pximg.net/user-profile/img/2018/06/11/22/00/29/14348260_c1f2b130248005062b7c6c358812160a_170.jpg")
 }
}
 */
