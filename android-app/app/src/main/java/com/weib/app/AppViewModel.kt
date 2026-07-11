package com.weib.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weib.app.data.AppRepository
import com.weib.app.data.ContentState
import com.weib.app.data.MobileUser
import com.weib.app.ui.AppDestination
import com.weib.app.ui.destinationsForRole
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.net.Uri

data class AppUiState(
    val restoring: Boolean = true,
    val user: MobileUser? = null,
    val restoredRole: String? = null,
    val captcha: Bitmap? = null,
    val captchaSeconds: Int = 0,
    val captchaRefreshing: Boolean = false,
    val loggingIn: Boolean = false,
    val authError: String? = null,
    val selected: AppDestination? = null,
    val content: ContentState = ContentState()
    ,val securityDialog: String? = null
    ,val actionMessage: String? = null
) {
    val role: String? get() = user?.role ?: restoredRole
    val loggedIn: Boolean get() = role == "seeker" || role == "boss"
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()
    private var timer: Job? = null

    init {
        viewModelScope.launch {
            repository.securityEvents.collect { reason ->
                val reduced = com.weib.app.data.realtime.ForcedLogoutReducer.reduce(
                    com.weib.app.data.realtime.SecurityState(repository.session.token(), null),
                    com.weib.app.data.realtime.SecurityEvent(reason))
                repository.session.clear()
                _state.value = AppUiState(restoring = false, securityDialog = reduced.dialog)
                refreshCaptcha(manual = false)
            }
        }
        viewModelScope.launch {
            val role = repository.session.restore()
            if (role == "seeker" || role == "boss") {
                val destination = destinationsForRole(role).first()
                _state.value = _state.value.copy(restoring = false, restoredRole = role, selected = destination)
                load(destination)
            } else {
                _state.value = _state.value.copy(restoring = false)
                refreshCaptcha(manual = false)
            }
        }
    }

    fun refreshCaptcha(manual: Boolean, username: String = "", password: String = "") {
        if (manual && (username.isBlank() || password.isBlank())) {
            _state.value = _state.value.copy(authError = "请先输入账号和密码，再刷新验证码")
            return
        }
        if (manual && _state.value.captchaSeconds > 115) {
            _state.value = _state.value.copy(authError = "刷新过于频繁，请稍后再试")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(captchaRefreshing = true, authError = null)
            runCatching { repository.captcha() }
                .onSuccess { image ->
                    val bitmap = BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)
                    _state.value = _state.value.copy(captcha = bitmap, captchaSeconds = image.expiresIn,
                        captchaRefreshing = false)
                    startTimer()
                }
                .onFailure { _state.value = _state.value.copy(captchaRefreshing = false, authError = it.message) }
        }
    }

    fun login(username: String, password: String, captcha: String, selectedRole: String) {
        if (username.isBlank() || password.isBlank() || captcha.isBlank()) {
            _state.value = _state.value.copy(authError = "账号、密码和验证码不能为空")
            return
        }
        if (_state.value.captchaSeconds <= 0) {
            _state.value = _state.value.copy(authError = "验证码已过期，请刷新后重试")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loggingIn = true, authError = null)
            runCatching { repository.login(username.trim(), password, captcha.trim(), selectedRole) }
                .onSuccess { result ->
                    val destination = destinationsForRole(result.user.role).first()
                    timer?.cancel()
                    _state.value = _state.value.copy(loggingIn = false, user = result.user,
                        restoredRole = null, selected = destination, captcha = null)
                    load(destination)
                }
                .onFailure { _state.value = _state.value.copy(loggingIn = false, authError = it.message) }
        }
    }

    fun select(destination: AppDestination) {
        _state.value = _state.value.copy(selected = destination)
        load(destination)
    }

    fun retry() { _state.value.selected?.let(::load) }

    fun apply(jobId: String) = runAction("投递成功") { repository.apply(jobId) }
    fun toggleFavorite(jobId: String) = runAction("收藏状态已更新") { repository.toggleFavorite(jobId) }
    fun withdraw(applicationId: String) = runAction("投递已撤回") { repository.withdraw(applicationId) }

    private fun runAction(success: String, call: suspend () -> com.weib.app.data.ApiEnvelope<com.google.gson.JsonElement>) {
        viewModelScope.launch {
            runCatching { call() }.onSuccess { result ->
                _state.value = _state.value.copy(actionMessage = if (result.code == 200) success else result.msg)
                if (result.code == 200) retry()
            }.onFailure { _state.value = _state.value.copy(actionMessage = it.message) }
        }
    }

    fun dismissActionMessage() { _state.value = _state.value.copy(actionMessage = null) }
    fun uploadResumeMedia(uri: Uri, kind: String) {
        viewModelScope.launch {
            runCatching { repository.uploadResumeMedia(uri, kind) }
                .onSuccess { _state.value = _state.value.copy(actionMessage = "上传成功"); retry() }
                .onFailure { _state.value = _state.value.copy(actionMessage = it.message ?: "上传失败") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _state.value = AppUiState(restoring = false)
            refreshCaptcha(manual = false)
        }
    }

    fun dismissSecurityDialog() { _state.value = _state.value.copy(securityDialog = null) }

    private fun load(destination: AppDestination) {
        val role = _state.value.role ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(content = ContentState(title = destination.label, loading = true))
            runCatching { repository.load(role, destination.route) }
                .onSuccess { result ->
                    if (result.code == 200) {
                        _state.value = _state.value.copy(content = ContentState(destination.label, result.data))
                    } else {
                        _state.value = _state.value.copy(content = ContentState(destination.label, error = result.msg))
                    }
                }
                .onFailure { _state.value = _state.value.copy(content = ContentState(destination.label, error = it.message)) }
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = viewModelScope.launch {
            while (_state.value.captchaSeconds > 0) {
                delay(1000)
                _state.value = _state.value.copy(captchaSeconds = (_state.value.captchaSeconds - 1).coerceAtLeast(0))
            }
        }
    }
}
