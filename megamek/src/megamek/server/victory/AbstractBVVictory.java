/*
 * MegaMek - Copyright (C) 2007-2008 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.server.victory;

import megamek.common.Game;
import megamek.common.Player;

import java.io.Serializable;

/**
 * abstract baseclass for bv-checking victory implementations
 */
public abstract class AbstractBVVictory implements IVictoryConditions, Serializable {
    private static final long serialVersionUID = -689891568905531049L;

    public int getFriendlyBV(Game game, Player player) {
        int ret = 0;
        for (Player other : game.getPlayersVector()) {
            if (other.isObserver()) {
                continue;
            }

            if (!other.isEnemyOf(player)) {
                ret += other.getBV();
            }
        }
        return ret;
    }

    public int getEnemyBV(Game game, Player player) {
        int ret = 0;
        for (Player other : game.getPlayersVector()) {
            if (other.isObserver()) {
                continue;
            }

            if (other.isEnemyOf(player)) {
                ret += other.getBV();
            }
        }
        return ret;
    }

    public int getEnemyInitialBV(Game game, Player player) {
        int ret = 0;
        for (Player other : game.getPlayersVector()) {
            if (other.isObserver()) {
                continue;
            }

            if (other.isEnemyOf(player)) {
                ret += other.getInitialBV();
            }
        }
        return ret;
    }
}
