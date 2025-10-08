buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {

        classpath("com.google.firebase:firebase-auth:16.0.4")
        classpath("com.google.android.gms:play-services-gcm:16.0.0")
        classpath("com.google.gms:google-services:4.4.0")
        classpath("com.android.support:support-fragment:28.0.0")
        classpath ("com.squareup.okhttp3:okhttp:4.12.0")
        classpath("junit:junit:4.13.2")
    }

}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.12.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false

}
