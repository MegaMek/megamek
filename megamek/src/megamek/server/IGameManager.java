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

package megamek.server;

import java.util.List;

import megamek.common.Player;
import megamek.common.game.IGame;
import megamek.common.interfaces.ReportEntry;
import megamek.common.net.packets.Packet;
import megamek.server.commands.ServerCommand;

/**
 * Provides common interface for the server to interact with different types of games. Currently only used for Total
 * Warfare scale, but allows expansion to BattleFore or Alpha Strike.
 */
public interface IGameManager {

    /**
     * @return The current {@link IGame game} instance.
     */
    IGame getGame();

    /**
     * Sets the current {@link IGame game} instance.
     */
    void setGame(IGame g);

    /**
     * Resets the {@link IGame game} instance. Resetting the game removes all content and returns to the lobby but keeps
     * connected players.
     */
    void resetGame();

    /**
     * Handles housekeeping for a disconnected player. Removes player from the game in the lounge or victory phase, or
     * during other phases if the player has no units. Otherwise, the player becomes a ghost.
     *
     * @param player The player that disconnected.
     */
    void disconnect(Player player);

    /**
     * Sends a player all information they need to update their Client game state to the GameManager's game state. This
     * should always include units, forces, map info, active attacks, round reports, among others. This is triggered
     * when a player first connects to the server. When the game is past the lobby phase, this also triggers generating
     * and sending a current player turn or advancing the phase if there are no remaining turns; in other words, this
     * sets the game in motion when the first player connects. A game starts past the lobby phase when it is loaded from
     * a save.
     *
     * @param connId The id of the player to update
     */
    void sendCurrentInfo(int connId);

    /**
     * Sends the given packet to all connections (all connected Clients = players).
     */
    void send(Packet packet);

    /**
     * Sends the given packet to the given connection (= player ID).
     */
    void send(int connId, Packet p);

    /**
     * Saves the game server-side. Will announce the save (or error) in chat.
     *
     * @param fileName The filename to use
     */
    default void saveGame(String fileName) {
        saveGame(fileName, true);
    }

    /**
     * Saves the game server-side. Will announce the save (or error) in chat if the given sendChat is true.
     *
     * @param fileName The filename to use
     * @param sendChat When true, the saving (or error) is announced in chat
     */
    void saveGame(String fileName, boolean sendChat);

    /**
     * save the game and send it to the specified connection
     *
     * @param connId    The connection id to send to
     * @param fileName  The filename to use
     * @param localPath The path to the file to be used on the client
     */
    void sendSaveGame(int connId, String fileName, String localPath);

    void removeAllEntitiesOwnedBy(Player player);

    /**
     * Handles all incoming packets. When overriding this, super() should normally be called to have the base
     * implementation of AbstractGameManager handle packets.
     *
     * @param connId The connection ID = player ID the packet came from
     * @param packet The packet to process
     */
    void handlePacket(int connId, Packet packet);

    /**
     * Handle CFR packets. //TODO: This concerns multi-threaded code and should be centralized!
     *
     * @param rp the CFR packet returned from a client
     */
    void handleCfrPacket(Server.ReceivedPacket rp);

    void requestGameMaster(Player player);

    List<ServerCommand> getCommandList(Server server);

    void addReport(ReportEntry r);

    /**
     * Calculates and sets any initial unit counts and BV/PV for all players, and thus should only be called at the
     * start of a game. The initial values are supposed to be stored for later comparison so that BV or unit losses over
     * the course of the game can be calculated.
     */
    void calculatePlayerInitialCounts();

    /**
     * Requests a team change for a player.
     *
     * @param teamID ID of the team the player will be passed to
     * @param player player
     */
    void requestTeamChangeForPlayer(int teamID, Player player);
}
