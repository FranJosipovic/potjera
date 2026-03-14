package com.fran.dev.potjera.android.app.game.models

data class BoardQuestion(
    var question: String,
    var choices: List<String>,
    var correctAnswer: String,
)