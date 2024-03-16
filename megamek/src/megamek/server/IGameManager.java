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
import megamek.common.Report;
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
     * Resets the {@link IGame game} instance.
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
     * @param connId The id of the player to update
     * Sends a player the info they need to look at the current phase. This is
     * triggered when a player first connects to the server.
     */
    void sendCurrentInfo(int connId);

    void saveGame(String fileName);

    void sendSaveGame(int connId, String fileName, String localPath);

    void removeAllEntitiesOwnedBy(Player player);

    void handlePacket(int connId, Packet packet);

    void handleCfrPacket(Server.ReceivedPacket rp);

    void requestGameMaster(Player player);

    void requestTeamChange(int teamId, Player player);

    List<ServerCommand> getCommandList(Server server);

    void addReport(Report r);
}
