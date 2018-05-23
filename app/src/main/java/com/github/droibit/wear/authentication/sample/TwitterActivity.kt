package com.github.droibit.wear.authentication.sample

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.wearable.authentication.OAuthClient
import android.widget.Toast
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.TwitterAuthToken
import com.twitter.sdk.android.core.TwitterCore
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.TwitterSession
import com.twitter.sdk.android.core.internal.TwitterApi
import com.twitter.sdk.android.core.internal.oauth.OAuthResponse
import com.twitter.sdk.android.core.internal.oauth.WearOAuthService
import com.twitter.sdk.android.core.models.Tweet
import com.twitter.sdk.android.core.services.StatusesService
import kotlinx.android.synthetic.main.activity_twitter.label
import timber.log.Timber

class TwitterActivity : FragmentActivity() {

  private lateinit var oauthClient: OAuthClient

  private lateinit var wearAuthService: WearOAuthService

  private val activeSession: TwitterSession?
    get() = TwitterCore.getInstance().sessionManager.activeSession

  private val statusesService: StatusesService
    get() = TwitterCore.getInstance().apiClient.statusesService

  private var token: TwitterAuthToken? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_twitter)

    oauthClient = OAuthClient.create(this)
    wearAuthService = WearOAuthService(
        TwitterCore.getInstance(),
        TwitterApi(),
        packageName
    )

    val activeSession = this.activeSession
    if (activeSession != null) {
      label.text = "@${activeSession.userName}"
      statusesService.homeTimeline(
          20, null, null, null, null, null, null
      )
          .enqueue(object : Callback<List<Tweet>>() {
            override fun success(result: Result<List<Tweet>>) {
              Timber.d(result.data.toString())
            }

            override fun failure(exception: TwitterException) {
              Timber.e(exception)
            }
          })
      return
    }

    wearAuthService.requestTempToken(object : Callback<OAuthResponse>() {
      override fun success(result: Result<OAuthResponse>) {
        Timber.d("success(response=${result.data}")
        this@TwitterActivity.token = result.data.authToken

        val requestUrl = wearAuthService.getAuthorizeUrl(token)
        oauthClient.sendAuthorizationRequest(Uri.parse(requestUrl), TwitterOAuthCallback())
      }

      override fun failure(exception: TwitterException) {
        Timber.e(exception)
      }
    })
  }

  override fun onDestroy() {
    super.onDestroy()

    oauthClient.destroy()
  }

  inner class TwitterOAuthCallback : OAuthClient.Callback() {

    override fun onAuthorizationError(errorCode: Int) {
      Timber.d("#onAuthorizationError(errorCode=$errorCode)")

      runOnUiThread {
        Toast.makeText(this@TwitterActivity, "Failed to authorization.", Toast.LENGTH_SHORT)
            .show()
      }
    }

    override fun onAuthorizationResponse(
      requestUrl: Uri,
      responseUrl: Uri
    ) {
      Timber.d("responseUrl=$responseUrl")

      val oauthVerifier = responseUrl.getQueryParameter("oauth_verifier")

      wearAuthService.requestAccessToken(
          object : Callback<OAuthResponse>() {
            override fun success(result: Result<OAuthResponse>) {
              val response = result.data
              Timber.d("#requestAccessToken(${result.data}")

              val session = TwitterSession(response.authToken, response.userId, response.userName)
              TwitterCore.getInstance()
                  .sessionManager.activeSession = session

              runOnUiThread {
                Toast.makeText(
                    this@TwitterActivity, "Login completed: ${response.userName}",
                    Toast.LENGTH_SHORT
                )
                    .show()
                label.text = "@${session.userName}"
              }
            }

            override fun failure(exception: TwitterException) {
              runOnUiThread {
                Toast.makeText(this@TwitterActivity, "Failed to authorization.", Toast.LENGTH_SHORT)
                    .show()
              }
            }
          }, token, oauthVerifier
      )
    }
  }
}
