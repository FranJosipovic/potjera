package com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PlayerVHunterGlobalState {
    Long hunterId;
    Long currentPlayerId;
}
