/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameEvent;
import megamek.common.event.GameListener;
import megamek.common.force.Forces;
import megamek.common.interfaces.PlayerTurn;
import megamek.common.interfaces.ReportEntry;
import megamek.common.interfaces.ServerOnly;
import megamek.common.options.IGameOptions;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;
import megamek.server.scriptedEvents.TriggeredEvent;

/**
 * Common interface for games with different rule sets, such as Total Warfare, BattleForce, or Alpha Strike.
 */
public interface IGame {

    MMLogger LOGGER = MMLogger.create(IGame.class);

    int DEFAULT_BOARD_ID = 0;

    // region Player turns

    @Nullable
    PlayerTurn getTurn();

    /**
     * @return True when there is at least one more player turn waiting to be played in the current game phase. //TODO
     *       this code from Game is surprising; the last available turn should be at size()-1, but apparently this
     *       works
     */
    default boolean hasMoreTurns() {
        return getTurnsList().size() > getTurnIndex();
    }

    /**
     * Returns the current turn index, i.e. the turn that should next be played by the corresponding player.
     */
    int getTurnIndex();

    /**
     * @return the current list of turns. If you're not the GameManager, don't even think about changing any of the
     *       turns.
     */
    List<? extends PlayerTurn> getTurnsList();

    // endregion
    IGameOptions getOptions();

    /**
     * @return The current game round, with 0 typically indicating deployment and 1 the first actual game round.
     */
    int getCurrentRound();

    /**
     * Sets the current game round to the given round number. See {@link #getCurrentRound()}. This method can be used in
     * both GameManager and Client.
     *
     * @param currentRound The new round number
     */
    void setCurrentRound(int currentRound);

    /**
     * Adds 1 to the current round value. This method is intended for server use only.
     */
    void incrementCurrentRound();

    // region Phase Management

    /**
     * @return The current phase of this game.
     */
    GamePhase getPhase();

    /**
     * Sets the current game phase to the given phase. May perform phase-dependent cleanup. This method is intended for
     * the GameManager.
     *
     * @param phase The new phase
     */
    void setPhase(GamePhase phase);

    /**
     * Sets the previous game phase to the given phase. This method is intended for the GameManager.
     *
     * @param lastPhase The phase to be remembered as the previous phase.
     */
    void setLastPhase(GamePhase lastPhase);

    /**
     * Sets the current game phase to the given phase. May perform phase-dependent cleanup and fire game events. This
     * method is intended for the Client. By default, this method calls {@link #setPhase(GamePhase)}. When overridden,
     * it'll usually make sense to call super(phase).
     *
     * @param phase The new phase
     */
    default void receivePhase(GamePhase phase) {
        setPhase(phase);
    }

    /**
     * Returns true when the current game phase should be played, meaning it is played in the current type of game and
     * there are possible actions in it in the present game state. The result may be different in other rounds.
     *
     * @return True when the current phase should be skipped entirely in this round
     *
     * @see #shouldSkipCurrentPhase()
     */
    boolean isCurrentPhasePlayable();

    /**
     * Returns true when the current game phase should be skipped, either because it is not played at all in the current
     * type of game or because the present game state dictates that there can be no actions in it. The result may be
     * different in other rounds. This is the opposite of {@link #isCurrentPhasePlayable()}.
     *
     * @return True when the current phase should be skipped entirely in this round
     *
     * @see #isCurrentPhasePlayable()
     */
    default boolean shouldSkipCurrentPhase() {
        return !isCurrentPhasePlayable();
    }

    // endregion

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
     * Removes the specified game listener.
     *
     * @param listener the game listener.
     */
    void removeGameListener(GameListener listener);

    /**
     * @return Whether there is an active claim for victory.
     */
    boolean isForceVictory();

    /** @return The Forces present in this game. Can be empty, but not null. */
    Forces getForces();

    /**
     * Replaces the game's Forces with the given forces.
     *
     * @param forces The new Forces object to use
     */
    void setForces(Forces forces);

    // PLAYERS //////////////

    /**
     * @param id a player id
     *
     * @return the individual player assigned the id parameter.
     */
    @Nullable
    Player getPlayer(int id);

    /**
     * @param id A player ID
     *
     * @return True when there is a player for the given ID
     */
    default boolean hasPlayer(int id) {
        return getPlayer(id) != null;
    }

