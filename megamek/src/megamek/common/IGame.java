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

    GameOptions getOptions();

    GamePhase getPhase();

    void fireGameEvent(GameEvent event);

    void addGameListener(GameListener listener);

    /**
     * @return Whether there is an active claim for victory.
     */
    boolean isForceVictory();

    /** @return The Forces present in this game. Can be empty, but not null. */
    Forces getForces();

    //region Players

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

    //endregion

    //region Teams

    /** @return The teams in the game. Implementations should make sure that this list can be safely modified. */
    List<Team> getTeams();

    /** @return The number of teams in the game. */
    int getNoOfTeams();

    void setupTeams();

    /**
     * @return a player's team, which may be null if they do not have a team
     */
    default @Nullable Team getTeamForPlayer(Player p) {
        for (Team team : getTeams()) {
            for (Player player : team.players()) {
                if (player.equals(p)) {
                    return team;
                }
            }
        }
        return null;
    }

    //endregion

    //region Units

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
     * This is a Client-side method to replace or add units that are sent from the server. This method is called from
     * AbstractClient and should typically not require an override.
     *
     * Adds the given units to the list of units or objects in the current game. When a unit's ID is already
     * present the currently assigned unit will be replaced with the given new one.
     *
     * @param units The units to add or use as a replacement for current units.
     */
    void replaceUnits(List<InGameObject> units);

    /**
     * True when this InGameObject can be used as a game piece (may appear on the map) in this game type
     * (e.g. Entity and SupplyCrate would be valid for a TW Game but AlphaStrikeElement or SBFFormation would not.
     * For an SBF Game, SBF Formations and SBF Units would be valid but AlphaStrikeElement and Entity would not.)
     * Ideally, to enable using the lobby for all types of games, all Game/Client/GameManager combinations should
     * be able to handle all types of units during the lobby phase, but remove and prevent from adding them
     * outside the lobby phase.
     *
     * @param object The InGameObject (unit or other) to test
     * @return True when the InGameObject fits this type of Game and can appear as a game piece on the map
     */
    boolean isValidForGameType(InGameObject object);

    //endregion
}
