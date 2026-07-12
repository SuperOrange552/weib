package com.weib.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.weib.app.AppUiState
import com.weib.app.AppViewModel
import com.weib.app.ui.theme.WeibBackground
import com.weib.app.ui.theme.WeibBody
import com.weib.app.ui.theme.WeibMuted
import com.weib.app.ui.theme.WeibPrimary
import com.weib.app.ui.theme.WeibTitle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

@Composable
fun WeibApp(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()
    state.securityDialog?.let { message ->
        AlertDialog(onDismissRequest = {}, title = { Text("账号安全提醒") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::dismissSecurityDialog) { Text("我知道了") } })
    }
    state.actionMessage?.let { message ->
        AlertDialog(onDismissRequest = viewModel::dismissActionMessage, text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::dismissActionMessage) { Text("确定") } })
    }
    when {
        state.restoring -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        !state.loggedIn -> LoginScreen(state, viewModel)
        else -> MainShell(state, viewModel)
    }
}

@Composable
private fun LoginScreen(state: AppUiState, viewModel: AppViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var captcha by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("SEEKER") }
    Box(Modifier.fillMaxSize().background(WeibBackground).padding(20.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(8.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("◆ 微招", style = MaterialTheme.typography.headlineMedium, color = WeibPrimary)
                Text("求职者 · 招聘者移动端", style = MaterialTheme.typography.bodyLarge, color = WeibBody)
                Text("选择本次登录身份", style = MaterialTheme.typography.titleMedium, color = WeibTitle)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = selectedRole == "SEEKER",
                        onClick = { selectedRole = "SEEKER" },
                        label = { Text("求职者") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedRole == "BOSS",
                        onClick = { selectedRole = "BOSS" },
                        label = { Text("招聘者") },
                        leadingIcon = { Icon(Icons.Default.BusinessCenter, null) },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("账号") }, singleLine = true)
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("密码") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(captcha, { captcha = it }, Modifier.weight(1f), label = { Text("验证码") }, singleLine = true)
                    Surface(shape = RoundedCornerShape(10.dp), color = Color.White, modifier = Modifier.size(112.dp, 56.dp)) {
                        when {
                            state.captchaRefreshing -> Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
                            state.captcha != null -> Image(state.captcha.asImageBitmap(), "验证码", Modifier.fillMaxSize())
                            else -> Box(contentAlignment = Alignment.Center) { Text("加载失败", color = WeibMuted) }
                        }
                    }
                }
                val minute = state.captchaSeconds / 60
                val second = state.captchaSeconds % 60
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (state.captchaSeconds > 0) "验证码有效期 %02d:%02d".format(minute, second) else "验证码已过期",
                        color = if (state.captchaSeconds > 0) WeibBody else MaterialTheme.colorScheme.error)
                    TextButton(onClick = { viewModel.refreshCaptcha(true, username, password) }, enabled = !state.captchaRefreshing) {
                        Text("刷新验证码")
                    }
                }
                state.authError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = { viewModel.login(username, password, captcha, selectedRole) }, Modifier.fillMaxWidth().height(50.dp),
                    enabled = !state.loggingIn, shape = RoundedCornerShape(10.dp)) {
                    if (state.loggingIn) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White) else Text("登录")
                }
                Text("管理员请使用 Web 管理后台", style = MaterialTheme.typography.bodyMedium, color = WeibMuted)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(state: AppUiState, viewModel: AppViewModel) {
    val role = state.role ?: return
    val destinations = destinationsForRole(role)
    Scaffold(
        containerColor = WeibBackground,
        topBar = {
            TopAppBar(title = {
                Column { Text("微招", fontWeight = FontWeight.Bold, color = WeibPrimary); Text(state.selected?.label.orEmpty(), style = MaterialTheme.typography.bodyMedium) }
            }, actions = { IconButton(onClick = viewModel::retry) { Icon(Icons.Default.Refresh, "刷新") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White))
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                destinations.forEach { destination ->
                    NavigationBarItem(selected = state.selected == destination, onClick = { viewModel.select(destination) },
                        icon = { Icon(iconFor(destination), destination.label) }, label = { Text(destination.label) })
                }
            }
        }
    ) { padding ->
        ContentScreen(state, viewModel::retry, viewModel::logout, viewModel::apply,
            viewModel::toggleFavorite, viewModel::withdraw, viewModel::uploadResumeMedia, viewModel::saveResume,
            viewModel::searchJobs, viewModel::loadNextJobs, viewModel::searchTalents, viewModel::loadNextTalents,
            viewModel::saveJob, viewModel::toggleJob, viewModel::openConversation,
            viewModel::closeConversation, viewModel::sendChatMessage, viewModel::requestFullResume,
            viewModel::decideResumeAccess, viewModel::viewAuthorizedResume, viewModel::closeAuthorizedResume,
            Modifier.padding(padding))
    }
}