    /**
     * @return The current players as a list. Implementations should make sure that this list can be safely modified.
     */
    List<Player> getPlayersList();

    /**
     * Adds the given Player to the game with the given game-unique id. // TODO : Can this be made a default method?
     *
     * @param id     The game-unique id of this player
     * @param player The Player object
     */
    void addPlayer(int id, Player player);

    /**
     * Sets the given Player to the given game-unique id. // TODO : Is this method useful? Why not use addPlayer that
     * also sets single-blind info?
     *
     * @param id     The game-unique id of this player
     * @param player The Player object
     */
    void setPlayer(int id, Player player);

    /**
     * Removes the player with the id from the game.
     *
     * @param id The player id
     */
    void removePlayer(int id);

    /**
     * @return The current number of active players in the game. This includes observers but not ghosts.
     */
    int getNoOfPlayers();

    // TEAMS //////////////

    /**
     * @return The teams in the game. Implementations should make sure that this list can be safely modified.
     */
    List<Team> getTeams();

    /** @return The number of teams in the game. */
    int getNoOfTeams();

    void setupTeams();

    @Nullable
    default Team getTeamForPlayer(Player player) {
        for (Team team : getTeams()) {
            if (team.hasPlayer(player)) {
                return team;
            }
        }
        return null;
    }

    // region Units

    /**
     * @return The next free ID for InGameObjects (unit/entity/formation/others).
     */
    int getNextEntityId();

    /**
     * @return the number of units owned by the player, regardless of their status, as long as they are in the game.
     */
    default int getEntitiesOwnedBy(Player player) {
        return (int) getInGameObjects().stream().filter(o -> o.getOwnerId() == player.getId()).count();
    }

    /**
     * @return The InGameObject associated with the given id, if there is one.
     */
    default Optional<InGameObject> getInGameObject(int id) {
        return getInGameObjects().stream().filter(o -> o.getId() == id).findAny();
    }

    /**
     * @return The InGameObject from those that are out of game (destroyed, fled, never deployed) associated with the
     *       given id, if there is one.
     */
    default Optional<InGameObject> getOutOfGameUnit(int id) {
        return getGraveyard().stream().filter(o -> o.getId() == id).findAny();
    }

    /**
     * looks for an entity by id number even if out of the game
     */
    default InGameObject getEntityFromAllSources(int id) {
        return getInGameObject(id).orElse(getOutOfGameUnit(id).orElse(null));
    }

    /**
     * @return A list of all InGameObjects of this game. This list is copied and may be safely modified.
     */
    List<InGameObject> getInGameObjects();

    /**
     * @return A list of all InGameObjects of this game with the given ids. The returned list may be safely modified.
     */
    default List<InGameObject> getInGameObjects(Collection<Integer> idList) {
        return getInGameObjects().stream().filter(o -> idList.contains(o.getId())).collect(Collectors.toList());
    }

    /**
     * This is a Client-side method to replace or add units that are sent from the server. Adds the given units to the
     * list of units or objects in the current game. When a unit's ID is already present the currently assigned unit
     * will be replaced with the given new one.
     *
     * @param units The units to add or use as a replacement for current units.
     */
    void replaceUnits(List<InGameObject> units);

    /**
     * @return a list of units that are destroyed or otherwise no longer part of the game. These should have a reason
     *       for their removal set.
     */
    List<InGameObject> getGraveyard();

    // endregion

    // region Board

    /**
     * Sets the given board as the game's board with the given boardId, possibly replacing the former board of the same
     * id. This method is written with the idea that a game might have more than one board. Game's legacy methods of
     * setBoard() and getBoard() use the boardId 0. This method is meant as a server-side method.
     *
     * @param boardId (currently ignored) The boardId to assign to that board
     * @param board   The board to use
     */
    void setBoard(int boardId, Board board);

    /**
     * Returns the board with the given boardId or null if the game does not have a board of that boardId.
     *
     * @param boardId The board's ID
     *
     * @return The board with the given ID
     */
    @Nullable
    default Board getBoard(int boardId) {
        return getBoards().get(boardId);
    }

    /**
     * Returns the board of the given location or null if the game does not have a board of that location's boardId.
     *
     * @param boardLocation The location
     *
     * @return The board with the given ID
     */
    @Nullable
    default Board getBoard(BoardLocation boardLocation) {
        return getBoards().get(boardLocation.boardId());
    }

