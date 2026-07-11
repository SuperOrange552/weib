package com.weib.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore("weib_session")

class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("access_token")
    private val roleKey = stringPreferencesKey("role")
    @Volatile private var cachedToken: String? = null

    fun token(): String? = cachedToken

    suspend fun restore(): String? {
        cachedToken = context.sessionDataStore.data.first()[tokenKey]
        return context.sessionDataStore.data.first()[roleKey]
    }

    suspend fun save(token: String, role: String) {
        cachedToken = token
        context.sessionDataStore.edit { it[tokenKey] = token; it[roleKey] = role }
    }

    suspend fun clear() {
        cachedToken = null
        context.sessionDataStore.edit { it.clear() }
    }
}