@Composable
private fun ContentScreen(state: AppUiState, retry: () -> Unit, logout: () -> Unit,
                          apply: (String) -> Unit, favorite: (String) -> Unit,
                          withdraw: (String) -> Unit, upload: (Uri, String) -> Unit,
                          saveResume: (Map<String, Any?>) -> Unit,
                          searchJobs: (String, String) -> Unit, loadNextJobs: () -> Unit,
                          searchTalents: (String) -> Unit, loadNextTalents: () -> Unit,
                          saveJob: (String?, Map<String, Any?>) -> Unit, toggleJob: (String, Boolean) -> Unit,
                          openConversation: (String) -> Unit, closeConversation: () -> Unit,
                          sendChatMessage: (String) -> Unit,
                          requestFullResume: (String) -> Unit, decideResumeAccess: (Long, Boolean) -> Unit,
                          viewAuthorizedResume: (Long) -> Unit, closeAuthorizedResume: () -> Unit,
                          modifier: Modifier) {
    when {
        state.content.loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.content.error != null -> ErrorState(state.content.error, retry, modifier)
        state.selected == AppDestination.Jobs -> JobList(state, apply, favorite, searchJobs, loadNextJobs, modifier)
        state.selected == AppDestination.Applications -> ApplicationList(state.content.data, withdraw, modifier)
        state.selected == AppDestination.Dashboard -> Dashboard(state.content.data, modifier)
        state.selected == AppDestination.Talent -> TalentList(state, searchTalents, loadNextTalents, requestFullResume, modifier)
        state.selected == AppDestination.BossJobs -> BossJobList(state.content.data, saveJob, toggleJob, modifier)
        state.selected == AppDestination.Messages -> MessageScreen(state, openConversation, closeConversation, sendChatMessage,
            decideResumeAccess, viewAuthorizedResume, closeAuthorizedResume, modifier)
        state.selected == AppDestination.Profile -> Profile(state.content.data, state.role, upload, saveResume, logout, modifier)
        else -> GenericList(state.content.title, state.content.data, modifier)
    }
}

@Composable
private fun JobList(state: AppUiState, apply: (String) -> Unit, favorite: (String) -> Unit,
                    search: (String, String) -> Unit, loadNext: () -> Unit, modifier: Modifier) {
    val array = state.jobs.items
    var keyword by remember { mutableStateOf(state.jobKeyword) }
    var city by remember { mutableStateOf(state.jobCity) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("发现好职位", style = MaterialTheme.typography.headlineMedium); Text("职位信息与 Web 端数据实时同步", color = WeibMuted) }
        item { OutlinedTextField(keyword,{keyword=it},Modifier.fillMaxWidth(),label={Text("搜索职位或公司")}); OutlinedTextField(city,{city=it},Modifier.fillMaxWidth(),label={Text("城市筛选")}); Button(onClick={search(keyword,city)},Modifier.fillMaxWidth()){Text("搜索") } }
        items(array, key = { it.string("id") }) { job -> WeibCard {
            Text(job.string("title", "职位名称"), style = MaterialTheme.typography.titleLarge, color = WeibTitle)
            Text(salary(job), style = MaterialTheme.typography.titleMedium, color = WeibPrimary)
            Text(listOf(job.string("city"), job.string("education"), job.string("experience")).filter { it.isNotBlank() }.joinToString(" · "), color = WeibBody)
            val company = job.getAsJsonObject("company")
            Text(company?.string("name", "企业信息") ?: "企业信息", color = WeibBody)
            Text("浏览量 ${job.int("viewCount")}", color = WeibMuted)
            val id = job.string("id")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { apply(id) }, enabled = id.isNotBlank() && job["applied"]?.asBoolean != true) { Text(if (job["applied"]?.asBoolean == true) "已投递" else "立即投递") }
                OutlinedButton(onClick = { favorite(id) }, enabled = id.isNotBlank()) { Text(if (job["favorited"]?.asBoolean == true) "取消收藏" else "收藏") }
            }
            TextButton(onClick={expandedId=if(expandedId==id)null else id}){Text(if(expandedId==id)"收起详情" else "查看详情")}
            if(expandedId==id){ HorizontalDivider(); Text(job.string("description","暂无职位描述")); Text(job.string("requirements","暂无任职要求")); company?.let{Text("公司：${it.string("name")}\n${it.string("description")}")}; Text("地址：${job.string("address","-")}") }
        } }
        if (array.isEmpty() && !state.jobs.refreshing) item { EmptyCard("暂无职位") }
        if (state.jobs.refreshing || state.jobs.appending) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        state.jobs.error?.let { message -> item { Button(onClick = { if (array.isEmpty()) search(keyword, city) else loadNext() }, Modifier.fillMaxWidth()) { Text("加载失败，点击重试：$message") } } }
        if (array.isNotEmpty() && state.jobs.hasNext && !state.jobs.appending) item { LaunchedEffect(state.jobs.page, keyword, city) { loadNext() } }
        if (array.isNotEmpty() && !state.jobs.hasNext) item { Text("没有更多职位了", color = WeibMuted, modifier = Modifier.fillMaxWidth().padding(12.dp)) }
    }
}

