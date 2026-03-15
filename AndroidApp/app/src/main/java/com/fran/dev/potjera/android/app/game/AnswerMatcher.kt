package com.fran.dev.potjera.android.app.game

object AnswerMatcher {

    fun isCorrect(input: String, answer: String, aliases: List<String>): Boolean {
        val normalizedInput = input.trim().lowercase()
        val normalizedAnswer = answer.trim().lowercase()
        val normalizedAliases = aliases.map { it.trim().lowercase() }

        return normalizedInput == normalizedAnswer ||
                normalizedAliases.any { it == normalizedInput } ||
                isFuzzyMatch(normalizedInput, normalizedAnswer) ||
                normalizedAliases.any { isFuzzyMatch(normalizedInput, it) }
    }

    private fun isFuzzyMatch(input: String, target: String): Boolean {
        val allowedDistance = when {
            target.length <= 4 -> 0
            target.length <= 7 -> 1
            else -> 2
        }
        return levenshtein(input, target) <= allowedDistance
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[a.length][b.length]
    }
}