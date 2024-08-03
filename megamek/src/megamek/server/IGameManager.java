/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.server;

import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.ReportEntry;
import megamek.common.net.packets.Packet;
import megamek.server.commands.ServerCommand;

import java.util.List;

/**
 * Provides common interface for the server to interact with different types of games. Currently only used
 * for Total Warfare scale, but allows expansion to BattleFore or Alpha Strike.
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
     * Resets the {@link IGame game} instance. Resetting the game removes all content and returns to
     * the lobby but keeps connected players.
     */
    void resetGame();

    /**
     * Handles housekeeping for a disconnected player. Removes player from the game in the lounge
     * or victory phase, or during other phases if the player has no units. Otherwise the player
     * becomes a ghost.
     *
     * @param player The player that disconnected.
     */
    void disconnect(Player player);

    /**
     * Sends a player all information they need to update their Client game state to the GameManager's game state.
     * This should always include units, forces, map info, active attacks, round reports, among others.
     * This is triggered when a player first connects to the server. When the game is past the lobby phase,
     * this also triggers generating and sending a current player turn or advancing the phase if there are
     * no remaining turns; in other words, this sets the game in motion when the first player connects. A
     * game starts past the lobby phase when it is loaded from a save.
     *
     * @param connId The id of the player to update
     */
    void sendCurrentInfo(int connId);

    /**
     * Saves the game server-side. Will announce the save (or error) in chat.
     *
     * @param fileName The filename to use
     */
    default void saveGame(String fileName) {
        saveGame(fileName, true);
    }

    /**
     * Saves the game server-side. Will announce the save (or error) in chat if the given sendChat
     * is true.
     *
     * @param fileName The filename to use
     * @param sendChat When true, the saving (or error) is announced in chat
     */
    void saveGame(String fileName, boolean sendChat);

    /**
     * save the game and send it to the specified connection
     *
     * @param connId The connection id to send to
     * @param fileName The filename to use
     * @param localPath The path to the file to be used on the client
     */
    void sendSaveGame(int connId, String fileName, String localPath);

    void removeAllEntitiesOwnedBy(Player player);

    /**
     * Handles all incoming packets. When overriding this, super() should normally be called to have the
     * base implementation of AbstractGameManager handle packets.
     *
     * @param connId The connection ID = player ID the packet came from
     * @param packet The packet to process
     */
    void handlePacket(int connId, Packet packet);

    /**
     * Handle CFR packets.
     * //TODO: This concerns multi-threaded code and should be centralized!
     * @param rp the CFR packet returned from a client
     */
    void handleCfrPacket(Server.ReceivedPacket rp);

    void requestGameMaster(Player player);

    void requestTeamChange(int teamId, Player player);

    List<ServerCommand> getCommandList(Server server);

    void addReport(ReportEntry r);

    /**
     * Calculates and sets any initial unit counts and BV/PV for all players, and thus should only be called at the
     * start of a game. The initital values are supposed to be stored for later comparison so that BV or unit
     * losses over the course of the game can be calculated.
     */
    void calculatePlayerInitialCounts();

}