@Composable
private fun MessageScreen(state: AppUiState, open: (String) -> Unit, close: () -> Unit,
                          send: (String) -> Unit, decide: (Long, Boolean) -> Unit,
                          viewResume: (Long) -> Unit, closeResume: () -> Unit, modifier: Modifier) {
    var draft by remember(state.activeConversation) { mutableStateOf("") }
    if (state.activeConversation != null) {
        val messages = state.chatMessages?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asJsonObject } ?: emptyList()
        LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = close) { Icon(Icons.Default.ArrowBack, "返回") }; Text("聊天", style = MaterialTheme.typography.headlineMedium) } }
            if (state.chatLoading) item { CircularProgressIndicator() }
            items(messages, key = { it.string("id") }) { message -> WeibCard { Text(message.string("content", "消息"), color = WeibBody); Text(message.string("createdAt"), color = WeibMuted) } }
            item { OutlinedTextField(draft,{draft=it},Modifier.fillMaxWidth(),label={Text("输入消息")},minLines=2); Button(onClick={send(draft);draft=""},Modifier.fillMaxWidth(),enabled=draft.isNotBlank()){Text("发送") } }
        }
        return
    }
    state.authorizedResume?.let { resume ->
        LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Button(onClick=closeResume){Text("返回消息")}; Text("已授权的完整简历",style=MaterialTheme.typography.headlineMedium); WeibCard { JsonSummary(resume); Text(resume.asJsonObject.string("phone"),color=WeibBody);Text(resume.asJsonObject.string("email"),color=WeibBody);Text(resume.asJsonObject.string("workExperience"),color=WeibBody);Text(resume.asJsonObject.string("projectExperience"),color=WeibBody) } }
        }; return
    }
    val conversations = when {
        state.content.data?.isJsonArray == true -> state.content.data.asJsonArray.map { it.asJsonObject }
        else -> emptyList()
    }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("消息", style = MaterialTheme.typography.headlineMedium) }
        val requests = state.resumeAccessRequests?.takeIf{it.isJsonArray}?.asJsonArray?.map{it.asJsonObject} ?: emptyList()
        if (requests.isNotEmpty()) item { Text("完整简历授权",style=MaterialTheme.typography.titleLarge) }
        items(requests,key={it.string("id")}) { request -> WeibCard { Text("申请状态：${request.string("status")}",color=WeibPrimary); val id=request["id"]?.asLong?:0L; if(state.role=="seeker"&&request.string("status")=="PENDING"){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={decide(id,true)}){Text("同意")};OutlinedButton(onClick={decide(id,false)}){Text("拒绝")}}}; if(state.role=="boss"&&request.string("status")=="APPROVED"){Button(onClick={viewResume(id)}){Text("查看完整简历")}} } }
        items(conversations) { conversation -> WeibCard {
            Text(conversation.string("jobTitle", conversation.string("companyName", "招聘沟通")), style = MaterialTheme.typography.titleLarge)
            Text(conversation.string("seekerName", conversation.string("companyName")), color = WeibBody)
            val id = conversation.string("conversationId")
            Button(onClick={open(id)},enabled=id.isNotBlank()){Text("进入聊天")}
        } }
        if (conversations.isEmpty()) item { EmptyCard("暂无会话") }
    }
}

