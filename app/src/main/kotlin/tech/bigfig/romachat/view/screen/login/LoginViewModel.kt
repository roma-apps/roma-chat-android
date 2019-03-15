/*
 * Copyright (c) 2019 Vasily Kabunov
 *
 * This file is a part of Roma.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Roma is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Roma; if not,
 * see <http://www.gnu.org/licenses>.
 */

package tech.bigfig.romachat.view.screen.login

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import tech.bigfig.romachat.BuildConfig
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.Repository
import tech.bigfig.romachat.data.entity.Account
import tech.bigfig.romachat.data.entity.AppCredentials
import tech.bigfig.romachat.utils.buildQueryString
import tech.bigfig.romachat.utils.canonicalizeDomain
import tech.bigfig.romachat.utils.validateDomain

/**
 * Login flow contains the following steps:
 * 1. Validate user entered domain and call *Login* to get credentials
 * 2. Using these credentials open OAuth url
 * 3. After OAuth login activity receives new intent with uri with a code
 * 4. Using this code fetch a token
 * 5. Validate credentials and store user data
 */
class LoginViewModel(application: Application, repository: Repository) : AndroidViewModel(application) {

    //LiveDatas to update UI
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()

    val isError: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage: MutableLiveData<String> = MutableLiveData()

    private val domainLiveData: MutableLiveData<String> = MutableLiveData()
    private var domain: String = ""
    private var appCredentials: AppCredentials? = null

    private val fetchOAuthLiveData: MutableLiveData<FetchOAuthTokenParams> = MutableLiveData()

    val isUserLoggedIn: LiveData<Boolean> = repository.isLoggedIn()

    private val loginTemporaryStorage: LoginTemporaryStorage = LoginTemporaryStorage(application)

    // User entered domain and clicked submit
    fun onSubmitClick(instanceValue: String) {
        isLoading.value = true

        domain = canonicalizeDomain(instanceValue)

        if (!validateDomain(domain)) {
            showError((getApplication() as Application).getString(R.string.error_invalid_domain), "domain = $domain")

            return
        }

        isError.value = false

        //initiate login
        domainLiveData.value = domain
    }

    // Subscribe to domain, make an API call, build OAuth uri or show any error
    val checkDomain: LiveData<Uri?> = Transformations.switchMap(domainLiveData) {
        Transformations.map(
            repository.login(
                it,
                (getApplication() as Application).getString(R.string.app_name),
                oauthRedirectUri,
                OAUTH_SCOPES,
                WEBSITE
            )
        ) { result ->

            isLoading.value = false

            if (result.error != null) {
                showError(
                    (getApplication() as Application).getString(R.string.error_failed_app_registration),
                    e = result.error
                )

                null
            } else {//success
                if (result.data != null) {
                    appCredentials = result.data
                    buildUri(result.data)
                } else {
                    null
                }
            }
        }
    }