    /**
     * Returns the complete map of boardIds/boards the game uses. The returned map is an unmodifiable view of the game's
     * map, but not a deep copy, so changes to a board will affect the game.
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
     * Sets the given board as the game's board with the given boardId, possibly replacing the former board of the same
     * id. This method is written with the idea that a game might have more than one board. This method is meant as a
     * client-side method and may fire game events.
     *
     * @param boardId (currently ignored) The boardId to assign to that board
     * @param board   The board to use
     */
    void receiveBoard(int boardId, Board board);

    /**
     * Sets the given boards as the game's boards, replacing all previous boards. This method is written with the idea
     * that a game might have more than one board. This method is meant as a client-side method and may fire game
     * events.
     *
     * @param boards The new boards
     */
    void receiveBoards(Map<Integer, Board> boards);

    default boolean boardExists(int boardId) {
        return getBoards().containsKey(boardId);
    }

    default void connectBoards(int lowerBoardId, int higherBoardId, Coords coords) {
        if (!boardExists(lowerBoardId) || !boardExists(higherBoardId)) {
            LOGGER.error("Can't set an enclosing board for non-existent boards.");
            return;
        }
        Board lowerBoard = getBoard(lowerBoardId);
        Board higherBoard = getBoard(higherBoardId);
        if ((lowerBoard.isLowAltitude() && !higherBoard.isSpace()) || (lowerBoard.isGround()
              && !higherBoard.isLowAltitude())
              || lowerBoard.isSpace() || higherBoard.isGround()) {
            LOGGER.error("Can only enclose a ground map in an atmosphere map or an atmosphere map in a space map.");
            return;
        }
        if (!higherBoard.contains(coords)) {
            LOGGER.error("Higher map doesn't contain the given coords.");
            return;
        }
        lowerBoard.setEnclosingBoard(higherBoardId);
        higherBoard.setEmbeddedBoard(lowerBoardId, coords);
    }

    /**
     * Returns true when the given Board has an (existing) enclosing Board, i.e. when the given Board occupies one or
     * more hexes of another board of a larger scale. E.g., this is true when there's an atmospheric map for a ground
     * map or a space map for an atmospheric map.
     *
     * @param boardId The board's ID
     *
     * @return True when the board is enclosed within another board
     */
    default boolean hasEnclosingBoard(int boardId) {
        return boardExists(boardId) && boardExists(getBoard(boardId).getEnclosingBoardId());
    }

