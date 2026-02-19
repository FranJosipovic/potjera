package com.fran.dev.potjera.android.app.domain.models.user

import androidx.viewpager2.adapter.StatefulAdapter

data class Statistic(
    val iconRes: Int,
    val value: String,
    val text: String
)

sealed class UserStatistic(val statistic: Statistic) {

    data class CorrectAnswers(val percentage: String = "0%") : UserStatistic(
        Statistic(iconRes = 1, value = percentage, text = "Correct Answers")
    )

    data class AvgAnswerTime(val seconds: String = "0s") : UserStatistic(
        Statistic(iconRes = 2, value = seconds, text = "Avg. Answer Time")
    )

    data class CoinsEarned(val amount: String = "0") : UserStatistic(
        Statistic(iconRes = 3, value = amount, text = "Coins Earned")
    )

    data class QuestionsAnswered(val count: String = "0") : UserStatistic(
        Statistic(iconRes = 4, value = count, text = "Questions Answered")
    )
}