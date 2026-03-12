package com.fran.dev.potjera.serverbackend.models.gamesession.coinbooster;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CoinBoosterGlobalState {
    List<CoinBoosterPlayerState> playerStates;
}
