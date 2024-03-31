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
package megamek.server;

import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.server.commands.ServerCommand;
import org.apache.logging.log4j.LogManager;

import java.util.Collections;
import java.util.List;

/**
 * This class manages an SBF game on the server side. As of 2024, this is under construction.
 */
public class SBFGameManager extends AbstractGameManager {

    private SBFGame game;

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
        game = (SBFGame) g;
    }

    @Override
    public void resetGame() {

    }

    @Override
    public void disconnect(Player player) {

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

    @Override
    public void sendCurrentInfo(int connId) {
        send(connId, packetHelper.createGameSettingsPacket());

        Player player = getGame().getPlayer(connId);
        if (null != player) {
            send(connId, new Packet(PacketCommand.SENDING_MINEFIELDS, player.getMinefields()));

            if (getGame().getPhase().isLounge()) {
//                send(connId, createMapSettingsPacket());
//                send(createMapSizesPacket());
                // LOUNGE triggers a Game.reset() on the client!
                // Send Entities *after* the Lounge Phase Change
                send(connId, packetHelper.createPhaseChangePacket());
//                if (doBlind()) {
//                    send(connId, createFilteredFullEntitiesPacket(player, null));
//                } else {
//                    send(connId, createFullEntitiesPacket());
//                }
            } else {
                send(connId, packetHelper.createCurrentRoundNumberPacket());
                send(connId, packetHelper.createBoardsPacket());
//                send(connId, createAllReportsPacket(player));
//
//                // Send entities *before* other phase changes.
//                if (doBlind()) {
//                    send(connId, createFilteredFullEntitiesPacket(player, null));
//                } else {
//                    send(connId, createFullEntitiesPacket());
//                }
//
//                setPlayerDone(player, getGame().getEntitiesOwnedBy(player) <= 0);
                send(connId, packetHelper.createPhaseChangePacket());
            }

            send(connId, packetHelper.createPlanetaryConditionsPacket());
//
            if (game.getPhase().isFiring() || game.getPhase().isTargeting()
                    || game.getPhase().isOffboard() || game.getPhase().isPhysical()) {
                // can't go above, need board to have been sent
//                send(connId, packetHelper.createAttackPacket(getGame().getActionsVector(), false));
//                send(connId, packetHelper.createAttackPacket(getGame().getChargesVector(), true));
//                send(connId, packetHelper.createAttackPacket(getGame().getRamsVector(), true));
//                send(connId, packetHelper.createAttackPacket(getGame().getTeleMissileAttacksVector(), true));
            }
//
            if (getGame().getPhase().hasTurns() && getGame().hasMoreTurns()) {
//                send(connId, createTurnVectorPacket());
//                send(connId, createTurnIndexPacket(connId));
            } else if (!getGame().getPhase().isLounge() && !getGame().getPhase().isStartingScenario()) {
                endCurrentPhase();
            }
//
//            send(connId, createArtilleryPacket(player));
//            send(connId, createFlarePacket());
//            send(connId, createSpecialHexDisplayPacket(connId));
//            send(connId, new Packet(PacketCommand.PRINCESS_SETTINGS, getGame().getBotSettings()));
        }
    }

    @Override
    protected void endCurrentPhase() {
        // stub
    }

    @Override
    protected void changePhase(GamePhase newPhase) {
        //stub
    }
}
