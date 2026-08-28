/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.server.totalWarfare;

import megamek.common.Hex;
import megamek.common.OffBoardDirection;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.GameTurn;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.turns.SpecificEntityTurn;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.TrainLayout;
import megamek.logging.MMLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Handles unit deployment for the TWGameManager (not minefields or arty auto hexes)
 */
public class DeploymentProcessor extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(DeploymentProcessor.class);

    DeploymentProcessor(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Receives a deployment packet from a Client connection. If valid, executes it and ends the current turn.
     */
    void receiveDeployment(Packet packet,
                           int connId) throws InvalidPacketDataException {
        Entity entity = getGame().getEntity(packet.getIntValue(0));

        if (entity == null) {
            LOGGER.error("Entity received was invalid");
            return;
        }

        Coords coords = packet.getCoords(1);
        int boardId = packet.getIntValue(2);
        int nFacing = packet.getIntValue(3);
        int elevation = packet.getIntValue(4);

        // Handle units that deploy loaded with other units.
        int loadedCount = packet.getIntValue(5);
        Vector<Entity> loadVector = new Vector<>();
        for (int i = 0; i < loadedCount; i++) {
            int loadedId = packet.getIntValue(7 + i);
            loadVector.addElement(getGame().getEntity(loadedId));
        }

        // is this the right phase?
        if (!getGame().getPhase().isDeployment()) {
            LOGGER.error("Server got deployment packet in wrong phase");
            return;
        }

        // can this player/entity act right now?
        final boolean assaultDrop = packet.getBooleanValue(6);
        // can this player/entity act right now?
        GameTurn turn = getGame().getTurn();

        if (getGame().getPhase().isSimultaneous(getGame())) {
            turn = getGame().getTurnForPlayer(connId);
        }

        boolean isLegalLocation = getGame().hasBoardLocation(coords, boardId)
                                  && getGame().getBoard(boardId).isLegalDeployment(coords, entity)
                                  && isLegalTrainFootprint(entity, coords, boardId, nFacing);

        if ((turn == null) || !turn.isValid(connId, entity, getGame())
            // FIXME: The combination with assault drop and the assault drop check dont look right:
            || !(isLegalLocation
                 || (assaultDrop && getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_ASSAULT_DROP)
                     && entity.canAssaultDrop()))) {

            String msg = "server got invalid deployment packet from connection " + connId;
            msg += ", Entity: " + entity.getShortName();
            LOGGER.error(msg);
            gameManager.send(connId, gameManager.getPacketHelper().createTurnListPacket());

            if (turn != null) {
                gameManager.send(connId, gameManager.getPacketHelper().createTurnIndexPacket(turn.playerId()));
            }
            return;
        }

        // looks like mostly everything's okay
        boolean setDone = true;
        DeploymentServerHelper deploymentServerHelper = new DeploymentServerHelper(gameManager);
        deploymentServerHelper.processDeployment(entity,
                                                 coords,
                                                 boardId,
                                                 nFacing,
                                                 elevation,
                                                 loadVector,
                                                 assaultDrop,
                                                 setDone);
        Hex hex = gameManager.getGame().getBoard(boardId).getHex(coords);
        addReport(gameManager.doSetLocationsExposure(entity, hex, false, entity.getElevation()));

        // Update Aero sensors for a space or atmospheric game
        if (entity instanceof IAero aero) {
            aero.updateSensorOptions();
        }

        // Update visibility indications if using double-blind.
        if (gameManager.doBlind()) {
            gameManager.updateVisibilityIndicator(null);
        }
        TWGameManager.datasetLogger.append(getGame(), true);
        gameManager.endCurrentTurn(entity);
    }

    /**
     * Used when an Entity that was loaded in another Entity in the Lounge is unloaded during deployment.
     *
     * @param packet the packet to be processed
     * @param connId the id for connection that received the packet.
     */
    void receiveDeploymentUnload(Packet packet,
                                 int connId) throws InvalidPacketDataException {
        Entity loader = getGame().getEntity(packet.getIntValue(0));
        Entity loaded = getGame().getEntity(packet.getIntValue(1));

        if (loader == null) {
            LOGGER.error("Received bad entity for loader unload");
            return;
        }

        if (loaded == null) {
            LOGGER.error("Received bad entity for loaded unload.");
            return;
        }

        if (!getGame().getPhase().isDeployment()) {
            String msg = "server received deployment unload packet " +
                         "outside of deployment phase from connection " +
                         connId;
            msg += ", Entity: " + loader.getShortName();
            LOGGER.error(msg);
            return;
        }

        // can this player/entity act right now?
        GameTurn turn = getGame().getTurn();
        if (getGame().getPhase().isSimultaneous(getGame())) {
            turn = getGame().getTurnForPlayer(connId);
        }

        if ((turn == null) || !turn.isValid(connId, loader, getGame())) {
            String msg = "server got invalid deployment unload packet from connection " + connId;
            msg += ", Entity: " + loader.getShortName();
            LOGGER.error(msg);
            gameManager.send(connId, gameManager.getPacketHelper().createTurnListPacket());
            gameManager.send(connId, gameManager.getPacketHelper().createTurnIndexPacket(connId));
            return;
        }

        // Unload and call entityUpdate
        gameManager.unloadUnit(loader, loaded, null, 0, 0, false, true);

        // Need to update the loader
        gameManager.entityUpdate(loader.getId());

        // Now need to add a turn for the unloaded unit, to be taken immediately
        // Turn forced to be immediate to avoid messy turn ordering issues
        // (aka, how do we add the turn with individual initiative?)
        getGame().insertTurnAfter(new SpecificEntityTurn(loaded.getOwnerId(), loaded.getId()),
                                  getGame().getTurnIndex() - 1);
        gameManager.send(gameManager.getPacketHelper().createTurnListPacket());
    }

    /**
     * Whether every hex a train would occupy is a legal deployment hex, not just the tractor's own.
     * <p>
     * The client refuses such a placement before sending it, but the placement arrives from the client, so the server
     * has to decide for itself rather than trust it. A unit towing nothing is always its own footprint.
     * </p>
     *
     * @param tractor the unit being deployed
     * @param coords  the hex it is being placed in
     * @param boardId the board it is being placed on
     * @param facing  the facing it is being placed with
     * @return true when the whole train fits in the deployment zone
     */
    private boolean isLegalTrainFootprint(Entity tractor,
                                          Coords coords,
                                          int boardId,
                                          int facing) {
        if (tractor.getAllTowedUnits().isEmpty()) {
            return true;
        }

        Board board = getGame().getBoard(boardId);
        for (Coords trainHex : TrainLayout.deploymentFootprint(getGame(), tractor, coords, facing)) {
            if (!getGame().hasBoardLocation(trainHex, boardId) || !board.isLegalDeployment(trainHex, tractor)) {
                LOGGER.warn("[Train] rejected deployment of {} at {} facing {}: trailer hex {} is not a legal "
                            + "deployment hex", tractor.getDisplayName(), coords, facing, trainHex);
                return false;
            }
        }
        return true;
    }

    /**
     * Gives every trailer the off board edge and distance of the tractor towing it, so a train goes off board as a
     * whole rather than being split between the board and the map edge.
     * <p>
     * Runs before deployment turns are generated. Without it a trailer behind an off board tractor would never be
     * placed at all: an off board tractor takes no deployment turn, so nothing would run to position its trailers,
     * and the trailer no longer deploys itself. See RFE #8506.
     * </p>
     */
    void followTractorsOffBoard() {
        List<String> trailersFollowingOffBoard = new ArrayList<>();

        for (Entity tractor : getGame().getEntitiesVector()) {
            if (!tractor.isOffBoard()
                || tractor.getAllTowedUnits().isEmpty()
                || (tractor.getOffBoardDistance() <= 0)
                || (tractor.getOffBoardDirection() == OffBoardDirection.NONE)) {
                continue;
            }
            for (int towedId : tractor.getAllTowedUnits()) {
                Entity trailer = getGame().getEntity(towedId);
                if ((trailer == null) || trailer.isOffBoard() || trailer.isDeployed()) {
                    continue;
                }
                trailer.setOffBoard(tractor.getOffBoardDistance(), tractor.getOffBoardDirection());
                trailersFollowingOffBoard.add(trailer.getDisplayName());
            }
        }

        if (!trailersFollowingOffBoard.isEmpty()) {
            LOGGER.info("[Train] {} trailer(s) follow their tractor off board: {}",
                        trailersFollowingOffBoard.size(), String.join(", ", trailersFollowingOffBoard));
        }
    }
}
