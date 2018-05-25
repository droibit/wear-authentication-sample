package com.github.droibit.wear.authentication.sample

import android.content.Intent
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.view.View
import com.crashlytics.android.Crashlytics

class MainActivity : WearableActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }

  fun onTwitterLoginButtonClick(v: View) {
    startActivity(Intent(this, TwitterActivity::class.java))
  }

  fun onCrashButtonClick(v: View) {
    Crashlytics.getInstance().crash()
  }
}
