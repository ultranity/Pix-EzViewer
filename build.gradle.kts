// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    val kotlin_version by extra("1.8.22")
    val agp_version by extra("8.0.2")
    val aboutlibraries_version by extra("10.8.2")
}

plugins {
    id("com.android.application") version "${extra["agp_version"]}" apply false
    id("com.android.library") version "${extra["agp_version"]}" apply false
    id("org.jetbrains.kotlin.android") version "${extra["kotlin_version"]}" apply false
    id("org.jmailen.kotlinter") version "3.15.0" apply false
    id("com.google.devtools.ksp") version "${extra["kotlin_version"]}-1.0.11" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "${extra["aboutlibraries_version"]}" apply false
}

allprojects {
    repositories {
        google()
        gradlePluginPortal()
        //jcenter()
        mavenCentral()
        maven("https://jitpack.io")
    }
}