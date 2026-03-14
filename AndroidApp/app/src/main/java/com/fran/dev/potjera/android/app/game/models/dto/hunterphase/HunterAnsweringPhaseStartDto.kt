package com.fran.dev.potjera.android.app.game.models.dto.hunterphase

data class HunterAnsweringPhaseStartDto(
    val hunterAnsweringState: HunterAnsweringStateDto,
    val question: HunterAnsweringQuestionDto,
    val endTimestamp: Long
)
