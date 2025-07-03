package com.getvymo.appium

enum class RunnerType {
    LOCAL,
    LAMBDA_TEST,
    BROWSER_STACK,
    SAUCE_LABS,
    OTHER
}

enum class Protocol(
    val scheme: String,
) {
    HTTP("http"),
    HTTPS("https");

    companion object {
        fun getByName(name: String): Protocol? {
            val lowercaseName = name.lowercase()
            return values().find { it.scheme.lowercase() == lowercaseName }
        }
    }
}