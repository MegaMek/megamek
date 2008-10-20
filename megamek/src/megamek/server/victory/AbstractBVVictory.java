/*
 * MegaMek - Copyright (C) 2007-2008 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.server.victory;

import java.io.Serializable;
import java.util.Enumeration;

import megamek.common.IGame;
import megamek.common.Player;

/**
 * abstract baseclass for bv-checking victory implementations
 */
public abstract class AbstractBVVictory implements Victory, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -689891568905531049L;

    public int getFriendlyBV(IGame game, Player player) {
        int ret = 0;
        for (Enumeration<Player> f = game.getPlayers(); f.hasMoreElements();) {
            Player other = f.nextElement();
            if (other.isObserver())
                continue;
            if (!other.isEnemyOf(player)) {
                ret += other.getBV();
            }
        }
        return ret;
    }

    public int getEnemyBV(IGame game, Player player) {
        int ret = 0;
        for (Enumeration<Player> f = game.getPlayers(); f.hasMoreElements();) {
            Player other = f.nextElement();
            if (other.isObserver())
                continue;
            if (other.isEnemyOf(player)) {
                ret += other.getBV();
            }
        }
        return ret;
    }

    public int getEnemyInitialBV(IGame game, Player player) {
        int ret = 0;
        for (Enumeration<Player> f = game.getPlayers(); f.hasMoreElements();) {
            Player other = f.nextElement();
            if (other.isObserver())
                continue;
            if (other.isEnemyOf(player)) {
                ret += other.getInitialBV();
            }
        }
        return ret;
    }

}