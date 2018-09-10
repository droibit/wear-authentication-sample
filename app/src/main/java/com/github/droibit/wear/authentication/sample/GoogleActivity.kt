package com.github.droibit.wear.authentication.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import kotlinx.android.synthetic.main.activity_google.label
import timber.log.Timber

class GoogleActivity : FragmentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_google)

    val lastSignedInAccount: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
    Timber.d("Google Account: ${lastSignedInAccount?.toJson()}")

    val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
        .build()
    if (!GoogleSignIn.hasPermissions(lastSignedInAccount, fitnessOptions)) {
      GoogleSignIn.requestPermissions(
          this,
          REQUEST_OAUTH_REQUEST_CODE,
          lastSignedInAccount,
          fitnessOptions
      )
    } else {
      label.text = "Already SignIn."
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent
  ) {
    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
        try {
          val account: GoogleSignInAccount = GoogleSignIn.getSignedInAccountFromIntent(data)
              .getResult(ApiException::class.java)
          Timber.d("Google SignIn succeeded: ${account.displayName}")
          label.text = "Succeeded."
        } catch (e: ApiException) {
          Timber.e(e)
          Toast.makeText(this, e.message, Toast.LENGTH_SHORT)
              .show()

        }
      }
    }
  }

  companion object {

    private const val REQUEST_OAUTH_REQUEST_CODE = 0x1001
  }
}
