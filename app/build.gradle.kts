/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
 * Copyright (c) 2019 Perol_Notsfsssf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

import java.io.FileInputStream
import java.util.Properties

// Create a variable called keystorePropertiesFile, and initialize it to your
// keystore.properties file, in the rootProject folder.
val keystorePropertiesFile = rootProject.file("keystore.properties")
// Initialize a new Properties() object called keystoreProperties.
val keystoreProperties = Properties()
// Load your keystore.properties file into the keystoreProperties object.
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    //id("org.jetbrains.kotlin.kapt")
    id("com.google.devtools.ksp")
    //id("kotlin-parcelize")
    kotlin("plugin.serialization")
    id("org.jmailen.kotlinter")
    id("com.mikepenz.aboutlibraries.plugin")
}
android {
    namespace = "com.perol.asdpl.pixivez"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.perol.asdpl.play.pixivez"
        minSdk = 21
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 29
        versionCode = 211
        versionName = "2.1.1"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        //vectorDrawables.useSupportLibrary = true
        ksp {
            arg("room.schemaLocation"  , "$projectDir/schemas")
            arg("room.incremental"     , "true")
            arg("room.expandProjection", "true")
        }
    }

    signingConfigs{
        create("config") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }
    flavorDimensions += "version"
    productFlavors{
        create("git") {
            dimension = "version"
            signingConfig = signingConfigs.getByName("config")
        }
        create("play") {
            dimension = "version"
            signingConfig = signingConfigs.getByName("config")
        }
        create("libre") {
            dimension = "version"
            applicationIdSuffix = ".libre"
            //versionNameSuffix = "-libre"
        }
    }
    splits {
        // Configures multiple APKs based on ABI.
        abi {
            // Enables building multiple APKs per ABI.
            isEnable = true
            // By default all ABIs are included, so use reset() and include to specify
            reset()
            // Specifies a list of ABIs that Gradle should create APKs for.
            include("x86_64", "arm64-v8a")

            // Specifies that we do not want to also generate a universal APK that includes all ABIs.
            isUniversalApk = true
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            //isZipAlignEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            //buildConfigField("Boolean", "ISGOOGLEPLAY", ISGOOGLEPLAY)
            setProperty("archivesBaseName", "PixEzViewer-" + defaultConfig.versionName)
        }
        getByName("debug") {
            //applicationIdSuffix ".debug"
            isMinifyEnabled = false
            //isZipAlignEnabled = true
            isShrinkResources = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("config")
            //packagingOptions {
            //    exclude "lib/*/libRSSupport.so"
            //    exclude "lib/*/librsjni.so"
            //}
        }
    }
    buildFeatures {
        //dataBinding = true
        viewBinding = true
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = false
    }

    /*    configurations.all {
            resolutionStrategy.force "com.google.code.findbugs:jsr305:1.3.9"
        }*/

    aboutLibraries {
        // - If the automatic registered android tasks are disabled, a similar thing can be achieved manually
        // - `./gradlew app:exportLibraryDefinitions -PaboutLibraries.exportPath=src/main/res/raw`
        // - the resulting file can for example be added as part of the SCM
        registerAndroidTasks = false
        // Enable pretty printing for the generated JSON file
        prettyPrint = false
    }
}


