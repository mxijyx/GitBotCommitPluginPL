package com.frshaka.gitbot.settings

data class GitBotSettingsState(
    var model: String = "arcee-ai/trinity-large-preview:free",
    var language: String = "PL",
    var promptPtBr: String = "",
    var promptEn: String = "",
    var promptPl: String = ""
)