package com.github.droibit.wear.authentication.sample

import android.app.Application
import com.facebook.stetho.Stetho
import com.twitter.sdk.android.core.Twitter
import timber.log.Timber
import timber.log.Timber.DebugTree

class SampleApplication : Application() {

  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) {
      Timber.plant(DebugTree())
    }
    Stetho.initializeWithDefaults(this)
    Twitter.initialize(this)
  }
}