dependencies {
    implementation(project(":ketch"))
    implementation(project(":BRVAH"))
    implementation(fileTree(mapOf("include" to listOf("*.*"), "dir" to "libs")))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.parcelable)

    //implementation(libs.androidx.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.appcompat)
    //implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation(libs.androidx.vectordrawable)
    //implementation("androidx.activity:activity:1.2.0")
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.recyclerview)
    //implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.webkit)
    //val navigation = "2.6.0"
    //implementation("androidx.navigation:navigation-fragment-ktx:$navigation")
    //implementation("androidx.navigation:navigation-ui-ktx:$navigation")
    //implementation("androidx.paging:paging-common-ktx:3.1.1")
    implementation(libs.flexbox)
    implementation(libs.material)

    /*/ (Java only)
    //implementation(libs.androidx.work.runtime)
    // Kotlin + coroutines
    implementation(libs.androidx.work.runtime.ktx)
    // optional - RxJava2 support
    implementation(libs.androidx.work.rxjava2)
    // optional - GCMNetworkManager support
    //implementation("androidx.work:work-gcm:$work_")
    // optional - Test helpers
    androidTestImplementation(libs.androidx.work.testing)
    // optional - Multiprocess support
    implementation(libs.androidx.work.multiprocess)*/

    //implementation("androidx.annotation:annotation:1.5.0")
    //implementation("org.jetbrains.kotlin:kotlin-reflect:${rootProject.extra["kotlin"]}")

    implementation(libs.room.runtime)
    // For Kotlin use kapt/ksp instead of annotationProcessor
    ksp(libs.room.compiler)
    // optional - Kotlin Extensions and Coroutines support for Room
    implementation(libs.room.ktx)
    // optional - RxJava support for Room
    // implementation("androidx.room:room-rxjava2:$room")
    // Test helpers
    testImplementation(libs.room.testing)

    // ViewModel and LiveData
    //implementation("androidx.lifecycle:lifecycle-extensions:$lifecycle")
    // use -ktx for Kotlin
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // ViewModel utilities for Compose
    //implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle")
    // LiveData
    implementation(libs.androidx.lifecycle.livedata.ktx)
    // Lifecycles only (without ViewModel or LiveData)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Annotation processor
    //ksp "androidx.lifecycle:lifecycle-compiler:$lifecycle"
    // alternately - if using Java8, use the following instead of lifecycle-compiler
    implementation(libs.androidx.lifecycle.common.java8)
    // optional - ReactiveStreams support for LiveData
    //implementation("androidx.lifecycle:lifecycle-reactivestreams-ktx:$lifecycle")
    // optional - ProcessLifecycleOwner provides a lifecycle for the whole application process
    //implementation(libs.androidx.lifecycle.process)

    //val arch = "2.1.0"
    // optional - Test helpers for LiveData
    //testImplementation("androidx.arch.core:core-testing:$arch")

    implementation(libs.aboutlibraries)
    implementation(libs.aboutlibraries.core)
    implementation(libs.aria.core)
    //kapt("com.arialyy.aria:compiler:3.8.12")
    implementation(libs.markwon.core)
    implementation(libs.jsoup)
    implementation(libs.zip4j)
    implementation(libs.androidndkgif)

    implementation(libs.flowlayout.lib)
    //implementation("com.youth.banner:banner:1.4.10")
    //implementation("io.github.youth5201314:banner:2.2.2")
    //implementation(libs.loopinglayout) replace with RepeatLayoutManager

    //implementation("com.dinuscxj:circleprogressbar:1.3.0") //use CircularProgressIndicator
    //implementation("com.github.SheHuan:NiceImageView:1.0.5") // included in project
    implementation(libs.subsampling.scale.image.view.androidx)
    implementation(libs.colorPicker)
    //implementation("com.github.ybq:Android-SpinKit:1.4.0")

    //implementation("io.github.cymchad:BaseRecyclerViewAdapterHelper:4.0.0-beta14")
    //implementation(libs.brvah) //included
    //implementation(libs.brv)
    implementation(libs.fastadapter)
    implementation(libs.fastadapter.extensions.binding)// view binding helpers
    implementation(libs.fastadapter.extensions.diff)// diff util helpers
    implementation(libs.fastadapter.extensions.drag)// drag support
    implementation(libs.fastadapter.extensions.paged)// paging support
    implementation(libs.fastadapter.extensions.scroll)// scroll helpers
    implementation(libs.fastadapter.extensions.swipe)// swipe support
    //implementation(libs.fastadapter.extensions.ui)// pre-defined ui components
    implementation(libs.fastadapter.extensions.utils)// needs the `expandable`, `drag` and `scroll` extension.

    /*val ViewBindingKTX = "2.1.0"
    run{
    implementation("com.github.DylanCaiCoding.ViewBindingKTX:viewbinding-ktx:$ViewBindingKTX")
    implementation("com.github.DylanCaiCoding.ViewBindingKTX:viewbinding-nonreflection-ktx:$ViewBindingKTX")
    implementation("com.github.DylanCaiCoding.ViewBindingKTX:viewbinding-base:$ViewBindingKTX")
    implementation("com.github.DylanCaiCoding.ViewBindingKTX:viewbinding-brvah:$ViewBindingKTX")
    }*/

    //implementation(libs.rxkotlin)
    //implementation(libs.rxjava)
    //implementation(libs.rxandroid)

    // okhttp3系列组件版本最高到 4.4.1
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.okhttp.dnsoverhttps)

    implementation(libs.glide)
    implementation(libs.glide.annotations)
    implementation(libs.glide.okhttp3.integration)
    //kapt("com.github.bumptech.glide:compiler:$glide")
    ksp(libs.glide.ksp)

    implementation(libs.glide.transformations)
    implementation(libs.gpuimage)
    implementation(libs.retrofit)
    //implementation(libs.retrofit.converter.gson)
    //implementation(libs.retrofit.kotlinx.serialization.converter) included for code  debug
    //implementation(libs.retrofit.adapter.rxjava2)


    // Material Dialogs: https://github.com/afollestad/material-dialogs
    implementation(libs.material.dialogs.core)
    implementation(libs.material.dialogs.files)
    implementation(libs.material.dialogs.bottomsheets)
    implementation(libs.material.dialogs.lifecycle)
    implementation(libs.material.dialogs.input)
    implementation(libs.drag.select.recyclerview)

    //implementation("org.greenrobot:eventbus:3.3.1") replace with kotlin flow

    //implementation("com.esotericsoftware.kryo:kryo:2.24.0")
    //implementation(libs.mmkv.static)
    implementation(libs.fastkv.java)
    implementation(libs.roaringBitmap)

    //implementation("com.just.agentweb:agentweb:4.1.4")
    //implementation("net.gotev:cookie-store:1.5.0")
    //implementation("net.gotev:cookie-store-okhttp:1.5.0")

    //TODO: check implementation("com.daimajia.swipelayout:library:1.2.0@aar")

    //implementation("com.robertlevonyan.view:CustomFloatingActionButton:3.1.5")
    //implementation("com.github.clans:fab:1.6.4")
    //implementation("com.github.mcxtzhang:SwipeDelMenuLayout:V1.3.0")
    //implementation("com.github.qingyc:GuideTipView:0.6")

    // 吐司框架：https://github.com/getActivity/Toaster
    //implementation("com.github.getActivity:Toaster:12.3")

    //implementation("com.github.sevar83:indeterminate-checkbox:1.0.5@aar")
    //implementation("com.github.SherlockGougou:BigImageViewPager:androidx-6.0.2")
    //implementation("me.jessyan:progressmanager:1.5.0")

    //buglyImplementation("com.tencent.bugly:crashreport:4.0.4")
    //"buglyImplementation"("com.tencent.bugly:crashreport_upgrade:1.6.1")
    //"buglyImplementation"("com.github.supersu-man:apkupdater-library:v2.0.0")

    debugImplementation(libs.blockcanary)
    debugImplementation(libs.blockcanary.ui)
    //debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")

    //debugImplementation("com.bytedance.tools.codelocator:codelocator-core:2.0.0")
    //debugImplementation("com.bytedance.tools.codelocator:codelocator-lancet-all:2.0.0")
}
