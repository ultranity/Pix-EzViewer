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
import java.util.Properties
import java.io.FileInputStream

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
    id("kotlin-parcelize")
    id("org.jmailen.kotlinter")
    id("com.mikepenz.aboutlibraries.plugin")
}
android {

    compileSdk = 33
    defaultConfig {
        applicationId = "com.perol.asdpl.play.pixivez"
        minSdk = 21
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 29
        versionCode = 115
        versionName = "1.8.5"
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
        create("bugly") {
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
        // Configures multiple APKs based on screen density.
        density {
            // Configures multiple APKs based on screen density.
            isEnable  = false
            // Specifies a list of screen densities Gradle should not create multiple APKs for.
            exclude("mdpi", "hdpi", "ldpi", "xhdpi", "xxxhdpi")
            // Specifies a list of compatible screen size settings for the manifest.
            //compatibleScreens "small","normal", "large", "xlarge"
        }
        // Configures multiple APKs based on ABI.
        abi {
            // Enables building multiple APKs per ABI.
            isEnable = true
            // By default all ABIs are included, so use reset() and include to specify
            reset()
            // Specifies a list of ABIs that Gradle should create APKs for.
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")

            // Specifies that we do not want to also generate a universal APK that includes all ABIs.
            isUniversalApk = true
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isZipAlignEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            //buildConfigField("Boolean", "ISGOOGLEPLAY", ISGOOGLEPLAY)
        }
        getByName("debug") {
            //applicationIdSuffix ".debug"
            isMinifyEnabled = false
            isZipAlignEnabled = true
            isShrinkResources = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("config")
            //packagingOptions {
            //    exclude "lib/*/libRSSupport.so"
            //    exclude "lib/*/librsjni.so"
            //}
        }
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        //dataBinding = true
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = false
    }
    namespace = "com.perol.asdpl.pixivez"

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
    implementation(fileTree(mapOf("include" to listOf("*.*"), "dir" to "libs")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${rootProject.extra["kotlin_version"]}")

    //implementation("androidx.core:core:1.3.1")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.vectordrawable:vectordrawable:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.webkit:webkit:1.7.0")
    //implementation("androidx.activity:activity:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    val navigation_version = "2.6.0"
    implementation("androidx.navigation:navigation-fragment-ktx:$navigation_version")
    implementation("androidx.navigation:navigation-ui-ktx:$navigation_version")
    //implementation("androidx.paging:paging-common-ktx:3.1.1")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.google.android.material:material:1.9.0")

    implementation("com.mikepenz:aboutlibraries-core:${rootProject.extra["aboutlibraries_version"]}")
    implementation("com.mikepenz:aboutlibraries:${rootProject.extra["aboutlibraries_version"]}")
    implementation("com.arialyy.aria:core:3.8.12")
    //kapt("com.arialyy.aria:compiler:3.8.12")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("com.waynejo:androidndkgif:0.3.3")

    implementation("com.hyman:flowlayout-lib:1.1.2")
    //implementation("com.youth.banner:banner:1.4.10")
    //implementation("io.github.youth5201314:banner:2.2.2")
    implementation("com.github.beksomega:loopinglayout:0.5.0")
    implementation("com.dinuscxj:circleprogressbar:1.3.0")
    //implementation("com.github.SheHuan:NiceImageView:1.0.5") // included in project
    implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")
    implementation("com.github.ybq:Android-SpinKit:1.4.0")

    //implementation("io.github.cymchad:BaseRecyclerViewAdapterHelper:4.0.0-beta14")
    implementation("io.github.cymchad:BaseRecyclerViewAdapterHelper:3.0.14")

    //implementation("androidx.annotation:annotation:1.5.0")
    //implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.20")

    val room_version = "2.5.2"
    run {
        implementation("androidx.room:room-runtime:$room_version")
        // For Kotlin use kapt/ksp instead of annotationProcessor
        ksp("androidx.room:room-compiler:$room_version")
        // optional - Kotlin Extensions and Coroutines support for Room
        implementation("androidx.room:room-ktx:$room_version")
        // optional - RxJava support for Room
        implementation("androidx.room:room-rxjava2:$room_version")
        // Test helpers
        testImplementation("androidx.room:room-testing:$room_version")
    }

    val lifecycle_version = "2.6.1"
    run {
        // ViewModel and LiveData
        //implementation("androidx.lifecycle:lifecycle-extensions:$lifecycle_version")
        // use -ktx for Kotlin
        // ViewModel
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
        // ViewModel utilities for Compose
        //implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version")
        // LiveData
        //implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
        // Lifecycles only (without ViewModel or LiveData)
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
        // Annotation processor
        //ksp "androidx.lifecycle:lifecycle-compiler:$lifecycle_version"
        // alternately - if using Java8, use the following instead of lifecycle-compiler
        implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycle_version")
        // optional - ReactiveStreams support for LiveData
        //implementation("androidx.lifecycle:lifecycle-reactivestreams-ktx:$lifecycle_version")
    }

    //val arch_version = "2.1.0"
    // optional - Test helpers for LiveData
    //testImplementation("androidx.arch.core:core-testing:$arch_version")

    implementation("io.reactivex.rxjava2:rxkotlin:2.4.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    
    // okhttp3系列组件版本最高到 4.4.1
    val okhttp3_version = "4.4.1"
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttp3_version")
    implementation("com.squareup.okhttp3:okhttp:$okhttp3_version")

    val glide_version = "4.15.1"
    run {
        implementation("com.github.bumptech.glide:glide:$glide_version")
        implementation("com.github.bumptech.glide:annotations:$glide_version")
        implementation("com.github.bumptech.glide:okhttp3-integration:$glide_version")
        //kapt("com.github.bumptech.glide:compiler:$glide_version")
        ksp("com.github.bumptech.glide:ksp:$glide_version")
    }

    implementation("jp.wasabeef:glide-transformations:4.3.0")

    val retrofit_version="2.9.0"
    run {
        implementation("com.squareup.retrofit2:retrofit:$retrofit_version")
        implementation("com.squareup.retrofit2:converter-gson:$retrofit_version")
        implementation("com.squareup.retrofit2:adapter-rxjava2:$retrofit_version")
    }

    // Material Dialogs: https://github.com/afollestad/material-dialogs
    val material_dialogs_version="3.3.0"
    run {
        implementation("com.afollestad.material-dialogs:core:$material_dialogs_version")
        implementation("com.afollestad.material-dialogs:files:$material_dialogs_version")
        implementation("com.afollestad.material-dialogs:bottomsheets:$material_dialogs_version")
        implementation("com.afollestad.material-dialogs:lifecycle:$material_dialogs_version")
        implementation("com.afollestad.material-dialogs:input:$material_dialogs_version")
        implementation("com.afollestad:drag-select-recyclerview:2.4.0")
    }

    implementation("org.greenrobot:eventbus:3.3.1")

    //implementation("com.esotericsoftware.kryo:kryo:2.24.0")
    implementation("com.tencent:mmkv-static:1.3.0")
    implementation("org.roaringbitmap:RoaringBitmap:0.9.45")

    //implementation("com.just.agentweb:agentweb:4.1.4")
    //implementation("net.gotev:cookie-store:1.5.0")
    //implementation("net.gotev:cookie-store-okhttp:1.5.0")

    implementation("com.daimajia.swipelayout:library:1.2.0@aar")

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

    //debugImplementation("io.github.knight-zxw:blockcanary:0.0.4")
    //debugImplementation("io.github.knight-zxw:blockcanary-ui:0.0.4")
    //debugImplementation("com.squareup.leakcanary:leakcanary-android:2.9.1")

    //debugImplementation("com.bytedance.tools.codelocator:codelocator-core:2.0.0")
    //debugImplementation("com.bytedance.tools.codelocator:codelocator-lancet-all:2.0.0")
}
