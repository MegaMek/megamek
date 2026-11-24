/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.util.List;
import java.util.Vector;

import jakarta.annotation.Nullable;
import megamek.client.ui.Messages;
import megamek.common.Hex;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;
import megamek.common.units.TrainLayout;
import megamek.common.units.VTOL;
import megamek.logging.MMLogger;

/**
 * Helper class for processing deployment whether it is from Deployment Phase or Movement Phase.
 */
public class DeploymentServerHelper {

    private static final MMLogger LOGGER = MMLogger.create(DeploymentServerHelper.class);
    private final TWGameManager gameManager;

    DeploymentServerHelper(TWGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Process a deployment packet by... deploying the entity! We load any other specified entities inside of it too.
     * Also, check that the deployment is valid.
     */
    public void processDeployment(Entity entity,
                                  Coords coords,
                                  int boardId,
                                  int nFacing,
                                  int elevation,
                                  Vector<Entity> loadVector,
                                  boolean assaultDrop,
                                  boolean setDone) {
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
        if (entity.isAero() && gameManager.getGame().useVectorMove()) {
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

        Hex hex = gameManager.getGame().getBoard(boardId).getHex(coords);
        if (assaultDrop) {
            entity.setAltitude(1);
            // from the sky!
            entity.setAssaultDropInProgress(true);
        } else if ((entity instanceof VTOL) && (entity.getExternalUnits().isEmpty())) {
            while ((Compute.stackingViolation(gameManager.getGame(),
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
            if (!entity.isSpaceborne() && entity instanceof IAero a) {
                // all spheroid craft should have velocity of zero in atmosphere
                // regardless of what was entered
                if (a.isSpheroid() || gameManager.getGame().getPlanetaryConditions()
                                                 .getAtmosphere()
                                                 .isLighterThan(Atmosphere.THIN)) {
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
            IBuilding bld = gameManager.getGame()
                                       .getBoard(boardId)
                                       .getBuildingAt(entity.getPosition());
            if ((bld != null) && (bld.getBuildingType() == BuildingType.WALL)) {
                entity.setElevation(hex.terrainLevel(Terrains.BLDG_ELEV));
            }

        }

        // when first entering a building, we need to roll what type
        // of basement it has
        IBuilding bldg = gameManager.getGame()
                                    .getBoard(boardId)
                                    .getBuildingAt(entity.getPosition());
        if ((bldg != null)) {
            if (bldg.rollBasement(entity.getPosition(),
                                  gameManager.getGame().getBoard(boardId),
                                  gameManager.getMainPhaseReport())) {
                gameManager.sendChangedHex(entity.getPosition(), boardId);
                Vector<IBuilding> buildings = new Vector<>();
                buildings.add(bldg);
                gameManager.sendChangedBuildings(buildings);
            }
            boolean collapse = gameManager.checkBuildingCollapseWhileMoving(bldg, entity, entity.getPosition());
            if (collapse) {
                // A WiGE deployed flying over the building is dropped over the rubble by the collapse (TW p.55)
                gameManager.addAffectedBldg(bldg, true);
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
                             !deployingVehicle.isHullDownCapable() ?
                             "vehicle type cannot hull down" :
                             "deploy hex is not fortified");
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

        clearHiddenIfAirborne(entity);

        entity.setDone(setDone);
        entity.setDeployed(true);
        gameManager.entityUpdate(entity.getId());

        deployTowedTrailers(entity);
    }

    /**
     * Checks whether a movement path tries to deploy a unit that is already on the board. Only an undeployed unit may
     * walk on; a {@code DEPLOY} step for a deployed one comes from a stale or bad path, and running it would move the
     * unit back to the step's hex (issue #9601).
     *
     * @param entity   the moving unit, as the server knows it
     * @param movePath the path the client sent
     *
     * @return {@code true} if the unit is already deployed and the path contains a {@code DEPLOY} step
     */
    static boolean deploysAlreadyDeployedUnit(Entity entity, MovePath movePath) {
        return entity.isDeployed() && movePath.contains(MoveStepType.DEPLOY);
    }

    /**
     * An airborne unit cannot be hidden; only one grounded at the start of the scenario can (TW errata v12.0, p.260,
     * Airborne Units). The lobby lets a VTOL or WiGE be set hidden because it may still deploy landed, so the check
     * can only be made here, once its deployment elevation is known. A hidden unit that deploys in the air deploys
     * visible. This is the authoritative check: it also catches a unit that a client, a bot or a loaded file sent
     * hidden.
     *
     * @param entity the unit that has just been placed at its deployment position and elevation
     */
    static void clearHiddenIfAirborne(Entity entity) {
        if (!entity.isHidden()) {
            return;
        }
        if (entity.canHide()) {
            LOGGER.debug("[Hidden] {}: deployed hidden at elevation {}", entity.getDisplayName(),
                  entity.getElevation());
        } else {
            entity.setHidden(false);
            LOGGER.debug("[Hidden] {}: deployed airborne (elevation {}, altitude {}), so it is not hidden",
                  entity.getDisplayName(), entity.getElevation(), entity.getAltitude());
        }
    }

    /**
     * Places the trailers of a train when its tractor deploys. Trailers get no deployment turn of their own, so this is
     * the only chance they have to reach the board.
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
        List<Coords> trainPath = TrainLayout.deploymentPath(tractor.getPosition(),
                                                            tractor.getFacing(),
                                                            trailerCount);
        java.util.List<Integer> trainFacings = new java.util.ArrayList<>();
        for (int step = 0; step < trainPath.size(); step++) {
            trainFacings.add(tractor.getFacing());
        }

        List<TrainLayout.TrainPlacement> placements = TrainLayout.computeLayout(
                gameManager.getGame(),
                tractor,
                tractor.getPosition(),
                tractor.getFacing(),
                trainPath,
                trainFacings);

        // The footprint was checked against the deployment zone in receiveDeployment, before the tractor was placed.

        TrainLayout.applyLayout(gameManager.getGame(), placements);

        for (TrainLayout.TrainPlacement placement : placements) {
            Entity trailer = gameManager.getGame().getEntity(placement.entityId());
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
     * refuses to set one on a hitched trailer, but the setting can survive from before the trailer was hitched, so it
     * is cleared here and logged rather than quietly changing where the unit ends up.
     * </p>
     *
     * @param tractor the unit that has just been deployed
     */
    void clearTrailerOffBoardSettings(Entity tractor) {
        java.util.List<String> clearedTrailers = new java.util.ArrayList<>();

        for (int towedId : tractor.getAllTowedUnits()) {
            Entity trailer = gameManager.getGame().getEntity(towedId);

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
        Coords illegalHex = TrainLayout.firstIllegalDeploymentHex(gameManager.getGame(), tractor, coords, boardId,
              facing);
        if (illegalHex != null) {
            LOGGER.warn("[Train] rejected deployment of {} at {} facing {}: trailer hex {} is not a legal "
                        + "deployment hex", tractor.getDisplayName(), coords, facing, illegalHex);
            return false;
        }
        return true;
    }

    public boolean isLegalDeployment(Coords coords,
                                     int boardId,
                                     Entity entity,
                                     int nFacing) {
        Game game = gameManager.getGame();
        if (game == null) {
            return false;
        }
        return game.hasBoardLocation(coords, boardId)
               && game.getBoard(boardId).isLegalDeployment(coords, entity)
               && isLegalTrainFootprint(entity, coords, boardId, nFacing);
    }

    /**
     * Finds a unit that the deploying unit would stack illegally with. Every hex the unit covers counts, so a
     * DropShip's whole seven-hex footprint is checked. The client makes the same check, but under double blind it
     * cannot see enemy units, so the server must make it too. Hidden units count as well: they are still physically in
     * the hex.
     * <p>
     * No check is made for an assault drop, which lands later and resolves its own landing hex, for a flying
     * aerospace unit, which never stacks, or for a VTOL carrying no external units, which
     * {@link #processDeployment} raises above any unit in the hex.
     * </p>
     *
     * @param entity      the unit being deployed
     * @param coords      the centre hex of the deployment
     * @param boardId     the board of the deployment
     * @param elevation   the elevation, or the altitude for an aerospace unit, the unit deploys at
     * @param assaultDrop {@code true} if the unit is making an assault drop
     *
     * @return the unit causing the violation, or {@code null} when the deployment does not break stacking
     */
    public @Nullable Entity findDeploymentStackingViolation(Entity entity, Coords coords, int boardId, int elevation,
          boolean assaultDrop) {
        Game game = gameManager.getGame();
        if ((game == null) || (coords == null) || !game.hasBoardLocation(coords, boardId)) {
            return null;
        }
        if (assaultDrop) {
            LOGGER.info("[Deploy] {} at {}: assault drop, stacking resolved on landing", entity.getDisplayName(),
                  coords.getBoardNum());
            return null;
        }
        if (entity.isAero() && (elevation > 0)) {
            LOGGER.info("[Deploy] {} at {}: aerospace in flight at altitude {}, no stacking", entity.getDisplayName(),
                  coords.getBoardNum(), elevation);
            return null;
        }
        if ((entity instanceof VTOL) && entity.getExternalUnits().isEmpty()) {
            LOGGER.info("[Deploy] {} at {}: VTOL is raised clear of any unit", entity.getDisplayName(),
                  coords.getBoardNum());
            return null;
        }
        Entity violation = Compute.stackingViolation(game, entity, elevation, coords, boardId, null,
              entity.climbMode(), false);
        if (violation == null) {
            LOGGER.info("[Deploy] {} at {}: no stacking violation", entity.getDisplayName(), coords.getBoardNum());
        } else {
            LOGGER.info("[Deploy] {} at {}: refused, stacking violation with {} at {}", entity.getDisplayName(),
                  coords.getBoardNum(), violation.getDisplayName(),
                  (violation.getPosition() == null) ? "?" : violation.getPosition().getBoardNum());
        }
        return violation;
    }

    /**
     * Refuses a deployment that breaks stacking. A player who can see the unit in the way is told which unit it is,
     * and is sent that unit so their own deployment check rules the hex out. A player who cannot see it, because it
     * is a hidden enemy or out of sight under double blind, is only told the hex is not available, so the refusal
     * gives nothing else away (issue 9580). The refused unit and its trailers are resent too, as the client already
     * marked them deployed.
     *
     * @param connId    the connection of the player whose deployment is refused
     * @param entity    the unit whose deployment is refused
     * @param blocker   the unit causing the stacking violation
     * @param coords    the refused deployment hex
     * @param turnOwner the player whose turn it is, or {@link Player#PLAYER_NONE} if unknown
     */
    void refuseStackedDeployment(int connId, Entity entity, Entity blocker, Coords coords, int turnOwner) {
        if (isVisibleToPlayer(blocker, gameManager.getGame().getPlayer(connId))) {
            gameManager.send(connId, gameManager.createEntityPacket(blocker.getId(), null));
            gameManager.sendServerChat(connId, Messages.getString("DeploymentDisplay.stackingRefused",
                  entity.getShortName(), coords.getBoardNum(), blocker.getShortName()));
        } else {
            LOGGER.info("[Deploy] {} at {}: the refused player cannot see {}, so it is not named or sent to them",
                  entity.getDisplayName(), coords.getBoardNum(), blocker.getDisplayName());
            gameManager.sendServerChat(connId, Messages.getString("DeploymentDisplay.stackingRefusedUnseen",
                  entity.getShortName(), coords.getBoardNum()));
        }
        resendRefusedUnit(connId, entity);
        gameManager.send(connId, gameManager.getPacketHelper().createTurnListPacket());
        if (turnOwner != Player.PLAYER_NONE) {
            gameManager.send(connId, gameManager.getPacketHelper().createTurnIndexPacket(turnOwner));
        }
    }

    /**
     * Refuses a walk-on into a hex that is not a legal deployment hex for the unit, or for the train it tows. The
     * player is told, sent the unit and its trailers as the client already marked them deployed, and handed the turn
     * back.
     *
     * @param connId    the connection of the player whose deployment is refused
     * @param entity    the unit whose deployment is refused
     * @param coords    the refused deployment hex; may be {@code null} if the path gave none
     * @param turnOwner the player whose turn it is
     */
    void refuseIllegalDeployment(int connId, Entity entity, @Nullable Coords coords, int turnOwner) {
        LOGGER.error("[Deploy] Refused an illegal deployment of {} at {} from connection {}", entity.getDisplayName(),
              (coords == null) ? "?" : coords.getBoardNum(), connId);
        gameManager.sendServerChat(connId, Messages.getString("DeploymentDisplay.cantDeployInto",
              entity.getShortName(), (coords == null) ? "?" : coords.getBoardNum()));
        resendRefusedUnit(connId, entity);
        gameManager.send(connId, gameManager.getPacketHelper().createTurnListPacket());
        gameManager.send(connId, gameManager.getPacketHelper().createTurnIndexPacket(turnOwner));
    }

    /**
     * Sends a player the server's copy of a unit whose deployment was refused, along with every trailer it tows. The
     * client places the whole train while the player picks a hex, so resending only the tractor would leave its
     * trailers standing on the board.
     *
     * @param connId the connection of the player whose deployment is refused
     * @param entity the unit whose deployment is refused
     */
    void resendRefusedUnit(int connId, Entity entity) {
        gameManager.send(connId, gameManager.createEntityPacket(entity.getId(), null));
        for (int trailerId : entity.getAllTowedUnits()) {
            if (gameManager.getGame().getEntity(trailerId) != null) {
                gameManager.send(connId, gameManager.createEntityPacket(trailerId, null));
            }
        }
    }

    /**
     * Whether a player already knows a unit is on the board. Without double blind every client holds every unit, and
     * only an enemy's hidden unit is kept from view. Under double blind it is whatever the player's side can see.
     *
     * @param unit   the unit to check
     * @param player the player who may or may not see it; may be {@code null} if the connection has no player
     *
     * @return {@code true} if the player can see the unit
     */
    boolean isVisibleToPlayer(Entity unit, @Nullable Player player) {
        if (player == null) {
            return false;
        }
        if (gameManager.doBlind()) {
            return gameManager.whoCanSee(unit).contains(player);
        }
        boolean isHiddenEnemy = unit.isHidden() && unit.getOwner().isEnemyOf(player);
        return !isHiddenEnemy;
    }
}
