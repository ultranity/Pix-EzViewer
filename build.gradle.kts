// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    val kotlinVersion by extra("1.8.22")
    val agpVersion by extra("8.0.2")
    val aboutlibrariesVersion by extra("10.8.2")
}

plugins {
    id("com.android.application") version "${extra["agpVersion"]}" apply false
    id("com.android.library") version "${extra["agpVersion"]}" apply false
    id("org.jetbrains.kotlin.android") version "${extra["kotlinVersion"]}" apply false
    id("org.jmailen.kotlinter") version "3.15.0" apply false
    id("com.google.devtools.ksp") version "${extra["kotlinVersion"]}-1.0.11" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "${extra["aboutlibrariesVersion"]}" apply false
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