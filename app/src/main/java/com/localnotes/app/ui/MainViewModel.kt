package com.localnotes.app.ui

import android.app.Activity
import android.app.Application
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localnotes.app.BuildConfig
import com.localnotes.app.NotesApp
import com.localnotes.app.auth.GoogleAuth
import com.localnotes.app.data.NoteEntity
import com.localnotes.app.data.UserAccount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val ready: Boolean = false,
    val account: UserAccount? = null,
    val notes: List<NoteEntity> = emptyList(),
    val query: String = "",
    val signingIn: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NotesApp
    private val session = app.session
    private val repository = app.repository

    private val _query = MutableStateFlow("")
    private val _signingIn = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val notesFlow = combine(session.account, _query) { account, query ->
        account to query
    }.flatMapLatest { (account, query) ->
        if (account == null) flowOf(emptyList()) else repository.observeNotes(account.id, query)
    }

    private val baseState = combine(
        session.account,
        session.ready,
        notesFlow,
        _query,
        _signingIn
    ) { account, ready, notes, query, signingIn ->
        AppUiState(
            ready = ready,
            account = account,
            notes = notes,
            query = query,
            signingIn = signingIn
        )
    }

    val uiState: StateFlow<AppUiState> =
        combine(baseState, _error) { state, error -> state.copy(error = error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        viewModelScope.launch(Dispatchers.IO) { repository.purgeOldDeleted() }
    }

    // ---------- 搜索 ----------

    fun setQuery(value: String) {
        _query.value = value
    }

    // ---------- 登录 / 登出 ----------

    fun signIn(activity: Activity) {
        if (_signingIn.value) return
        viewModelScope.launch {
            _signingIn.value = true
            _error.value = null
            try {
                val account = GoogleAuth.signIn(activity, BuildConfig.GOOGLE_WEB_CLIENT_ID)
                session.save(account)
            } catch (e: CancellationException) {
                throw e
            } catch (e: GetCredentialCancellationException) {
                _error.value = null // 用户主动取消，不当作错误
            } catch (e: NoCredentialException) {
                _error.value = "这台设备上没有可用的 Google 账户。请先在系统设置 → 账户里添加，或改用其他登录方式。"
            } catch (e: GetCredentialException) {
                _error.value = "Google 登录失败：${e.message ?: "未知错误"}\n\n常见原因：OAuth 客户端 ID 配置有误，或模拟器未安装 Google Play 服务。"
            } catch (e: Exception) {
                _error.value = e.message ?: "登录失败，请重试"
            } finally {
                _signingIn.value = false
            }
        }
    }

    /** 仅调试用：未配置 OAuth 客户端 ID 时，先以「本机账户」进入，方便验证笔记功能 */
    fun continueWithoutGoogle() {
        viewModelScope.launch {
            session.save(
                UserAccount(
                    id = "local-device",
                    email = "local@device",
                    displayName = "本机模式",
                    photoUrl = null
                )
            )
        }
    }

    fun signOut() {
        viewModelScope.launch { session.clear() }
    }

    fun dismissError() {
        _error.value = null
    }

    // ---------- 笔记 ----------

    suspend fun loadNote(id: Long): NoteEntity? = repository.find(id)

    /** 返回笔记 id；未登录时返回 null */
    suspend fun upsertNote(
        id: Long?,
        title: String,
        content: String,
        pinned: Boolean
    ): Long? {
        val owner = session.account.value?.id ?: return null
        return repository.upsert(id, owner, title, content, pinned)
    }

    suspend fun deleteNote(id: Long) = repository.softDelete(id)

    suspend fun restoreNote(id: Long) = repository.restore(id)

    suspend fun togglePin(note: NoteEntity) = repository.setPinned(note.id, !note.pinned)
}
