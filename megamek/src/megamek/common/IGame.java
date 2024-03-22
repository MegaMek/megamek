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

    GameOptions getOptions();

    GamePhase getPhase();

    void setPhase(GamePhase phase);

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
     *
     * @param id The game-unique id of this player
     * @param player The Player object
     */
    void addPlayer(int id, Player player);

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

    //region Board

    /**
     * Sets the given board as the game's board with the given boardId, possibly replacing the former board
     * of the same id. -- This method is written with the idea that a game might have more than one board.
     * Currently, the boardId will be ignored and the given board will be the game's single board. --
     * This method is meant as a server-side method and will not
     *
     * @param board The board to use
     * @param boardId (currently ignored) The boardId to assing to that board
     */
    void setBoard(Board board, int boardId);

    //endregion
}
