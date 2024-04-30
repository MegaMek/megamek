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

import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameEvent;
import megamek.common.event.GameListener;
import megamek.common.force.Forces;
import megamek.common.options.GameOptions;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Common interface for games with different rule sets, such as Total Warfare, BattleForce, or Alpha Strike.
 */
public interface IGame {

    @Nullable
    GameTurn getTurn();

    boolean hasMoreTurns();

    GameOptions getOptions();

    GamePhase getPhase();

    /**
     * @return The current game round, with 0 typically indicating deployment and 1 the first
     * actual game round.
     */
    int getCurrentRound();

    /**
     * Sets the current game round to the given round number. See {@link #getCurrentRound()}. This
     * method can be used in both GameManager and Client.
     *
     * @param currentRound The new round number
     */
    void setCurrentRound(int currentRound);

    /**
     * Sets the current game phase to the given phase. May perform phase-dependent cleanup.
     * This method is intended for the GameManager.
     *
     * @param phase The new phase
     */
    void setPhase(GamePhase phase);

    /**
     * Sets the current game phase to the given phase. May perform phase-dependent cleanup and fire
     * game events. This method is intended for the Client. By default, this method calls
     * {@link #setPhase(GamePhase)}. When overridden, it'll usually make sense to call super(phase).
     *
     * @param phase The new phase
     */
    default void receivePhase(GamePhase phase) {
        setPhase(phase);
    }

    /**
     * Fires the given GameEvent, sending the event to all GameListener of this game.
     *
     * @param event the game event.
     */
    void fireGameEvent(GameEvent event);

    /**
     * Adds a GameListener to this game. The GameListener will receive any subsequently fired GameEvents.
     *
     * @param listener The GameListener to add
     */
    void addGameListener(GameListener listener);

    /**
     * @return Whether there is an active claim for victory.
     */
    boolean isForceVictory();

    /** @return The Forces present in this game. Can be empty, but not null. */
    Forces getForces();

    // PLAYERS //////////////

    /**
     * @param id a player id
     * @return the individual player assigned the id parameter.
     */
    @Nullable Player getPlayer(int id);

    /** @return An enumeration of {@link Player players} in the game. */
    @Deprecated
    Enumeration<Player> getPlayers();

    /** @return The current players as a list. */
    @Deprecated
    Vector<Player> getPlayersVector();

    /** @return The current players as a list. Implementations should make sure that this list can be safely modified. */
    List<Player> getPlayersList();

    /**
     * Adds the given Player to the game with the given game-unique id.
     * // TODO : Can this be made a default method?
     *
     * @param id The game-unique id of this player
     * @param player The Player object
     */
    void addPlayer(int id, Player player);

    /**
     * Sets the given Player to the given game-unique id.
     * // TODO : Is this method useful? Why not use addPlayer that also sets single-blind info?
     *
     * @param id The game-unique id of this player
     * @param player The Player object
     */
    void setPlayer(int id, Player player);

    /**
     * Removes the player with the id from the game.
     *
     * @param id The player id
     */
    void removePlayer(int id);

    /** @return The current number of active players in the game. This includes observers but not ghosts. */
    int getNoOfPlayers();

    // TEAMS //////////////

    /** @return The teams in the game. Implementations should make sure that this list can be safely modified. */
    List<Team> getTeams();

    /** @return The number of teams in the game. */
    int getNoOfTeams();

    void setupTeams();

    @Nullable default Team getTeamForPlayer(Player player) {
        for (Team team : getTeams()) {
            if (team.hasPlayer(player)) {
                return team;
            }
        }
        return null;
    }

    // UNITS //////////////

    /** @return The next free id for InGameObjects (units and others). */
    int getNextEntityId();

    /**
     * @return the number of units owned by the player, regardless of their
     *         status, as long as they are in the game.
     */
    default int getEntitiesOwnedBy(Player player) {
        return (int) getInGameObjects().stream().filter(o -> o.getOwnerId() == player.getId()).count();
    }

    /** @return The InGameObject associated with the given id, if there is one. */
    default Optional<InGameObject> getInGameObject(int id) {
        return getInGameObjects().stream().filter(o -> o.getId() == id).findAny();
    }

    /** @return A list of all InGameObjects of this game. This list is copied and may be safely modified. */
    List<InGameObject> getInGameObjects();

    /** @return A list of all InGameObjects of this game with the given ids. The returned list may be safely modified. */
    default List<InGameObject> getInGameObjects(Collection<Integer> idList) {
        return getInGameObjects().stream().filter(o -> idList.contains(o.getId())).collect(Collectors.toList());
    }

    /**
     * This is a Client-side method to replace or add units that are sent from the server.
     *
     * Adds the given units to the list of units or objects in the current game. When a unit's ID is already
     * present the currently assigned unit will be replaced with the given new one.
     *
     * @param units The units to add or use as a replacement for current units.
     */
    void replaceUnits(List<InGameObject> units);

    //region Board

    /**
     * Sets the given board as the game's board with the given boardId, possibly replacing the former board
     * of the same id. This method is written with the idea that a game might have more than one board.
     * Game's legacy methods of setBoard() and getBoard() use the boardId 0.
     * This method is meant as a server-side method.
     *
     * @param boardId (currently ignored) The boardId to assing to that board
     * @param board   The board to use
     */
    void setBoard(int boardId, Board board);

    /**
     * Returns the board with the given boardId or null if the game does not have a board of that boardId.
     *
     * @param boardId The board's ID
     * @return The board with the given ID
     */
    @Nullable
    default Board getBoard(int boardId) {
        return getBoards().get(boardId);
    }

    /**
     * Returns the complete map of boardIds/boards the game uses. The returned map is an unmodifiable view
     * of the game's map, but not a deep copy, so changes to a board will affect the game.
     *
     * @return The game's boards and their IDs
     */
    Map<Integer, Board> getBoards();

    /**
     * Returns the game's board. This method internally uses the boardId 0 for every call, see {@link #getBoard(int)}.
     * It can eventually be replaced to allow multiple maps for any type of game.
     *
     * @return The game's board (using ID = 0)
     */
    default Board getBoard() {
        return getBoard(0);
    }

    /**
     * Sets the given board as the game's board with the given boardId, possibly replacing the former board
     * of the same id. This method is written with the idea that a game might have more than one board.
     * This method is meant as a client-side method and may fire game events.
     *
     * @param boardId (currently ignored) The boardId to assing to that board
     * @param board   The board to use
     */
    void receiveBoard(int boardId, Board board);

    /**
     * Sets the given boards as the game's boards, replacing all previous boards.
     * This method is written with the idea that a game might have more than one board.
     * This method is meant as a client-side method and may fire game events.
     *
     * @param boards The new boards
     */
    void receiveBoards(Map<Integer, Board> boards);

    //endregion
}
