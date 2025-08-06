/*
 * Copyright (c) 2007-2008 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.server.victory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import megamek.common.Game;
import megamek.common.Player;

/**
 * This class represents the player-agreed /victory through chat commands
 */
public class PlayerAgreedVictory implements VictoryCondition, Serializable {

    @Override
    public VictoryResult checkVictory(Game game, Map<String, Object> ctx) {
        if (!game.isForceVictory()) {
            return VictoryResult.noResult();
        }
        final int victoryPlayerId = game.getVictoryPlayerId();
        final int victoryTeam = game.getVictoryTeam();
        List<Player> activePlayers = game.getPlayersList();
        activePlayers.removeIf(Player::isObserver);
        boolean forceVictory = true;

        // Individual victory
        if (victoryPlayerId != Player.PLAYER_NONE) {
            if (activePlayers.stream().filter(p -> p.getId() != victoryPlayerId).anyMatch(Player::doesNotAdmitDefeat)) {
                forceVictory = false;
            }
        }
        // Team victory
        if (victoryTeam != Player.TEAM_NONE) {
            if (activePlayers.stream().filter(p -> p.getTeam() != victoryTeam).anyMatch(Player::doesNotAdmitDefeat)) {
                forceVictory = false;
            }
        }

        if (forceVictory) {
            return new VictoryResult(true, victoryPlayerId, victoryTeam);
        } else {
            return VictoryResult.noResult();
        }
    }
}
