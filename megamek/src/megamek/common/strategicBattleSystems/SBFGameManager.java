/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.strategicBattleSystems;

import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.net.packets.Packet;
import megamek.server.IGameManager;
import megamek.server.Server;
import megamek.server.commands.ServerCommand;
import org.apache.logging.log4j.LogManager;

import java.util.Collections;
import java.util.List;

/**
 * This class manages an SBF game on the server side. As of 2024, this is under construction.
 */
public class SBFGameManager implements IGameManager {

    private IGame game;

    @Override
    public IGame getGame() {
        return game;
    }

    @Override
    public void setGame(IGame g) {
        if (!(g instanceof SBFGame)) {
            LogManager.getLogger().error("Attempted to set game to incorrect class.");
            return;
        }
        game = g;
    }

    @Override
    public void resetGame() {

    }

    @Override
    public void disconnect(Player player) {

    }

    @Override
    public void sendCurrentInfo(int connId) {

    }

    @Override
    public void saveGame(String fileName) {

    }

    @Override
    public void sendSaveGame(int connId, String fileName, String localPath) {

    }

    @Override
    public void removeAllEntitiesOwnedBy(Player player) {

    }

    @Override
    public void handlePacket(int connId, Packet packet) {

    }

    @Override
    public void handleCfrPacket(Server.ReceivedPacket rp) {

    }

    @Override
    public void requestGameMaster(Player player) {

    }

    @Override
    public void requestTeamChange(int teamId, Player player) {

    }

    @Override
    public List<ServerCommand> getCommandList(Server server) {
        return Collections.emptyList();
    }

    @Override
    public void addReport(Report r) {

    }

    @Override
    public void calculatePlayerInitialCounts() {

    }
}
