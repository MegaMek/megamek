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

import megamek.common.event.GameEvent;
import megamek.common.event.GameListener;
import megamek.common.force.Forces;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This is a base class to derive all types of Game (TW, AS, BF, SBF...) from. Any such game will have players, units
 * (InGameObjects) and Forces (even if empty); the base class manages these.
 */
public abstract class AbstractGame implements IGame {

    /** The players present in the game mapped to their id as key. */
    protected final ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();

    /** The InGameObjects (units such as Entity and others) present in the game mapped to their id as key. */
    protected final ConcurrentHashMap<Integer, InGameObject> inGameObjects = new ConcurrentHashMap<>();

    /** The teams present in the game. */
    protected final CopyOnWriteArrayList<Team> teams = new CopyOnWriteArrayList<>();

    protected transient Vector<GameListener> gameListeners = new Vector<>();

    /**
     * The forces present in the game. The top level force holds all forces and force-less entities
     * and should therefore not be shown.
     */
    protected Forces forces = new Forces(this);

    @Override
    public Forces getForces() {
        return forces;
    }

    @Override
    public Player getPlayer(int id) {
        return players.get(id);
    }

    @Override
    public void addPlayer(int id, Player player) {
        players.put(id, player);
    }

    @Override
    @Deprecated
    public Vector<Player> getPlayersVector() {
        return new Vector<>(players.values());
    }

    @Override
    @Deprecated
    public Enumeration<Player> getPlayers() {
        return players.elements();
    }

    @Override
    public List<Player> getPlayersList() {
        return new ArrayList<>(players.values());
    }

    @Override
    public int getNoOfPlayers() {
        return players.size();
    }

    @Override
    public List<Team> getTeams() {
        return new ArrayList<>(teams);
    }

    @Override
    public int getNoOfTeams() {
        return teams.size();
    }

    @Override
    public List<InGameObject> getInGameObjects() {
        return new ArrayList<>(inGameObjects.values());
    }

    /**
     * Adds the specified game listener to receive board events from this board.
     *
     * @param listener the game listener.
     */
    @Override
    public void addGameListener(GameListener listener) {
        // Since gameListeners is transient, it could be null
        if (gameListeners == null) {
            gameListeners = new Vector<>();
        }
        gameListeners.add(listener);
    }

    /**
     * Removes the specified game listener.
     *
     * @param listener the game listener.
     */
    public void removeGameListener(GameListener listener) {
        // Since gameListeners is transient, it could be null
        if (gameListeners == null) {
            gameListeners = new Vector<>();
        }
        gameListeners.remove(listener);
    }

    /**
     * @return All registered GameListeners.
     */
    public List<GameListener> getGameListeners() {
        // Since gameListeners is transient, it could be null
        if (gameListeners == null) {
            gameListeners = new Vector<>();
        }
        return Collections.unmodifiableList(gameListeners);
    }


    /**
     * Processes game events occurring on this connection by dispatching them to
     * any registered GameListener objects.
     *
     * @param event the game event.
     */
    public void processGameEvent(GameEvent event) {
        // Since gameListeners is transient, it could be null
        if (gameListeners == null) {
            gameListeners = new Vector<>();
        }
        // This iteration must allow concurrent modification of the list!
        for (Enumeration<GameListener> e = gameListeners.elements(); e.hasMoreElements(); ) {
            event.fireEvent(e.nextElement());
        }
    }
}