@Composable
private fun BossJobList(data: JsonElement?, save: (String?, Map<String, Any?>) -> Unit,
                        toggle: (String, Boolean) -> Unit, modifier: Modifier) {
    val jobs = data?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asJsonObject } ?: emptyList()
    var showForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }; var city by remember { mutableStateOf("") }
    var salaryMin by remember { mutableStateOf("") }; var salaryMax by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }; var experience by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }; var requirements by remember { mutableStateOf("") }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("职位管理", style = MaterialTheme.typography.headlineMedium); Button(onClick = { showForm = !showForm }) { Text(if (showForm) "收起" else "发布职位") } } }
        if (showForm) item { WeibCard {
            OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("职位名称")})
            OutlinedTextField(city,{city=it},Modifier.fillMaxWidth(),label={Text("城市")})
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(salaryMin,{salaryMin=it},Modifier.weight(1f),label={Text("最低薪资")});OutlinedTextField(salaryMax,{salaryMax=it},Modifier.weight(1f),label={Text("最高薪资")})}
            OutlinedTextField(education,{education=it},Modifier.fillMaxWidth(),label={Text("学历要求")}); OutlinedTextField(experience,{experience=it},Modifier.fillMaxWidth(),label={Text("经验要求")})
            OutlinedTextField(description,{description=it},Modifier.fillMaxWidth(),label={Text("职位描述")},minLines=3); OutlinedTextField(requirements,{requirements=it},Modifier.fillMaxWidth(),label={Text("任职要求")},minLines=3)
            Button(onClick={save(null,mapOf("title" to title,"city" to city,"salaryMin" to salaryMin.toIntOrNull(),"salaryMax" to salaryMax.toIntOrNull(),"education" to education,"experience" to experience,"description" to description,"requirements" to requirements,"address" to city,"tags" to ""))},Modifier.fillMaxWidth(),enabled=title.isNotBlank()&&city.isNotBlank()&&description.isNotBlank()){Text("确认发布")}
        } }
        items(jobs, key={it.string("id")}) { job -> WeibCard { Text(job.string("title","职位"),style=MaterialTheme.typography.titleLarge,color=WeibTitle); Text(salary(job),color=WeibPrimary); Text("${job.string("city")} · ${job.string("education")} · ${job.string("experience")}",color=WeibBody); Text("状态：${job.string("status")}",color=WeibMuted); val active=job.string("status")=="active"; Button(onClick={toggle(job.string("id"),active)}){Text(if(active)"关闭职位" else "重新开放") } } }
        if (jobs.isEmpty() && !showForm) item { EmptyCard("暂无已发布职位") }
    }
}

@Composable
private fun TalentList(state: AppUiState, search: (String) -> Unit, loadNext: () -> Unit,
                       requestResume: (String) -> Unit, modifier: Modifier) {
    var query by remember { mutableStateOf(state.talentQuery) }
    val talents = state.talents.items
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("发现合适人才", style = MaterialTheme.typography.headlineMedium); Text("仅展示求职者公开的简历摘要", color = WeibMuted) }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("搜索姓名、学校、专业或技能") }); Button(onClick = { search(query) }, Modifier.fillMaxWidth()) { Text("搜索人才") } }
        items(talents, key = { it.string("id") }) { talent -> WeibCard {
            Text(talent.string("name", "求职者"), style = MaterialTheme.typography.titleLarge, color = WeibTitle)
            Text(listOf(talent.string("education"), talent.string("school"), talent.string("major")).filter(String::isNotBlank).joinToString(" · "), color = WeibBody)
            Text(talent.string("skills", "暂未填写技能"), color = WeibBody)
            Text(talent.string("selfIntroduction", "暂未填写自我介绍"), color = WeibMuted)
            Button(onClick = { requestResume(talent.string("seekerId")) }, enabled = talent.string("seekerId").isNotBlank()) { Text("申请查看完整简历") }
        } }
        if (talents.isEmpty() && !state.talents.refreshing) item { EmptyCard("暂无公开人才") }
        if (state.talents.refreshing || state.talents.appending) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        state.talents.error?.let { message -> item { Button(onClick = { if (talents.isEmpty()) search(query) else loadNext() }, Modifier.fillMaxWidth()) { Text("加载失败，点击重试：$message") } } }
        if (talents.isNotEmpty() && state.talents.hasNext && !state.talents.appending) item { LaunchedEffect(state.talents.page, query) { loadNext() } }
        if (talents.isNotEmpty() && !state.talents.hasNext) item { Text("没有更多人才了", color = WeibMuted, modifier = Modifier.fillMaxWidth().padding(12.dp)) }
    }
}

