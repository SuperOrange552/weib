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

@Composable
fun WeibApp(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()
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
    Box(Modifier.fillMaxSize().background(WeibBackground).padding(20.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(8.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("◆ 微招", style = MaterialTheme.typography.headlineMedium, color = WeibPrimary)
                Text("求职者 · 招聘者移动端", style = MaterialTheme.typography.bodyLarge, color = WeibBody)
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
                Button(onClick = { viewModel.login(username, password, captcha) }, Modifier.fillMaxWidth().height(50.dp),
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
        ContentScreen(state, viewModel::retry, viewModel::logout, Modifier.padding(padding))
    }
}

@Composable
private fun ContentScreen(state: AppUiState, retry: () -> Unit, logout: () -> Unit, modifier: Modifier) {
    when {
        state.content.loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.content.error != null -> ErrorState(state.content.error, retry, modifier)
        state.selected == AppDestination.Jobs -> JobList(state.content.data, modifier)
        state.selected == AppDestination.Dashboard -> Dashboard(state.content.data, modifier)
        state.selected == AppDestination.Profile -> Profile(state.content.data, logout, modifier)
        else -> GenericList(state.content.title, state.content.data, modifier)
    }
}

@Composable
private fun JobList(data: JsonElement?, modifier: Modifier) {
    val array = data?.asJsonObject?.getAsJsonArray("content")?.mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject } ?: emptyList()
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("发现好职位", style = MaterialTheme.typography.headlineMedium); Text("职位信息与 Web 端数据实时同步", color = WeibMuted) }
        items(array) { job -> WeibCard {
            Text(job.string("title", "职位名称"), style = MaterialTheme.typography.titleLarge, color = WeibTitle)
            Text(salary(job), style = MaterialTheme.typography.titleMedium, color = WeibPrimary)
            Text(listOf(job.string("city"), job.string("education"), job.string("experience")).filter { it.isNotBlank() }.joinToString(" · "), color = WeibBody)
            val company = job.getAsJsonObject("company")
            Text(company?.string("name", "企业信息") ?: "企业信息", color = WeibBody)
            Text("浏览量 ${job.int("viewCount")}", color = WeibMuted)
        } }
        if (array.isEmpty()) item { EmptyCard("暂无职位") }
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
private fun Profile(data: JsonElement?, logout: () -> Unit, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("我的", style = MaterialTheme.typography.headlineMedium) }
        item { WeibCard { JsonSummary(data); Text("论坛、投诉与申诉", style = MaterialTheme.typography.titleMedium); Text("用户提交后由 Web 管理后台审核", color = WeibMuted) } }
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
