// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
}

plugins {
    id("com.android.application") version libs.versions.agp apply false
    id("com.android.library") version libs.versions.agp apply false
    id("org.jetbrains.kotlin.android") version libs.versions.kotlin apply false
    id("org.jmailen.kotlinter") version "3.15.0" apply false
    id("com.google.devtools.ksp") version libs.versions.kotlin.get() + "-1.0.16" apply false
    id("com.mikepenz.aboutlibraries.plugin") version libs.versions.aboutlibraries apply false
    kotlin("plugin.serialization") version libs.versions.kotlin apply false
}

allprojects {
    repositories {
        google()
        gradlePluginPortal()
        //jcenter()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://repo.repsy.io/mvn/chrynan/public")
    }
}