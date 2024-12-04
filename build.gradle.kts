// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
}

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlinter) apply false
    alias(libs.plugins.aboutlibraries) apply false
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