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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.OffBoardDirection;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.game.GameTurn;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.turns.SpecificEntityTurn;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.IAero;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;
import megamek.common.units.TrainLayout;
import megamek.common.units.VTOL;
import megamek.logging.MMLogger;

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
    void receiveDeployment(Packet packet, int connId) throws InvalidPacketDataException {
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
        processDeployment(entity, coords, boardId, nFacing, elevation, loadVector, assaultDrop);

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
    void receiveDeploymentUnload(Packet packet, int connId) throws InvalidPacketDataException {
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
                LOGGER.error("{} can not load entity #{}", entity, loaded);
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

        // For returning climb out units, restore the exit altitude
        // (entity was not never deployed if returning from off-map)
        if (!entity.wasNeverDeployed() && entity instanceof IAero aeroReturning) {
            int exitAlt = aeroReturning.getExitAltitude();
            if (exitAlt > 0) {
                elevation = exitAlt;
                aeroReturning.setExitAltitude(0);  // Clear after use
            }
        }

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
            while ((Compute.stackingViolation(getGame(), entity, coords, null, entity.climbMode(), false) != null) &&
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
                if (a.isSpheroid() || getGame().getPlanetaryConditions()
                      .getAtmosphere()
                      .isLighterThan(Atmosphere.THIN)) {
                    a.setCurrentVelocity(0);
                    a.setNextVelocity(0);
                }
                // make sure that entity is above the level of the hex if in
                // atmosphere
                if (getGame().getBoard(boardId).isLowAltitude()
                      && (entity.getAltitude() <= hex.ceiling(true))) {
                    // you can't be grounded on low atmosphere map
                    entity.setAltitude(hex.ceiling(true) + 1);
                }
            }
        } else {
            IBuilding bld = getGame().getBoard(boardId).getBuildingAt(entity.getPosition());
            if ((bld != null) && (bld.getBuildingType() == BuildingType.WALL)) {
                entity.setElevation(hex.terrainLevel(Terrains.BLDG_ELEV));
            }

        }

        boolean wigeFlyover = entity.getMovementMode() == EntityMovementMode.WIGE &&
              hex.containsTerrain(Terrains.BLDG_ELEV) &&
              entity.getElevation() > hex.terrainLevel(Terrains.BLDG_ELEV);

        // when first entering a building, we need to roll what type
        // of basement it has
        IBuilding bldg = getGame().getBoard(boardId).getBuildingAt(entity.getPosition());
        if ((bldg != null)) {
            if (bldg.rollBasement(entity.getPosition(),
                  getGame().getBoard(boardId),
                  gameManager.getMainPhaseReport())) {
                gameManager.sendChangedHex(entity.getPosition(), boardId);
                Vector<IBuilding> buildings = new Vector<>();
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

        // If deploying a BuildingEntity, add building terrain to all hexes it occupies
        if (entity instanceof AbstractBuildingEntity buildingEntity) {
            buildingEntity.updateBuildingEntityHexes(boardId, gameManager);
        }

        // A vehicle may only be hull-down in a fortified ("infantry-built") hex, and only hull-down-capable vehicles
        // can do so (not Large Vehicles, not naval/submarine) - TO:AR p.19. Re-validate authoritatively here and clear
        // an illegal hull-down state, because combat code such as Tank.rollHitLocation keys off isHullDown() without
        // re-checking terrain, so a state from an old/crafted client or corrupted save could otherwise grant the
        // hull-down hit-location benefit on open ground.
        if ((entity instanceof Tank deployingVehicle) && entity.isHullDown()) {
            boolean fortifiedHex = hex.containsTerrain(Terrains.FORTIFIED);
            if (!deployingVehicle.isHullDownCapable() || !fortifiedHex) {
                entity.setHullDown(false);
                LOGGER.debug("[HullDown] {}: cleared illegal deploy hull-down - {}", entity.getDisplayName(),
                      !deployingVehicle.isHullDownCapable() ? "vehicle type cannot hull down" : "deploy hex is not fortified");
            } else {
                LOGGER.debug("[HullDown] {}: deployed hull-down on a fortified hex", entity.getDisplayName());
            }
        }

        // Infantry deploying onto a fortified hex already gets the dug-in cover from the terrain, so it cannot also be
        // separately dug in (the two postures don't stack - TO:AR p.106 / TO:AUE p.153). Keep the fortified-hex state
        // and clear the redundant dug-in, mirroring what completeFortification does for co-located infantry.
        if ((entity instanceof Infantry deployingInfantry)
              && (deployingInfantry.getDugIn() != Infantry.DUG_IN_NONE)
              && hex.containsTerrain(Terrains.FORTIFIED)) {
            deployingInfantry.setDugIn(Infantry.DUG_IN_NONE);
            LOGGER.debug("[Fortify] {}: cleared redundant dug-in - deployed onto a fortified hex",
                  entity.getDisplayName());
        }

        entity.setDone(true);
        entity.setDeployed(true);
        gameManager.entityUpdate(entity.getId());
        addReport(gameManager.doSetLocationsExposure(entity, hex, false, entity.getElevation()));

        deployTowedTrailers(entity);
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
     *
     * @return true when the whole train fits in the deployment zone
     */
    private boolean isLegalTrainFootprint(Entity tractor, Coords coords, int boardId, int facing) {
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

    /**
     * Places the trailers of a train when its tractor deploys. Trailers get no deployment turn of their own, so this
     * is the only chance they have to reach the board.
     *
     * @param tractor the unit that has just been deployed
     */
    private void deployTowedTrailers(Entity tractor) {
        if (tractor.getAllTowedUnits().isEmpty()) {
            return;
        }

        // A hitched train deploys wherever its tractor does, so a trailer keeps no off board setting of its own. The
        // lobby does not let one be set on a hitched trailer, but it can be set before the trailer is hitched, so
        // clear it here rather than leaving the trailer thinking it belongs somewhere else.
        clearTrailerOffBoardSettings(tractor);

        int trailerCount = tractor.getAllTowedUnits().size();
        List<Coords> trainPath = TrainLayout.deploymentPath(tractor.getPosition(), tractor.getFacing(), trailerCount);
        List<Integer> trainFacings = new ArrayList<>();
        for (int step = 0; step < trainPath.size(); step++) {
            trainFacings.add(tractor.getFacing());
        }

        List<TrainLayout.TrainPlacement> placements = TrainLayout.computeLayout(getGame(), tractor,
              tractor.getPosition(), tractor.getFacing(), trainPath, trainFacings);

        // The footprint was checked against the deployment zone in receiveDeployment, before the tractor was placed.

        TrainLayout.applyLayout(getGame(), placements);

        for (TrainLayout.TrainPlacement placement : placements) {
            Entity trailer = getGame().getEntity(placement.entityId());
            if (trailer == null) {
                continue;
            }
            trailer.setBoardId(tractor.getBoardId());
            trailer.setElevation(tractor.getElevation());
            trailer.setSecondaryFacing(trailer.getFacing());
            trailer.setDone(true);
            trailer.setDeployed(true);
            gameManager.entityUpdate(trailer.getId());
        }

        LOGGER.info("[Train] {} deployed at {} with {} trailer(s) placed behind it",
              tractor.getDisplayName(), tractor.getPosition(), trailerCount);
    }

    /**
     * Clears any off board setting left on the trailers of a train that is deploying.
     * <p>
     * A hitched train goes wherever its tractor goes, so a trailer never carries a deployment of its own. The lobby
     * refuses to set one on a hitched trailer, but the setting can survive from before the trailer was hitched, so
     * it is cleared here and logged rather than quietly changing where the unit ends up.
     * </p>
     *
     * @param tractor the unit that has just been deployed
     */
    void clearTrailerOffBoardSettings(Entity tractor) {
        List<String> clearedTrailers = new ArrayList<>();

        for (int towedId : tractor.getAllTowedUnits()) {
            Entity trailer = getGame().getEntity(towedId);

            if ((trailer != null) && trailer.isOffBoard()) {
                trailer.setOffBoard(0, OffBoardDirection.NONE);
                clearedTrailers.add(trailer.getDisplayName());
            }
        }

        if (!clearedTrailers.isEmpty()) {
            LOGGER.info("[Train] {} trailer(s) had an off board setting of their own; a train deploys with {}: {}",
                  clearedTrailers.size(), tractor.getDisplayName(), String.join(", ", clearedTrailers));
        }
    }

}