@Composable
private fun ApplicationList(data: JsonElement?, withdraw: (String) -> Unit, modifier: Modifier) {
    val list = data?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asJsonObject } ?: emptyList()
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("我的投递", style = MaterialTheme.typography.headlineMedium) }
        items(list) { app -> WeibCard {
            Text(app.string("jobTitle", "职位"), style = MaterialTheme.typography.titleLarge)
            Text(app.string("companyName"), color = WeibBody)
            Text("状态：${app.string("status", "-")}", color = WeibPrimary)
            app["interviewTime"]?.takeIf { !it.isJsonNull }?.let { Text("面试时间：${it.asString}") }
            val id = app.string("id")
            if (app.string("status").lowercase() in listOf("pending", "viewed")) {
                OutlinedButton(onClick = { withdraw(id) }, enabled = id.isNotBlank()) { Text("撤回投递") }
            }
        } }
        if (list.isEmpty()) item { EmptyCard("暂无投递记录") }
    }
}

@Composable
private fun Dashboard(data: JsonElement?, modifier: Modifier) {
    val obj = data?.takeIf { it.isJsonObject }?.asJsonObject
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("招聘工作台", style = MaterialTheme.typography.headlineMedium); Text("职位和人才数据概览", color = WeibMuted) }
        if (obj?.get("companyRegistered")?.asBoolean == false) item { EmptyCard("请先在 Web 端完成企业入驻审核") }
        else {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("职位", obj?.int("jobCount") ?: 0, Modifier.weight(1f)); StatCard("在招", obj?.int("activeJobCount") ?: 0, Modifier.weight(1f)); StatCard("投递", obj?.int("applicationCount") ?: 0, Modifier.weight(1f))
            } }
            obj?.getAsJsonObject("company")?.let { company -> item { WeibCard { Text(company.string("name", "公司"), style = MaterialTheme.typography.titleLarge); Text("审核状态：${company.string("auditStatus", "-")}") } } }
        }
    }
}

@Composable
private fun GenericList(title: String, data: JsonElement?, modifier: Modifier) {
    val elements = when {
        data == null || data.isJsonNull -> emptyList()
        data.isJsonArray -> data.asJsonArray.toList()
        data.isJsonObject && data.asJsonObject.has("notifications") -> data.asJsonObject.getAsJsonArray("notifications").toList()
        data.isJsonObject && data.asJsonObject.has("content") -> data.asJsonObject.getAsJsonArray("content").toList()
        else -> listOf(data)
    }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(title, style = MaterialTheme.typography.headlineMedium) }
        items(elements) { element -> WeibCard { JsonSummary(element) } }
        if (elements.isEmpty()) item { EmptyCard("暂无$title") }
    }
}

