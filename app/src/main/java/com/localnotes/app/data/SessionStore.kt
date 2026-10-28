package com.localnotes.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 登录后的账户信息（只用于展示 + 隔离本机笔记） */
data class UserAccount(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?
) {
    val initial: String
        get() = (displayName?.trim()?.take(1) ?: email.take(1)).uppercase()
}

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/**
 * 登录态用 DataStore 持久化，App 重启后仍保持登录，无需再次点 Google 登录。
 * 注意：这里只存身份标识，不存 token；笔记数据只在 Room 里。
 */
class SessionStore(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _account = MutableStateFlow<UserAccount?>(null)
    private val _ready = MutableStateFlow(false)

    val account: StateFlow<UserAccount?> = _account.asStateFlow()

    /** 首次读取完成前为 false，用于避免登录页闪一下 */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        scope.launch {
            appContext.sessionDataStore.data.collect { prefs ->
                val id = prefs[KEY_ID]
                val email = prefs[KEY_EMAIL]
                _account.value = if (!id.isNullOrBlank() && !email.isNullOrBlank()) {
                    UserAccount(
                        id = id,
                        email = email,
                        displayName = prefs[KEY_NAME],
                        photoUrl = prefs[KEY_PHOTO]
                    )
                } else {
                    null
                }
                _ready.value = true
            }
        }
    }

    suspend fun save(account: UserAccount) {
        appContext.sessionDataStore.edit { prefs ->
            prefs[KEY_ID] = account.id
            prefs[KEY_EMAIL] = account.email
            prefs[KEY_NAME] = account.displayName.orEmpty()
            prefs[KEY_PHOTO] = account.photoUrl.orEmpty()
        }
    }

    suspend fun clear() {
        appContext.sessionDataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_ID = stringPreferencesKey("account_id")
        val KEY_EMAIL = stringPreferencesKey("account_email")
        val KEY_NAME = stringPreferencesKey("account_name")
        val KEY_PHOTO = stringPreferencesKey("account_photo")
    }
}
