package com.weib.app.ui

enum class AppDestination(val route: String, val label: String) {
    Jobs("jobs", "职位"),
    Applications("applications", "投递"),
    Dashboard("dashboard", "工作台"),
    BossJobs("boss_jobs", "职位"),
    BossApplications("boss_applications", "候选人"),
    Talent("talent", "人才"),
    Messages("messages", "消息"),
    Forum("forum", "论坛"),
    Profile("profile", "我的")
}

fun destinationsForRole(role: String): List<AppDestination> = when (role) {
    "seeker" -> listOf(AppDestination.Jobs, AppDestination.Applications, AppDestination.Messages,
        AppDestination.Forum, AppDestination.Profile)
    "boss" -> listOf(AppDestination.Dashboard, AppDestination.BossJobs, AppDestination.BossApplications, AppDestination.Talent,
        AppDestination.Messages, AppDestination.Forum, AppDestination.Profile)
    else -> emptyList()
}