@Composable
private fun Profile(data: JsonElement?, role: String?, upload: (Uri, String) -> Unit,
                    saveResume: (Map<String, Any?>) -> Unit, logout: () -> Unit, modifier: Modifier) {
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { uri -> upload(uri, "avatar") } }
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { uri -> upload(uri, "attachment") } }
    val resume = data?.takeIf { it.isJsonObject }?.asJsonObject
    var realName by remember(data) { mutableStateOf(resume?.string("realName").orEmpty()) }
    var phone by remember(data) { mutableStateOf(resume?.string("phone").orEmpty()) }
    var email by remember(data) { mutableStateOf(resume?.string("email").orEmpty()) }
    var school by remember(data) { mutableStateOf(resume?.string("school").orEmpty()) }
    var major by remember(data) { mutableStateOf(resume?.string("major").orEmpty()) }
    var skills by remember(data) { mutableStateOf(resume?.string("skills").orEmpty()) }
    var introduction by remember(data) { mutableStateOf(resume?.string("selfIntroduction").orEmpty()) }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("我的", style = MaterialTheme.typography.headlineMedium) }
        item { WeibCard { JsonSummary(data); Text("论坛、投诉与申诉", style = MaterialTheme.typography.titleMedium); Text("用户提交后由 Web 管理后台审核", color = WeibMuted) } }
        if (role == "seeker") item { WeibCard {
            Text("编辑简历", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(realName,{realName=it},Modifier.fillMaxWidth(),label={Text("姓名")})
            OutlinedTextField(phone,{phone=it},Modifier.fillMaxWidth(),label={Text("手机号")})
            OutlinedTextField(email,{email=it},Modifier.fillMaxWidth(),label={Text("邮箱")})
            OutlinedTextField(school,{school=it},Modifier.fillMaxWidth(),label={Text("毕业院校")})
            OutlinedTextField(major,{major=it},Modifier.fillMaxWidth(),label={Text("专业")})
            OutlinedTextField(skills,{skills=it},Modifier.fillMaxWidth(),label={Text("技能")})
            OutlinedTextField(introduction,{introduction=it},Modifier.fillMaxWidth(),label={Text("自我介绍")},minLines=3)
            Button(onClick={saveResume(mapOf("id" to resume?.get("id")?.takeIf{!it.isJsonNull}?.asLong,"realName" to realName,"phone" to phone,"email" to email,"school" to school,"major" to major,"skills" to skills,"selfIntroduction" to introduction))},Modifier.fillMaxWidth(),enabled=realName.isNotBlank()&&phone.isNotBlank()&&email.contains("@")){Text("保存简历")}
            Text("简历文件", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { avatarPicker.launch("image/*") }, Modifier.fillMaxWidth()) { Text("选择并上传头像") }
            OutlinedButton(onClick = { attachmentPicker.launch("application/pdf") }, Modifier.fillMaxWidth()) { Text("选择并上传简历附件") }
        } }
        item { OutlinedButton(onClick = logout, Modifier.fillMaxWidth()) { Text("退出登录") } }
    }
}

@Composable private fun JsonSummary(element: JsonElement?) {
    if (element == null || element.isJsonNull) { Text("暂无数据", color = WeibMuted); return }
    if (!element.isJsonObject) { Text(element.toString(), color = WeibBody); return }
    val obj = element.asJsonObject
    val title = listOf("title", "jobTitle", "name", "companyName", "realName", "content").firstNotNullOfOrNull { key -> obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString }
    Text(title ?: "信息", style = MaterialTheme.typography.titleMedium, color = WeibTitle)
    listOf("status", "seekerName", "city", "createdAt", "updatedAt", "interviewTime").forEach { key ->
        obj.get(key)?.takeIf { it.isJsonPrimitive }?.let { Text("${label(key)}：${it.asString}", color = WeibBody) }
    }
}

@Composable private fun WeibCard(content: @Composable ColumnScope.() -> Unit) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(3.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content) }
@Composable private fun EmptyCard(message: String) = WeibCard { Text(message, style = MaterialTheme.typography.titleMedium); Text("下拉或点击右上角刷新", color = WeibMuted) }
@Composable private fun StatCard(label: String, value: Int, modifier: Modifier) = Card(modifier, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value.toString(), style = MaterialTheme.typography.titleLarge, color = WeibPrimary); Text(label, color = WeibMuted) } }
@Composable private fun ErrorState(message: String, retry: () -> Unit, modifier: Modifier) = Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error); Text(message, color = WeibBody); Button(onClick = retry) { Text("重试") } } }

private fun iconFor(destination: AppDestination) = when (destination) {
    AppDestination.Jobs, AppDestination.BossJobs -> Icons.Default.Work
    AppDestination.Applications -> Icons.Default.Assignment
    AppDestination.Dashboard -> Icons.Default.Dashboard
    AppDestination.Talent -> Icons.Default.Groups
    AppDestination.Messages -> Icons.Default.Chat
    AppDestination.Forum -> Icons.Default.Forum
    AppDestination.Profile -> Icons.Default.Person
}
private fun JsonObject.string(key: String, fallback: String = ""): String = get(key)?.takeIf { it.isJsonPrimitive }?.asString ?: fallback
private fun JsonObject.int(key: String): Int = get(key)?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
private fun salary(job: JsonObject): String { val min = job.int("salaryMin"); val max = job.int("salaryMax"); return if (min == 0 && max == 0) "薪资面议" else "$min-$max" }
private fun label(key: String) = mapOf("status" to "状态", "seekerName" to "求职者", "city" to "城市", "createdAt" to "创建时间", "updatedAt" to "更新时间", "interviewTime" to "面试时间")[key] ?: key
