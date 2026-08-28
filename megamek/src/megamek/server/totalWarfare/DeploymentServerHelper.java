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
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

public class DeploymentServerHelper {

    private static final MMLogger LOGGER = MMLogger.create(DeploymentServerHelper.class);
    private TWGameManager gameManager;

    DeploymentServerHelper(TWGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Process a deployment packet by... deploying the entity! We load any other specified entities inside of it too.
     * Also, check that the deployment is valid.
     */
    public void processDeployment(megamek.common.units.Entity entity,
                                  megamek.common.board.Coords coords,
                                  int boardId,
                                  int nFacing,
                                  int elevation,
                                  java.util.Vector<megamek.common.units.Entity> loadVector,
                                  boolean assaultDrop,
                                  boolean setDone) {
        for (megamek.common.units.Entity loaded : loadVector) {
            if (loaded.getTransportId() != megamek.common.units.Entity.NONE) {
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
        if (entity.isAero() && gameManager.getGame().useVectorMove()) {
            megamek.common.units.IAero a = (megamek.common.units.IAero) entity;
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
        if (!entity.wasNeverDeployed() && entity instanceof megamek.common.units.IAero aeroReturning) {
            int exitAlt = aeroReturning.getExitAltitude();
            if (exitAlt > 0) {
                elevation = exitAlt;
                aeroReturning.setExitAltitude(0);  // Clear after use
            }
        }

        // entity.isAero will check if a unit is a LAM in Fighter mode
        if (entity instanceof megamek.common.units.IAero aero && entity.isAero()) {
            entity.setAltitude(elevation);
            if ((elevation == 0) && !entity.isSpaceborne()) {
                aero.land();
            } else {
                aero.liftOff(elevation);
            }
        } else {
            entity.setElevation(elevation);
        }

        Hex hex = gameManager.getGame().getBoard(boardId).getHex(coords);
        if (assaultDrop) {
            entity.setAltitude(1);
            // from the sky!
            entity.setAssaultDropInProgress(true);
        } else if ((entity instanceof megamek.common.units.VTOL) && (entity.getExternalUnits().isEmpty())) {
            while ((megamek.common.compute.Compute.stackingViolation(gameManager.getGame(),
                                                                     entity,
                                                                     coords,
                                                                     null,
                                                                     entity.climbMode(),
                                                                     false) != null) &&
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
            if (!entity.isSpaceborne() && entity instanceof megamek.common.units.IAero a) {
                // all spheroid craft should have velocity of zero in atmosphere
                // regardless of what was entered
                if (a.isSpheroid() || gameManager.getGame().getPlanetaryConditions()
                                                 .getAtmosphere()
                                                 .isLighterThan(megamek.common.planetaryConditions.Atmosphere.THIN)) {
                    a.setCurrentVelocity(0);
                    a.setNextVelocity(0);
                }
                // make sure that entity is above the level of the hex if in
                // atmosphere
                if (gameManager.getGame().getBoard(boardId).isLowAltitude()
                    && (entity.getAltitude() <= hex.ceiling(true))) {
                    // you can't be grounded on low atmosphere map
                    entity.setAltitude(hex.ceiling(true) + 1);
                }
            }
        } else {
            megamek.common.units.IBuilding bld = gameManager.getGame()
                                                            .getBoard(boardId)
                                                            .getBuildingAt(entity.getPosition());
            if ((bld != null) && (bld.getBuildingType() == megamek.common.enums.BuildingType.WALL)) {
                entity.setElevation(hex.terrainLevel(megamek.common.units.Terrains.BLDG_ELEV));
            }

        }

        boolean wigeFlyover = entity.getMovementMode() == megamek.common.units.EntityMovementMode.WIGE &&
                              hex.containsTerrain(megamek.common.units.Terrains.BLDG_ELEV) &&
                              entity.getElevation() > hex.terrainLevel(megamek.common.units.Terrains.BLDG_ELEV);

        // when first entering a building, we need to roll what type
        // of basement it has
        megamek.common.units.IBuilding bldg = gameManager.getGame()
                                                         .getBoard(boardId)
                                                         .getBuildingAt(entity.getPosition());
        if ((bldg != null)) {
            if (bldg.rollBasement(entity.getPosition(),
                                  gameManager.getGame().getBoard(boardId),
                                  gameManager.getMainPhaseReport())) {
                gameManager.sendChangedHex(entity.getPosition(), boardId);
                java.util.Vector<megamek.common.units.IBuilding> buildings = new java.util.Vector<>();
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
        if (entity instanceof megamek.common.units.AbstractBuildingEntity buildingEntity) {
            buildingEntity.updateBuildingEntityHexes(boardId, gameManager);
        }

        // A vehicle may only be hull-down in a fortified ("infantry-built") hex, and only hull-down-capable vehicles
        // can do so (not Large Vehicles, not naval/submarine) - TO:AR p.19. Re-validate authoritatively here and clear
        // an illegal hull-down state, because combat code such as Tank.rollHitLocation keys off isHullDown() without
        // re-checking terrain, so a state from an old/crafted client or corrupted save could otherwise grant the
        // hull-down hit-location benefit on open ground.
        if ((entity instanceof megamek.common.units.Tank deployingVehicle) && entity.isHullDown()) {
            boolean fortifiedHex = hex.containsTerrain(megamek.common.units.Terrains.FORTIFIED);
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
        if ((entity instanceof megamek.common.units.Infantry deployingInfantry)
            && (deployingInfantry.getDugIn() != megamek.common.units.Infantry.DUG_IN_NONE)
            && hex.containsTerrain(megamek.common.units.Terrains.FORTIFIED)) {
            deployingInfantry.setDugIn(megamek.common.units.Infantry.DUG_IN_NONE);
            LOGGER.debug("[Fortify] {}: cleared redundant dug-in - deployed onto a fortified hex",
                         entity.getDisplayName());
        }

        entity.setDone(setDone);
        entity.setDeployed(true);
        gameManager.entityUpdate(entity.getId());

        deployTowedTrailers(entity);
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
        java.util.List<megamek.common.board.Coords> trainPath = megamek.common.units.TrainLayout.deploymentPath(tractor.getPosition(),
                                                                                                                tractor.getFacing(),
                                                                                                                trailerCount);
        java.util.List<Integer> trainFacings = new java.util.ArrayList<>();
        for (int step = 0; step < trainPath.size(); step++) {
            trainFacings.add(tractor.getFacing());
        }

        java.util.List<megamek.common.units.TrainLayout.TrainPlacement> placements = megamek.common.units.TrainLayout.computeLayout(
                gameManager.getGame(),
                tractor,
                tractor.getPosition(),
                tractor.getFacing(),
                trainPath,
                trainFacings);

        // The footprint was checked against the deployment zone in receiveDeployment, before the tractor was placed.

        megamek.common.units.TrainLayout.applyLayout(gameManager.getGame(), placements);

        for (megamek.common.units.TrainLayout.TrainPlacement placement : placements) {
            megamek.common.units.Entity trailer = gameManager.getGame().getEntity(placement.entityId());
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
        java.util.List<String> clearedTrailers = new java.util.ArrayList<>();

        for (int towedId : tractor.getAllTowedUnits()) {
            megamek.common.units.Entity trailer = gameManager.getGame().getEntity(towedId);

            if ((trailer != null) && trailer.isOffBoard()) {
                trailer.setOffBoard(0, megamek.common.OffBoardDirection.NONE);
                clearedTrailers.add(trailer.getDisplayName());
            }
        }

        if (!clearedTrailers.isEmpty()) {
            LOGGER.info("[Train] {} trailer(s) had an off board setting of their own; a train deploys with {}: {}",
                        clearedTrailers.size(), tractor.getDisplayName(), String.join(", ", clearedTrailers));
        }
    }
}
