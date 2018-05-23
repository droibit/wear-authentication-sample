package com.twitter.sdk.android.core.internal.oauth;

import android.support.wearable.authentication.OAuthClient;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthException;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.internal.TwitterApi;
import com.twitter.sdk.android.core.internal.network.UrlUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.TreeMap;
import okhttp3.ResponseBody;

public class WearOAuthService extends OAuthService {

  private static final String RESOURCE_OAUTH = "oauth";
  //private static final String CALLBACK_URL = "twittersdk://callback";
  private static final String PARAM_SCREEN_NAME = "screen_name";
  private static final String PARAM_USER_ID = "user_id";

  private final String packageName;

  OAuth1aService.OAuthApi api;

  public WearOAuthService(TwitterCore twitterCore, TwitterApi api, String packageName) {
    super(twitterCore, api);
    this.api = getRetrofit().create(OAuth1aService.OAuthApi.class);
    this.packageName = packageName;
  }

  /**
   * Requests a temp token to start the Twitter sign-in flow.
   *
   * @param callback The callback interface to invoke when the request completes.
   */
  public void requestTempToken(final Callback<OAuthResponse> callback) {
    final TwitterAuthConfig config = getTwitterCore().getAuthConfig();
    final String url = getTempTokenUrl();

    api.getTempToken(new OAuth1aHeaders().getAuthorizationHeader(config, null,
        buildCallbackUrl(), "POST", url, null)).enqueue(getCallbackWrapper(callback));
  }

  String getTempTokenUrl() {
    return getApi().getBaseHostUrl() + "/oauth/request_token";
  }

  public String buildCallbackUrl() {
    return OAuthClient.WEAR_REDIRECT_URL_PREFIX + packageName;
  }

  /**
   * Requests a Twitter access token to act on behalf of a user account.
   *
   * @param callback The callback interface to invoke when when the request completes.
   */
  public void requestAccessToken(final Callback<OAuthResponse> callback,
      TwitterAuthToken requestToken, String verifier) {
    final String url = getAccessTokenUrl();
    final String authHeader = new OAuth1aHeaders().getAuthorizationHeader(getTwitterCore()
        .getAuthConfig(), requestToken, null, "POST", url, null);

    api.getAccessToken(authHeader, verifier).enqueue(getCallbackWrapper(callback));
  }

  String getAccessTokenUrl() {
    return getApi().getBaseHostUrl() + "/oauth/access_token";
  }

  /**
   * @param requestToken The request token.
   * @return authorization url that can be used to get a verifier code to get access token.
   */
  public String getAuthorizeUrl(TwitterAuthToken requestToken) {
    // https://api.twitter.com/oauth/authorize?oauth_token=%s
    return getApi().buildUponBaseHostUrl(RESOURCE_OAUTH, "authorize")
        .appendQueryParameter(OAuthConstants.PARAM_TOKEN, requestToken.token)
        .build()
        .toString();
  }

  /**
   * @return {@link OAuthResponse} parsed from the
   * response, may be {@code null} if the response does not contain an auth token and secret.
   */
  public static OAuthResponse parseAuthResponse(String response) {
    final TreeMap<String, String> params = UrlUtils.getQueryParams(response, false);
    final String token = params.get(OAuthConstants.PARAM_TOKEN);
    final String secret = params.get(OAuthConstants.PARAM_TOKEN_SECRET);
    final String userName = params.get(PARAM_SCREEN_NAME);
    final long userId;
    if (params.containsKey(PARAM_USER_ID)) {
      userId = Long.parseLong(params.get(PARAM_USER_ID));
    } else {
      userId = 0L;
    }
    if (token == null || secret == null) {
      return null;
    } else {
      return new OAuthResponse(new TwitterAuthToken(token, secret), userName, userId);
    }
  }

  Callback<ResponseBody> getCallbackWrapper(final Callback<OAuthResponse> callback) {
    return new Callback<ResponseBody>() {

      @Override
      public void success(Result<ResponseBody> result) {
        //Try to get response body
        BufferedReader reader = null;
        final StringBuilder sb = new StringBuilder();
        try {
          try {
            reader = new BufferedReader(
                new InputStreamReader(result.data.byteStream()));
            String line;

            while ((line = reader.readLine()) != null) {
              sb.append(line);
            }
          } finally {
            if (reader != null) {
              reader.close();
            }
          }
          final String responseAsStr = sb.toString();
          final OAuthResponse authResponse = parseAuthResponse(responseAsStr);
          if (authResponse == null) {
            callback.failure(new TwitterAuthException(
                "Failed to parse auth response: " + responseAsStr));
          } else {
            callback.success(new Result<>(authResponse, null));
          }
        } catch (IOException e) {
          callback.failure(new TwitterAuthException(e.getMessage(), e));
        }
      }

      @Override
      public void failure(TwitterException exception) {
        callback.failure(exception);
      }
    };
  }
}