    private fun buildUri(appCredentials: AppCredentials): Uri {
        /* To authorize this app and log in it's necessary to redirect to the domain given,
         * activity_login there, and the server will redirect back to the app with its response. */
        val parameters = HashMap<String, String>()
        parameters["client_id"] = appCredentials.clientId
        parameters["redirect_uri"] = oauthRedirectUri
        parameters["response_type"] = "code"
        parameters["scope"] = OAUTH_SCOPES
        return Uri.parse(
            "https://$domain$ENDPOINT_AUTHORIZE?${buildQueryString(
                parameters
            )}"
        )
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(LOG_TAG, "onCleared, storing params")
        //store already fetched params because activity might be killed while oauth
        loginTemporaryStorage.save(LoginStoredParams(domain, appCredentials))
    }

    //Called when activity receives intent after OAuth login
    fun onOauthRedirect(uri: Uri) {
        if (uri.toString().startsWith(oauthRedirectUri)) {
            // This should either have returned an authorization code or an error.
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")

            // Restore values if activity was recreated before oauth callback
            val stored = loginTemporaryStorage.read()
            if (domain.isEmpty() && stored.domain?.isNotEmpty() == true) domain = stored.domain
            if (appCredentials == null && stored.appCredentials != null) appCredentials = stored.appCredentials

            if (code != null && domain.isNotEmpty() && appCredentials != null) {
                isLoading.value = true

                /* Since authorization has succeeded, the final step to log in is to exchange
                 * the authorization code for an access token. */
                fetchOAuthLiveData.value =
                    FetchOAuthTokenParams(domain, appCredentials!!.clientId, appCredentials!!.clientSecret, code)

            } else if (error != null) {
                /* Authorization failed. Put the error response where the user can read it and they
                 * can try again. */
                showError((getApplication() as Application).getString(R.string.error_authorization_denied), error)
            } else {
                // This case means a junk response was received somehow.
                showError((getApplication() as Application).getString(R.string.error_authorization_unknown))
            }
        } else {
            // first show or user cancelled login
            isLoading.value = false
        }

    }

    // Subscribe to OAuth result and fetch token or show errors
    private val fetchOAuthToken: LiveData<String?> = Transformations.switchMap(fetchOAuthLiveData) {
        Transformations.map(
            repository.fetchOAuthToken(
                it.domain,
                it.clientId,
                it.clientSecret,
                it.redirectUri,
                it.code
            )
        ) { result ->

            isLoading.value = false

            if (result.error != null) {
                showError(
                    (getApplication() as Application).getString(R.string.error_retrieving_oauth_token),
                    e = result.error
                )

                null
            } else {//success
                if (result.data != null && !result.data.accessToken.isEmpty()) {
                    Log.d(LOG_TAG, "oauth token = $result.data.accessToken")

                    repository.addNewAccount(result.data.accessToken, domain)

                    result.data.accessToken
                } else {
                    showError((getApplication() as Application).getString(R.string.error_retrieving_oauth_token))

                    null
                }
            }
        }
    }

    // After fetching token verify credentials and get account
    val getAccount: LiveData<Account?> = Transformations.switchMap(fetchOAuthToken) { token ->
        Transformations.map(repository.verifyAccount())
        { result ->
            isLoading.value = false

            if (result.error != null) {
                showError(
                    (getApplication() as Application).getString(R.string.error_authorization_unknown),
                    e = result.error
                )

                null
            } else {//success
                if (result.data != null) {

                    repository.updateAccount(result.data)

                    result.data
                } else {
                    showError((getApplication() as Application).getString(R.string.error_authorization_unknown))

                    null
                }
            }
        }
    }

    fun showError(error: String, logError: String = "", e: Throwable? = null) {
        isError.value = true
        errorMessage.value = error

        val sb = StringBuilder(error)
        if (!logError.isEmpty()) sb.append(" ").append(logError)
        if (e != null) sb.append(" ").append(Log.getStackTraceString(e))

        Log.e(LOG_TAG, sb.toString())

        isLoading.value = false
    }

    private val oauthRedirectUri: String
        get() {
            val scheme = (getApplication() as Application).getString(R.string.oauth_scheme)
            val host = BuildConfig.APPLICATION_ID
            return "$scheme://$host/"
        }

    inner class FetchOAuthTokenParams(
        val domain: String,
        val clientId: String,
        val clientSecret: String,
        val code: String,
        val redirectUri: String = oauthRedirectUri
    )

    companion object {
        private const val LOG_TAG = "LoginViewModel"

        private const val OAUTH_SCOPES = "read write follow push"
        private const val WEBSITE = "https://romaapp.github.io/"

        private const val ENDPOINT_AUTHORIZE = "/oauth/authorize"
    }

    class ModelFactory(private val application: Application, private val repository: Repository) :
        ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass == LoginViewModel::class.java) {
                LoginViewModel(application, repository) as T
            } else throw IllegalArgumentException("Wrong view model class")
        }
    }
}

/**
 * Since the login flow requires opening browser and return to the app after some time, we are storing previously
 * fetched data to avoid losing them if activity dies
 */
class LoginStoredParams(
    val domain: String?,
    val appCredentials: AppCredentials?
)

class LoginTemporaryStorage(context: Context) {

    private val preferences: SharedPreferences;

    init {
        preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    fun save(data: LoginStoredParams) {
        preferences.edit()
            .putString(DOMAIN, data.domain)
            .putString(CLIENT_ID, data.appCredentials?.clientId)
            .putString(CLIENT_SECRET, data.appCredentials?.clientSecret)
            .apply()
    }

    fun read(): LoginStoredParams {

        return LoginStoredParams(
            preferences.getString(DOMAIN, ""),
            AppCredentials(preferences.getString(CLIENT_ID, "") ?: "", preferences.getString(CLIENT_SECRET, "") ?: "")
        )
    }

    companion object {
        private const val FILE_NAME = "tech.bigfig.roma.loginprefs"
        private const val DOMAIN = "domain"
        private const val CLIENT_ID = "clientId"
        private const val CLIENT_SECRET = "clientSecret"
    }
}