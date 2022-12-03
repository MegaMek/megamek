/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import megamek.common.force.Forces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractGame implements IGame {

    /** The players present in the game mapped to their id as key. */
    protected final ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();

    // Not yet used, should replace Entity-List
    protected final ConcurrentHashMap<Integer, Entity> inGameObjects = new ConcurrentHashMap<>();

    /**
     * The forces present in the game. The top level force holds all forces and force-less entities
     * and should therefore not be shown.
     */
    private final Forces forces = new Forces(this);

    @Override
    public Player getPlayer(int id) {
        return players.get(id);
    }

    @Override
    public Vector<Player> getPlayersVector() {
        return new Vector<>(players.values());
    }

    @Override
    public int getNoOfPlayers() {
        return players.size();
    }

    @Override
    public List<InGameObject> getInGameObjects(Collection<Integer> idList) {
        return new ArrayList<>(inGameObjects.values());
    }

    @Override
    public Forces getForces() {
        return forces;
    }
}