    default Optional<Board> getEnclosingBoard(int boardId) {
        if (boardExists(boardId) && hasEnclosingBoard(boardId)) {
            return Optional.of(getBoard(getBoard(boardId).getEnclosingBoardId()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * @return True when both given units are not null and reside on the same board. Only checks the board IDs, not the
     *       positions (which could be null or invalid).
     */
    default boolean onTheSameBoard(@Nullable Targetable entity1, @Nullable Targetable entity2) {
        return (entity1 != null) && (entity2 != null) && (entity1.getBoardId() == entity2.getBoardId());
    }

    /**
     * Returns true when both given units or objects are on directly connected, "adjacent" boards, such as a ground map
     * and its enclosing atmospheric map. Returns false if they are on connected maps that are one or more other maps
     * "apart", such as a ground map and a connected high-altitude map or two ground maps enclosed within a single
     * atmospheric map. Also returns false when the two are on unconnected maps.
     *
     * @param entity1 The first unit or object to test
     * @param entity2 The second unit or object to test
     *
     * @return True when both units or objects are on directly connected boards
     */
    default boolean onDirectlyConnectedBoards(@Nullable Targetable entity1, @Nullable Targetable entity2) {
        if ((entity1 != null) && (entity2 != null)) {
            Board board1 = getBoard(entity1);
            Board board2 = getBoard(entity2);
            return (board1.getBoardId() != -1) && (board2.getBoardId() != -1) &&
                  ((board1.getEnclosingBoardId() == board2.getBoardId()) ||
                        (board2.getEnclosingBoardId() == board1.getBoardId()));
        } else {
            return false;
        }
    }

    /**
     * @param targetable The targetable to check
     *
     * @return The board ID of the board that the given Targetable is on.
     */
    default Board getBoard(Targetable targetable) {
        return getBoard(targetable.getBoardId());
    }

    /**
     * Returns true if the given targetable is not null and has a position that exists, i.e. its position and board ID
     * are on an actual board. When this returns true, calling getHex for its location will return a non-null hex.
     *
     * @param targetable The targetable to check
     *
     * @return True when its location exists and is on a board
     */
    default boolean hasBoardLocationOf(@Nullable Targetable targetable) {
        return (targetable != null) && hasBoardLocation(targetable.getPosition(), targetable.getBoardId());
    }

    /**
     * Returns true if the given boardLocation really exists, i.e. is not null, its board ID is an actual board in the
     * game and its coords are contained in that board. This means that a hex can be found for this boardLocation.
     *
     * @param boardLocation The location to test
     *
     * @return True when the location exists and is on a board
     */
    default boolean hasBoardLocation(@Nullable BoardLocation boardLocation) {
        return (boardLocation != null) && !boardLocation.isNoLocation()
              && hasBoardLocation(boardLocation.coords(), boardLocation.boardId());
    }

    /**
     * Returns true if the given coords and boardID really exist, are not null, the board ID is an actual board in the
     * game and the coords are contained in that board. This means that a hex can be found for these values, and it will
     * not be null (unless the board data is corrupted).
     *
     * @param coords  The coords to test
     * @param boardId The board ID to test
     *
     * @return True when the location exists and is on a board
     */
    default boolean hasBoardLocation(@Nullable Coords coords, int boardId) {
        return hasBoard(boardId) && getBoard(boardId).contains(coords);
    }

    /**
     * Returns true if the given boardLocation points to an existing board, i.e. its board ID is an actual board in the
     * game. Does not check the location's coords.
     *
     * @param boardLocation The location to test
     *
     * @return True when the location is not null and its board exists
     */
    default boolean hasBoard(@Nullable BoardLocation boardLocation) {
        return (boardLocation != null) && !boardLocation.isNoLocation() && hasBoard(boardLocation.boardId());
    }

    /**
     * Returns true if the given bboard ID is an actual board in the game.
     *
     * @param boardId The board ID to test
     *
     * @return True when the board exists
     */
    default boolean hasBoard(int boardId) {
        return getBoards().containsKey(boardId);
    }

    /**
     * Returns the hex for the given location, i.e. the hex at the coords and on the board ID of the given location.
     * Returns null when the board doesn't exist or when there is no hex at the given coords. The various
     * hasBoardLocation() methods can be used to make sure that a non-null hex can be found.
     *
     * @param boardLocation The location to query
     *
     * @return The hex at the given location
     *
     * @see #hasBoardLocation(Coords, int)
     * @see #hasBoardLocationOf(Targetable)
     */
    default @Nullable Hex getHex(BoardLocation boardLocation) {
        return hasBoardLocation(boardLocation) ? getBoard(boardLocation).getHex(boardLocation.coords()) : null;
    }

    /**
     * Returns the hex for the given location, i.e. the hex at the coords and on the board of the given ID. Returns null
     * when the board doesn't exist or when there is no hex at the given coords.
     *
     * @param coords  The location's coords
     * @param boardId The location's board ID
     *
     * @return The hex at the given location
     *
     * @see #hasBoardLocation(Coords, int)
     * @see #hasBoardLocationOf(Targetable)
     */
    default @Nullable Hex getHex(Coords coords, int boardId) {
        return hasBoardLocation(coords, boardId) ? getBoard(boardId).getHex(coords) : null;
    }

    /**
     * Returns the hex that the given Targetable is at, i.e. the hex at the position and on the board ID of the given
     * Targetable. Returns null when targetable is null, the board doesn't exist or when there is no hex at its position
     * or the position is null.
     *
     * @param targetable The unit or object
     *
     * @return The hex at the position of the Targetable
     *
     * @see #hasBoardLocation(Coords, int)
     * @see #hasBoardLocationOf(Targetable)
     */
    default @Nullable Hex getHexOf(Targetable targetable) {
        return targetable == null ? null : getHex(targetable.getPosition(), targetable.getBoardId());
    }

    /**
     * Returns true when the given location exists in this game (i.e., is part of a board) and the board it is on is a
     * space board, including high-altitude boards (even atmospheric hexes on such a board).
     *
     * @param boardLocation The location to test
     *
     * @return True when the location is part of a space board
     */
    default boolean isOnSpaceMap(@Nullable BoardLocation boardLocation) {
        return hasBoardLocation(boardLocation) && getBoard(boardLocation).isSpace();
    }

    /**
     * @param boardLocation The location to check
     *
     * @return True when the location is not null and a valid ground board location
     */
    default boolean isOnGroundMap(@Nullable BoardLocation boardLocation) {
        return hasBoardLocation(boardLocation) && getBoard(boardLocation).isGround();
    }

    /**
     * @param targetable The target to check
     *
     * @return True when the targetable is considered to be on a ground board (not an atmospheric or space board). This
     *       is true for units that are deployed either offboard or on a valid ground board location; for other targets
     *       such as hexes or buildings, this is true when on a valid ground board location. This is safe to call
     *       regardless of what the position or board ID of the targetable might be.
     */
    default boolean isOnGroundMap(Targetable targetable) {
        // offboard artillery, when deployed, can only be "on" a ground map
        if (targetable instanceof Entity entity) {
            return entity.isDeployed() && (entity.isOffBoard() || isOnGroundMap(targetable.getBoardLocation()));
        } else {
            return isOnGroundMap(targetable.getBoardLocation());
        }
    }

    default boolean isOnSpaceMap(Targetable targetable) {
        return isOnSpaceMap(targetable.getBoardLocation());
    }

    default boolean hasConnectedBoard(Board board) {
        return hasEnclosingBoard(board.getBoardId()) || !board.embeddedBoardCoords().isEmpty();
    }

    default boolean isOnAtmosphericMap(BoardLocation boardLocation) {
        return hasBoardLocation(boardLocation) && getBoard(boardLocation).isLowAltitude();
    }

    /**
     * Returns true when the given targetable is in a hex of an atmospheric board. Returns false if it is null or has an
     * invalid position (invalid board ID or position not on the board).
     *
     * @param targetable The object/unit/target to check
     *
     * @return True when the targetable is on an atmospheric board
     */
    default boolean isOnAtmosphericMap(Targetable targetable) {
        return (targetable != null) && isOnAtmosphericMap(targetable.getBoardLocation());
    }

    /**
     * Returns true when both given units or objects are on boards that are connected at least through a common high
     * altitude map. For two connected maps, an aerospace fighter can reach one from the other, traversing atmospheric
     * and/or high atmospheric maps. Also returns true when both are on the same board.
     * <p>
     * When two maps are not connected they're part of different hierarchies of maps and therefore, nothing happening on
     * one can influence the other. It is possible to set up games of such unrelated map clusters, but it is not
     * advisable. Such games could just as well be played separately from each other and suffer a lower chance of MM
     * crashing both...
     *
     * @param entity1 The first unit or object to test
     * @param entity2 The second unit or object to test
     *
     * @return True when both units or objects are on connected boards (or the same board)
     */
    default boolean onConnectedBoards(@Nullable Targetable entity1, @Nullable Targetable entity2) {
        return (entity1 != null) && (entity2 != null) && areConnectedBoards(entity1.getBoardId(), entity2.getBoardId());
    }

    /**
     * @return True when this game has at least one space board (including high-altitude) and at least one non-space
     *       board (low altitude or ground).
     */
    default boolean hasSpaceAndAtmosphericBoards() {
        return hasSpaceBoard() && hasNonSpaceBoard();
    }

    /**
     * @return True when this game has at least one space board (including high-altitude).
     */
    default boolean hasSpaceBoard() {
        return getBoards().values().stream().anyMatch(Board::isSpace);
    }

    /**
     * @return True when this game has at least one non-space board (low altitude or ground).
     */
    default boolean hasNonSpaceBoard() {
        return getBoards().values().stream().anyMatch(b -> b.isGround() || b.isLowAltitude());
    }

    /**
     * @return True when this game has at least one ground board.
     */
    default boolean hasGroundBoard() {
        return getBoards().values().stream().anyMatch(Board::isGround);
    }

    /**
     * Returns true when both given boards are connected at least through a common high altitude map. When two boards
     * are connected, a fighter unit can reach one from the other, traversing atmospheric and/or high atmospheric maps.
     * Also returns true if the boards are one and the same.
     * <p>
     * When two maps are not connected they're part of different hierarchies of maps and therefore, nothing happening on
     * one can influence the other. It is possible to set up games of such unrelated map clusters, but it is not
     * advisable. Such games could just as well be played separately from each other and suffer a lower chance of MM
     * crashing both...
     *
     * @param boardId1 The first board ID
     * @param boardId2 The second board ID
     *
     * @return True when the given boards are connected at least through a common high atmosphere map
     */
    default boolean areConnectedBoards(int boardId1, int boardId2) {
        List<Integer> hierarchy1 = getAllEnclosingBoards(boardId1);
        hierarchy1.add(boardId1);
        List<Integer> hierarchy2 = getAllEnclosingBoards(boardId2);
        hierarchy2.add(boardId2);
        return !Collections.disjoint(hierarchy1, hierarchy2);
    }

    /**
     * Returns a list of IDs of all enclosing boards of the given board. These are at most two other boards; for a
     * ground board, the enclosing atmospheric board (if present) and that one's enclosing high-altitude map (if
     * present). For an atmospheric map, this will be at most the enclosing high-altitude map (if present); for any
     * space map, the returned List will be empty.
     *
     * @param boardId The board to find enclosed boards for
     *
     * @return All enclosing boards in the hierarchy of the given board (between zero and two boards)
     */
    default List<Integer> getAllEnclosingBoards(int boardId) {
        List<Integer> allEnclosingBoards = new ArrayList<>();
        if (hasEnclosingBoard(boardId)) {
            Board board = getBoard(boardId);
            Board enclosingBoard = getEnclosingBoard(board);
            allEnclosingBoards.add(enclosingBoard.getBoardId());
            if (hasEnclosingBoard(enclosingBoard.getBoardId())) {
                Board secondEnclosingBoard = getEnclosingBoard(enclosingBoard);
                allEnclosingBoards.add(secondEnclosingBoard.getBoardId());
            }
        }
        return allEnclosingBoards;
    }

    default @Nullable Board getEnclosingBoard(Board board) {
        return getBoard(board.getEnclosingBoardId());
    }

    /**
     * Returns the common enclosing board of the two given units/targets. For two units on the same board, this board is
     * returned. When one unit is on a higher board and the other on a connected lower board (ground is lower than
     * atmospheric is lower than space), the higher of the two is returned. When two units are on ground boards with an
     * atmospheric board connecting the two, the atmospheric board is returned. For a S2O or O2S attack situation, the
     * space board is returned. If any of the two units is null, is not deployed or otherwise off board, not on
     * connected boards, the return value is empty.
     *
     * @param object1 The first unit or object
     * @param object2 The second unit or object
     *
     * @return The "lowest" common enclosing board, if any
     */
    default Optional<Board> commonEnclosingBoard(@Nullable Targetable object1, @Nullable Targetable object2) {
        if ((object1 == null) || (object2 == null) || !hasBoardLocationOf(object1) || !hasBoardLocationOf(object2)) {
            return Optional.empty();
        } else {
            List<Integer> hierarchy1 = getAllEnclosingBoards(object1.getBoardId());
            hierarchy1.add(object1.getBoardId());
            List<Integer> hierarchy2 = getAllEnclosingBoards(object2.getBoardId());
            hierarchy2.add(object2.getBoardId());
            // Keep only shared boards of the two hierarchies; the "lowest" of these is the correct one
            hierarchy1.retainAll(hierarchy2);
            if (hierarchy1.isEmpty()) {
                return Optional.empty();
            } else {
                hierarchy1.sort(Comparator.comparingInt(id -> getBoard(id).getBoardType().orderValue()));
                return Optional.of(getBoard(hierarchy1.get(0)));
            }
        }
    }

    // endregion

    /**
     * Returns a new ReportEntry with the given report message ID. The ReportEntry subclass returned depends on the
     * implementation in the IGame subclass.
     *
     * @param messageId The message ID from report-messages.properties
     *
     * @return A new report of an appropriate type and message
     */
    ReportEntry getNewReport(int messageId);

    // region Scripted Events

    /**
     * @return All scripted events present in this game. Note that these will typically only be present on the Server
     *       side and the Clients will only receive the results of those events.
     */
    @ServerOnly
    List<TriggeredEvent> scriptedEvents();

    /**
     * Add a scripted event to this game's scripted events list.
     *
     * @param event The new event to add
     */
    default void addScriptedEvent(TriggeredEvent event) {}

    // endregion

    default Map<Integer, Integer> getTeamByPlayer() {
        Map<Integer, Integer> teamByPlayer = new HashMap<>();
        for (var player : getPlayersList()) {
            teamByPlayer.put(player.getId(), player.getTeam());
        }
        return teamByPlayer;
    }
}
