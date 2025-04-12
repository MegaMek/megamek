/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.server.totalwarfare;

import megamek.common.*;
import megamek.common.enums.BuildingType;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.Atmosphere;
import megamek.logging.MMLogger;

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
    void receiveDeployment(Packet packet, int connId) {
        Entity entity = getGame().getEntity(packet.getIntValue(0));

        if (entity == null) {
            LOGGER.error("Entity received was invalid");
            return;
        }

        Coords coords = (Coords) packet.getObject(1);
        int boardId = (int) packet.getObject(2);
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
                && getGame().getBoard(boardId).isLegalDeployment(coords, entity);

        if ((turn == null) || !turn.isValid(connId, entity, getGame())
                  // FIXME: The combination with assaultdrop and the assaultdrop check dont look right:
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
        processDeployment(entity, coords, boardId, nFacing, elevation, loadVector, assaultDrop);

        // Update Aero sensors for a space or atmospheric game
        if (entity instanceof IAero aero) {
            aero.updateSensorOptions();
        }

        // Update visibility indications if using double blind.
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
    void receiveDeploymentUnload(Packet packet, int connId) {
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
        getGame().insertTurnAfter(new SpecificEntityTurn(loaded.getOwnerId(), loaded.getId()), getGame().getTurnIndex() - 1);
        // getGame().insertNextTurn(new GameTurn.SpecificEntityTurn(
        // loaded.getOwnerId(), loaded.getId()));
        gameManager.send(gameManager.getPacketHelper().createTurnListPacket());
    }

    /**
     * Process a deployment packet by... deploying the entity! We load any other specified entities inside of it too.
     * Also, check that the deployment is valid.
     */
    private void processDeployment(Entity entity, Coords coords, int boardId, int nFacing, int elevation,
          Vector<Entity> loadVector,
          boolean assaultDrop) {
        for (Entity loaded : loadVector) {
            if (loaded.getTransportId() != Entity.NONE) {
                // we probably already loaded this unit in the chat lounge
                continue;
            }
            if (loaded.getPosition() != null) {
                // Something is fishy in Denmark.
                LOGGER.error(entity + " can not load entity #" + loaded);
                break;
            }
            // Have the deployed unit load the indicated unit.
            gameManager.loadUnit(entity, loaded, loaded.getTargetBay());
        }

        /*
         * deal with starting velocity for advanced movement. Probably not the
         * best place to do it, but what are you going to do
         */
        if (entity.isAero() && getGame().useVectorMove()) {
            IAero a = (IAero) entity;
            int[] v = { 0, 0, 0, 0, 0, 0 };

            // if this is the entity's first time deploying, we want to respect the
            // "velocity" setting from the lobby
            if (entity.wasNeverDeployed()) {
                if (a.getCurrentVelocityActual() > 0) {
                    v[nFacing] = a.getCurrentVelocityActual();
                    entity.setVectors(v);
                }
                // this means the entity is coming back from off board, so we'll rotate the
                // velocity vector by 180
                // and set it to 1/2 the magnitude
            } else {
                for (int x = 0; x < 6; x++) {
                    v[(x + 3) % 6] = entity.getVector(x) / 2;
                }

                entity.setVectors(v);
            }
        }

        entity.setPosition(coords);
        entity.setBoardId(boardId);
        entity.setFacing(nFacing);
        entity.setSecondaryFacing(nFacing);

        // entity.isAero will check if a unit is a LAM in Fighter mode
        if (entity instanceof IAero aero && entity.isAero()) {
            entity.setAltitude(elevation);
            if ((elevation == 0) && !entity.isSpaceborne()) {
                aero.land();
            } else {
                aero.liftOff(elevation);
            }
        } else {
            entity.setElevation(elevation);
        }

        Hex hex = getGame().getBoard(boardId).getHex(coords);
        if (assaultDrop) {
            entity.setAltitude(1);
            // from the sky!
            entity.setAssaultDropInProgress(true);
        } else if ((entity instanceof VTOL) && (entity.getExternalUnits().isEmpty())) {
            while ((Compute.stackingViolation(getGame(), entity, coords, null, entity.climbMode()) != null) &&
                         (entity.getElevation() <= 500)) {
                entity.setElevation(entity.getElevation() + 1);
            }
        } else if (entity.isAero()) {
            // if the entity is airborne, then we don't want to set its
            // elevation below, because that will
            // default to 999
            if (entity.isAirborne()) {
                entity.setElevation(0);
            }
            if (!entity.isSpaceborne() && entity instanceof IAero a) {
                // all spheroid craft should have velocity of zero in atmosphere
                // regardless of what was entered
                if (a.isSpheroid() || getGame().getPlanetaryConditions().getAtmosphere().isLighterThan(Atmosphere.THIN)) {
                    a.setCurrentVelocity(0);
                    a.setNextVelocity(0);
                }
                // make sure that entity is above the level of the hex if in
                // atmosphere
                if (getGame().getBoard(boardId).inAtmosphere()
                          && (entity.getAltitude() <= hex.ceiling(true))) {
                    // you can't be grounded on low atmosphere map
                    entity.setAltitude(hex.ceiling(true) + 1);
                }
            }
        } else {
            Building bld = getGame().getBoard(boardId).getBuildingAt(entity.getPosition());
            if ((bld != null) && (bld.getType() == BuildingType.WALL)) {
                entity.setElevation(hex.terrainLevel(Terrains.BLDG_ELEV));
            }

        }

        boolean wigeFlyover = entity.getMovementMode() == EntityMovementMode.WIGE &&
                                    hex.containsTerrain(Terrains.BLDG_ELEV) &&
                                    entity.getElevation() > hex.terrainLevel(Terrains.BLDG_ELEV);

        // when first entering a building, we need to roll what type
        // of basement it has
        Building bldg = getGame().getBoard(boardId).getBuildingAt(entity.getPosition());
        if ((bldg != null)) {
            if (bldg.rollBasement(entity.getPosition(), getGame().getBoard(boardId), gameManager.getMainPhaseReport())) {
                gameManager.sendChangedHex(entity.getPosition(), boardId);
                Vector<Building> buildings = new Vector<>();
                buildings.add(bldg);
                gameManager.sendChangedBuildings(buildings);
            }
            boolean collapse = gameManager.checkBuildingCollapseWhileMoving(bldg, entity, entity.getPosition());
            if (collapse) {
                gameManager.addAffectedBldg(bldg, true);
                if (wigeFlyover) {
                    // If the building is collapsed by a WiGE flying over it, the WiGE drops one
                    // level of elevation.
                    entity.setElevation(entity.getElevation() - 1);
                }
            }
        }

        entity.setDone(true);
        entity.setDeployed(true);
        gameManager.entityUpdate(entity.getId());
        addReport(gameManager.doSetLocationsExposure(entity, hex, false, entity.getElevation()));
    }
}
