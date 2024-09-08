/*
 * Copyright (c) 2007-2008 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.victory;

import megamek.common.Game;
import megamek.common.Player;

import java.io.Serializable;

/**
 * This is a base interface with default helper methods for bv-checking VictoryConditions
 */
public interface BvVictoryCondition extends VictoryCondition, Serializable {

    default int getFriendlyBV(Game game, Player player) {
        return game.getPlayersList().stream()
                .filter(Player::isNotObserver)
                .filter(p -> !p.isEnemyOf(player))
                .mapToInt(Player::getBV).sum();
    }

    default int getEnemyBV(Game game, Player player) {
        return game.getPlayersList().stream()
                .filter(Player::isNotObserver)
                .filter(p -> p.isEnemyOf(player))
                .mapToInt(Player::getBV).sum();
    }

    default int getEnemyInitialBV(Game game, Player player) {
        return game.getPlayersList().stream()
                .filter(Player::isNotObserver)
                .filter(p -> p.isEnemyOf(player))
                .mapToInt(Player::getInitialBV).sum();
    }
}
