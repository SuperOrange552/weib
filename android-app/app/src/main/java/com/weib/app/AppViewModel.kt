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
import com.google.gson.JsonObject
import com.weib.app.data.PageSlice
import com.weib.app.data.PagingReducer
import com.weib.app.data.PagingState

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
    ,val jobs: PagingState<JsonObject> = PagingState()
    ,val jobKeyword: String = ""
    ,val jobCity: String = ""
    ,val talents: PagingState<JsonObject> = PagingState()
    ,val talentQuery: String = ""
    ,val activeConversation: String? = null
    ,val chatMessages: com.google.gson.JsonElement? = null
    ,val chatLoading: Boolean = false
    ,val resumeAccessRequests: com.google.gson.JsonElement? = null
    ,val authorizedResume: com.google.gson.JsonElement? = null
    ,val activeForumPost: Long? = null
    ,val forumComments: com.google.gson.JsonElement? = null
    ,val complaints: com.google.gson.JsonElement? = null
    ,val appeals: com.google.gson.JsonElement? = null
    ,val forumImageUrl: String? = null
    ,val evidenceImageUrl: String? = null
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

    fun searchJobs(keyword: String, city: String) {
        _state.value = _state.value.copy(jobKeyword = keyword, jobCity = city)
        loadJobs(refresh = true)
    }

    fun loadNextJobs() = loadJobs(refresh = false)

    fun searchTalents(query: String) {
        _state.value = _state.value.copy(talentQuery = query)
        loadTalents(refresh = true)
    }

    fun loadNextTalents() = loadTalents(refresh = false)

    fun openConversation(id: String) {
        if (id.isBlank()) return
        _state.value = _state.value.copy(activeConversation = id, chatLoading = true)
        viewModelScope.launch {
            runCatching { repository.chatMessages(id) }
                .onSuccess { result -> _state.value = _state.value.copy(chatMessages = result.data, chatLoading = false, actionMessage = if (result.code == 200) null else result.msg) }
                .onFailure { _state.value = _state.value.copy(chatLoading = false, actionMessage = it.message) }
        }
    }

    fun closeConversation() { _state.value = _state.value.copy(activeConversation = null, chatMessages = null) }

    fun sendChatMessage(content: String) {
        val id = _state.value.activeConversation ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.sendMessage(id, content.trim()) }
                .onSuccess { result -> if (result.code == 200) openConversation(id) else _state.value = _state.value.copy(actionMessage = result.msg) }
                .onFailure { _state.value = _state.value.copy(actionMessage = it.message) }
        }
    }

    fun requestFullResume(seekerId: String) = runAction("完整简历申请已发送") { repository.requestResumeAccess(seekerId) }

    fun decideResumeAccess(id: Long, approved: Boolean) = runAction(if (approved) "已同意完整简历授权" else "已拒绝完整简历授权") {
        repository.decideResumeAccess(id, approved)
    }

    fun viewAuthorizedResume(id: Long) {
        viewModelScope.launch {
            runCatching { repository.authorizedResume(id) }
                .onSuccess { result -> _state.value = _state.value.copy(authorizedResume = result.data, actionMessage = if (result.code == 200) null else result.msg) }
                .onFailure { _state.value = _state.value.copy(actionMessage = it.message) }
        }
    }

    fun closeAuthorizedResume() { _state.value = _state.value.copy(authorizedResume = null) }
    fun createForumPost(fields: Map<String, Any?>) = runAction("帖子发布成功") { repository.createForumPost(fields) }
    fun likeForumPost(id: Long) = runAction("点赞成功") { repository.likeForumPost(id) }
    fun favoriteForumPost(id: Long) = runAction("收藏成功") { repository.favoriteForumPost(id) }
    fun openForumPost(id: Long) { viewModelScope.launch { runCatching { repository.forumComments(id) }.onSuccess { _state.value = _state.value.copy(activeForumPost=id,forumComments=it.data) } } }
    fun closeForumPost() { _state.value = _state.value.copy(activeForumPost=null,forumComments=null) }
    fun commentForumPost(content: String) { val id=_state.value.activeForumPost?:return; runAction("评论成功") { repository.createForumComment(id,content) } }
    fun createComplaint(fields: Map<String, Any?>) = runAction("投诉已提交") { repository.createComplaint(fields) }
    fun createAppeal(fields: Map<String, Any?>) = runAction("申诉已提交") { repository.createAppeal(fields) }
    fun uploadForumImage(uri: Uri) = uploadImage(uri, false)
    fun uploadEvidenceImage(uri: Uri) = uploadImage(uri, true)
    fun searchForum(query: String, sectionId: Long?) { viewModelScope.launch { _state.value=_state.value.copy(content=ContentState("论坛",loading=true)); runCatching{repository.forumPosts(0,query,sectionId)}.onSuccess{r->_state.value=_state.value.copy(content=if(r.code==200)ContentState("论坛",r.data)else ContentState("论坛",error=r.msg))}.onFailure{_state.value=_state.value.copy(content=ContentState("论坛",error=it.message))} } }
    private fun uploadImage(uri: Uri, evidence: Boolean) { viewModelScope.launch { runCatching { repository.uploadImage(uri,evidence) }.onSuccess { url->_state.value=if(evidence)_state.value.copy(evidenceImageUrl=url,actionMessage="证据图片上传成功") else _state.value.copy(forumImageUrl=url,actionMessage="论坛图片上传成功") }.onFailure { _state.value=_state.value.copy(actionMessage=it.message) } } }

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
    fun saveResume(fields: Map<String, Any?>) {
        viewModelScope.launch {
            runCatching { repository.saveResume(fields) }
                .onSuccess { result ->
                    _state.value = _state.value.copy(actionMessage = if (result.code == 200) "简历保存成功" else result.msg)
                    if (result.code == 200) retry()
                }.onFailure { _state.value = _state.value.copy(actionMessage = it.message ?: "简历保存失败") }
        }
    }
    fun updateApplication(id: String, status: String) = runAction("投递状态已更新") { repository.updateApplicationStatus(id, status) }
    fun toggleJob(id: String, active: Boolean) = runAction("职位状态已更新") { if (active) repository.closeJob(id) else repository.reopenJob(id) }
    fun saveJob(id: String?, fields: Map<String, Any?>) = runAction(if (id == null) "职位发布成功" else "职位更新成功") {
        if (id == null) repository.createJob(fields) else repository.updateJob(id, fields)
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
        if (destination == AppDestination.Messages) loadResumeAccessRequests()
        if (destination == AppDestination.Profile) loadModeration()
        if (destination == AppDestination.Jobs) {
            loadJobs(refresh = true)
            return
        }
        if (destination == AppDestination.Talent) {
            loadTalents(refresh = true)
            return
        }
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

    private fun loadResumeAccessRequests() {
        viewModelScope.launch {
            runCatching { repository.resumeAccessRequests() }
                .onSuccess { result -> if (result.code == 200) _state.value = _state.value.copy(resumeAccessRequests = result.data) }
        }
    }
    private fun loadModeration() {
        viewModelScope.launch { runCatching { repository.myComplaints() }.onSuccess { if(it.code==200)_state.value=_state.value.copy(complaints=it.data) } }
        viewModelScope.launch { runCatching { repository.myAppeals() }.onSuccess { if(it.code==200)_state.value=_state.value.copy(appeals=it.data) } }
    }

    private fun loadJobs(refresh: Boolean) {
        if (_state.value.role != "seeker") return
        val current = _state.value.jobs
        val loading = if (refresh) PagingReducer.startRefresh(current) else PagingReducer.startAppend(current)
        if (!refresh && loading === current) return
        _state.value = _state.value.copy(jobs = loading)
        val requestedPage = if (refresh) 0 else current.page + 1
        val keyword = _state.value.jobKeyword
        val city = _state.value.jobCity
        viewModelScope.launch {
            runCatching { repository.jobs(requestedPage, keyword, city) }
                .onSuccess { result ->
                    val data = result.data?.takeIf { it.isJsonObject }?.asJsonObject
                    if (result.code != 200 || data == null) {
                        _state.value = _state.value.copy(jobs = PagingReducer.pageFailed(_state.value.jobs, result.msg ?: "职位加载失败"))
                    } else {
                        val items = data.getAsJsonArray("content")?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject } ?: emptyList()
                        val page = data.get("number")?.asInt ?: data.get("page")?.asInt ?: requestedPage
                        val totalPages = data.get("totalPages")?.asInt ?: (page + 1)
                        _state.value = _state.value.copy(jobs = PagingReducer.pageLoaded(
                            _state.value.jobs, PageSlice(items, page, totalPages)
                        ) { it.get("id")?.asString.orEmpty() })
                    }
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(jobs = PagingReducer.pageFailed(_state.value.jobs, error.message ?: "网络异常"))
                }
        }
    }

    private fun loadTalents(refresh: Boolean) {
        if (_state.value.role != "boss") return
        val current = _state.value.talents
        val loading = if (refresh) PagingReducer.startRefresh(current) else PagingReducer.startAppend(current)
        if (!refresh && loading === current) return
        _state.value = _state.value.copy(talents = loading)
        val requestedPage = if (refresh) 0 else current.page + 1
        val query = _state.value.talentQuery
        viewModelScope.launch {
            runCatching { repository.talents(requestedPage, query) }
                .onSuccess { result ->
                    val data = result.data?.takeIf { it.isJsonObject }?.asJsonObject
                    if (result.code != 200 || data == null) {
                        _state.value = _state.value.copy(talents = PagingReducer.pageFailed(_state.value.talents, result.msg ?: "人才加载失败"))
                    } else {
                        val items = data.getAsJsonArray("content")?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject } ?: emptyList()
                        val page = data.get("currentPage")?.asInt ?: data.get("page")?.asInt ?: data.get("number")?.asInt ?: requestedPage
                        val totalPages = data.get("totalPages")?.asInt ?: (page + 1)
                        _state.value = _state.value.copy(talents = PagingReducer.pageLoaded(
                            _state.value.talents, PageSlice(items, page, totalPages)
                        ) { it.get("id")?.asString.orEmpty() })
                    }
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(talents = PagingReducer.pageFailed(_state.value.talents, error.message ?: "网络异常"))
                }
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
