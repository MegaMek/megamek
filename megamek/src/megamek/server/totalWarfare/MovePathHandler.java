/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.common.bays.Bay.UNSET_BAY;

import java.util.*;
import java.util.stream.Collectors;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.panels.phaseDisplay.MovementDisplay;
import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.IndustrialElevator;
import megamek.common.LosEffects;
import megamek.common.MPCalculationSetting;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.AirMekRamAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.ClearMinefieldAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.RamAttackAction;
import megamek.common.actions.UnjamAction;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.bays.Bay;
import megamek.common.board.Board;
import megamek.common.board.BoardHelper;
import megamek.common.board.BoardLocation;
import megamek.common.board.BridgeConstruction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.*;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.event.GameToastEvent;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.moves.ClimbingHelper;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.turns.SpecificEntityTurn;
import megamek.common.units.*;
import megamek.common.weapons.TeleMissile;
import megamek.logging.MMLogger;
import megamek.server.ServerHelper;
import megamek.server.SmokeCloud;

/**
 * Processes an Entity's MovePath when an ENTITY_MOVE packet is received.
 */
class MovePathHandler extends AbstractTWRuleHandler {
    private static final MMLogger logger = MMLogger.create(MovePathHandler.class);


    private final Entity entity;
    private final MovePath md;
    private final Map<UnitTargetPair, LosEffects> losCache;

    private boolean sideslipped = false;
    private Coords lastPos;
    private Coords curPos;
    private int curBoardId;
    private int curFacing;
    private int curVTOLElevation;
    private int lastElevation;
    private int curAltitude;
    private boolean curClimbMode;
    // if the entity already used some MPs,
    // it previously tried to get up and fell,
    // and then got another turn. set moveType
    // and overallMoveType accordingly
    // (these are all cleared by Entity.newRound)
    private int distance;
    private int mpUsed;
    private EntityMovementType moveType;
    private EntityMovementType overallMoveType;
    private EntityMovementType lastStepMoveType;
    private boolean firstStep;
    private MoveStep prevStep = null;
    private boolean wasProne;
    private boolean fellDuringMovement;
    private boolean crashedDuringMovement;
    private boolean dropshipStillUnloading;
    private boolean detectedHiddenHazard;
    private boolean turnOver;
    private int prevFacing;
    private Hex prevHex;
    private boolean isInfantry;
    private AttackAction charge;
    private RamAttackAction ram;
    // cache this here, otherwise changing MP in the turn causes
    // erroneous gravity PSRs
    private int cachedGravityLimit = -1;
    private int thrustUsed = 0;
    private int j = 0;
    private boolean recovered = false;
    private Entity loader = null;
    private boolean continueTurnFromPBS = false;
    private boolean continueTurnFromFishtail = false;
    private boolean continueTurnFromLevelDrop = false;
    private boolean continueTurnFromCliffAscent = false;
    // Track if we already sent a special report popup for EMP mines during this move
    private boolean sentEMPPopupThisMove = false;

    // get a list of coordinates that the unit passed through this turn
    // so that I can later recover potential bombing targets
    // it may already have some values
    private final Vector<Coords> passedThrough = new Vector<>();
    private final List<Integer> passedThroughFacing = new ArrayList<>();
    private final List<Entity> hiddenEnemies = new ArrayList<>();
    private final Vector<UnitLocation> movePath = new Vector<>();

    private Report report;
    private PilotingRollData rollTarget;

    /**
     * Steps through an entity movement packet, executing it.
     *
     * @param gameManager The server's GameManager
     * @param entity      The Entity that is moving
     * @param md          The MovePath that defines how the Entity moves
     * @param losCache    A cache that stores Los between various Entities and targets. In double-blind games, we may
     *                    need to compute a lot of LosEffects, so caching them can really speed things up.
     */
    MovePathHandler(TWGameManager gameManager, Entity entity, MovePath md, Map<UnitTargetPair, LosEffects> losCache) {
        super(gameManager);
        this.entity = entity;
        this.md = md;
        this.losCache = (losCache == null) ? new HashMap<>() : losCache;
    }

    /**
     * Moves an industrial elevator platform along with the unit riding it. Only applies to elevator ascend/descend
     * steps; the platform tracks the unit's new elevation and the updated state is broadcast to all clients.
     *
     * @param step         the move step being processed
     * @param curPos       the unit's current hex
     * @param curBoardId   the board the unit is on
     * @param curElevation the unit's elevation after the step
     */
    private void moveElevatorPlatformWithEntity(MoveStep step, Coords curPos, int curBoardId, int curElevation) {
        if ((step.getType() != MoveStepType.ELEVATOR_ASCEND) && (step.getType() != MoveStepType.ELEVATOR_DESCEND)) {
            return;
        }
        IndustrialElevator elevator = getGame().getIndustrialElevator(BoardLocation.of(curPos, curBoardId));
        if (elevator != null) {
            elevator.setPlatformLevel(curElevation);
            // Update the hex so the tileset shows the platform at its new level
            if (elevator.syncDisplayLevel(getGame())) {
                gameManager.sendChangedHex(curPos, curBoardId);
            }
            gameManager.sendIndustrialElevatorUpdate();
        }
    }

    /**
     * Handles a unit that enters an industrial elevator shaft when the platform is not at the unit's level: the unit
     * falls to the platform (if it is below) or to the shaft bottom. The fall is skipped for jumping units and for
     * elevator ascend/descend steps, where the platform moves with the unit.
     *
     * @param step         the move step being processed
     * @param curHex       the hex the unit is in
     * @param curPos       the unit's current hex
     * @param curBoardId   the board the unit is on
     * @param curElevation the unit's elevation after the step
     * @param stepMoveType the movement type of this step
     *
     * @return {@code true} if the unit fell, which ends its movement
     */
    private boolean entityFallsDownElevatorShaft(MoveStep step, Hex curHex, Coords curPos, int curBoardId,
          int curElevation, EntityMovementType stepMoveType) {
        boolean isElevatorStep = (step.getType() == MoveStepType.ELEVATOR_ASCEND)
              || (step.getType() == MoveStepType.ELEVATOR_DESCEND);
        if (!curHex.containsTerrain(Terrains.INDUSTRIAL_ELEVATOR)
              || (stepMoveType == EntityMovementType.MOVE_JUMP)
              || isElevatorStep) {
            return false;
        }
        IndustrialElevator elevator = getGame().getIndustrialElevator(BoardLocation.of(curPos, curBoardId));
        if ((elevator == null) || !elevator.isFunctional() || elevator.isPlatformAt(curElevation)) {
            return false;
        }
        // Fall to the platform if it is below the unit, otherwise to the shaft bottom
        int platformLevel = elevator.getPlatformLevel();
        int fallToLevel = (platformLevel < curElevation) ? platformLevel : elevator.getShaftBottom();
        int fallDistance = curElevation - fallToLevel;
        if (fallDistance <= 0) {
            return false;
        }
        logger.debug("[IndustrialElevator] {} entered shaft at {} with platform at level {}; falling {} levels",
              entity.getShortName(), curPos, platformLevel, fallDistance);
        Report fallReport = new Report(5298, Report.PUBLIC);
        fallReport.subject = entity.getId();
        fallReport.add(entity.getDisplayName());
        addReport(fallReport);

        // The unit falls within the same hex
        PilotingRollData pilotingRoll = entity.getBasePilotingRoll(stepMoveType);
        pilotingRoll.addModifier(fallDistance, "fell down elevator shaft");
        addReport(gameManager.doEntityFallsInto(entity, curElevation, curPos, curPos, pilotingRoll, false, 0));
        fellDuringMovement = true;
        return true;
    }

    /**
     * Adds the report announcing a unit's successful use of an industrial elevator. Only applies to elevator
     * ascend/descend steps.
     *
     * @param step         the move step being processed
     * @param curElevation the unit's elevation after the step
     */
    private void reportElevatorUse(MoveStep step, int curElevation) {
        if ((step.getType() != MoveStepType.ELEVATOR_ASCEND) && (step.getType() != MoveStepType.ELEVATOR_DESCEND)) {
            return;
        }
        String direction = (step.getType() == MoveStepType.ELEVATOR_ASCEND)
              ? Messages.getString("IndustrialElevator.directionUp")
              : Messages.getString("IndustrialElevator.directionDown");
        Report elevatorReport = new Report(5299, Report.PUBLIC);
        elevatorReport.subject = entity.getId();
        elevatorReport.add(entity.getDisplayName());
        elevatorReport.add(direction);
        elevatorReport.add(curElevation);
        addReport(elevatorReport);
    }

    void processMovement() {
        // TacOps Climbing: check if a climbing/dangling entity lost its climbing ability
        // due to actuator damage since last turn (TO:AR p.20)
        // Climbing requires 1+ arms; dangling requires 2 arms
        if (entity.isClimbing() && (entity instanceof Mek climbingMek)) {
            int climbableArms = ClimbingHelper.countClimbableArms(climbingMek);
            boolean cannotHoldOn = (climbableArms == 0)
                  || (entity.isDangling() && (climbableArms < 2));
            if (cannotHoldOn) {
                logger.debug("[FALL-TRACE] Climbing/dangling Mek {} lost required arms, auto-falling " +
                      "from elevation {} in hex {}",
                      entity.getDisplayName(), entity.getElevation(), entity.getPosition());
                // Surface as a special-report toast (kill-feed style) AND mirror to chat so the
                // player sees the auto-fall immediately, not just buried in the round report.
                // Matches the building-too-damaged path in TWGameManager.checkClimbingEntitiesOnBuilding.
                Report armsLostReport = new Report(6465, Report.PUBLIC);
                armsLostReport.add(entity.getDisplayName());
                addReport(armsLostReport);
                Vector<Report> armsLostSpecial = new Vector<>();
                armsLostSpecial.add(armsLostReport);
                gameManager.send(gameManager.createSpecialReportPacket(armsLostSpecial));
                gameManager.sendServerChat(Messages.getString(
                      "MovementDisplay.ClimbingDialog.armsLostChat",
                      entity.getDisplayName()));
                // Clear climbing/dangling flags BEFORE doEntityFallsInto so the entity
                // updates pushed during fall processing carry the cleared state to clients.
                // Otherwise the client copy retains isClimbing=true and the next-turn
                // selectEntity dialog wrongly fires "can no longer hold on" while prone.
                entity.setClimbing(false);
                entity.setDangling(false);
                entity.setClimbingLevelsChosen(0);
                PilotingRollData autoFallRoll = new PilotingRollData(entity.getId(),
                      TargetRoll.AUTOMATIC_FAIL, "climbing arms destroyed");
                addReport(gameManager.doEntityFallsInto(entity, entity.getElevation(),
                      entity.getPosition(), entity.getPosition(), autoFallRoll, true, 0));
                addNewLines();
                // The Mek just fell — terminate movement processing immediately. Without
                // this return, processMovement would fall through to the edge-dangle block
                // and then iterate the player's remaining steps in processSteps, executing
                // movement on a prone unit (or worse, on a Mek that just lost the arms it
                // would need to do that). Mirrors the early-return pattern used by the
                // EDGE DANGLE and DROP server branches below.
                entity.setDone(true);
                entity.moved = EntityMovementType.MOVE_WALK;
                entity.mpUsed = 0;
                entity.delta_distance = 0;
                gameManager.entityUpdate(entity.getId());
                return;
            }
        }

        // TacOps Dangle-and-Drop (TO:AR p.20)
        // Detect dangle/drop intent:
        // - Climbing/dangling entity with DOWN step(s): 1 DOWN = dangle, 2 DOWN = drop
        // - Entity at elevated position with CLIMB_MODE_ON and no movement = edge dangle initiation
        boolean hasDownStep = md.contains(MoveStepType.DOWN);
        // Edge dangle: entity starts at high elevation, moves FORWARDS to lower hex with climb mode
        boolean isEdgeDangle = false;
        Coords edgeDangleTargetPos = null;
        // Edge dangle is a walking-only descent (TO:AR p.20). Jumps clear the drop with jump
        // jets and use normal jump-landing mechanics, so a jump from a cliff-top hex must NOT
        // false-trigger the dangle handler — that would put the unit into a climbing state at
        // the destination, which it never was.
        boolean isJumpPath = md.contains(MoveStepType.START_JUMP)
              || (md.getLastStepMovementType() == EntityMovementType.MOVE_JUMP);
        // canClimb (1 arm) — not canDangle (2 arms) — so a one-armed Mek can still
        // initiate edge climb-down. Whether the chosen action is dangle vs climb-down
        // is determined later by entity.climbingLevelsChosen; dangle requires 2 arms
        // and the client dialog already filters that option.
        if (!entity.isClimbing() && !entity.isDangling()
              && (entity instanceof Mek)
              && ClimbingHelper.canClimb(entity)
              && getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_CLIMBING)
              && md.getFinalClimbMode()
              && md.contains(MoveStepType.FORWARDS)
              && !isJumpPath) {
            // Check if the FORWARDS step goes to a lower hex (3+ levels down)
            // Entity must not have moved (target must be adjacent to starting position)
            MoveStep lastStep = md.getLastStep();
            if (lastStep != null) {
                Coords targetPos = lastStep.getPosition();
                boolean entityAtStart = entity.getPosition().distance(targetPos) == 1;
                if (entityAtStart && ClimbingHelper.isAtEdge(entity, targetPos, getGame())) {
                    isEdgeDangle = true;
                    edgeDangleTargetPos = targetPos;
                }
            }
        }
        // Server-side validation: dangle/drop only valid when TacOps Climbing is enabled.
        // The entity's climbing/dangling state is server-controlled, so this also implicitly
        // validates that the prior dangle initiation was approved.
        boolean tacOpsClimbingEnabled = getGame().getOptions()
              .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_CLIMBING);
        boolean canProcessDangle = tacOpsClimbingEnabled
              && (entity.isClimbing() || entity.isDangling()) && hasDownStep;
        logger.debug("[DANGLE-TRACE] processMovement check: canProcessDangle={}, isEdgeDangle={}, " +
                    "hasDownStep={}, isClimbing={}, isDangling={}, elevation={}, climbMode={}, stepCount={}",
              canProcessDangle, isEdgeDangle, hasDownStep, entity.isClimbing(), entity.isDangling(),
              entity.getElevation(), md.getFinalClimbMode(), md.length());
        if (isEdgeDangle && (edgeDangleTargetPos != null)) {
            // Edge initiation: move entity to lower hex, face the cliff/building.
            // Discriminator: entity.climbingLevelsChosen > 0 = controlled climb-down (PSR per
            // level); 0 = standard dangle (no PSR, fixed 2 levels). The client picks one or the
            // other from the cliff-top dialog and pushes via sendUpdateEntity.
            Coords originalPos = entity.getPosition();
            Hex srcHex = getGame().getBoard(entity.getBoardId()).getHex(originalPos);
            int cliffTopAlt = srcHex.getLevel() + entity.getElevation();
            Hex destHex = getGame().getBoard(entity.getBoardId()).getHex(edgeDangleTargetPos);
            int facingToCliff = edgeDangleTargetPos.direction(originalPos);
            int chosenEdgeDescent = entity.getClimbingLevelsChosen();
            if ((chosenEdgeDescent > 0) && (entity instanceof Mek edgeMek)
                  && ClimbingHelper.canClimb(entity)) {
                // EDGE CLIMB-DOWN (TO:AR p.20): PSR per level. On failure, fall from current
                // descended elevation. Mek ends in lower hex at (cliffTopAlt - levelsDescended).
                // Total drop measures to the destination hex's FLOOR — water bottom for water,
                // basement for basements — so a Mek descending off a bridge into adjacent water
                // can ride the cliff face all the way down, not stop at the water surface.
                int costPerLevel = ClimbingHelper.getClimbingMPCostPerLevel(edgeMek);
                int climbableArms = ClimbingHelper.countClimbableArms(edgeMek);
                int totalDrop = cliffTopAlt - destHex.floor();
                int levelsToDescend = Math.min(chosenEdgeDescent, totalDrop);
                logger.debug("[CLIMB-TRACE] Server processing EDGE climb-down: entity={}, "
                            + "from={} (alt {}), to={} (level {}), levelsToDescend={}, costPerLevel={}",
                      entity.getDisplayName(), originalPos, cliffTopAlt,
                      edgeDangleTargetPos, destHex.getLevel(), levelsToDescend, costPerLevel);
                // Move to lower hex first so the PSR rolls happen at the new position.
                entity.setPosition(edgeDangleTargetPos);
                entity.setFacing(facingToCliff);
                entity.setSecondaryFacing(facingToCliff);
                int levelsDescended = 0;
                boolean fellWhileDescending = false;
                for (int i = 1; i <= levelsToDescend; i++) {
                    PilotingRollData psr = entity.getBasePilotingRoll(EntityMovementType.MOVE_WALK);
                    psr.append(new PilotingRollData(entity.getId(),
                          ClimbingHelper.CLIMBING_PSR_MODIFIER,
                          "climbing down (level " + i + " of " + levelsToDescend + ")"));
                    if (climbableArms == 1) {
                        psr.append(new PilotingRollData(entity.getId(),
                              ClimbingHelper.ONE_ARM_PSR_MODIFIER, "climbing with one arm"));
                    }
                    int psrElevation = (cliffTopAlt - destHex.getLevel()) - levelsDescended;
                    if (gameManager.doSkillCheckWhileMoving(entity, psrElevation,
                          edgeDangleTargetPos, edgeDangleTargetPos, psr, true) > 0) {
                        // PSR failed - fall from current climb-down elevation
                        entity.setClimbing(false);
                        entity.setDangling(false);
                        entity.setClimbingLevelsChosen(0);
                        fellWhileDescending = true;
                        break;
                    }
                    levelsDescended++;
                }
                if (!fellWhileDescending) {
                    int floorRelative = destHex.floor() - destHex.getLevel();
                    int finalElevation = Math.max(floorRelative,
                          (cliffTopAlt - destHex.getLevel()) - levelsDescended);
                    entity.setElevation(finalElevation);
                    // Climbing flag clears only at the actual hex floor — for a water hex that
                    // means the water bottom, not the surface. Anywhere above the floor the Mek
                    // is still clinging to the (above- or below-water) cliff face and can
                    // continue the descent next turn.
                    if (entityHasReachedFloor(entity)) {
                        entity.setClimbing(false);
                        entity.setDangling(false);
                        Report groundReport = new Report(6463, Report.PUBLIC);
                        groundReport.add(entity.getDisplayName());
                        gameManager.getMainPhaseReport().add(groundReport);
                    } else {
                        entity.setClimbing(true);
                        entity.setDangling(false);
                    }
                    entity.setClimbingLevelsChosen(0);
                }
                entity.setDone(true);
                entity.moved = EntityMovementType.MOVE_WALK;
                entity.mpUsed = levelsDescended * costPerLevel;
                entity.delta_distance = 0;
                gameManager.entityUpdate(entity.getId());
                return;
            }
            // Standard EDGE DANGLE: 2 levels per turn, no PSR. Requires 2 functional arms
            // (canDangle). If a one-armed Mek's path somehow falls through to here (client
            // dialog hides Dangle in that case, but defensively guard) the path is rejected
            // — the unit ends turn at the cliff top with no movement, so the player can pick
            // a different action.
            if (!ClimbingHelper.canDangle(entity)) {
                logger.warn("[DANGLE-TRACE] Server rejecting EDGE dangle: entity={} lacks two "
                            + "functional climbing arms; chosenLevels was 0 (dangle path).",
                      entity.getDisplayName());
                entity.setDone(true);
                entity.moved = EntityMovementType.MOVE_NONE;
                entity.mpUsed = 0;
                entity.delta_distance = 0;
                gameManager.entityUpdate(entity.getId());
                return;
            }
            int dangleElevation = cliffTopAlt - ClimbingHelper.DANGLE_LEVELS_PER_TURN
                  - destHex.getLevel();
            dangleElevation = Math.max(0, dangleElevation);
            logger.debug("[DANGLE-TRACE] Server processing EDGE dangle: entity={}, " +
                        "from={} (alt {}), to={} (level {}), dangleElevation={}",
                  entity.getDisplayName(), originalPos, cliffTopAlt,
                  edgeDangleTargetPos, destHex.getLevel(), dangleElevation);
            Report dangleReport = new Report(6462, Report.PUBLIC);
            dangleReport.add(entity.getDisplayName());
            dangleReport.add(ClimbingHelper.DANGLE_LEVELS_PER_TURN);
            gameManager.getMainPhaseReport().add(dangleReport);
            // Move to lower hex, face the cliff/building
            entity.setPosition(edgeDangleTargetPos);
            entity.setFacing(facingToCliff);
            entity.setSecondaryFacing(facingToCliff);
            entity.setElevation(dangleElevation);
            entity.setDangling(true);
            entity.setClimbingLevelsChosen(0);
            // Dangle only clears when the Mek has reached the actual hex floor — water bottom
            // for a water hex, basement floor for a basement hex, plain elev 0 otherwise.
            if (entityHasReachedFloor(entity)) {
                entity.setDangling(false);
                Report groundReport = new Report(6463, Report.PUBLIC);
                groundReport.add(entity.getDisplayName());
                gameManager.getMainPhaseReport().add(groundReport);
            }
            entity.setDone(true);
            entity.moved = EntityMovementType.MOVE_WALK;
            entity.mpUsed = 0;
            entity.delta_distance = 0;
            gameManager.entityUpdate(entity.getId());
            return;
        }
        if (canProcessDangle) {
            int downStepCount = (int) md.getStepVector().stream()
                  .filter(s -> s.getType() == MoveStepType.DOWN)
                  .count();
            // Controlled CLIMB DOWN (TO:AR p.20): same MP cost and PSRs as climbing up.
            // Discriminated from DANGLE (1 bare DOWN) and DROP (2 bare DOWN) by the
            // CLIMB_MODE_ON marker step the client adds before the DOWN steps.
            // Edge dangle uses CLIMB_MODE_ON + FORWARDS (no DOWN), so no false positive.
            // entity.climbingLevelsChosen isn't transmitted to the server — the path is.
            boolean hasClimbModeOnMarker = md.contains(MoveStepType.CLIMB_MODE_ON);
            if (hasClimbModeOnMarker && (downStepCount > 0) && (entity instanceof Mek descendingMek)
                  && ClimbingHelper.canClimb(entity)) {
                int costPerLevel = ClimbingHelper.getClimbingMPCostPerLevel(descendingMek);
                int climbableArms = ClimbingHelper.countClimbableArms(descendingMek);
                int currentElevation = entity.getElevation();
                int floorRelativeForDescent = hexFloorRelative(entity);
                // Cap requested descent at the hex floor (water bottom for water hexes, ground
                // for dry hexes) and against available walking MP. The first cap previously read
                // `currentElevation`, hardcoding ground-at-0 and blocking any descent below the
                // water surface even though the underwater cliff face is climbable.
                int descendableLevels = currentElevation - floorRelativeForDescent;
                int requestedLevels = Math.min(downStepCount, descendableLevels);
                int availableMP = entity.getWalkMP();
                int affordableLevels = (costPerLevel > 0) ? (availableMP / costPerLevel) : 0;
                int levelsToDescend = Math.min(requestedLevels, affordableLevels);
                if (levelsToDescend < requestedLevels) {
                    logger.warn("[CLIMB-TRACE] Server capping CLIMB_DOWN: entity={} requested {} "
                                + "levels but {} walk MP at {} MP/level only allows {} (clinging at "
                                + "intermediate elevation).",
                          entity.getDisplayName(), requestedLevels, availableMP,
                          costPerLevel, levelsToDescend);
                }
                logger.debug("[CLIMB-TRACE] Server processing CLIMB_DOWN: entity={}, " +
                            "currentElevation={}, levelsToDescend={}, costPerLevel={}",
                      entity.getDisplayName(), currentElevation, levelsToDescend, costPerLevel);
                int levelsDescended = 0;
                boolean fellWhileDescending = false;
                for (int i = 1; i <= levelsToDescend; i++) {
                    PilotingRollData psr = entity.getBasePilotingRoll(EntityMovementType.MOVE_WALK);
                    psr.append(new PilotingRollData(entity.getId(),
                          ClimbingHelper.CLIMBING_PSR_MODIFIER,
                          "climbing down (level " + i + " of " + levelsToDescend + ")"));
                    if (climbableArms == 1) {
                        psr.append(new PilotingRollData(entity.getId(),
                              ClimbingHelper.ONE_ARM_PSR_MODIFIER,
                              "climbing with one arm"));
                    }
                    int psrElevation = currentElevation - levelsDescended;
                    if (gameManager.doSkillCheckWhileMoving(entity, psrElevation,
                          entity.getPosition(), entity.getPosition(), psr, true) > 0) {
                        // PSR failed - fall from current climb-down elevation
                        entity.setClimbing(false);
                        entity.setDangling(false);
                        entity.setClimbingLevelsChosen(0);
                        fellWhileDescending = true;
                        break;
                    }
                    levelsDescended++;
                }
                if (!fellWhileDescending) {
                    int newElevation = Math.max(floorRelativeForDescent,
                          currentElevation - levelsDescended);
                    entity.setElevation(newElevation);
                    // Climbing flag clears only when the Mek truly hits the hex floor — water
                    // bottom counts, water surface doesn't. Above the floor the Mek is still
                    // clinging and the next-turn dialog should reoffer Climb Down / Drop / Cling.
                    if (entityHasReachedFloor(entity)) {
                        entity.setClimbing(false);
                        entity.setDangling(false);
                        Report groundReport = new Report(6463, Report.PUBLIC);
                        groundReport.add(entity.getDisplayName());
                        addReport(groundReport);
                    } else {
                        entity.setClimbing(true);
                        entity.setDangling(false);
                    }
                    entity.setClimbingLevelsChosen(0);
                }
                entity.mpUsed = levelsDescended * costPerLevel;
                entity.delta_distance = 0;
                entity.moved = EntityMovementType.MOVE_WALK;
                entity.setDone(true);
                gameManager.entityUpdate(entity.getId());
                return;
            }
            boolean isDrop = (downStepCount >= 2);

            if (isDrop) {
                // DROP from climbing/dangling position: 4 MP required, leaping PSRs (TO:AR p.20).
                // Reject if the unit lacks the MP to drop — prevents a malformed client path
                // from triggering a free drop.
                int availableMP = entity.getWalkMP();
                if (availableMP < ClimbingHelper.DROP_MP_COST) {
                    logger.warn("[DANGLE-TRACE] Server rejecting DROP: entity={} has {} walk MP, "
                                + "needs {} for drop. Treating as cling.",
                          entity.getDisplayName(), availableMP, ClimbingHelper.DROP_MP_COST);
                    entity.setDone(true);
                    entity.moved = EntityMovementType.MOVE_NONE;
                    entity.mpUsed = 0;
                    entity.delta_distance = 0;
                    gameManager.entityUpdate(entity.getId());
                    return;
                }
                // From dangling: reduce modifiers by 2 (TO:AR p.20)
                // From climbing (not dangling): standard leaping modifiers.
                // dropDistance is the DRY portion of the fall — only levels above the hex surface
                // count for PSR/leg-damage purposes. Water/basement below the surface sinks the
                // Mek the rest of the way with no additional damage (water cushions the landing).
                int dropDistance = Math.max(0, entity.getElevation());
                int modifierReduction = entity.isDangling() ? ClimbingHelper.DANGLE_LEVELS_PER_TURN : 0;
                int effectiveDistance = Math.max(0, dropDistance - modifierReduction);
                logger.debug("[DANGLE-TRACE] Server processing DROP: entity={}, " +
                            "dropDistance={}, effectiveDistance={}, isDangling={}, modifierReduction={}",
                      entity.getDisplayName(), dropDistance, effectiveDistance,
                      entity.isDangling(), modifierReduction);

                // PSR 1: Leg damage check (modifier = 2 * effectiveDistance)
                String legDamageDesc = entity.isDangling()
                      ? String.format("hanging from level %d, effective height for leg damage roll %d",
                      dropDistance, effectiveDistance)
                      : String.format("dropping from level %d, leg damage roll", dropDistance);
                String fallDesc = entity.isDangling()
                      ? String.format("hanging from level %d, effective height for fall roll %d",
                      dropDistance, effectiveDistance)
                      : String.format("dropping from level %d, fall roll", dropDistance);
                if (effectiveDistance > 0) {
                    rollTarget = entity.getBasePilotingRoll(EntityMovementType.MOVE_WALK);
                    entity.addPilotingModifierForTerrain(rollTarget, entity.getPosition(),
                          entity.getBoardId());
                    rollTarget.append(new PilotingRollData(entity.getId(),
                          2 * effectiveDistance, legDamageDesc));
                    if (0 < gameManager.doSkillCheckWhileMoving(entity, entity.getElevation(),
                          entity.getPosition(), entity.getPosition(), rollTarget, false)) {
                        // Leg damage equal to effective distance
                        addReport(gameManager.damageEntity(entity,
                              new HitData(Mek.LOC_LEFT_LEG), effectiveDistance));
                        addReport(gameManager.damageEntity(entity,
                              new HitData(Mek.LOC_RIGHT_LEG), effectiveDistance));
                        addNewLines();
                        addReport(gameManager.criticalEntity(entity,
                              Mek.LOC_LEFT_LEG, false, 0, 0));
                        addNewLines();
                        addReport(gameManager.criticalEntity(entity,
                              Mek.LOC_RIGHT_LEG, false, 0, 0));
                    }

                    // PSR 2: Fall check (modifier = effectiveDistance)
                    rollTarget = entity.getBasePilotingRoll(EntityMovementType.MOVE_WALK);
                    entity.addPilotingModifierForTerrain(rollTarget, entity.getPosition(),
                          entity.getBoardId());
                    rollTarget.append(new PilotingRollData(entity.getId(),
                          effectiveDistance, fallDesc));
                    if (0 < gameManager.doSkillCheckWhileMoving(entity, entity.getElevation(),
                          entity.getPosition(), entity.getPosition(), rollTarget, true)) {
                        // Fall from elevation
                        entity.setDangling(false);
                        entity.setClimbing(false);
                        entity.setClimbingLevelsChosen(0);
                        entity.setDone(true);
                        entity.moved = EntityMovementType.MOVE_WALK;
                        gameManager.entityUpdate(entity.getId());
                        return;
                    }
                }

                // Both PSRs passed (or reducedDistance was 0) - safe landing. Land on the actual
                // hex floor: for dry hexes that's elev 0, for water/basement hexes the Mek sinks
                // through and settles on the floor below the surface.
                Hex landingHex = getGame().getBoard(entity.getBoardId()).getHex(entity.getPosition());
                int landingElevation = (landingHex != null)
                      ? landingHex.floor() - landingHex.getLevel()
                      : 0;
                entity.setElevation(landingElevation);
                entity.setDangling(false);
                entity.setClimbing(false);
                entity.setClimbingLevelsChosen(0);
                Report landReport = new Report(6463, Report.PUBLIC);
                landReport.add(entity.getDisplayName());
                addReport(landReport);
                entity.setDone(true);
                entity.moved = EntityMovementType.MOVE_WALK;
                gameManager.entityUpdate(entity.getId());
                return;

            } else if (!isDrop && ClimbingHelper.canDangle(entity)) {
                // DANGLE: lower by 2 levels, spend full turn. The descent extends into water /
                // basement depth if present (dangle is just hanging-and-lowering — works on the
                // underwater cliff face too, capped at the actual hex floor).
                int dangleFloor = hexFloorRelative(entity);
                int dangleableLevels = entity.getElevation() - dangleFloor;
                int dangleLevels = Math.min(ClimbingHelper.DANGLE_LEVELS_PER_TURN,
                      dangleableLevels);
                int newElevation = Math.max(dangleFloor, entity.getElevation() - dangleLevels);
                logger.debug("[DANGLE-TRACE] Server processing dangle: entity={}, " +
                            "currentElevation={}, dangleLevels={}, newElevation={}",
                      entity.getDisplayName(), entity.getElevation(), dangleLevels, newElevation);
                Report dangleReport = new Report(6462, Report.PUBLIC);
                dangleReport.add(entity.getDisplayName());
                dangleReport.add(dangleLevels);
                addReport(dangleReport);
                entity.setElevation(newElevation);
                entity.setClimbing(false);
                entity.setDangling(true);
                entity.setClimbingLevelsChosen(0);
                // Dangling flag clears only at the actual hex floor (water bottom for water,
                // ground for dry). Above the floor the Mek is still dangling and the next-turn
                // dialog should reoffer descent options.
                if (entityHasReachedFloor(entity)) {
                    entity.setDangling(false);
                    Report groundReport = new Report(6463, Report.PUBLIC);
                    groundReport.add(entity.getDisplayName());
                    addReport(groundReport);
                }
                entity.setDone(true);
                entity.moved = EntityMovementType.MOVE_WALK;
                gameManager.entityUpdate(entity.getId());
                return;
            }
        }

        if (md.getMpUsed() > 0) {
            // All auto-hit hexes for this unit (not including preset targets) are cleared
            // if any MP are expended.
            entity.aTracker.clearHitHexMods();
        }

        if (md.contains(MoveStepType.EJECT)) {
            if (entity.isLargeCraft() && !entity.isCarcass()) {
                report = new Report(2026);
                report.subject = entity.getId();
                report.addDesc(entity);
                addReport(report);
                Aero ship = (Aero) entity;
                ship.setEjecting(true);
                gameManager.entityUpdate(ship.getId());
                Coords legalPos = entity.getPosition();
                // Get the step so we can pass it in and get the abandon coords from it
                for (final ListIterator<MoveStep> i = md.getSteps(); i
                      .hasNext(); ) {
                    final MoveStep step = i.next();
                    if (step.getType() == MoveStepType.EJECT) {
                        legalPos = step.getTargetPosition();
                    }
                }
                addReport(gameManager.ejectSpacecraft(ship, ship.isSpaceborne(),
                      (ship.isAirborne() && !ship.isSpaceborne()), legalPos));
                // If we're grounded or destroyed by crew loss, end movement
                if (entity.isDoomed() || (!entity.isSpaceborne() && !entity.isAirborne())) {
                    return;
                }
            } else if ((entity instanceof Mek) || (entity instanceof Aero)) {
                report = new Report(2020);
                report.subject = entity.getId();
                report.add(entity.getCrew().getName());
                report.addDesc(entity);
                addReport(report);
                addReport(gameManager.ejectEntity(entity, false));
                return;
            } else if ((entity instanceof Tank) && !entity.isCarcass()) {
                report = new Report(2025);
                report.subject = entity.getId();
                report.addDesc(entity);
                addReport(report);
                addReport(gameManager.ejectEntity(entity, false));
                return;
            }
        }

        // Combat Vehicle Escape Pod launch (TO:AUE p.121)
        if (md.contains(MoveStepType.LAUNCH_ESCAPE_POD)) {
            if ((entity instanceof Tank tank) && tank.canLaunchEscapePod()) {
                // Find the step to get the player-chosen landing hex
                Coords landingCoords = null;
                for (MoveStep step : md.getStepVector()) {
                    if (step.getType() == MoveStepType.LAUNCH_ESCAPE_POD) {
                        landingCoords = step.getEscapePodLandingCoords();
                        break;
                    }
                }
                addReport(gameManager.launchCombatVehicleEscapePod(tank, landingCoords));
                // Mark entity's turn as complete so client advances to next unit
                entity.setDone(true);
                gameManager.entityUpdate(entity.getId());
                return;
            }
        }

        // Handle Mek abandonment announcements during Movement Phase (TacOps:AR p.165)
        // Mek must be prone and shutdown; crew exits during End Phase of following turn
        if (md.contains(MoveStepType.ABANDON) && (entity instanceof Mek mek) && mek.canAbandon()) {
            Vector<Report> abandonReports = gameManager.announceUnitAbandonment(entity);
            for (Report abandonReport : abandonReports) {
                addReport(abandonReport);
            }
            entity.setDone(true);
            gameManager.entityUpdate(entity.getId());
            return;
        }

        if (md.contains(MoveStepType.CAREFUL_STAND)) {
            entity.setCarefulStand(true);
        }

        entity.setJumpingWithMechanicalBoosters(md.contains(MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER));

        if (md.contains(MoveStepType.BACKWARDS)) {
            entity.setMovedBackwards(true);
            if (md.getMpUsed() > entity.getWalkMP()) {
                entity.setPowerReverse(true);
            }
        }

        if (md.contains(MoveStepType.TAKEOFF) && entity.isAero()) {
            if (!usingAeroOnGroundMovement() && !MovementDisplay.hasAtmosphericMapForLiftOff(getGame(), entity)) {
                logger.warn("Received lift off without aero-on-ground movement and without atmospheric map.");
            } else {
                IAero aero = (IAero) entity;
                int boardId = entity.getBoardId();
                if (usingAeroOnGroundMovement()) {
                    entity.setPosition(entity.getPosition().translated(entity.getFacing(), aero.getTakeOffLength()));
                } else {
                    positionOnAtmosphericMap();
                }
                aero.setCurrentVelocity(1);
                aero.liftOff(1);
                if (entity instanceof Dropship) {
                    gameManager.applyDropShipProximityDamage(md.getFinalCoords(), boardId, true, md.getFinalFacing(),
                          entity);
                }
                gameManager.checkForTakeoffDamage(aero);
            }
            entity.setDone(true);
            gameManager.entityUpdate(entity.getId());
            return;
        }

        if (md.contains(MoveStepType.VERTICAL_TAKE_OFF) && entity.isAero() && (entity instanceof IAero aero)) {
            if (!usingAeroOnGroundMovement() && !MovementDisplay.hasAtmosphericMapForLiftOff(getGame(), entity)) {
                logger.warn("Received lift off without aero-on-ground movement and without atmospheric map.");
            } else {
                rollTarget = aero.checkVerticalTakeOff();
                if (gameManager.doVerticalTakeOffCheck(entity, rollTarget)) {
                    int boardId = entity.getBoardId();
                    if (!usingAeroOnGroundMovement()) {
                        positionOnAtmosphericMap();
                    }
                    aero.setCurrentVelocity(1);
                    aero.liftOff(1);
                    if (entity instanceof Dropship dropship) {
                        gameManager.applyDropShipProximityDamage(md.getFinalCoords(), boardId, dropship);
                    }
                    gameManager.checkForTakeoffDamage(aero);
                }
            }
            entity.setDone(true);
            gameManager.entityUpdate(entity.getId());
            return;
        }

        if ((md.contains(MoveStepType.LAND) || md.contains(MoveStepType.VERTICAL_LAND)) && entity.isAero()) {
            IAero aero = (IAero) entity;
            boolean isVertical = md.contains(MoveStepType.VERTICAL_LAND);
            entity.setBoardId(md.getFinalBoardId());
            rollTarget = aero.getLandingControlRoll(md);
            gameManager.attemptLanding(entity, rollTarget, gameManager.getMainPhaseReport());
            if (isVertical && entity instanceof Dropship) {
                gameManager.applyDropShipLandingDamage(md.getFinalCoords(), md.getFinalBoardId(), (Dropship) aero);
            }
            Coords finalPosition = isVertical ?
                  md.getFinalCoords() :
                  md.getFinalCoords().translated(md.getFinalFacing(), aero.getLandingLength());
            gameManager.checkLandingTerrainEffects(aero, isVertical, md.getFinalCoords(), finalPosition,
                  md.getFinalBoardId(), md.getFinalFacing());
            aero.land();
            entity.setPosition(finalPosition);
            entity.setDone(true);
            gameManager.entityUpdate(entity.getId());
            return;
        }

        // okay, proceed with movement calculations
        lastPos = entity.getPosition();
        curPos = entity.getPosition();
        curBoardId = entity.getBoardId();
        boolean tookMagmaDamageAtStart = false; // Used to check for start/end magma damage
        curFacing = entity.getFacing();
        curVTOLElevation = entity.getElevation();
        lastElevation = entity.getElevation();
        curAltitude = entity.getAltitude();
        curClimbMode = entity.climbMode();
        // if the entity already used some MPs,
        // it previously tried to get up and fell,
        // and then got another turn. set moveType
        // and overallMoveType accordingly
        // (these are all cleared by Entity.newRound)
        distance = entity.delta_distance;
        mpUsed = entity.mpUsed;
        moveType = entity.moved;
        wasProne = entity.isProne();
        fellDuringMovement = false;
        crashedDuringMovement = false;
        dropshipStillUnloading = false;
        detectedHiddenHazard = false;
        prevFacing = curFacing;
        prevHex = getGame().getBoard(curBoardId).getHex(curPos);
        isInfantry = entity instanceof Infantry;
        // cache this here, otherwise changing MP in the turn causes
        // erroneous gravity PSRs

        // Replace ordinary passed through for these? Must also store the board id
        if (md.getFlightPathHex() != null && !md.getFlightPathHex().isNoLocation()) {
            BoardLocation location = md.getFlightPathHex();
            entity.setPassedThrough(new Vector<>(BoardHelper.coordsLine(getGame().getBoard(location),
                  location.coords(),
                  md.getFinalFacing())));
        }

        // get a list of coordinates that the unit passed through this turn
        // so that I can later recover potential bombing targets
        // it may already have some values
        passedThrough.addAll(entity.getPassedThrough());
        passedThrough.add(curPos);
        passedThroughFacing.addAll(entity.getPassedThroughFacing());
        passedThroughFacing.add(curFacing);

        // Compile the move - don't clip
        // Clipping could affect hidden units; illegal steps aren't processed
        md.compile(getGame(), entity, false);

        // if advanced movement is being used then set the new vectors based on
        // move path
        entity.setVectors(md.getFinalVectors());

        overallMoveType = md.getLastStepMovementType();

        Hex startingHex = getGame().getHex(entity.getBoardLocation());
        if (startingHex == null) {
            logger.warn(String.format("Attempted to skip turn for %s (%s) which is not on the board!", entity,
                  entity.getOwner().getName()));
            return;
        }

        // check for starting in liquid magma
        if ((startingHex.terrainLevel(Terrains.MAGMA) == 2) && (entity.getElevation() == 0)) {
            gameManager.doMagmaDamage(entity, false);
            tookMagmaDamageAtStart = true;
        }

        // check for starting in hazardous liquid
        if (startingHex.containsTerrain(Terrains.HAZARDOUS_LIQUID) && (entity.getElevation() <= 0)) {
            int depth = startingHex.containsTerrain(Terrains.WATER) ? startingHex.terrainLevel(Terrains.WATER) : 0;
            gameManager.doHazardousLiquidDamage(entity, false, depth);
        }

        // set acceleration used to default
        if (entity.isAero()) {
            ((IAero) entity).setAccLast(false);
        }

        // check for dropping troops and drop them
        if (entity.isDropping() && !md.contains(MoveStepType.HOVER)) {
            entity.setAltitude(entity.getAltitude() - getGame().getPlanetaryConditions().getDropRate());
            // they may have changed their facing
            if (md.length() > 0) {
                entity.setFacing(md.getFinalFacing());
            }
            passedThrough.add(entity.getPosition());
            entity.setPassedThrough(passedThrough);
            passedThroughFacing.add(entity.getFacing());
            entity.setPassedThroughFacing(passedThroughFacing);
            // We may still need to process any conversions for dropping LAMs
            if (entity instanceof LandAirMek && md.contains(MoveStepType.CONVERT_MODE)) {
                entity.setMovementMode(md.getFinalConversionMode());
                entity.setConvertingNow(true);
                report = new Report(1210);
                report.subject = entity.getId();
                report.addDesc(entity);
                if (entity.getMovementMode() == EntityMovementMode.WIGE) {
                    report.messageId = 2452;
                } else if (entity.getMovementMode() == EntityMovementMode.AERODYNE) {
                    report.messageId = 2453;
                } else {
                    report.messageId = 2450;
                }
                addReport(report);
            }
            entity.setDone(true);
            gameManager.entityUpdate(entity.getId());
            return;
        }

        // iterate through steps
        firstStep = true;
        turnOver = false;

        if (getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) {
            for (Entity e : getGame().getEntitiesVector()) {
                if (e.isHidden() && e.isEnemyOf(entity) && (e.getPosition() != null)) {
                    hiddenEnemies.add(e);
                }
            }
        }

        lastStepMoveType = md.getLastStepMovementType();

        processSteps();

        // If a unit started & ended its turn in magma, let's damage it again (TO:AR 35) TODO: build report for end of move
        if (tookMagmaDamageAtStart && prevHex.terrainLevel(Terrains.MAGMA) == 2
              && !(entity.getElevation() > 0 || entity.getMovementMode() == EntityMovementMode.HOVER)) {
            report = new Report(2404);
            report.addDesc(entity);
            report.subject = entity.getId();
            addReport(report);
            gameManager.doMagmaDamage(entity, false);
        }

        // Defensive: clear stale climbing/dangling flags when the entity ends its turn
        // at ground level and is not standing on a building roof. Catches state leaks
        // (e.g., dangling flag carried forward through a climb-up) that would otherwise
        // re-trigger the continue-climbing dialog at the start of the next turn.
        // Exception: a Mek partway up a multi-turn climb ends at the SOURCE hex of the
        // climb (see processSteps partial-climb branch). When the start elevation was
        // negative (water bottom, basement), that intermediate elevation can land at 0
        // while the Mek is genuinely clinging to the adjacent bridge/building. Preserve
        // climbing in that case so the continue-climbing dialog fires next turn.
        if ((entity.isClimbing() || entity.isDangling()) && (entity.getElevation() == 0)) {
            Hex finalHex = getGame().getBoard(entity.getBoardId()).getHex(curPos);
            boolean onBuildingRoof = (finalHex != null)
                  && finalHex.containsTerrain(Terrains.BUILDING)
                  && (entity.getElevation() >= finalHex.terrainLevel(Terrains.BLDG_ELEV));
            boolean clingingToAdjacentClimbable = entity.isClimbing()
                  && isClingingToAdjacentClimbable(entity, curPos, finalHex);
            if (!onBuildingRoof && !clingingToAdjacentClimbable) {
                logger.debug("[CLIMB-TRACE] Clearing stale climbing flags at end of move: entity={}, "
                            + "climbing={}, dangling={}, elevation=0 in hex {}",
                      entity.getDisplayName(), entity.isClimbing(), entity.isDangling(), curPos);
                entity.setClimbing(false);
                entity.setDangling(false);
                entity.setClimbingLevelsChosen(0);
            }
        }

        // set entity parameters
        logger.debug("End of movement: entity={}, curPos={}, climbing={}, elevation={}, curVTOLElevation={}",
              entity.getDisplayName(), curPos, entity.isClimbing(), entity.getElevation(), curVTOLElevation);
        entity.setPosition(curPos);
        entity.setFacing(curFacing);
        entity.setSecondaryFacing(curFacing);
        entity.delta_distance = distance;
        entity.moved = moveType;
        entity.mpUsed = mpUsed;
        if (md.isAllUnderwater(getGame())) {
            entity.underwaterRounds++;
            if ((entity instanceof ConvInfantry infantry) && (infantry.getMount() != null)
                  && entity.getMovementMode().isSubmarine()
                  && entity.underwaterRounds > infantry.getMount().getUWEndurance()) {
                report = new Report(2412);
                report.addDesc(entity);
                addReport(report);
                gameManager.destroyEntity(entity, "mount drowned");
            }
        } else {
            entity.underwaterRounds = 0;
        }
        entity.setClimbMode(curClimbMode);
        if (!sideslipped && !fellDuringMovement && !crashedDuringMovement
              && (entity.getMovementMode() == EntityMovementMode.VTOL)) {
            entity.setElevation(curVTOLElevation);
        }
        entity.setAltitude(curAltitude);
        entity.setClimbMode(curClimbMode);

        // add a list of places passed through
        entity.setPassedThrough(passedThrough);
        entity.setPassedThroughFacing(passedThroughFacing);
        entity.setPassedThroughBoardId(entity.getBoardId());

        // Replace ordinary passed through for aerospace on atmospheric board that designate a flight path on a ground
        // board
        if ((md.getFlightPathHex() != null) && !md.getFlightPathHex().isNoLocation()) {
            BoardLocation location = md.getFlightPathHex();
            entity.setPassedThrough(new Vector<>(BoardHelper.coordsLine(getGame().getBoard(location),
                  location.coords(),
                  md.getFinalFacing())));
            List<Integer> facings = new ArrayList<>();
            for (int i = 0; i < entity.getPassedThrough().size(); i++) {
                facings.add(md.getFinalFacing());
            }
            entity.setPassedThroughFacing(facings);
            entity.setPassedThroughBoardId(location.boardId());
        }

        // if we ran with destroyed hip or gyro, we need a psr
        rollTarget = entity.checkRunningWithDamage(overallMoveType);
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE && entity.canFall()) {
            gameManager.doSkillCheckInPlace(entity, rollTarget);
        }

        // if we sprinted with MASC or a supercharger, then we need a PSR
        rollTarget = entity.checkSprintingWithMASCXorSupercharger(overallMoveType,
              entity.mpUsed);
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE && entity.canFall()) {
            gameManager.doSkillCheckInPlace(entity, rollTarget);
        }

        // if we used ProtoMek myomer booster, roll 2d6
        // pilot damage on a 2
        if ((entity instanceof ProtoMek) && ((ProtoMek) entity).hasMyomerBooster()
              && (md.getMpUsed() > entity.getRunMP(MPCalculationSetting.NO_MYOMER_BOOSTER))) {
            report = new Report(2373);
            report.addDesc(entity);
            report.subject = entity.getId();
            Roll diceRoll = Compute.rollD6(2);
            report.add(diceRoll);

            if (diceRoll.getIntValue() > 2) {
                report.choose(true);
                addReport(report);
            } else {
                report.choose(false);
                addReport(report);
                addReport(gameManager.damageCrew(entity, 1));
            }
        }

        rollTarget = entity.checkSprintingWithMASCAndSupercharger(overallMoveType, entity.mpUsed);
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
            gameManager.doSkillCheckInPlace(entity, rollTarget);
        }
        if ((md.getLastStepMovementType() == EntityMovementType.MOVE_SPRINT)
              && (md.hasActiveMASC() || md.hasActiveSupercharger()) && entity.canFall()) {
            gameManager.doSkillCheckInPlace(entity, entity.getBasePilotingRoll(EntityMovementType.MOVE_SPRINT));
        }

        if (entity.isAirborne() && entity.isAero()) {

            IAero a = (IAero) entity;
            int thrust = md.getMpUsed();

            // consume fuel
            if (((entity.isAero())
                  && getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_FUEL_CONSUMPTION))
                  || (entity instanceof TeleMissile)) {
                int fuelUsed = ((IAero) entity).getFuelUsed(thrust);

                // if we're a gas hog, aerospace fighter and going faster than walking, then use
                // 2x fuel
                if (((overallMoveType == EntityMovementType.MOVE_RUN) ||
                      (overallMoveType == EntityMovementType.MOVE_SPRINT) ||
                      (overallMoveType == EntityMovementType.MOVE_OVER_THRUST)) &&
                      entity.hasQuirk(OptionsConstants.QUIRK_NEG_GAS_HOG)) {
                    fuelUsed *= 2;
                }

                a.useFuel(fuelUsed);
            }

            // JumpShips and space stations need to reduce accumulated thrust if
            // they spend some
            if (entity instanceof Jumpship jumpship) {
                double penalty = 0.0;
                // JumpShips do not accumulate thrust when they make a turn or
                // change velocity
                if (md.contains(MoveStepType.TURN_LEFT) || md.contains(MoveStepType.TURN_RIGHT)) {
                    // I need to subtract the station keeping thrust from their
                    // accumulated thrust
                    // because they did not actually use it
                    penalty = jumpship.getStationKeepingThrust();
                }
                if (thrust > 0) {
                    penalty = thrust;
                }
                if (penalty > 0.0) {
                    jumpship.setAccumulatedThrust(Math.max(0, jumpship.getAccumulatedThrust() - penalty));
                }
            }

            // check to see if thrust exceeded SI

            rollTarget = a.checkThrustSITotal(thrust, overallMoveType);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                getGame().addControlRoll(new PilotingRollData(entity.getId(), 0,
                      "Thrust spent during turn exceeds SI"));
            }

            if (!getGame().getBoard(entity.getBoardId()).isSpace()) {
                rollTarget = a.checkVelocityDouble(md.getFinalVelocity(),
                      overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    getGame().addControlRoll(new PilotingRollData(entity.getId(), 0,
                          "Velocity greater than 2x safe thrust"));
                }

                rollTarget = a.checkDown(md.getFinalNDown(), overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    getGame().addControlRoll(
                          new PilotingRollData(entity.getId(), md.getFinalNDown(),
                                "descended more than two altitudes"));
                }

                // check for hovering
                rollTarget = a.checkHover(md);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    getGame().addControlRoll(
                          new PilotingRollData(entity.getId(), 0, "hovering"));
                }

                // check for aero stall
                rollTarget = a.checkStall(md);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    report = new Report(9391);
                    report.subject = entity.getId();
                    report.addDesc(entity);
                    addReport(report);
                    getGame().addControlRoll(new PilotingRollData(entity.getId(), 0,
                          "stalled out"));
                    entity.setAltitude(entity.getAltitude() - 1);
                    // check for crash
                    if (gameManager.checkCrash(entity)) {
                        addReport(gameManager.processCrash(entity, 0, entity.getPosition()));
                    }
                }

                // check to see if spheroids should lose one altitude
                if (a.isSpheroid() && !a.isSpaceborne()
                      && a.isAirborne() && (md.getFinalNDown() == 0) && (md.getMpUsed() == 0)) {
                    report = new Report(9392);
                    report.subject = entity.getId();
                    report.addDesc(entity);
                    addReport(report);
                    entity.setAltitude(entity.getAltitude() - 1);
                    // check for crash
                    if (gameManager.checkCrash(entity)) {
                        addReport(gameManager.processCrash(entity, 0, entity.getPosition()));
                    }
                } else if (entity instanceof EscapePods && entity.isAirborne() && md.getFinalVelocity() < 2) {
                    // Atmospheric Escape Pods that drop below velocity 2 lose altitude as dropping
                    // units
                    entity.setAltitude(entity.getAltitude()
                          - getGame().getPlanetaryConditions().getDropRate());
                    report = new Report(6676);
                    report.subject = entity.getId();
                    report.addDesc(entity);
                    report.add(getGame().getPlanetaryConditions().getDropRate());
                    addReport(report);
                }
            }
        }

        // We need to check for the removal of hull-down for tanks.
        // Tanks can just drive out of hull-down: if the tank was hull-down
        // and doesn't end hull-down we can remove the hull-down status
        if (entity.isHullDown() && !md.getFinalHullDown()
              && (entity instanceof Tank
              || (entity instanceof QuadVee && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE))) {
            logger.debug("[HullDown] {}: hull-down cleared on move - path did not end hull-down",
                  entity.getDisplayName());
            entity.setHullDown(false);
        }

        // If the entity is being swarmed, erratic movement may dislodge the
        // fleas.
        final int swarmerId = entity.getSwarmAttackerId();
        if ((Entity.NONE != swarmerId) && md.contains(MoveStepType.SHAKE_OFF_SWARMERS)) {
            final Entity swarmer = getGame().getEntity(swarmerId);
            if (swarmer != null) {


                rollTarget = entity.getBasePilotingRoll(overallMoveType);

                entity.addPilotingModifierForTerrain(rollTarget);

                // Add a +4 modifier.
                if (md.getLastStepMovementType() == EntityMovementType.MOVE_VTOL_RUN) {
                    rollTarget.addModifier(2, "dislodge swarming infantry with VTOL movement");
                } else {
                    rollTarget.addModifier(4, "dislodge swarming infantry");
                }

                // If the swarmer has Assault claws, give a 1 modifier.
                // We can stop looking when we find our first match.
                for (Mounted<?> mount : swarmer.getMisc()) {
                    EquipmentType equip = mount.getType();
                    if (equip.hasFlag(MiscType.F_MAGNET_CLAW)) {
                        rollTarget.addModifier(1, "swarmer has magnetic claws");
                        break;
                    }
                }

                // okay, print the info
                report = new Report(2125);
                report.subject = entity.getId();
                report.addDesc(entity);
                addReport(report);

                // roll
                final Roll diceRoll = Compute.rollD6(2);
                report = new Report(2130);
                report.subject = entity.getId();
                report.add(rollTarget.getValueAsString());
                report.add(rollTarget.getDesc());
                report.add(diceRoll);

                if (diceRoll.getIntValue() < rollTarget.getValue()) {
                    report.choose(false);
                    addReport(report);
                } else {
                    // Dislodged swarmers don't get turns.
                    getGame().removeTurnFor(swarmer);
                    gameManager.send(gameManager.getPacketHelper().createTurnListPacket());

                    // Update the report and the swarmer's status.
                    report.choose(true);
                    addReport(report);
                    entity.setSwarmAttackerId(Entity.NONE);
                    swarmer.setSwarmTargetId(Entity.NONE);

                    Hex curHex = getGame().getBoard(curBoardId).getHex(curPos);

                    // Did the infantry fall into water?
                    if (curHex.terrainLevel(Terrains.WATER) > 0) {
                        // Swarming infantry die.
                        swarmer.setPosition(curPos);
                        report = new Report(2135);
                        report.subject = entity.getId();
                        report.indent();
                        report.addDesc(swarmer);
                        addReport(report);
                        addReport(gameManager.destroyEntity(swarmer, "a watery grave", false));
                    } else {
                        // Swarming infantry take a 3d6 point hit.
                        // ASSUMPTION : damage should not be doubled.
                        report = new Report(2140);
                        report.subject = entity.getId();
                        report.indent();
                        report.addDesc(swarmer);
                        report.add("3d6");
                        addReport(report);
                        addReport(gameManager.damageEntity(swarmer,
                              swarmer.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT),
                              Compute.d6(3)));
                        addNewLines();
                        swarmer.setPosition(curPos);
                    }
                    gameManager.entityUpdate(swarmerId);
                } // End successful-PSR
            }
        } // End try-to-dislodge-swarmers

        // but the danger isn't over yet! landing from a jump can be risky!
        if ((overallMoveType == EntityMovementType.MOVE_JUMP) && !entity.isMakingDfa()) {
            final Hex curHex = getGame().getBoard(curBoardId).getHex(curPos);

            // check for damaged criticalSlots
            rollTarget = entity.checkLandingWithDamage(overallMoveType);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                gameManager.doSkillCheckInPlace(entity, rollTarget);
            }

            // check for prototype JJs
            rollTarget = entity.checkLandingWithPrototypeJJ(overallMoveType);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                gameManager.doSkillCheckInPlace(entity, rollTarget);
            }

            // check for jumping into heavy woods
            if (getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_PSR_JUMP_HEAVY_WOODS)) {
                rollTarget = entity.checkLandingInHeavyWoods(overallMoveType, curHex);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    gameManager.doSkillCheckInPlace(entity, rollTarget);
                }
            }

            // Mechanical jump boosters fall damage
            if (md.shouldMechanicalJumpCauseFallDamage()) {
                gameManager.getMainPhaseReport().addAll(gameManager.doEntityFallsInto(entity,
                      entity.getElevation(), md.getJumpPathHighestPoint(),
                      curPos, entity.getBasePilotingRoll(overallMoveType),
                      false, entity.getMechanicalJumpBoosterMP()));
            }

            // jumped into water?
            int waterLevel = curHex.terrainLevel(Terrains.WATER);
            if (curHex.containsTerrain(Terrains.ICE) && (waterLevel > 0)) {
                if (!(entity instanceof Infantry)) {
                    // check for breaking ice
                    Roll diceRoll = Compute.rollD6(1);
                    report = new Report(2122);
                    report.add(entity.getDisplayName(), true);
                    report.add(diceRoll);
                    report.subject = entity.getId();
                    addReport(report);

                    if (diceRoll.getIntValue() >= 4) {
                        // oops!
                        entity.setPosition(curPos);
                        addReport(gameManager.resolveIceBroken(curPos));
                        curPos = entity.getPosition();
                    } else {
                        // TacOps: immediate PSR with +4 for terrain. If you
                        // fall then may break the ice after all
                        rollTarget = entity.checkLandingOnIce(overallMoveType, curHex);
                        if (!gameManager.doSkillCheckInPlace(entity, rollTarget)) {
                            // apply damage now, or it will show up as a
                            // possible breach, if ice is broken
                            entity.applyDamage();
                            Roll diceRoll2 = Compute.rollD6(1);
                            report = new Report(2118);
                            report.addDesc(entity);
                            report.add(diceRoll2);
                            report.subject = entity.getId();
                            addReport(report);

                            if (diceRoll2.getIntValue() == 6) {
                                entity.setPosition(curPos);
                                addReport(gameManager.resolveIceBroken(curPos));
                                curPos = entity.getPosition();
                            }
                        }
                    }
                }
            } else if (!(prevStep.climbMode() && curHex.containsTerrain(Terrains.BRIDGE))
                  && !(entity.getMovementMode().isHoverOrWiGE())) {
                rollTarget = entity.checkWaterMove(waterLevel, overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    // For falling elevation, Entity must not on hex surface
                    int currElevation = entity.getElevation();
                    entity.setElevation(0);
                    boolean success = gameManager.doSkillCheckInPlace(entity, rollTarget);
                    if (success) {
                        entity.setElevation(currElevation);
                    }
                }
                if (waterLevel > 1) {
                    // Any swarming infantry will be destroyed.
                    gameManager.drownSwarmer(entity, curPos);
                }
            }

            // check for black ice
            boolean useBlackIce = getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_BLACK_ICE);
            boolean goodTemp = getGame().getPlanetaryConditions()
                  .getTemperature() <= PlanetaryConditions.BLACK_ICE_TEMP;
            boolean goodWeather = getGame().getPlanetaryConditions().getWeather().isIceStorm();
            if ((useBlackIce && goodTemp) || goodWeather) {
                if (ServerHelper.checkEnteringBlackIce(gameManager, curPos, curHex, useBlackIce, goodTemp,
                      goodWeather)) {
                    rollTarget = entity.checkLandingOnBlackIce(overallMoveType, curHex);
                    if (!gameManager.doSkillCheckInPlace(entity, rollTarget)) {
                        entity.applyDamage();
                    }
                }
            }

            // check for building collapse
            IBuilding bldg = getGame().getBoard(curBoardId).getBuildingAt(curPos);
            if (bldg != null) {
                gameManager.checkForCollapse(bldg, curPos, true,
                      gameManager.getMainPhaseReport());
            }

            // Don't interact with terrain when jumping onto a building or a bridge
            if (entity.getElevation() == 0) {
                ServerHelper.checkAndApplyMagmaCrust(curHex, entity.getElevation(), entity, curPos, true,
                      gameManager.getMainPhaseReport(), gameManager);
                ServerHelper.checkEnteringMagma(curHex, entity.getElevation(), entity, gameManager);
                ServerHelper.checkEnteringHazardousLiquid(curHex, entity.getElevation(), entity, gameManager);
                ServerHelper.checkEnteringUltraSublevel(curHex, entity.getElevation(), entity, gameManager);

                // jumped into swamp? maybe stuck!
                if (curHex.getBogDownModifier(entity.getMovementMode(),
                      entity instanceof LargeSupportTank) != TargetRoll.AUTOMATIC_SUCCESS) {
                    if (entity instanceof Mek) {
                        entity.setStuck(true);
                        report = new Report(2121);
                        report.add(entity.getDisplayName(), true);
                        report.subject = entity.getId();
                        addReport(report);
                        // check for quicksand
                        addReport(gameManager.checkQuickSand(curPos));
                    } else if (!entity.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
                        rollTarget = new PilotingRollData(entity.getId(),
                              5, "entering boggy terrain");
                        rollTarget.append(new PilotingRollData(entity.getId(),
                              curHex.getBogDownModifier(entity.getMovementMode(),
                                    entity instanceof LargeSupportTank),
                              "avoid bogging down"));
                        if (0 < gameManager.doSkillCheckWhileMoving(entity, entity.getElevation(), curPos, curPos,
                              rollTarget, false)) {
                            entity.setStuck(true);
                            report = new Report(2081);
                            report.add(entity.getDisplayName());
                            report.subject = entity.getId();
                            addReport(report);
                            // check for quicksand
                            addReport(gameManager.checkQuickSand(curPos));
                        }
                    }
                }
            }

            // If the entity is being swarmed, jumping may dislodge the fleas.
            if (Entity.NONE != swarmerId) {
                final Entity swarmer = getGame().getEntity(swarmerId);
                if (swarmer != null) {
                    rollTarget = entity.getBasePilotingRoll(overallMoveType);

                    entity.addPilotingModifierForTerrain(rollTarget);

                    // Add a +4 modifier.
                    rollTarget.addModifier(4, "dislodge swarming infantry");

                    // If the swarmer has Assault claws, give a 1 modifier.
                    // We can stop looking when we find our first match.
                    if (swarmer.hasWorkingMisc(MiscTypeFlag.F_MAGNET_CLAW)) {
                        rollTarget.addModifier(1, "swarmer has magnetic claws");
                    }

                    // okay, print the info
                    report = new Report(2125);
                    report.subject = entity.getId();
                    report.addDesc(entity);
                    addReport(report);

                    // roll
                    final Roll diceRoll = Compute.rollD6(2);
                    report = new Report(2130);
                    report.subject = entity.getId();
                    report.add(rollTarget.getValueAsString());
                    report.add(rollTarget.getDesc());
                    report.add(diceRoll);

                    if (diceRoll.getIntValue() < rollTarget.getValue()) {
                        report.choose(false);
                        addReport(report);
                    } else {
                        // Dislodged swarmers don't get turns.
                        getGame().removeTurnFor(swarmer);
                        gameManager.send(gameManager.getPacketHelper().createTurnListPacket());

                        // Update the report and the swarmer's status.
                        report.choose(true);
                        addReport(report);
                        entity.setSwarmAttackerId(Entity.NONE);
                        swarmer.setSwarmTargetId(Entity.NONE);

                        // Did the infantry fall into water?
                        if (curHex.terrainLevel(Terrains.WATER) > 0) {
                            // Swarming infantry die.
                            swarmer.setPosition(curPos);
                            report = new Report(2135);
                            report.subject = entity.getId();
                            report.indent();
                            report.addDesc(swarmer);
                            addReport(report);
                            addReport(gameManager.destroyEntity(swarmer, "a watery grave", false));
                        } else {
                            // Swarming infantry take a 3d6 point hit.
                            // ASSUMPTION : damage should not be doubled.
                            report = new Report(2140);
                            report.subject = entity.getId();
                            report.indent();
                            report.addDesc(swarmer);
                            report.add("3d6");
                            addReport(report);
                            addReport(gameManager.damageEntity(swarmer,
                                  swarmer.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT),
                                  Compute.d6(3)));
                            addNewLines();
                            swarmer.setPosition(curPos);
                        }
                        gameManager.entityUpdate(swarmerId);
                    } // End successful-PSR
                }
            } // End try-to-dislodge-swarmers

            // one more check for inferno wash-off
            gameManager.checkForWashedInfernos(entity, curPos, entity.getBoardId());

            // a jumping tank needs to roll for movement damage
            if (entity instanceof Tank) {
                int modifier = 0;
                if (curHex.containsTerrain(Terrains.ROUGH)
                      || curHex.containsTerrain(Terrains.WOODS)
                      || curHex.containsTerrain(Terrains.JUNGLE)) {
                    modifier = 1;
                }
                report = new Report(2126);
                report.subject = entity.getId();
                report.addDesc(entity);
                gameManager.getMainPhaseReport().add(report);
                gameManager.getMainPhaseReport().addAll(gameManager.vehicleMotiveDamage((Tank) entity, modifier,
                      false, -1, true));
                Report.addNewline(gameManager.getMainPhaseReport());
            }

        } // End entity-is-jumping

        // If converting to another mode, set the final movement mode and report it
        if (entity.isConvertingNow()) {
            report = new Report(1210);
            report.subject = entity.getId();
            report.addDesc(entity);
            if (entity instanceof QuadVee && entity.isProne()
                  && entity.getConversionMode() == QuadVee.CONV_MODE_MEK) {
                // Fall while converting to vehicle mode cancels conversion.
                entity.setConvertingNow(false);
                report.messageId = 2454;
            } else {
                // LAMs converting from fighter mode need to have the elevation set properly.
                if (entity.isAero()) {
                    if (md.getFinalConversionMode() == EntityMovementMode.WIGE
                          && entity.getAltitude() > 0 && entity.getAltitude() <= 3) {
                        entity.setElevation(entity.getAltitude() * 10);
                        entity.setAltitude(0);
                    } else {
                        Hex hex = getGame().getBoard(curBoardId).getHex(entity.getPosition());
                        if (hex.containsTerrain(Terrains.BLDG_ELEV)) {
                            entity.setElevation(hex.terrainLevel(Terrains.BLDG_ELEV));
                        } else {
                            entity.setElevation(0);
                        }
                    }
                }
                entity.setMovementMode(md.getFinalConversionMode());
                if (entity instanceof Mek && ((Mek) entity).hasTracks()) {
                    report.messageId = 2455;
                    report.choose(entity.getMovementMode() == EntityMovementMode.TRACKED);
                } else if (entity.getMovementMode() == EntityMovementMode.TRACKED
                      || entity.getMovementMode() == EntityMovementMode.WHEELED) {
                    report.messageId = 2451;
                } else if (entity.getMovementMode() == EntityMovementMode.WIGE) {
                    report.messageId = 2452;
                } else if (entity.getMovementMode() == EntityMovementMode.AERODYNE) {
                    report.messageId = 2453;
                } else {
                    report.messageId = 2450;
                }
                if (entity.isAero()) {
                    int altitude = entity.getAltitude();
                    if (altitude == 0 && md.getFinalElevation() >= 8) {
                        altitude = 1;
                    }
                    if (altitude == 0) {
                        ((IAero) entity).land();
                    } else {
                        ((IAero) entity).liftOff(altitude);
                    }
                }
            }
            addReport(report);
        }

        // update entity's locations' exposure
        gameManager.addReport(gameManager.doSetLocationsExposure(entity,
              getGame().getBoard(curBoardId).getHex(curPos), false, entity.getElevation()));

        // Check the falls_end_movement option to see if it should be able to
        // move on.
        // Need to check here if the 'Mek actually went from non-prone to prone
        // here because 'fellDuringMovement' is sometimes abused just to force
        // another turn and so doesn't reliably tell us.
        boolean continueTurnFromFall = !(getGame().getOptions()
              .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_FALLS_END_MOVEMENT)
              && (entity instanceof Mek) && !wasProne && entity.isProne())
              && (fellDuringMovement && !entity.isCarefulStand()) // Careful standing takes up the whole turn
              && !turnOver && (entity.mpUsed < entity.getRunMP())
              && (overallMoveType != EntityMovementType.MOVE_JUMP);
        if ((continueTurnFromFall || continueTurnFromPBS || continueTurnFromFishtail || continueTurnFromLevelDrop
              || continueTurnFromCliffAscent
              || detectedHiddenHazard)
              && entity.isSelectableThisTurn() && !entity.isDoomed()) {
            entity.applyDamage();
            entity.setDone(false);
            entity.setTurnInterrupted(true);

            GameTurn newTurn = new SpecificEntityTurn(entity.getOwner().getId(), entity.getId());
            // Need to set the new turn's multiTurn state
            newTurn.setMultiTurn(true);
            getGame().insertNextTurn(newTurn);
            // brief everybody on the turn update
            gameManager.send(gameManager.getPacketHelper().createTurnListPacket());

            // let everyone know about what just happened
            // Skip if we already sent an EMP popup this move (avoid duplicate popups)
            if ((gameManager.getMainPhaseReport().size() > 1) && !sentEMPPopupThisMove) {
                gameManager.send(entity.getOwner().getId(), gameManager.createSpecialReportPacket());
            }
        } else {
            if (entity.getMovementMode() == EntityMovementMode.WIGE) {
                Hex hex = getGame().getBoard(curBoardId).getHex(curPos);
                if (md.automaticWiGELanding(false)) {
                    // try to land safely; LAMs require a psr when landing with gyro or leg actuator
                    // damage and ProtoMeks always require a roll
                    int elevation = (null == prevStep) ? entity.getElevation() : prevStep.getElevation();
                    if (entity.hasETypeFlag(Entity.ETYPE_LAND_AIR_MEK) && entity instanceof LandAirMek landAirMek) {
                        addReport(gameManager.landAirMek(landAirMek,
                              entity.getPosition(),
                              elevation,
                              entity.delta_distance));
                    } else if (entity.hasETypeFlag(Entity.ETYPE_PROTOMEK) && entity instanceof ProtoMek protoMek) {
                        gameManager.getMainPhaseReport()
                              .addAll(gameManager.landGliderPM(protoMek,
                                    entity.getPosition(),
                                    elevation,
                                    entity.delta_distance));
                    } else {
                        report = new Report(2123);
                        report.addDesc(entity);
                        report.subject = entity.getId();
                        addReport(report);
                    }

                    if (hex.containsTerrain(Terrains.BLDG_ELEV)) {
                        IBuilding bldg = getGame().getBoard(curBoardId).getBuildingAt(entity.getPosition());
                        entity.setElevation(hex.terrainLevel(Terrains.BLDG_ELEV));
                        gameManager.addAffectedBldg(bldg,
                              gameManager.checkBuildingCollapseWhileMoving(bldg, entity, entity.getPosition()));
                    } else if (entity.isLocationProhibited(entity.getPosition(), 0) && !hex.hasPavement()) {
                        // crash
                        report = new Report(2124);
                        report.addDesc(entity);
                        report.subject = entity.getId();
                        addReport(report);

                        if (entity instanceof Tank tankEntity) {
                            addReport(gameManager.crashVTOLorWiGE(tankEntity));
                        }
                    } else {
                        entity.setElevation(0);
                    }

                    // Check for stacking violations in the target hex
                    Entity violation = Compute.stackingViolation(getGame(),
                          entity, entity.getPosition(), null, entity.climbMode(), false);
                    if (violation != null) {
                        PilotingRollData prd = new PilotingRollData(
                              violation.getId(), 2, "fallen on");
                        if (violation instanceof Dropship) {
                            violation = entity;
                            prd = null;
                        }
                        Coords targetDest = Compute.getValidDisplacement(getGame(),
                              violation.getId(), entity.getPosition(), 0);
                        if (targetDest != null) {
                            addReport(gameManager.doEntityDisplacement(violation,
                                  entity.getPosition(), targetDest, prd));

                            // Update the violating entity's position on the
                            // client.
                            gameManager.entityUpdate(violation.getId());
                        } else {
                            // ack! automatic death! Tanks
                            // suffer an ammo/power plant hit.
                            // TODO : a Mek suffers a Head Blown Off crit.
                            addReport(gameManager.destroyEntity(violation,
                                  "impossible displacement",
                                  violation instanceof Mek,
                                  violation instanceof Mek));
                        }
                    }
                } else if (!entity.hasETypeFlag(Entity.ETYPE_LAND_AIR_MEK)
                      && !entity.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {

                    // we didn't land, so we go to elevation 1 above the terrain
                    // features
                    // it might have been higher than one due to the extra MPs
                    // it can spend to stay higher during movement, but should
                    // end up at one

                    entity.setElevation(Math.min(entity.getElevation(),
                          1 + hex.maxTerrainFeatureElevation(
                                getGame().getBoard(curBoardId).isLowAltitude())));
                }
            }

            // If we've somehow gotten here as an airborne LAM with a destroyed side torso
            // (such as conversion while dropping), crash now.
            if (entity instanceof LandAirMek
                  && (entity.isLocationBad(Mek.LOC_RIGHT_TORSO) || entity.isLocationBad(Mek.LOC_LEFT_TORSO))) {
                report = new Report(9710);
                report.subject = entity.getId();
                report.addDesc(entity);
                if (entity.isAirborneVTOLorWIGE()) {
                    addReport(report);
                    gameManager.crashAirMek(entity, new PilotingRollData(entity.getId(), TargetRoll.AUTOMATIC_FAIL,
                          "side torso destroyed"), gameManager.getMainPhaseReport());
                } else if (entity.isAirborne() && entity.isAero()) {
                    addReport(report);
                    addReport(gameManager.processCrash(entity, ((IAero) entity).getCurrentVelocity(),
                          entity.getPosition()));
                }
            }

            entity.setDone(true);
        }

        if (dropshipStillUnloading) {
            // turns should have already been inserted, but we need to set the entity as not done
            entity.setDone(false);
        }

        // If the entity is being swarmed, update the attacker's position.
        if (Entity.NONE != swarmerId) {
            final Entity swarmer = getGame().getEntity(swarmerId);
            if (swarmer != null) {
                swarmer.setPosition(curPos);
                // If the hex is on fire, and the swarming infantry is
                // *not* Battle Armor, it drops off.
                if (!(swarmer instanceof BattleArmor) && getGame().getBoard(curBoardId)
                      .getHex(curPos).containsTerrain(Terrains.FIRE)) {
                    swarmer.setSwarmTargetId(Entity.NONE);
                    entity.setSwarmAttackerId(Entity.NONE);
                    report = new Report(2145);
                    report.subject = entity.getId();
                    report.indent();
                    report.add(swarmer.getShortName(), true);
                    addReport(report);
                }
                gameManager.entityUpdate(swarmerId);
            }
        }

        // Update the entity's position,
        // unless it is off the game map.
        if (!getGame().isOutOfGame(entity)) {
            gameManager.entityUpdate(entity.getId(), movePath, true, losCache);
            if (entity.isDoomed()) {
                gameManager.send(gameManager.createRemoveEntityPacket(entity.getId(),
                      entity.getRemovalCondition()));
            }
        }

        // If the entity is towing trailers, update the position of those trailers
        if (!entity.getAllTowedUnits().isEmpty()) {
            // initialize with a copy (no need to initialize to an empty list first)
            List<Integer> reversedTrailers = new ArrayList<>(entity.getAllTowedUnits());

            // reverse in-place
            Collections.reverse(reversedTrailers);

            // no need to initialize to an empty list first
            List<Coords> trailerPath = gameManager.initializeTrailerCoordinates(entity, reversedTrailers);
            gameManager.processTrailerMovement(entity, trailerPath);
        }

        // recovered units should now be recovered and dealt with
        if (entity.isAero() && recovered && (loader != null)) {

            if (loader.isCapitalFighter()) {
                if (!(loader instanceof FighterSquadron)) {
                    // this is a solo capital fighter so we need to add a new
                    // squadron and load both the loader and loaded
                    FighterSquadron fighterSquadron = new FighterSquadron();
                    fighterSquadron.setDeployed(true);
                    fighterSquadron.setId(getGame().getNextEntityId());
                    fighterSquadron.setCurrentVelocity(((Aero) loader).getCurrentVelocity());
                    fighterSquadron.setNextVelocity(((Aero) loader).getNextVelocity());
                    fighterSquadron.setVectors(loader.getVectors());
                    fighterSquadron.setFacing(loader.getFacing());
                    fighterSquadron.setOwner(entity.getOwner());
                    // set velocity and heading the same as parent entity
                    getGame().addEntity(fighterSquadron);
                    gameManager.send(gameManager.createAddEntityPacket(fighterSquadron.getId()));
                    // make him not get a move this turn
                    fighterSquadron.setDone(true);
                    // place on board
                    fighterSquadron.setPosition(loader.getPosition());
                    gameManager.loadUnit(fighterSquadron, loader, UNSET_BAY);
                    loader = fighterSquadron;
                    gameManager.entityUpdate(fighterSquadron.getId());
                }
                loader.load(entity);
            } else {
                loader.recover(entity);
                entity.setRecoveryTurn(5);
            }

            // The loaded unit is being carried by the loader.
            entity.setTransportId(loader.getId());

            // Remove the loaded unit from the screen.
            entity.setPosition(null);

            // Update the loaded unit.
            gameManager.entityUpdate(entity.getId());
        }

        // even if load was unsuccessful, I may need to update the loader
        if (null != loader) {
            gameManager.entityUpdate(loader.getId());
        }

        // if using double-blind, update the player on new units he might see
        if (gameManager.doBlind()) {
            gameManager.send(entity.getOwner().getId(),
                  gameManager.createFilteredFullEntitiesPacket(entity.getOwner(), losCache));
        }

        // if we generated a charge attack, report it now
        if (charge != null) {
            gameManager.send(gameManager.getPacketHelper().createChargeAttackPacket(charge));
        }

        // if we generated a ram attack, report it now
        if (ram != null) {
            gameManager.send(gameManager.getPacketHelper().createChargeAttackPacket(ram));
        }
        if ((entity instanceof Mek) && entity.hasEngine() && ((Mek) entity).isIndustrial()
              && !entity.hasEnvironmentalSealing()
              && (entity.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)) {
            if ((!entity.isProne()
                  && (getGame().getBoard(curBoardId).getHex(entity.getPosition())
                  .terrainLevel(Terrains.WATER) >= 2))
                  || (entity.isProne()
                  && (getGame().getBoard(curBoardId).getHex(entity.getPosition())
                  .terrainLevel(Terrains.WATER) == 1))) {
                ((Mek) entity).setJustMovedIntoIndustrialKillingWater(true);

            } else {
                ((Mek) entity).setJustMovedIntoIndustrialKillingWater(false);
                ((Mek) entity).setShouldDieAtEndOfTurnBecauseOfWater(false);
            }
        }

        // check for fleeing
        if (md.contains(MoveStepType.FLEE)) {
            if (entity.canFlee(entity.getPosition())) {
                addReport(gameManager.processLeaveMap(md));
            } else {
                report = new Report(2017, Report.PUBLIC);
                report.indent();
                addReport(report);
            }
        }
    }

    /**
     * Returns the entity-relative elevation of its current hex's actual floor — 0 for dry hexes,
     * negative for water/basement hexes. Multi-arity descents (climb-down, dangle, drop) treat
     * this as the lower bound: descending past elev 0 in a water hex is fine as long as the Mek
     * stays at or above this floor.
     */
    private int hexFloorRelative(Entity entity) {
        Hex hex = getGame().getBoard(entity.getBoardId()).getHex(entity.getPosition());
        return (hex == null) ? 0 : hex.floor() - hex.getLevel();
    }

    /**
     * Returns true when the entity has reached the actual floor of its current hex — dry ground
     * for a normal hex, water bottom for a water hex, basement floor for a basement hex. Used by
     * every climb / dangle / drop completion path: only at the hex floor is the descent really
     * "done"; anywhere above it (clinging on a cliff face, on a building wall, or on the
     * underwater portion of either) the unit is still in the air/water column and the climbing
     * or dangling flag must stay set so the next-turn dialog fires.
     */
    private boolean entityHasReachedFloor(Entity entity) {
        return entity.getElevation() == hexFloorRelative(entity);
    }

    /**
     * Returns true when the entity at {@code curPos} looks like it is clinging to a climbable
     * feature (bridge or building) in its facing hex — i.e. that hex has a bridge or building roof
     * higher than the entity's current absolute altitude. Used by the end-of-movement defensive
     * cleanup to distinguish a legitimate mid-multi-turn climb (where the partial-climb branch in
     * {@link #processSteps} intentionally leaves {@code climbing=true} in the SOURCE hex) from a
     * stale flag that should be wiped. Without this gate, a Mek that climbed out of deep water onto
     * a bridge stops mid-climb at water-surface (elevation 0), gets its climbing flag cleared, and
     * never sees the continue-climbing dialog on the following turn.
     *
     * @param entity   the entity being checked
     * @param curPos   the entity's current hex (source hex of an in-progress climb)
     * @param curHex   the resolved hex for {@code curPos}, or null if the board doesn't have it
     * @return true if the facing hex has a climbable feature above the entity
     */
    private boolean isClingingToAdjacentClimbable(Entity entity, Coords curPos, @Nullable Hex curHex) {
        if (curHex == null) {
            return false;
        }
        Coords adjacent = curPos.translated(entity.getFacing());
        Hex adjacentHex = getGame().getBoard(entity.getBoardId()).getHex(adjacent);
        if (adjacentHex == null) {
            return false;
        }
        int entityAbsAlt = curHex.getLevel() + entity.getElevation();
        int adjacentBase = adjacentHex.getLevel();
        if (adjacentHex.containsTerrain(Terrains.BRIDGE)
              && (adjacentBase + adjacentHex.terrainLevel(Terrains.BRIDGE_ELEV)) > entityAbsAlt) {
            return true;
        }
        if (adjacentHex.containsTerrain(Terrains.BUILDING)
              && (adjacentBase + adjacentHex.terrainLevel(Terrains.BLDG_ELEV)) > entityAbsAlt) {
            return true;
        }
        // Cliff edges count too: the climbable feature may be a bare elevation difference
        // (the adjacent hex's bedrock sits well above the entity). A Mek that climbed down a
        // cliff face into deep water and now hangs at water surface (elev 0) is in this state.
        return (adjacentBase - entityAbsAlt) > entity.getMaxElevationChange();
    }

    /**
     * Places the entity on the atmospheric map in the hex corresponding to its current ground map. Used for lift-off
     * when aero-on-ground movement is not used. Before doing this, test if this can be done with
     * hasAtmosphericMapForLiftOff().
     */
    private void positionOnAtmosphericMap() {
        // without aero on ground movement, lift off places the aero directly on the atmospheric map, TW p. 88
        Board groundBoard = getGame().getBoard(entity);
        Board lowAltitudeBoard = getGame().getEnclosingBoard(groundBoard);
        entity.setBoardId(lowAltitudeBoard.getBoardId());
        entity.setPosition(lowAltitudeBoard.embeddedBoardPosition(groundBoard.getBoardId()));
    }


    /**
     * Iterate through the steps of the movement path and handle each step.
     */
    private void processSteps() {
        MoveStep previousStep = null;
        for (final ListIterator<MoveStep> i = md.getSteps(); i.hasNext(); ) {
            if (i.hasPrevious()) {
                previousStep = i.previous();
                // move iterator back to original position; we know this is safe due to for loop terminator
                i.next();
            }
            final MoveStep step = i.next();
            EntityMovementType stepMoveType = step.getMovementType(md.isEndStep(step));
            wasProne = entity.isProne();
            boolean isPavementStep = step.isPavementStep();
            entity.inReverse = step.isThisStepBackwards();
            boolean entityFellWhileAttemptingToStand = false;
            boolean isOnGround = !i.hasNext();
            isOnGround |= stepMoveType != EntityMovementType.MOVE_JUMP;
            isOnGround &= step.getElevation() < 1;

            // Check for hidden units point-blank shots
            if (getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) {
                for (Entity hiddenEntity : hiddenEnemies) {
                    int dist = hiddenEntity.getPosition().distance(step.getPosition());
                    // Checking for same hex and stacking violation; do _not_ ignore hidden units here.
                    // This covers entities moving into a hex occupied hidden enemies that would cause a stacking
                    // violation, to wit:
                    // 1. Entity moving into a hex at ground / water bottom height occupied by a hidden enemy
                    // 2. Entity jumping into a hex occupied by a hidden enemy
                    // Does not cover VTOLs ending in an occupied hex above the height of the enemy, or ASFs overflying
                    // hidden enemies, or ground units ending their movement adjacent to a hex hiding an enemy (see
                    // next clause).
                    if ((dist == 0) && !continueTurnFromPBS &&
                          (Compute.stackingViolation(getGame(), this.entity,
                                step.getPosition(), null, this.entity.climbMode(), false
                          ) != null)
                    ) {
                        // Attempting to move into hex of a hidden unit detects the unit
                        hiddenEntity.setHidden(false);

                        // If first step, use this step as the previous step - probably impossible, but safe.
                        if (previousStep == null) {
                            previousStep = step;
                        }
                        // Set location per previous step; this prevents destroyed entities appearing at move start loc.
                        this.entity.setPosition(previousStep.getPosition());
                        this.entity.setFacing(previousStep.getFacing());
                        this.entity.setSecondaryFacing(previousStep.getFacing());
                        boolean jumping = stepMoveType == EntityMovementType.MOVE_JUMP;

                        // Handle prompting for possible PBS.
                        gameManager.getMainPhaseReport().addAll(processPossiblePBS(step, hiddenEntity));

                        // Handle jumping unit's domino effect now; this does not apply to normal movement
                        if (jumping) {
                            // handle domino effect; report immediately
                            addReport(
                                  gameManager.doEntityDisplacement(
                                        this.entity,
                                        previousStep.getPosition(),
                                        step.getPosition(),
                                        new PilotingRollData(this.entity.getId(), 0,
                                              "Domino effect from jumping into hidden unit!")
                                  )
                            );
                        } else {
                            // Not domino effect from jumping so mover stops short of the occupied hex;
                            // report halted movement
                            report = new Report(9962);
                            report.subject = this.entity.getId();
                            report.addDesc(this.entity);
                            report.add(step.getPosition().getBoardNum());
                            addReport(report);
                            addNewLines();
                            addNewLines();
                        }

                        // If we aren't at the end, send a special report
                        if ((getGame().getTurnIndex() + 1) < getGame().getTurnsList().size()) {
                            gameManager.send(hiddenEntity.getOwner().getId(), gameManager.createSpecialReportPacket());
                            gameManager.send(this.entity.getOwner().getId(), gameManager.createSpecialReportPacket());
                        }

                        // End this entity's turn _without_ updating its position to the current step, because the
                        // current step position is actually illegal; it will keep the previous step's position.
                        this.entity.setDone(true);
                        gameManager.entityUpdate(this.entity.getId(), movePath, true, losCache);
                        if (jumping) {
                            break;
                        }

                        return;


                        // Potential point-blank shot when not causing stacking violation, but only in some situations:
                        // 1. mover is ground unit _and_ ends its movement adjacent to / in the hidden unit's hex;
                        // 2. mover is Aerospace and hidden unit is within detection range of its flight path
                        //    (with or without Active Probe).
                        // and the revealed hidden unit has not already made a pointblank shot this turn.
                    } else if (
                          (dist <= 1) && !hiddenEntity.madePointblankShot() &&
                                ((!this.entity.isAirborne() && md.isEndStep(step)) ||
                                      (this.entity.isAirborne() && (dist == ((this.entity.getBAPRange() > 0) ? 1 : 0))))
                    ) {
                        // Hidden unit should always be revealed as the PBS trigger _is_ getting revealed.
                        hiddenEntity.setHidden(false);

                        // If not set, BV icons could have wrong facing
                        this.entity.setPosition(step.getPosition());
                        this.entity.setFacing(step.getFacing());
                        this.entity.setSecondaryFacing(step.getFacing());

                        gameManager.getMainPhaseReport().addAll(processPossiblePBS(step, hiddenEntity));
                        gameManager.entityUpdate(hiddenEntity.getId());

                        // If we aren't at the end, send a special report
                        if ((getGame().getTurnIndex() + 1) < getGame().getTurnsList().size()) {
                            gameManager.send(hiddenEntity.getOwner().getId(), gameManager.createSpecialReportPacket());
                            gameManager.send(this.entity.getOwner().getId(), gameManager.createSpecialReportPacket());
                        }

                        curFacing = this.entity.getFacing();
                        curPos = this.entity.getPosition();
                        mpUsed = step.getMpUsed();
                    } else if (Compute.canDetectHidden(this.entity, dist, md.isEndStep(step))) {
                        // There are a variety of other ways to detect a hidden unit.
                        // Reveal the detected unit and add the report to the movement report.
                        // This does _not_ trigger a Pointblank Shot
                        hiddenEntity.setHidden(false);
                        gameManager.entityUpdate(hiddenEntity.getId());
                        report = new Report(9960);
                        report.addDesc(this.entity);
                        report.subject = this.entity.getId();
                        report.add(hiddenEntity.getPosition().getBoardNum());
                        gameManager.getMainPhaseReport().addElement(report);
                    }
                }
            }

            // stop for illegal movement
            if (stepMoveType == EntityMovementType.MOVE_ILLEGAL) {
                break;
            }

            // stop if the entity already killed itself
            if (entity.isDestroyed() || entity.isDoomed()) {
                break;
            }

            if (entity.getMovementMode() == EntityMovementMode.WIGE) {
                if (step.getType() == MoveStepType.UP && !entity.isAirborneVTOLorWIGE()) {
                    entity.setWigeLiftoffHover(true);
                } else if (step.getType() == MoveStepType.HOVER) {
                    entity.setWigeLiftoffHover(true);
                    entity.setAssaultDropInProgress(false);
                } else if (step.getType() == MoveStepType.DOWN && step.getClearance() == 0) {
                    // If this is the first step, use the Entity's starting elevation
                    int elevation = (prevStep == null) ? entity.getElevation() : prevStep.getElevation();
                    if (entity instanceof LandAirMek) {
                        addReport(gameManager.landAirMek((LandAirMek) entity, step.getPosition(), elevation,
                              distance));
                    } else if (entity instanceof ProtoMek) {
                        addReport(gameManager.landGliderPM((ProtoMek) entity, step.getPosition(), elevation,
                              distance));
                    }
                    // landing always ends movement whether successful or not
                }
            }

            // check for MASC failure on first step
            // also check Tanks because they can have superchargers that act
            // like MASc
            if (firstStep) {
                if (entity instanceof VTOL) {
                    // No roll for failure, but +3 on rolls to avoid sideslip.
                    entity.setMASCUsed(md.hasActiveMASC());
                } else if ((entity instanceof Mek) || (entity instanceof Tank)) {
                    // Not necessarily a fall, but we need to give them a new turn to plot movement
                    // with
                    // likely reduced MP.
                    fellDuringMovement = gameManager.checkMASCFailure(entity, md)
                          || gameManager.checkSuperchargerFailure(entity, md);
                }
            }

            if (firstStep) {
                rollTarget = entity.checkGunningIt(overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    int mof = gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos,
                          curPos, rollTarget, false);
                    if (mof > 0) {
                        // Since this is the first step, we don't have a previous step so we'll pass
                        // this one in case it's needed to process a skid.
                        if (gameManager.processFailedVehicleManeuver(entity, curPos, 0, step,
                              step.isThisStepBackwards(), lastStepMoveType, distance, 2, mof)) {
                            if (md.hasActiveMASC() || md.hasActiveSupercharger()) {
                                mpUsed = entity.getRunMP();
                            } else {
                                mpUsed = entity.getRunMPWithoutMASC();
                            }

                            turnOver = true;
                            distance = entity.delta_distance;
                            curFacing = entity.getFacing();
                            entity.setSecondaryFacing(curFacing);
                            break;
                        } else if (entity.getFacing() != curFacing) {
                            // If the facing doesn't change we had a minor fishtail that doesn't require
                            // stopping movement.
                            continueTurnFromFishtail = true;
                            curFacing = entity.getFacing();
                            entity.setSecondaryFacing(curFacing);
                            break;
                        }
                    }
                }
            }

            // Check for failed maneuver for overdrive on first step. The rules for
            // overdrive do not
            // state this explicitly, but since combining overdrive with gunning it requires
            // two rolls
            // and gunning does state explicitly that the roll is made before movement, this
            // implies the same for overdrive.
            if (firstStep && (overallMoveType == EntityMovementType.MOVE_SPRINT
                  || overallMoveType == EntityMovementType.MOVE_VTOL_SPRINT)) {
                rollTarget = entity.checkUsingOverdrive(EntityMovementType.MOVE_SPRINT);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    int mof = gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos,
                          curPos, rollTarget, false);
                    if (mof > 0) {
                        if (gameManager.processFailedVehicleManeuver(entity, curPos, 0, step,
                              step.isThisStepBackwards(),
                              lastStepMoveType, distance, 2, mof)) {
                            if (md.hasActiveMASC() || md.hasActiveSupercharger()) {
                                mpUsed = entity.getRunMP();
                            } else {
                                mpUsed = entity.getRunMPWithoutMASC();
                            }

                            turnOver = true;
                            distance = entity.delta_distance;
                            curFacing = entity.getFacing();
                            entity.setSecondaryFacing(curFacing);
                            break;
                        } else if (entity.getFacing() != curFacing) {
                            // If the facing doesn't change we had a minor fishtail that doesn't require
                            // stopping movement.
                            continueTurnFromFishtail = true;
                            curFacing = entity.getFacing();
                            entity.setSecondaryFacing(curFacing);
                            break;
                        }
                    }
                }
            }

            if (step.getType() == MoveStepType.CONVERT_MODE) {
                entity.setConvertingNow(true);

                // Non-omni QuadVees converting to vehicle mode dump any riding BA in the
                // starting hex if they fail to make an anti-mek check.
                // http://bg.battletech.com/forums/index.php?topic=55263.msg1271423#msg1271423
                if (entity instanceof QuadVee && entity.getConversionMode() == QuadVee.CONV_MODE_MEK
                      && !entity.isOmni()) {
                    for (Entity rider : entity.getExternalUnits()) {
                        addReport(gameManager.checkDropBAFromConverting(entity, rider, curPos, curFacing,
                              false, false, false));
                    }
                } else if ((entity.getEntityType() & Entity.ETYPE_LAND_AIR_MEK) != 0) {
                    // External units on LAMs, including swarmers, fall automatically and take
                    // damage,
                    // and the LAM itself may take one or more criticalSlots.
                    for (Entity rider : entity.getExternalUnits()) {
                        addReport(gameManager.checkDropBAFromConverting(entity, rider, curPos, curFacing, true, true,
                              true));
                    }
                    final int swarmerId = entity.getSwarmAttackerId();
                    if (Entity.NONE != swarmerId) {
                        addReport(gameManager.checkDropBAFromConverting(entity, getGame().getEntity(swarmerId),
                              curPos, curFacing, true, true, true));
                    }
                }

                continue;
            }

            // did the entity move?
            boolean didMove = step.getDistance() > distance;

            // check for aero stuff
            if (entity.isAirborne() && entity.isAero()) {
                IAero a = (IAero) entity;
                j++;

                // increment straight moves (can't do it at end, because not all
                // steps may be processed)
                a.setStraightMoves(step.getNStraight());

                // TODO : change the way this check is made
                if (!didMove && (md.length() != j)) {
                    thrustUsed += step.getMp();
                } else {
                    // if this was the last move and distance was zero, then add
                    // thrust
                    if (!didMove && (md.length() == j)) {
                        thrustUsed += step.getMp();
                    }
                    // then we moved to a new hex or the last step so check
                    // conditions
                    // structural damage
                    rollTarget = a.checkThrustSI(thrustUsed, overallMoveType);
                    if ((rollTarget.getValue() != TargetRoll.CHECK_FALSE)
                          && !(entity instanceof FighterSquadron) && !getGame().useVectorMove()) {
                        if (!gameManager.doSkillCheckInSpace(entity, rollTarget)) {
                            a.setSI(a.getSI() - 1);
                            if (entity instanceof LandAirMek) {
                                addReport(gameManager.criticalEntity(entity, Mek.LOC_CENTER_TORSO, false, 0, 1));
                            }
                            // check for destruction
                            if (a.getSI() == 0) {
                                // Lets auto-eject if we can!
                                if (a instanceof LandAirMek lam) {
                                    // LAMs eject if the CT destroyed switch is on
                                    if (lam.isAutoEject()
                                          && (!getGame().getOptions()
                                          .booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                          || (getGame().getOptions()
                                          .booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                          && lam.isCondEjectCTDest()))) {
                                        addReport(gameManager.ejectEntity(entity, true, false));
                                    }
                                } else {
                                    // Aeros eject if the SI Destroyed switch is on
                                    Aero aero = (Aero) a;
                                    if (aero.isAutoEject()
                                          && (!getGame().getOptions()
                                          .booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                          || (getGame().getOptions()
                                          .booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                          && aero.isCondEjectSIDest()))) {
                                        addReport(gameManager.ejectEntity(entity, true, false));
                                    }
                                }
                                addReport(gameManager.destroyEntity(entity, "Structural Integrity Collapse",
                                      false));
                            }
                        }
                    }

                    // check for pilot damage
                    int hits = entity.getCrew().getHits();
                    int health = 6 - hits;

                    if ((thrustUsed > (2 * health)) && !getGame().useVectorMove()
                          && !(entity instanceof TeleMissile)) {
                        int targetRoll = 2 + (thrustUsed - (2 * health))
                              + (2 * hits);
                        gameManager.resistGForce(entity, targetRoll);
                    }

                    thrustUsed = 0;
                }

                if (step.getType() == MoveStepType.RETURN) {
                    a.setCurrentVelocity(md.getFinalVelocity());
                    entity.setAltitude(curAltitude);
                    gameManager.processLeaveMap(md, true, Compute.roundsUntilReturn(getGame(), entity));
                    return;
                }

                if (step.getType() == MoveStepType.OFF) {
                    a.setCurrentVelocity(md.getFinalVelocity());
                    entity.setAltitude(curAltitude);
                    gameManager.processLeaveMap(md, true, -1);
                    return;
                }

                rollTarget = a.checkRolls(step, overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    getGame().addControlRoll(new PilotingRollData(entity.getId(), 0, "excess roll"));
                }

                rollTarget = a.checkManeuver(step, overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    if (!gameManager.doSkillCheckManeuver(entity, rollTarget)) {
                        a.setFailedManeuver(true);
                        int forward = Math.max(step.getVelocityLeft() / 2, 1);
                        if (forward < step.getVelocityLeft()) {
                            fellDuringMovement = true;
                        }
                        // multiply forward by 16 when on ground hexes
                        if (getGame().getBoard(curBoardId).isGround()) {
                            forward *= 16;
                        }
                        while (forward > 0) {
                            curPos = curPos.translated(step.getFacing());
                            forward--;
                            distance++;
                            a.setStraightMoves(a.getStraightMoves() + 1);
                            // make sure it didn't fly off the map
                            if (!getGame().getBoard(curBoardId).contains(curPos)) {
                                curPos = nudgeOntoBoard(curPos, step.getFacing());
                                a.setCurrentVelocity(md.getFinalVelocity());
                                gameManager.processLeaveMap(md, true, Compute.roundsUntilReturn(getGame(), entity));
                                return;
                                // make sure it didn't crash
                            } else if (gameManager.checkCrash(entity, curPos, curBoardId, step.getAltitude())) {
                                addReport(gameManager.processCrash(entity, step.getVelocity(), curPos));
                                forward = 0;
                                fellDuringMovement = false;
                                crashedDuringMovement = true;
                            }
                        }
                        break;
                    }
                }

                // if out of control, check for possible collision
                if (didMove && a.isOutControlTotal()) {
                    Iterator<Entity> targets = getGame().getEntities(step.getPosition());
                    if (targets.hasNext()) {
                        // Somebody here so check to see if there is a collision
                        int checkRoll = Compute.d6(2);
                        // TODO : change this to 11 for Large Craft
                        int targetRoll = 11;
                        if ((a instanceof Dropship) || (entity instanceof Jumpship)) {
                            targetRoll = 10;
                        }
                        if (checkRoll >= targetRoll) {
                            // this gets complicated, I need to check for each
                            // unit type
                            // by order of movement sub-phase
                            Vector<Integer> potentialSpaceStation = new Vector<>();
                            Vector<Integer> potentialWarShip = new Vector<>();
                            Vector<Integer> potentialJumpShip = new Vector<>();
                            Vector<Integer> potentialDropShip = new Vector<>();
                            Vector<Integer> potentialSmallCraft = new Vector<>();
                            Vector<Integer> potentialASF = new Vector<>();

                            while (targets.hasNext()) {
                                int id = targets.next().getId();
                                Entity ce = getGame().getEntity(id);
                                // if we are in atmosphere and not the same altitude
                                // then skip
                                if (!getGame().getBoard(curBoardId).isSpace() && (ce.getAltitude() != curAltitude)) {
                                    continue;
                                }
                                // you can't collide with yourself
                                if (ce.equals(a)) {
                                    continue;
                                }
                                switch (ce) {
                                    case SpaceStation ignored -> potentialSpaceStation.addElement(id);
                                    case Warship ignored -> potentialWarShip.addElement(id);
                                    case Jumpship ignored -> potentialJumpShip.addElement(id);
                                    case Dropship ignored -> potentialDropShip.addElement(id);
                                    case SmallCraft ignored -> potentialSmallCraft.addElement(id);
                                    case null, default ->
                                        // ASF can actually include anything,
                                        // because we might
                                        // have combat dropping troops
                                          potentialASF.addElement(id);
                                }
                            }

                            // ok now go through and see if these have anybody in them
                            int chosen;
                            Entity target;
                            Coords destination;

                            if (!potentialSpaceStation.isEmpty()) {
                                chosen = Compute.randomInt(potentialSpaceStation.size());
                                target = getGame().getEntity(potentialSpaceStation.elementAt(chosen));
                                destination = target.getPosition();
                                if (gameManager.processCollision(entity, target, lastPos)) {
                                    curPos = destination;
                                    break;
                                }
                            } else if (!potentialWarShip.isEmpty()) {
                                chosen = Compute.randomInt(potentialWarShip.size());
                                target = getGame().getEntity(potentialWarShip.elementAt(chosen));
                                destination = target.getPosition();
                                if (gameManager.processCollision(entity, target, lastPos)) {
                                    curPos = destination;
                                    break;
                                }
                            } else if (!potentialJumpShip.isEmpty()) {
                                chosen = Compute.randomInt(potentialJumpShip.size());
                                target = getGame().getEntity(potentialJumpShip.elementAt(chosen));
                                destination = target.getPosition();
                                if (gameManager.processCollision(entity, target, lastPos)) {
                                    curPos = destination;
                                    break;
                                }
                            } else if (!potentialDropShip.isEmpty()) {
                                chosen = Compute.randomInt(potentialDropShip.size());
                                target = getGame().getEntity(potentialDropShip.elementAt(chosen));
                                destination = target.getPosition();
                                if (gameManager.processCollision(entity, target, lastPos)) {
                                    curPos = destination;
                                    break;
                                }
                            } else if (!potentialSmallCraft.isEmpty()) {
                                chosen = Compute.randomInt(potentialSmallCraft.size());
                                target = getGame().getEntity(potentialSmallCraft.elementAt(chosen));
                                destination = target.getPosition();
                                if (gameManager.processCollision(entity, target, lastPos)) {
                                    curPos = destination;
                                    break;
                                }
                            } else if (!potentialASF.isEmpty()) {
                                chosen = Compute.randomInt(potentialASF.size());
                                target = getGame().getEntity(potentialASF.elementAt(chosen));
                                destination = target.getPosition();
                                if (gameManager.processCollision(entity, target, lastPos)) {
                                    curPos = destination;
                                    break;
                                }
                            }
                        }
                    }
                }

                // if in the atmosphere, check for a potential crash
                if (gameManager.checkCrash(entity, step.getPosition(), step.getBoardId(), step.getAltitude())) {
                    addReport(gameManager.processCrash(entity, md.getFinalVelocity(), curPos));
                    crashedDuringMovement = true;
                    // don't do the rest
                    break;
                }

                // handle fighter launching
                if (step.getType() == MoveStepType.LAUNCH) {
                    TreeMap<Integer, Vector<Integer>> launched = step.getLaunched();
                    Set<Integer> bays = launched.keySet();
                    Iterator<Integer> bayIter = bays.iterator();
                    Bay currentBay;
                    while (bayIter.hasNext()) {
                        int bayId = bayIter.next();
                        currentBay = entity.getFighterBays().elementAt(bayId);
                        Vector<Integer> launches = launched.get(bayId);
                        int nLaunched = launches.size();
                        // need to make some decisions about how to handle the
                        // distribution
                        // of fighters to doors beyond the launch rate. The most
                        // sensible thing
                        // is probably to distribute them evenly.
                        int doors = currentBay.getCurrentDoors();
                        int[] distribution = new int[doors];
                        for (int l = 0; l < nLaunched; l++) {
                            distribution[l % doors] = distribution[l % doors] + 1;
                        }
                        // ok, now lets launch them
                        report = new Report(9380);
                        report.add(entity.getDisplayName());
                        report.subject = entity.getId();
                        report.add(nLaunched);
                        report.add("bay " + currentBay.getBayNumber() + " (" + doors + " doors)");
                        addReport(report);
                        int currentDoor = 0;
                        int fighterCount = 0;
                        boolean doorDamage = false;
                        for (int fighterId : launches) {
                            // check to see if we are in the same door
                            fighterCount++;

                            // check for door damage
                            Report doorReport = null;
                            if (!doorDamage && (distribution[currentDoor] > 2) && (fighterCount > 2)) {
                                doorReport = new Report(9378);
                                doorReport.subject = entity.getId();
                                doorReport.indent(2);
                                Roll diceRoll = Compute.rollD6(2);
                                doorReport.add(diceRoll);

                                if (diceRoll.getIntValue() == 2) {
                                    doorDamage = true;
                                    doorReport.choose(true);
                                    currentBay.destroyDoorNext();
                                } else {
                                    doorReport.choose(false);
                                }
                                doorReport.newlines++;
                            }

                            if (fighterCount > distribution[currentDoor]) {
                                // move to a new door
                                currentDoor++;
                                fighterCount = 0;
                                doorDamage = false;
                            }
                            int bonus = Math.max(0,
                                  distribution[currentDoor] - 2);

                            Entity fighter = getGame().getEntity(fighterId);
                            if (!gameManager.launchUnit(entity, fighter, curPos, curFacing, step.getVelocity(),
                                  step.getAltitude(), step.getVectors(), bonus)) {
                                logger.error("Server was told to unload {} from {} into {}",
                                      fighter.getDisplayName(),
                                      entity.getDisplayName(),
                                      curPos.getBoardNum());
                            }
                            if (doorReport != null) {
                                addReport(doorReport);
                            }
                        }
                    }
                    // now apply any damage to bay doors
                    entity.resetBayDoors();
                }

                // handle DropShip undocking
                if (step.getType() == MoveStepType.UNDOCK) {
                    TreeMap<Integer, Vector<Integer>> launched = step.getLaunched();
                    Set<Integer> collars = launched.keySet();
                    for (int collarId : collars) {
                        Vector<Integer> launches = launched.get(collarId);
                        int nLaunched = launches.size();
                        // ok, now lets launch them
                        report = new Report(9380);
                        report.add(entity.getDisplayName());
                        report.subject = entity.getId();
                        report.add(nLaunched);
                        report.add("collar " + collarId);
                        addReport(report);
                        for (int dropShipId : launches) {
                            // check to see if we are in the same door
                            Entity ds = getGame().getEntity(dropShipId);
                            if (!gameManager.launchUnit(entity, ds, curPos, curFacing,
                                  step.getVelocity(), step.getAltitude(),
                                  step.getVectors(), 0)) {
                                logger.error("Error! Server was told to unload {} from {} into {}",
                                      ds.getDisplayName(),
                                      entity.getDisplayName(),
                                      curPos.getBoardNum());
                            }
                        }
                    }
                }

                // handle combat drops
                if (step.getType() == MoveStepType.DROP) {
                    TreeMap<Integer, Vector<Integer>> dropped = step.getLaunched();
                    Set<Integer> bays = dropped.keySet();
                    Iterator<Integer> bayIter = bays.iterator();
                    Transporter currentBay;
                    while (bayIter.hasNext()) {
                        int bayId = bayIter.next();
                        currentBay = entity.getTransports().elementAt(bayId);
                        Vector<Integer> drops = dropped.get(bayId);
                        int nDropped = drops.size();
                        // ok, now lets drop them
                        report = new Report(9386);
                        report.add(entity.getDisplayName());
                        report.subject = entity.getId();
                        report.add(nDropped);
                        addReport(report);
                        Report dropEntityReport;
                        for (int unitId : drops) {
                            Entity drop = getGame().getEntity(unitId);
                            dropEntityReport = new Report(9374);
                            dropEntityReport.add(drop.getShortName(), true);
                            addReport(dropEntityReport);
                            // Infantry don't have a chance to destroy doors, and
                            // currently this applies to Bays only.
                            if (!drop.isInfantry() && Compute.d6(2) == 2) {
                                if (currentBay instanceof Bay cbBay) {
                                    report = new Report(9390);
                                    report.subject = entity.getId();
                                    report.indent(1);
                                    report.add(currentBay.getTransporterType());
                                    addReport(report);
                                    cbBay.destroyDoorNext();
                                }
                            }
                            gameManager.dropUnit(drop, entity, curPos, step.getAltitude());
                        }
                    }
                    // now apply any damage to bay doors
                    entity.resetBayDoors();
                }
            }

            // check piloting skill for getting up
            rollTarget = entity.checkGetUp(step, overallMoveType);

            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                // Unless we're an ICE- or fuel cell-powered IndustrialMek,
                // standing up builds heat.
                if ((entity instanceof Mek) && entity.hasEngine() && !(((Mek) entity).isIndustrial()
                      && ((entity.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)
                      || (entity.getEngine().getEngineType() == Engine.FUEL_CELL)))) {
                    entity.heatBuildup += 1;
                }
                entity.setProne(false);
                // entity.setHullDown(false);
                wasProne = false;
                getGame().resetPSRs(entity);
                entityFellWhileAttemptingToStand = !gameManager.doSkillCheckInPlace(entity, rollTarget);
            }
            // did the entity just fall?
            if (entityFellWhileAttemptingToStand) {
                moveType = stepMoveType;
                curFacing = entity.getFacing();
                curPos = entity.getPosition();
                mpUsed = step.getMpUsed();
                fellDuringMovement = true;
                if (!entity.isCarefulStand()) {
                    break;
                }
            } else if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                entity.setHullDown(false);
            }

            if (step.getType() == MoveStepType.UNJAM_RAC) {
                entity.setUnjammingRAC(true);
                getGame().addAction(new UnjamAction(entity.getId()));

                // for Aeros this will end movement prematurely
                // if we break
                if (!(entity.isAirborne())) {
                    break;
                }
            }

            if (step.getType() == MoveStepType.LAY_MINE) {
                gameManager.layMine(entity, step.getMineToLay(), step.getPosition());
                continue;
            }

            if (step.getType() == MoveStepType.CLEAR_MINEFIELD) {
                ClearMinefieldAction cma = new ClearMinefieldAction(entity.getId(), step.getMinefield());
                entity.setClearingMinefield(true);
                getGame().addAction(cma);
                break;
            }

            if ((step.getType() == MoveStepType.SEARCHLIGHT)
                  && entity.hasSearchlight()) {
                final boolean SearchOn = !entity.isUsingSearchlight();
                entity.setSearchlightState(SearchOn);
                if (gameManager.doBlind()) { // if double-blind, we may need to filter the
                    // players that receive this message
                    List<Player> playersVector = getGame().getPlayersList();
                    Vector<Player> vCanSee = gameManager.whoCanSee(entity);
                    for (Player p : playersVector) {
                        if (vCanSee.contains(p)) { // Player sees the unit
                            gameManager.sendServerChat(p.getId(),
                                  entity.getDisplayName()
                                        + " switched searchlight "
                                        + (SearchOn ? "on" : "off") + '.');
                        } else {
                            gameManager.sendServerChat(p.getId(),
                                  "An unseen unit" + " switched searchlight "
                                        + (SearchOn ? "on" : "off") + '.');
                        }
                    }
                } else { // No double-blind, everyone can see this
                    gameManager.sendServerChat(
                          entity.getDisplayName() + " switched searchlight "
                                + (SearchOn ? "on" : "off") + '.');
                }
            }

            // set most step parameters
            moveType = stepMoveType;
            distance = step.getDistance();
            mpUsed = step.getMpUsed();

            if (cachedGravityLimit < 0) {
                cachedGravityLimit = EntityMovementType.MOVE_JUMP == moveType
                      ? (step.isUsingMekJumpBooster()
                         ? entity.getMechanicalJumpBoosterMP(MPCalculationSetting.NO_GRAVITY)
                         : entity.getJumpMP(MPCalculationSetting.NO_GRAVITY))
                      : entity.getRunningGravityLimit();
            }
            // check for charge
            if (step.getType() == MoveStepType.CHARGE) {
                if (entity.canCharge()) {
                    gameManager.checkExtremeGravityMovement(entity, step, lastStepMoveType,
                          curPos, cachedGravityLimit);
                    Targetable target = step.getTarget(getGame());
                    if (target != null) {
                        ChargeAttackAction caa = new ChargeAttackAction(
                              entity.getId(), target.getTargetType(),
                              target.getId(), target.getPosition());
                        entity.setDisplacementAttack(caa);
                        getGame().addCharge(caa);
                        charge = caa;
                    } else {
                        String message = "Illegal charge!! " + entity.getDisplayName() +
                              " is attempting to charge a null target!";
                        logger.info(message);
                        gameManager.sendServerChat(message);
                        return;
                    }
                } else if (entity.isAirborneVTOLorWIGE() && entity.canRam()) {
                    gameManager.checkExtremeGravityMovement(entity, step, lastStepMoveType,
                          curPos, cachedGravityLimit);
                    Targetable target = step.getTarget(getGame());
                    if (target != null) {
                        AirMekRamAttackAction raa = new AirMekRamAttackAction(
                              entity.getId(), target.getTargetType(),
                              target.getId(), target.getPosition());
                        entity.setDisplacementAttack(raa);
                        entity.setRamming(true);
                        getGame().addCharge(raa);
                        charge = raa;
                    } else {
                        String message = "Illegal charge!! " + entity.getDisplayName()
                              + " is attempting to charge a null target!";
                        logger.info(message);
                        gameManager.sendServerChat(message);
                        return;
                    }
                } else {
                    gameManager.sendServerChat("Illegal charge!! I don't think "
                          + entity.getDisplayName()
                          + " should be allowed to charge,"
                          + " but the client of "
                          + entity.getOwner().getName() + " disagrees.");
                    gameManager.sendServerChat("Please make sure "
                          + entity.getOwner().getName()
                          + " is running MegaMek " + MMConstants.VERSION
                          + ", or if that is already the case, submit a bug report at https://github.com/MegaMek/megamek/issues");
                    return;
                }
                break;
            }

            // check for dfa
            if (step.getType() == MoveStepType.DFA) {
                if (entity.canDFA()) {
                    gameManager.checkExtremeGravityMovement(entity, step, lastStepMoveType, curPos, cachedGravityLimit);
                    Targetable target = step.getTarget(getGame());

                    int targetType;
                    int targetID;

                    // if it's a valid target, then simply pass along the type and ID
                    if (target != null) {
                        targetID = target.getId();
                        targetType = target.getTargetType();
                        // if the target has become invalid somehow, or was incorrectly declared in the
                        // first place
                        // log the error, then put some defaults in for the DFA and proceed as if the
                        // target had been moved/destroyed
                    } else {
                        String errorMessage = "Illegal DFA by " + entity.getDisplayName()
                              + " against non-existent entity at " + step.getTargetPosition();
                        gameManager.sendServerChat(errorMessage);
                        logger.error(errorMessage);
                        targetID = Entity.NONE;
                        // doesn't really matter, DFA processing will cut out early if target resolves
                        // as null
                        targetType = Targetable.TYPE_ENTITY;
                    }

                    DfaAttackAction daa = new DfaAttackAction(entity.getId(),
                          targetType, targetID,
                          step.getPosition());
                    entity.setDisplacementAttack(daa);
                    entity.setElevation(step.getElevation());
                    getGame().addCharge(daa);
                    charge = daa;

                } else {
                    gameManager.sendServerChat("Illegal DFA!! I don't think "
                          + entity.getDisplayName()
                          + " should be allowed to DFA,"
                          + " but the client of "
                          + entity.getOwner().getName() + " disagrees.");
                    gameManager.sendServerChat("Please make sure "
                          + entity.getOwner().getName()
                          + " is running MegaMek " + MMConstants.VERSION
                          + ", or if that is already the case, submit a bug report at https://github.com/MegaMek/megamek/issues");
                    return;
                }

                break;
            }

            // check for ram
            if (step.getType() == MoveStepType.RAM) {
                if (entity.canRam()) {
                    Targetable target = step.getTarget(getGame());
                    RamAttackAction raa = new RamAttackAction(entity.getId(),
                          target.getTargetType(), target.getId(),
                          target.getPosition());
                    entity.setRamming(true);
                    getGame().addRam(raa);
                    ram = raa;
                } else {
                    gameManager.sendServerChat("Illegal ram!! I don't think "
                          + entity.getDisplayName()
                          + " should be allowed to charge,"
                          + " but the client of "
                          + entity.getOwner().getName() + " disagrees.");
                    gameManager.sendServerChat("Please make sure "
                          + entity.getOwner().getName()
                          + " is running MegaMek " + MMConstants.VERSION
                          + ", or if that is already the case, submit a bug report at https://github.com/MegaMek/megamek/issues");
                    return;
                }
                break;
            }

            if (step.isVTOLBombingStep()) {
                ((IBomber) entity).setVTOLBombTarget(step.getTarget(getGame()));
            } else if (step.isStrafingStep() && (entity instanceof VTOL)) {
                ((VTOL) entity).getStrafingCoords().add(step.getPosition());
            }

            if ((step.getType() == MoveStepType.ACC) || (step.getType() == MoveStepType.ACCELERATION)) {
                if (entity.isAero()) {
                    IAero a = (IAero) entity;
                    if (step.getType() == MoveStepType.ACCELERATION) {
                        a.setAccLast(true);
                    } else {
                        a.setAccDecNow(true);
                        a.setCurrentVelocity(a.getCurrentVelocity() + 1);
                    }
                    a.setNextVelocity(a.getNextVelocity() + 1);
                }
            }

            if ((step.getType() == MoveStepType.DEC) || (step.getType() == MoveStepType.DECELERATION)) {
                if (entity.isAero()) {
                    IAero a = (IAero) entity;
                    if (step.getType() == MoveStepType.DECELERATION) {
                        a.setAccLast(true);
                    } else {
                        a.setAccDecNow(true);
                        a.setCurrentVelocity(a.getCurrentVelocity() - 1);
                    }
                    a.setNextVelocity(a.getNextVelocity() - 1);
                }
            }

            if (step.getType() == MoveStepType.EVADE) {
                entity.setEvading(true);
            }

            if (step.getType() == MoveStepType.BRACE) {
                entity.setBraceLocation(step.getBraceLocation());
            }

            if (step.getType() == MoveStepType.SHUTDOWN) {
                entity.performManualShutdown();
                gameManager.sendServerChat(entity.getDisplayName() + " has shutdown.");
            }

            if (step.getType() == MoveStepType.STARTUP) {
                entity.performManualStartup();
                gameManager.sendServerChat(entity.getDisplayName() + " has started up.");
            }

            if (step.getType() == MoveStepType.SELF_DESTRUCT) {
                entity.setSelfDestructing(true);
            }

            if (step.getType() == MoveStepType.ROLL) {
                if (entity.isAero()) {
                    IAero a = (IAero) entity;
                    a.setRolled(!a.isRolled());
                }
            }

            // check for dig in or fortify
            if (entity instanceof Infantry inf) {
                if (step.getType() == MoveStepType.DIG_IN) {
                    // A unit that has been hitting the deck long enough may convert straight to dug in. TO:AR p.106.
                    inf.beginDigIn(inf.canDigInFromDeck());
                    continue;
                } else if (step.getType() == MoveStepType.HIT_THE_DECK) {
                    inf.setHitTheDeck(true);
                    continue;
                } else if (step.getType() == MoveStepType.FORTIFY) {
                    // Building a fortified hex requires fieldworks-capable equipment (TO:AUE p.153). The move
                    // step is already gated client-side, so this is a defensive server-side enforcement.
                    if (!inf.hasWorkingMisc(MiscType.F_TRENCH_CAPABLE)) {
                        logger.debug("[Fortify] {}: fortify rejected - no fieldworks-capable equipment "
                              + "(F_TRENCH_CAPABLE)", entity.getDisplayName());
                        continue;
                    }
                    inf.beginFortify();
                    continue;
                } else if (step.getType() == MoveStepType.BUILD_BRIDGE) {
                    processBuildBridgeStep(inf, step);
                    continue;
                } else if (step.getType() == MoveStepType.CANCEL_BRIDGE) {
                    processCancelBridgeStep(inf);
                    continue;
                } else if (step.getType() == MoveStepType.RESUME_BRIDGE) {
                    processResumeBridgeStep(inf);
                    continue;
                } else if (step.getType() == MoveStepType.PAUSE_BRIDGE) {
                    processPauseBridgeStep(inf);
                    continue;
                } else if (step.getType() == MoveStepType.ABANDON_BRIDGE) {
                    processAbandonBridgeStep(inf);
                    continue;
                } else if ((step.getType() != MoveStepType.TURN_LEFT)
                      && (step.getType() != MoveStepType.TURN_RIGHT)) {
                    // other movement clears dug in and hitting the deck status
                    inf.clearGroundPostures();
                }

                if (step.getType() == MoveStepType.TAKE_COVER) {
                    if (Infantry.hasValidCover(getGame(), step.getPosition(),
                          step.getElevation())) {
                        inf.setTakingCover(true);
                    } else {
                        gameManager.sendServerChat(entity.getDisplayName()
                              + " failed to take cover: "
                              + "no valid unit found in "
                              + step.getPosition());
                    }
                }
            }

            // Fieldworks: a vehicle or a Mek with fieldworks-capable equipment - bulldozer, backhoe or equivalent -
            // builds a fortified hex (Vehicles and Fieldworks, TO:AUE p.153, Corrected Sixth Printing). A vehicle or
            // backhoe-equipped Mek may also clear rubble.
            if ((entity instanceof Fortifiable fortifier) && (step.getType() == MoveStepType.FORTIFY)) {
                // Defensive server-side check mirroring the MoveStep legality.
                if (!entity.hasWorkingMisc(MiscType.F_TRENCH_CAPABLE)) {
                    logger.debug("[Fortify] {}: fortify rejected - no fieldworks-capable equipment "
                          + "(F_TRENCH_CAPABLE)", entity.getDisplayName());
                } else {
                    fortifier.beginFortify();
                }
            } else if ((entity instanceof RubbleClearer) && (step.getType() == MoveStepType.CLEAR_RUBBLE)) {
                beginRubbleClearing(entity, step.getPosition());
            }

            // If we have turned, check whether we have fulfilled any turn mode
            // requirements.
            if ((step.getType() == MoveStepType.TURN_LEFT
                  || step.getType() == MoveStepType.TURN_RIGHT)
                  && entity.usesTurnMode()) {
                int straight = 0;
                if (prevStep != null) {
                    straight = prevStep.getNStraight();
                }
                rollTarget = entity.checkTurnModeFailure(overallMoveType, straight,
                      md.getMpUsed(), step.getPosition());
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    int mof = gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos,
                          curPos, rollTarget, false);
                    if (mof > 0) {
                        if (gameManager.processFailedVehicleManeuver(entity, curPos,
                              step.getFacing() - curFacing,
                              (null == prevStep) ? step : prevStep,
                              step.isThisStepBackwards(),
                              lastStepMoveType, distance, mof, mof)) {
                            if (md.hasActiveMASC() || md.hasActiveSupercharger()) {
                                mpUsed = entity.getRunMP();
                            } else {
                                mpUsed = entity.getRunMPWithoutMASC();
                            }

                            turnOver = true;
                            distance = entity.delta_distance;
                        } else {
                            continueTurnFromFishtail = true;
                        }
                        curFacing = entity.getFacing();
                        curPos = entity.getPosition();
                        entity.setSecondaryFacing(curFacing);
                        break;
                    }
                }
            }

            if (step.getType() == MoveStepType.BOOTLEGGER) {
                rollTarget = entity.getBasePilotingRoll();
                entity.addPilotingModifierForTerrain(rollTarget);
                rollTarget.addModifier(0, "bootlegger maneuver");
                int mof = gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                      curPos, curPos, rollTarget, false);
                if (mof > 0) {
                    // If the bootlegger maneuver fails, we treat it as a turn in a random
                    // direction.
                    gameManager.processFailedVehicleManeuver(entity, curPos, Compute.d6() < 4 ? -1 : 1,
                          (null == prevStep) ? step : prevStep,
                          step.isThisStepBackwards(), lastStepMoveType, distance, 2, mof);
                    curFacing = entity.getFacing();
                    curPos = entity.getPosition();
                    break;
                }
            }

            // set last step parameters
            curPos = step.getPosition();
            if (!(step.isUsingMekJumpBooster() && step.isJumping())) {
                curFacing = step.getFacing();
            }
            // check if a building PSR will be needed later, before setting the
            // new elevation
            int buildingMove = entity.checkMovementInBuilding(step, prevStep, curPos, lastPos);
            curVTOLElevation = step.getElevation();
            curAltitude = step.getAltitude();
            int curElevation = step.getElevation();
            curClimbMode = step.climbMode();
            // set elevation in case of collapses
            entity.setElevation(step.getElevation());
            // set climb mode in case of skid
            entity.setClimbMode(curClimbMode);

            Hex curHex = getGame().getBoard(curBoardId).getHex(curPos);

            // when first entering a building, we need to roll what type
            // of basement it has
            if (isOnGround && curHex.containsTerrain(Terrains.BUILDING)) {
                IBuilding bldg = getGame().getBoard(curBoardId).getBuildingAt(curPos);
                if (bldg.rollBasement(curPos, getGame().getBoard(curBoardId), gameManager.getMainPhaseReport())) {
                    gameManager.sendChangedHex(curPos);
                    Vector<IBuilding> buildings = new Vector<>();
                    buildings.add(bldg);
                    gameManager.sendChangedBuildings(buildings);
                }
            }

            // Industrial elevator: the platform rides with the unit, an off-platform unit falls down
            // the shaft, and a successful ride is reported. A fall ends the unit's movement.
            moveElevatorPlatformWithEntity(step, curPos, curBoardId, curElevation);
            if (entityFallsDownElevatorShaft(step, curHex, curPos, curBoardId, curElevation, stepMoveType)) {
                curElevation = entity.getElevation();
                break;
            }
            reportElevatorUse(step, curElevation);

            // check for automatic unstick
            if (entity.canUnstickByJumping() && entity.isStuck()
                  && (moveType == EntityMovementType.MOVE_JUMP)) {
                entity.setStuck(false);
                entity.setCanUnstickByJumping(false);
            }

            // check for leap
            if (!lastPos.equals(curPos)
                  && (stepMoveType != EntityMovementType.MOVE_JUMP) && (entity instanceof Mek)
                  && !entity.isAirborne() && (step.getClearance() <= 0) // Don't check airborne LAMs
                  && getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEAPING)) {
                int leapDistance = (lastElevation
                      + getGame().getBoard(curBoardId).getHex(lastPos).getLevel())
                      - (curElevation + curHex.getLevel());
                logger.debug("[LEAP-TRACE] Leap check: lastPos={}, curPos={}, lastElevation={}, " +
                      "curElevation={}, leapDistance={}, isClimbing={}, isDangling={}",
                      lastPos, curPos, lastElevation, curElevation, leapDistance,
                      entity.isClimbing(), entity.isDangling());
                if (leapDistance > 2) {
                    logger.debug("[LEAP-TRACE] Leaping {} levels from {} to {}",
                          leapDistance, lastPos, curPos);
                    // skill check for leg damage
                    rollTarget = entity.getBasePilotingRoll(stepMoveType);
                    entity.addPilotingModifierForTerrain(rollTarget, curPos, step.getBoardId());
                    rollTarget.append(new PilotingRollData(entity.getId(),
                          2 * leapDistance, Messages.getString("TacOps.leaping.leg_damage")));
                    if (0 < gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                          lastPos, curPos, rollTarget, false)) {
                        // do leg damage
                        addReport(gameManager.damageEntity(entity, new HitData(Mek.LOC_LEFT_LEG), leapDistance));
                        addReport(gameManager.damageEntity(entity, new HitData(Mek.LOC_RIGHT_LEG), leapDistance));
                        addNewLines();
                        addReport(gameManager.criticalEntity(entity, Mek.LOC_LEFT_LEG, false, 0, 0));
                        addNewLines();
                        addReport(gameManager.criticalEntity(entity, Mek.LOC_RIGHT_LEG, false, 0, 0));
                        if (entity instanceof QuadMek) {
                            addReport(gameManager.damageEntity(entity, new HitData(Mek.LOC_LEFT_ARM), leapDistance));
                            addReport(gameManager.damageEntity(entity, new HitData(Mek.LOC_RIGHT_ARM), leapDistance));
                            addNewLines();
                            addReport(gameManager.criticalEntity(entity, Mek.LOC_LEFT_ARM, false, 0, 0));
                            addNewLines();
                            addReport(gameManager.criticalEntity(entity, Mek.LOC_RIGHT_ARM, false, 0, 0));
                        }
                    }
                    // skill check for fall
                    rollTarget = entity.getBasePilotingRoll(stepMoveType);
                    entity.addPilotingModifierForTerrain(rollTarget, curPos, step.getBoardId());
                    rollTarget.append(new PilotingRollData(entity.getId(),
                          leapDistance, Messages.getString("TacOps.leaping.fall_damage")));
                    if (0 < gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                          lastPos, curPos, rollTarget, false)) {
                        entity.setElevation(lastElevation);
                        addReport(gameManager.doEntityFallsInto(entity, lastElevation,
                              lastPos, curPos,
                              entity.getBasePilotingRoll(overallMoveType), false));
                    }
                }
            }

            // Check for infantry gliding down terrain with glider wings (IO p.85)
            if (!lastPos.equals(curPos) && (entity instanceof ConvInfantry infantry)
                  && infantry.hasAbility(OptionsConstants.MD_PL_GLIDER)
                  && infantry.canUseGliderWings()) {
                int glideDistance = (lastElevation + getGame().getBoard(curBoardId).getHex(lastPos).getLevel())
                      - (curElevation + curHex.getLevel());
                // Report if descending more than normal max (1 for regular infantry)
                if (glideDistance > infantry.getMaxElevationChange()) {
                    Report r = new Report(2522);
                    r.subject = entity.getId();
                    r.indent(1);
                    r.addDesc(infantry);
                    r.add(glideDistance);
                    addReport(r);
                }
            }

            // Check for skid.
            rollTarget = entity.checkSkid(moveType, prevHex, overallMoveType,
                  prevStep, step, prevFacing, curFacing, lastPos, curPos,
                  isInfantry, distance - 1);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                // Have an entity-meaningful PSR message.
                boolean psrFailed;
                int startingFacing = entity.getFacing();
                if (entity instanceof Mek) {
                    // We need to ensure that falls will happen from the proper
                    // facing
                    entity.setFacing(curFacing);
                    psrFailed = (0 < gameManager.doSkillCheckWhileMoving(entity,
                          lastElevation, lastPos, lastPos, rollTarget, true));
                } else {
                    psrFailed = (0 < gameManager.doSkillCheckWhileMoving(entity,
                          lastElevation, lastPos, lastPos, rollTarget, false));
                }

                // Does the entity skid?
                if (psrFailed) {

                    if (entity instanceof Tank) {
                        addReport(gameManager.vehicleMotiveDamage((Tank) entity, 0));
                    }

                    curPos = lastPos;
                    int skidDistance = (int) Math.round((double) (distance - 1) / 2);
                    int skidDirection = prevFacing;

                    // All charge damage is based upon
                    // the pre-skid move distance.
                    entity.delta_distance = distance - 1;

                    // Attacks against a skidding target have additional +2.
                    moveType = EntityMovementType.MOVE_SKID;

                    // What is the first hex in the skid?
                    if (step.isThisStepBackwards()) {
                        skidDirection = (skidDirection + 3) % 6;
                    }

                    if (gameManager.processSkid(entity, curPos, prevStep.getElevation(),
                          skidDirection, skidDistance, prevStep,
                          lastStepMoveType)) {
                        return;
                    }

                    // set entity parameters
                    curFacing = entity.getFacing();
                    curPos = entity.getPosition();
                    entity.setSecondaryFacing(curFacing);

                    // skid consumes all movement
                    if (md.hasActiveMASC() || md.hasActiveSupercharger()) {
                        mpUsed = entity.getRunMP();
                    } else {
                        mpUsed = entity.getRunMPWithoutMASC();
                    }

                    entity.moved = moveType;
                    fellDuringMovement = true;
                    turnOver = true;
                    distance = entity.delta_distance;
                    break;

                } else { // End failed-skid-psr
                    // If the check succeeded, restore the facing we had before
                    // if it failed, the fall will have changed facing
                    entity.setFacing(startingFacing);
                }

            } // End need-skid-psr

            // check sideslip
            if ((entity instanceof VTOL)
                  || (entity.getMovementMode() == EntityMovementMode.HOVER)
                  || (entity.getMovementMode() == EntityMovementMode.WIGE
                  && step.getClearance() > 0)) {
                rollTarget = entity.checkSideSlip(moveType, prevHex,
                      overallMoveType, prevStep, prevFacing, curFacing,
                      lastPos, curPos, distance, md.hasActiveMASC());
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    int moF = gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                          lastPos, curPos, rollTarget, false);
                    if (moF > 0) {
                        int elev;
                        int sideslipDistance;
                        int skidDirection;
                        Coords start;
                        if (step.getType() == MoveStepType.LATERAL_LEFT
                              || step.getType() == MoveStepType.LATERAL_RIGHT
                              || step.getType() == MoveStepType.LATERAL_LEFT_BACKWARDS
                              || step.getType() == MoveStepType.LATERAL_RIGHT_BACKWARDS) {
                            // A failed controlled sideslip always results in moving one additional hex
                            // in the direction of the intentional sideslip.
                            elev = step.getElevation();
                            sideslipDistance = 1;
                            skidDirection = lastPos.direction(curPos);
                            start = curPos;
                        } else {
                            elev = (null == prevStep) ? curElevation : prevStep.getElevation();
                            // maximum distance is hexes moved / 2
                            sideslipDistance = Math.min(moF, distance / 2);
                            skidDirection = prevFacing;
                            start = lastPos;
                        }
                        if (sideslipDistance > 0) {
                            sideslipped = true;
                            report = new Report(2100);
                            report.subject = entity.getId();
                            report.addDesc(entity);
                            report.add(sideslipDistance);
                            addReport(report);

                            if (gameManager.processSkid(entity, start, elev, skidDirection,
                                  sideslipDistance, (null == prevStep) ? step : prevStep,
                                  lastStepMoveType)) {
                                return;
                            }

                            if (!entity.isDestroyed() && !entity.isDoomed()
                                  && (mpUsed < entity.getRunMP())) {
                                fellDuringMovement = true; // No, but it should
                                // work...
                            }

                            if ((entity.getElevation() == 0)
                                  && ((entity.getMovementMode() == EntityMovementMode.VTOL)
                                  || (entity.getMovementMode() == EntityMovementMode.WIGE))) {
                                turnOver = true;
                            }
                            // set entity parameters
                            curFacing = step.getFacing();
                            curPos = entity.getPosition();
                            entity.setSecondaryFacing(curFacing);
                            break;
                        }
                    }
                }
            }

            // check if we've moved into rubble
            boolean isLastStep = step.equals(md.getLastStep());
            rollTarget = entity.checkRubbleMove(step, overallMoveType, curHex,
                  lastPos, curPos, isLastStep, isPavementStep);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos, curPos,
                      rollTarget, true);
            }

            // check if we are using reckless movement
            rollTarget = entity.checkRecklessMove(step, overallMoveType, curHex,
                  lastPos, curPos, prevHex);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                if (entity instanceof Mek) {
                    gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos,
                          curPos, rollTarget, true);
                } else if (entity instanceof Tank) {
                    if (0 < gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                          lastPos, curPos, rollTarget, false)) {
                        // assume VTOLs in flight are always in clear terrain
                        if ((0 == curHex.terrainsPresent())
                              || (step.getClearance() > 0)) {
                            if (entity instanceof VTOL) {
                                report = new Report(2208);
                            } else {
                                report = new Report(2206);
                            }
                            report.addDesc(entity);
                            report.subject = entity.getId();
                            addReport(report);
                            mpUsed = step.getMpUsed() + 1;
                            fellDuringMovement = true;
                            break;
                        }
                        report = new Report(2207);
                        report.addDesc(entity);
                        report.subject = entity.getId();
                        addReport(report);
                        // until we get a rules clarification assume that the
                        // entity is both giver and taker
                        // for charge damage
                        HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                        addReport(gameManager.damageEntity(entity, hit, ChargeAttackAction
                              .getDamageTakenBy(entity, entity)));
                        turnOver = true;
                        break;
                    }
                }
            }

            // check for breaking magma crust unless we are jumping over the hex
            // Let's check for hazardous liquid damage too
            if (stepMoveType != EntityMovementType.MOVE_JUMP) {
                if (!curPos.equals(lastPos)) {
                    ServerHelper.checkAndApplyMagmaCrust(curHex, step.getElevation(), entity, curPos, false,
                          gameManager.getMainPhaseReport(), gameManager);
                    ServerHelper.checkEnteringMagma(curHex, step.getElevation(), entity, gameManager);
                    ServerHelper.checkEnteringHazardousLiquid(curHex, step.getElevation(), entity, gameManager);
                    ServerHelper.checkEnteringUltraSublevel(curHex, entity.getElevation(), entity, gameManager);
                }
            }

            if (step.getType() == MoveStepType.CHAFF) {
                List<Mounted<?>> chaffDispensers = entity.getMiscEquipment(MiscType.F_CHAFF_POD)
                      .stream().filter(Mounted::isReady)
                      .collect(Collectors.toList());
                if (!chaffDispensers.isEmpty()) {
                    chaffDispensers.getFirst().setFired(true);
                    gameManager.createSmoke(curPos, getGame().getBoard(step.getBoardId()),
                          SmokeCloud.SMOKE_CHAFF_LIGHT, 1);
                    Hex hex = getGame().getBoard(curBoardId).getHex(curPos);
                    hex.addTerrain(new Terrain(Terrains.SMOKE, SmokeCloud.SMOKE_CHAFF_LIGHT));
                    gameManager.sendChangedHex(curPos);
                    report = new Report(2512)
                          .addDesc(entity)
                          .subject(entity.getId());

                    addReport(report);
                }
            }

            // check if we jumped into magma
            boolean jumpedIntoMagma = false;
            if (!i.hasNext() && curHex.terrainLevel(Terrains.MAGMA) == 2) {
                jumpedIntoMagma = (moveType == EntityMovementType.MOVE_JUMP);
            }
            if (curHex.terrainLevel(Terrains.MAGMA) != 2 || jumpedIntoMagma) {
                // check if we've moved into a swamp
                rollTarget = entity.checkBogDown(step, lastStepMoveType, curHex,
                      lastPos, curPos, lastElevation, isPavementStep);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    if (0 < gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos,
                          curPos, rollTarget, false)) {
                        entity.setStuck(true);
                        entity.setCanUnstickByJumping(true);
                        report = new Report(2081);
                        report.add(entity.getDisplayName());
                        report.subject = entity.getId();
                        addReport(report);
                        // check for quicksand
                        addReport(gameManager.checkQuickSand(curPos));
                        // check for accidental stacking violation (not ignoring hidden units here)
                        Entity violation = Compute.stackingViolation(getGame(),
                              entity, curPos, null, entity.climbMode(), false);
                        if (violation != null) {
                            // target gets displaced, because of low elevation
                            int direction = lastPos.direction(curPos);
                            Coords targetDest = Compute.getValidDisplacement(getGame(),
                                  entity.getId(), curPos, direction);
                            addReport(gameManager.doEntityDisplacement(violation, curPos,
                                  targetDest,
                                  new PilotingRollData(violation.getId(), 0,
                                        "domino effect")));
                            // Update the violating entity's position on the client.
                            gameManager.entityUpdate(violation.getId());
                        }
                        break;
                    }
                }
            }

            // check to see if we are a mek and we've moved OUT of fire
            Hex lastHex = getGame().getBoard(curBoardId).getHex(lastPos);
            if (entity.tracksHeat() && !entity.isAirborne()) {
                if (!lastPos.equals(curPos) && (prevStep != null)
                      && ((lastHex.containsTerrain(Terrains.FIRE) && (prevStep.getElevation() <= 1))
                      || (lastHex.containsTerrain(Terrains.MAGMA) && (prevStep.getElevation() == 0)))
                      && ((stepMoveType != EntityMovementType.MOVE_JUMP)
                      // Bug #828741 -- jumping bypasses fire, but not on the first step
                      // getMpUsed -- total MP used to this step
                      // getMp -- MP used in this step
                      // the difference will always be 0 on the "first step" of a jump,
                      // and >0 on a step in the midst of a jump
                      || (0 == (step.getMpUsed() - step.getMp())))) {
                    int heat = 0;
                    if (lastHex.containsTerrain(Terrains.FIRE)) {
                        heat += 2;
                    }
                    if (lastHex.terrainLevel(Terrains.MAGMA) == 1) {
                        heat += 2;
                    } else if (lastHex.terrainLevel(Terrains.MAGMA) == 2) {
                        heat += 5;
                    }
                    boolean isMekWithHeatDissipatingArmor = (entity instanceof Mek)
                          && ((Mek) entity).hasIntactHeatDissipatingArmor();
                    if (isMekWithHeatDissipatingArmor) {
                        heat /= 2;
                    }
                    entity.heatFromExternal += heat;
                    report = new Report(2115);
                    report.subject = entity.getId();
                    report.addDesc(entity);
                    report.add(heat);
                    addReport(report);
                    if (isMekWithHeatDissipatingArmor) {
                        report = new Report(5550);
                        addReport(report);
                    }
                }
            }

            // check to see if we are not a mek and we've moved INTO fire
            if (!(entity instanceof Mek)) {
                boolean underwater = getGame().getBoard(curBoardId).getHex(curPos)
                      .containsTerrain(Terrains.WATER)
                      && (getGame().getBoard(curBoardId).getHex(curPos).depth() > 0)
                      && (step.getElevation() < getGame().getBoard(curBoardId).getHex(curPos).getLevel());
                if (getGame().getBoard(curBoardId).getHex(curPos).containsTerrain(
                      Terrains.FIRE) && !lastPos.equals(curPos)
                      && (stepMoveType != EntityMovementType.MOVE_JUMP)
                      && (step.getElevation() <= 1) && !underwater) {
                    gameManager.doFlamingDamage(entity, curPos);
                }
            }

            if ((getGame().getBoard(curBoardId).getHex(curPos).terrainLevel(Terrains.SMOKE) == SmokeCloud.SMOKE_GREEN)
                  && !stepMoveType.equals(EntityMovementType.MOVE_JUMP) && entity.antiTSMVulnerable()) {
                addReport(gameManager.doGreenSmokeDamage(entity));
            }

            // check for extreme gravity movement
            if (!i.hasNext() && !firstStep) {
                gameManager.checkExtremeGravityMovement(entity, step, lastStepMoveType, curPos, cachedGravityLimit);
            }

            // check for revealed minefields;
            // unless we get errata about it, we assume that the check is done
            // every time we enter a new hex
            // Also perform per-step Hidden Unit checks if TacOps Advanced Active Probe is enabled.
            // Aerospace BAP hidden unit detection also occurs here (with or without the above option)
            if ((getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_BAP) || entity.isAerospace())
                  && !lastPos.equals(curPos)) {
                if (ServerHelper.detectMinefields(getGame(),
                      entity,
                      curPos,
                      gameManager.getMainPhaseReport(),
                      gameManager)
                      ||
                      ServerHelper.detectHiddenUnits(getGame(), entity, curPos, gameManager.getMainPhaseReport(),
                            gameManager)) {
                    detectedHiddenHazard = true;

                    if (i.hasNext() && (stepMoveType != EntityMovementType.MOVE_JUMP)) {
                        md.clear();
                    }
                }
            }

            // check for minefields. have to check both new hex and new elevation
            // VTOLs may land and submarines may rise or lower into a minefield
            // jumping units may end their movement with a turn but should still check at
            // end of movement
            if (!lastPos.equals(curPos) || (lastElevation != curElevation) ||
                  ((stepMoveType == EntityMovementType.MOVE_JUMP) && !i.hasNext())) {
                boolean boom = false;
                if (isOnGround) {
                    boom = gameManager.checkVibraBombs(entity, curPos, false, lastPos, curPos,
                          gameManager.getMainPhaseReport());
                    
                    boom |= gameManager.handleTripwire(entity, lastPos, curPos, stepMoveType, gameManager.getMainPhaseReport());
                    boom |= gameManager.handlePitfall(entity, curPos, gameManager.getMainPhaseReport());
                    
                    // Collect EMP reports separately for popup, then add to main report
                    Vector<Report> empReports = new Vector<>();
                    boolean empBoom = gameManager.checkEMPMines(entity, curPos, empReports);
                    // Send popup FIRST with only EMP reports, before adding to mainPhaseReport
                    if (empBoom && !empReports.isEmpty()) {
                        gameManager.send(entity.getOwner().getId(),
                              gameManager.createSpecialReportPacket(empReports));
                        sentEMPPopupThisMove = true;
                    }
                    // Now add to main phase report for end-of-phase display
                    gameManager.getMainPhaseReport().addAll(empReports);
                    boom = empBoom || boom;
                }
                if (getGame().containsMinefield(curPos)) {
                    // set the new position temporarily, because
                    // infantry otherwise would get double damage
                    // when moving from clear into mined woods
                    entity.setPosition(curPos);
                    if (gameManager.enterMinefield(entity, curPos, step.getElevation(),
                          isOnGround, gameManager.getMainPhaseReport())) {
                        // resolve any piloting rolls from damage unless unit
                        // was jumping
                        if (stepMoveType != EntityMovementType.MOVE_JUMP) {
                            addReport(gameManager.resolvePilotingRolls(entity));
                            getGame().resetPSRs(entity);
                        }
                        boom = true;
                    }
                    if (wasProne || !entity.isProne()) {
                        entity.setPosition(lastPos);
                    }
                }
                // did anything go boom?
                if (boom) {
                    // set fell during movement so that entity will get another
                    // chance to move with any motive damage
                    // taken account of (functions the same as MASC failure)
                    // only do this if they had more steps (and they were not
                    // jumping
                    if (i.hasNext() && (stepMoveType != EntityMovementType.MOVE_JUMP)) {
                        md.clear();
                        fellDuringMovement = true;
                    }
                    // reset mines if anything detonated
                    gameManager.resetMines();
                }
            }

            // infantry discovers minefields if they end their move
            // in a minefield.
            if (!lastPos.equals(curPos) && !i.hasNext() && isInfantry) {
                if (getGame().containsMinefield(curPos)) {
                    Player owner = entity.getOwner();
                    for (Minefield mf : getGame().getMinefields(curPos)) {
                        if (!owner.containsMinefield(mf)) {
                            report = new Report(2120);
                            report.subject = entity.getId();
                            report.add(entity.getShortName(), true);
                            addReport(report);
                            gameManager.revealMinefield(owner, mf);
                        }
                    }
                }
            }

            // check if we've moved into water
            rollTarget = entity.checkWaterMove(step, lastStepMoveType, curHex,
                  lastPos, curPos, isPavementStep);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                // Swarmers need special handling.
                final int swarmerId = entity.getSwarmAttackerId();
                boolean swarmerDone = true;
                Entity swarmer = null;
                if (Entity.NONE != swarmerId) {
                    swarmer = getGame().getEntity(swarmerId);
                    swarmerDone = swarmer.isDone();
                }

                // Now do the skill check.
                entity.setFacing(curFacing);
                gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos, curPos, rollTarget, true);

                // Swarming infantry platoons may drown.
                if (curHex.terrainLevel(Terrains.WATER) > 1) {
                    gameManager.drownSwarmer(entity, curPos);
                }

                // Do we need to remove a game turn for the swarmer
                if (!swarmerDone && (swarmer != null)
                      && (swarmer.isDoomed() || swarmer.isDestroyed())) {
                    // We have to diddle with the swarmer's
                    // status to get its turn removed.
                    swarmer.setDone(false);
                    swarmer.setUnloaded(false);

                    // Dead entities don't take turns.
                    getGame().removeTurnFor(swarmer);
                    gameManager.send(gameManager.getPacketHelper().createTurnListPacket());

                    // Return the original status.
                    swarmer.setDone(true);
                    swarmer.setUnloaded(true);
                }

                // check for inferno wash-off
                gameManager.checkForWashedInfernos(entity, curPos, step.getBoardId());
            }

            // In water, may or may not be a new hex, necessary to
            // check during movement, for breach damage, and always
            // set dry if appropriate
            // TODO : possibly make the locations local and set later
            addReport(gameManager.doSetLocationsExposure(entity, curHex,
                  stepMoveType == EntityMovementType.MOVE_JUMP,
                  step.getElevation()));

            // check for breaking ice by breaking through from below
            if ((lastElevation < 0) && (step.getElevation() == 0)
                  && lastHex.containsTerrain(Terrains.ICE)
                  && lastHex.containsTerrain(Terrains.WATER)
                  && (stepMoveType != EntityMovementType.MOVE_JUMP)
                  && !lastPos.equals(curPos)) {
                // need to temporarily reset entity's position so it doesn't
                // fall in the ice
                entity.setPosition(curPos);
                report = new Report(2410);
                report.addDesc(entity);
                addReport(report);
                addReport(gameManager.resolveIceBroken(lastPos));
                // ok now set back
                entity.setPosition(lastPos);
            }
            // check for breaking ice by stepping on it
            if (curHex.containsTerrain(Terrains.ICE)
                  && curHex.containsTerrain(Terrains.WATER)
                  && (stepMoveType != EntityMovementType.MOVE_JUMP)
                  && !lastPos.equals(curPos) && !(entity instanceof Infantry)
                  && !(isPavementStep && curHex.containsTerrain(Terrains.BRIDGE))) {
                if (step.getElevation() == 0) {
                    Roll diceRoll = Compute.rollD6(1);
                    report = new Report(2118);
                    report.addDesc(entity);
                    report.add(diceRoll);
                    report.subject = entity.getId();
                    addReport(report);

                    if (diceRoll.getIntValue() == 6) {
                        entity.setPosition(curPos);
                        addReport(gameManager.resolveIceBroken(curPos));
                        curPos = entity.getPosition();
                    }
                }
                // or intersecting it
                else if ((step.getElevation() + entity.height()) == 0) {
                    report = new Report(2410);
                    report.addDesc(entity);
                    addReport(report);
                    addReport(gameManager.resolveIceBroken(curPos));
                }
            }

            // Check for black ice
            int minTemp = -30;
            boolean useBlackIce = getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_BLACK_ICE);
            boolean goodTemp = getGame().getPlanetaryConditions().getTemperature() <= minTemp;
            boolean goodWeather = getGame().getPlanetaryConditions().getWeather().isIceStorm();
            if (isPavementStep && ((useBlackIce && goodTemp) || goodWeather)) {
                if (!curHex.containsTerrain(Terrains.BLACK_ICE)) {
                    int blackIceChance = Compute.d6(1);
                    if (blackIceChance > 4) {
                        curHex.addTerrain(new Terrain(Terrains.BLACK_ICE, 1));
                        gameManager.sendChangedHex(curPos);
                    }
                }
            }

            // Handle loading units.
            if (step.getType() == MoveStepType.LOAD) {

                // Find the unit being loaded.  We will _try_ to use the one selected by the player.
                Targetable target = step.getTarget(getGame());
                Entity loaded = null;
                if (target != null) {
                    // Load designated target entity; checks should have already been done in UI.
                    loaded = (Entity) target;
                } else {
                    // Randomly select a likely-looking entity from those in the allowed loading area
                    Iterator<Entity> entities = getGame().getEntities(
                          Compute.getLoadableCoords(entity, curPos, curBoardId)
                    );
                    while (entities.hasNext()) {

                        // Is the other unit friendly and not the current entity?
                        loaded = entities.next();

                        // This should never ever happen, but just in case...
                        if (loaded == null) {
                            continue;
                        }

                        if (!entity.isEnemyOf(loaded) && !entity.equals(loaded)) {
                            // The moving unit should be able to load the other
                            // unit and the other should be able to have a turn.
                            break;

                        } else {
                            // Nope. Discard it.
                            loaded = null;
                        }

                    } // Move to the next entity in the loading area.
                }

                // We were supposed to find someone to load.
                if (loaded == null) {
                    logger.error("Could not find unit for {} to load in {}", entity.getShortName(), curPos);
                } else {
                    // We did!  But things might have changed since choosing them.  Double-check validity.
                    if (!entity.canLoad(loaded) || !loaded.isLoadableThisTurn()) {
                        // Something is fishy in Denmark.
                        logger.error("{} can not load {}", entity.getShortName(), loaded.getShortName());
                        loaded = null;
                    } else {
                        // Have the deployed unit load the indicated unit.
                        gameManager.loadUnit(entity, loaded, loaded.getTargetBay());
                    }
                }

            } // End STEP_LOAD

            // Handle towing units.
            if (step.getType() == MoveStepType.TOW) {

                // Find the unit being loaded.
                Entity loaded;
                loaded = getGame().getEntity(entity.getTowing());

                // This should never ever happen, but just in case...
                if (loaded == null) {
                    logger.error("Could not find unit for {} to tow.", entity.getShortName());
                    continue;
                }

                // The moving unit should be able to tow the other
                // unit and the other should be able to have a turn.
                // FIXME: I know this check duplicates functions already performed when enabling
                // the Tow button.
                // This code made more sense as borrowed from "Load" where we actually rechecked
                // the hex for the target unit.
                // Do we need it here for safety, client/server sync or can this be further
                // streamlined?
                if (!entity.canTow(loaded.getId())) {
                    // Something is fishy in Denmark.
                    logger.error("{} can not tow {}", entity.getShortName(), loaded.getShortName());
                } else {
                    // Have the deployed unit load the indicated unit.
                    gameManager.towUnit(entity, loaded);
                }
            } // End STEP_TOW

            // Handle mounting units to small craft/DropShip
            if (step.getType() == MoveStepType.MOUNT) {
                Targetable targetToMountInto = step.getTarget(getGame());
                if (targetToMountInto instanceof Entity entityToMountInto) {
                    if (!entityToMountInto.canLoad(entity)) {
                        // Something is fishy in Denmark.
                        logger.error("(Mounted) {} can not load {}", entityToMountInto.getShortName(),
                              entity.getShortName());
                    } else {
                        // Have the indicated unit load this unit.
                        entity.setDone(true);
                        gameManager.loadUnit(entityToMountInto, entity, entity.getTargetBay());
                        Bay currentBay = entityToMountInto.getBay(entity);
                        if ((null != currentBay) && (Compute.d6(2) == 2)) {
                            report = new Report(9390);
                            report.subject = entity.getId();
                            report.indent(1);
                            report.add(currentBay.getTransporterType());
                            addReport(report);
                            currentBay.destroyDoorNext();
                        }
                        // Stop looking.
                        curPos = entity.getPosition();
                        gameManager.entityUpdate(entityToMountInto.getId());
                        return;
                    }
                }
            } // End STEP_MOUNT

            if (step.getType() == MoveStepType.PICKUP_CARGO) {
                var carryableObjects = getGame().getGroundObjects(step.getPosition());
                carryableObjects.addAll(getGame().getEntitiesVector(step.getPosition())
                      .stream()
                      .filter(entity::canPickupCarryableObject)
                      .toList());
                Integer cargoPickupIndex;

                // if there's only one object on the ground, let's just get that one and ignore
                // any parameters
                if (carryableObjects.size() == 1) {
                    cargoPickupIndex = 0;
                } else {
                    cargoPickupIndex = step.getAdditionalData(MoveStep.CARGO_PICKUP_KEY);
                }

                Integer cargoPickupLocation = step.getAdditionalData(MoveStep.CARGO_LOCATION_KEY);

                // there have to be objects on the ground, and we have to be trying to pick up one of them
                if ((!carryableObjects.isEmpty()) &&
                      (cargoPickupIndex != null) && (cargoPickupIndex >= 0)
                      && (cargoPickupIndex < carryableObjects.size())) {

                    ICarryable pickupTarget = carryableObjects.get(cargoPickupIndex);
                    // FIXME #7640: Update once we can properly specify any transporter an entity has, and properly load into that transporter.
                    if (entity.maxGroundObjectTonnage() >= pickupTarget.getTonnage() || ((entity.getTransports().size()
                          > (Integer.MAX_VALUE - cargoPickupLocation))
                          && (entity.getTransports()
                          .get(Integer.MAX_VALUE - cargoPickupLocation) instanceof ExternalCargo externalCargo
                          && externalCargo.canLoadCarryable(pickupTarget)))) {
                        pickupTarget.processPickupStep(step, cargoPickupLocation, gameManager, entity,
                              overallMoveType);
                    } else {
                        logger.warn(
                              "{} attempted to pick up object but it is too heavy. Carry capacity: {}, object weight: {}",
                              entity.getShortName(),
                              entity.maxGroundObjectTonnage(),
                              pickupTarget.getTonnage());
                    }
                } else {
                    logger.warn("{} attempted to pick up non existent object at coords {}, index {}",
                          entity.getShortName(),
                          step.getPosition(),
                          cargoPickupIndex);
                }
            }

            if (step.getType() == MoveStepType.DROP_CARGO) {
                Integer cargoLocation = step.getAdditionalData(MoveStep.CARGO_LOCATION_KEY);
                ICarryable cargo = null;

                // if we're not supplied a specific location, then the assumption is we only have one piece of cargo,
                // and we're going to just drop that one
                if (cargoLocation == null) {
                    cargo = entity.getDistinctCarriedObjects().getFirst();
                } else if (entity.getCarriedObject(cargoLocation) != null) {
                    cargo = entity.getCarriedObject(cargoLocation);
                } else if ((cargoLocation >= 0) && (Integer.MAX_VALUE - cargoLocation < entity.getTransports()
                      .size())) {
                    // FIXME #7640: Update once we can properly specify any transporter an entity has, and properly load into that transporter.
                    Transporter transporter = entity.getTransports().get(Integer.MAX_VALUE - cargoLocation);
                    if (transporter instanceof ExternalCargo externalCargo) {
                        cargo = externalCargo.getCarryables().stream().findFirst().orElse(null);
                    }
                }
                if (cargo == null) {
                    logger.error("No cargo to drop at location {}", cargoLocation);
                    return;
                }

                entity.dropCarriedObject(cargo, isLastStep);
                if (cargo instanceof Entity carriedEntity) {
                    gameManager.unloadUnit(entity, carriedEntity, step.getPosition(), step.getFacing(),
                          step.getElevation());
                }


                boolean cargoDestroyed = false;

                if (!isLastStep) {
                    cargoDestroyed = gameManager.damageCargo(step.isFlying() || step.isJumping(), entity, cargo);
                }

                // note that this should not be moved into the "!isLastStep" block above as cargo may be either
                // unloaded peacefully or dumped on the move
                if (!cargoDestroyed) {
                    if (cargo instanceof GroundObject) {
                        getGame().placeGroundObject(step.getPosition(), cargo);
                    }

                    report = new Report(2514);
                    report.subject = entity.getId();
                    report.add(entity.getDisplayName());
                    report.add(cargo.generalName());
                    report.add(step.getPosition().toFriendlyString());
                    addReport(report);

                    // a drop changes board state. Send an update for the overall ground object
                    // list.
                    if (cargo instanceof GroundObject) {
                        gameManager.sendGroundObjectUpdate();
                    } else if (cargo instanceof Entity) {
                        gameManager.send(gameManager.getPacketHelper().createTurnListPacket());
                    }
                }
            }

            // handle fighter recovery, and also DropShip docking with another large craft
            if (step.getType() == MoveStepType.RECOVER) {

                loader = getGame().getEntity(step.getRecoveryUnit());
                boolean isDS = (entity instanceof Dropship);

                rollTarget = entity.getBasePilotingRoll(overallMoveType);
                if (loader.mpUsed > 0) {
                    rollTarget.addModifier(5, "carrier used thrust");
                }
                if (entity.getPartialRepairs().booleanOption("aero_collar_crit")) {
                    rollTarget.addModifier(2, "misrepaired docking collar");
                }
                if (isDS && (((Dropship) entity).getCollarType() == Dropship.COLLAR_PROTOTYPE)) {
                    rollTarget.addModifier(2, "prototype kf-boom");
                }
                Roll diceRoll = Compute.rollD6(2);

                if (isDS) {
                    report = new Report(9388);
                } else {
                    report = new Report(9381);
                }

                report.subject = entity.getId();
                report.add(entity.getDisplayName());
                report.add(loader.getDisplayName());
                report.add(rollTarget);
                report.add(diceRoll);
                report.newlines = 0;
                report.indent(1);

                if (diceRoll.getIntValue() < rollTarget.getValue()) {
                    report.choose(false);
                    addReport(report);
                    // damage unit
                    HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                    addReport(gameManager.damageEntity(entity, hit,
                          2 * (rollTarget.getValue() - diceRoll.getIntValue())));
                } else {
                    report.choose(true);
                    addReport(report);
                    recovered = true;
                }
                // check for door damage
                if (diceRoll.getIntValue() == 2) {
                    loader.damageDoorRecovery(entity);
                    report = new Report(9384);
                    report.subject = entity.getId();
                    report.indent(0);
                    report.add(loader.getDisplayName());
                    addReport(report);
                }
            }

            // handle fighter squadron joining
            if (step.getType() == MoveStepType.JOIN) {
                loader = getGame().getEntity(step.getRecoveryUnit());
                recovered = true;
            }

            // Handle unloading units.
            if (step.getType() == MoveStepType.UNLOAD) {
                Targetable unloaded = step.getTarget(getGame());
                Bay currentBay = (unloaded instanceof Entity ulEntity) ? entity.getBay(ulEntity) : null;
                Coords unloadPos = curPos;
                int unloadFacing = curFacing;

                // If the step has a targetPosition, use that
                if (null != step.getTargetPosition()) {
                    unloadPos = step.getTargetPosition();
                    unloadFacing = curPos.direction(unloadPos);
                }

                if (!gameManager.unloadUnit(entity, unloaded, unloadPos, unloadFacing,
                      step.getElevation())) {
                    logger.error("Server was told to unload {} from {} into {}",
                          unloaded.getDisplayName(),
                          entity.getDisplayName(),
                          curPos.getBoardNum());
                } else {
                    // Report unloading; anyone who can see the carrier should see the new unit too.
                    report = new Report(2514);
                    report.subject = unloaded.getId();
                    report.add(entity.getDisplayName());
                    report.add(unloaded.generalName());
                    report.add(unloadPos.toFriendlyString());
                    addReport(report);

                    // Report glider wings landing safely (IO p.85)
                    if ((entity instanceof VTOL) && (unloaded instanceof Infantry)) {
                        Infantry infantry = (Infantry) unloaded;
                        if (infantry.hasAbility(OptionsConstants.MD_PL_GLIDER)) {
                            report = new Report(2521);
                            report.subject = unloaded.getId();
                            report.indent(1);
                            report.addDesc(infantry);
                            addReport(report);
                        }
                    }
                }

                // some additional stuff to take care of for small
                // craft/DropShip unloading
                if ((entity instanceof SmallCraft) && (unloaded instanceof Entity)) {
                    if ((null != currentBay) && (!(unloaded.isInfantry()))
                          && (Compute.d6(2) == 2)
                    ) {
                        report = new Report(9390);
                        report.subject = entity.getId();
                        report.indent(1);
                        report.add(currentBay.getTransporterType());
                        addReport(report);
                        currentBay.destroyDoorNext();
                    }
                    // now apply any damage to bay doors
                    entity.resetBayDoors();
                    gameManager.entityUpdate(entity.getId());
                    // ok now add another turn for the transport so it can
                    // continue to unload units
                    if (!entity.getUnitsUnloadableFromBays().isEmpty()) {
                        dropshipStillUnloading = true;
                        GameTurn newTurn = new SpecificEntityTurn(
                              entity.getOwner().getId(), entity.getId());
                        // Need to set the new turn's multiTurn state
                        newTurn.setMultiTurn(true);
                        getGame().insertNextTurn(newTurn);
                    }
                    // ok add another turn for the unloaded entity so that it can move
                    if (!(unloaded instanceof Infantry)) {
                        GameTurn newTurn = new SpecificEntityTurn(
                              ((Entity) unloaded).getOwner().getId(),
                              unloaded.getId());
                        // Need to set the new turn's multiTurn state
                        newTurn.setMultiTurn(true);
                        getGame().insertNextTurn(newTurn);
                    }
                    // brief everybody on the turn update
                    gameManager.send(gameManager.getPacketHelper().createTurnListPacket());
                }
            }

            // Handle disconnecting trailers.
            if (step.getType() == MoveStepType.DISCONNECT) {
                Targetable unloaded = step.getTarget(getGame());
                Coords unloadPos = curPos;
                if (null != step.getTargetPosition()) {
                    unloadPos = step.getTargetPosition();
                }
                if (!gameManager.disconnectUnit(entity, unloaded, unloadPos)) {
                    logger.error("Server was told to disconnect {} from {} into {}",
                          unloaded.getDisplayName(),
                          entity.getDisplayName(),
                          curPos.getBoardNum());
                }
            }

            // moving backwards over elevation change
            if (((step.getType() == MoveStepType.BACKWARDS)
                  || (step.getType() == MoveStepType.LATERAL_LEFT_BACKWARDS)
                  || (step.getType() == MoveStepType.LATERAL_RIGHT_BACKWARDS))
                  && !(md.isJumping() && step.isUsingMekJumpBooster())
                  && (lastHex.getLevel() + lastElevation != curHex.getLevel() + step.getElevation())
                  && !(entity instanceof VTOL)
                  && !(curClimbMode
                  && curHex.containsTerrain(Terrains.BRIDGE)
                  && ((curHex.terrainLevel(Terrains.BRIDGE_ELEV) + curHex.getLevel()) == (prevHex.getLevel()
                  + (prevHex.containsTerrain(Terrains.BRIDGE)
                  ? prevHex.terrainLevel(Terrains.BRIDGE_ELEV)
                  : 0))))) {

                // per TacOps, if the mek is walking backwards over an elevation change and
                // falls
                // it falls into the lower hex. The caveat is if it already fell from some other
                // PSR in this
                // invocation of processMovement, then it can't fall again.
                if ((entity instanceof Mek)
                      && (curHex.getLevel() < getGame().getBoard(curBoardId).getHex(lastPos).getLevel())
                      && !entity.hasFallen()) {
                    rollTarget = entity.getBasePilotingRoll(overallMoveType);
                    rollTarget.addModifier(0, "moving backwards over an elevation change");
                    gameManager.doSkillCheckWhileMoving(entity, entity.getElevation(),
                          curPos, curPos, rollTarget, true);
                } else if ((entity instanceof Mek) && !entity.hasFallen()) {
                    rollTarget = entity.getBasePilotingRoll(overallMoveType);
                    rollTarget.addModifier(0, "moving backwards over an elevation change");
                    gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos, lastPos, rollTarget, true);
                } else if (entity instanceof Tank) {
                    rollTarget = entity.getBasePilotingRoll(overallMoveType);
                    rollTarget.addModifier(0, "moving backwards over an elevation change");
                    if (gameManager.doSkillCheckWhileMoving(entity, entity.getElevation(), curPos, lastPos,
                          rollTarget, false) < 0) {
                        curPos = lastPos;
                    }
                }
            }

            // Handle non-infantry moving into a building.
            if (buildingMove > 0) {
                // Get the building being exited.
                IBuilding bldgExited = null;
                if ((buildingMove & 1) == 1) {
                    bldgExited = getGame().getBoard(curBoardId).getBuildingAt(lastPos);
                }

                // Get the building being entered.
                IBuilding bldgEntered = null;
                if ((buildingMove & 2) == 2) {
                    bldgEntered = getGame().getBoard(curBoardId).getBuildingAt(curPos);
                }

                // ProtoMeks changing levels within a building cause damage
                if (((buildingMove & 8) == 8) && (entity instanceof ProtoMek)) {
                    IBuilding bldg = getGame().getBoard(curBoardId).getBuildingAt(curPos);
                    Vector<Report> vBuildingReport = gameManager.damageBuilding(bldg, 1, curPos);
                    for (Report report : vBuildingReport) {
                        report.subject = entity.getId();
                    }
                    addReport(vBuildingReport);
                }

                boolean collapsed = false;
                if ((bldgEntered != null)) {
                    String reason = getReason(bldgExited, bldgEntered);

                    gameManager.passBuildingWall(entity,
                          bldgEntered,
                          lastPos,
                          curPos,
                          distance,
                          reason,
                          step.isThisStepBackwards(),
                          lastStepMoveType,
                          true);
                    gameManager.addAffectedBldg(bldgEntered, collapsed);
                }

                // Clean up the entity if it has been destroyed.
                if (entity.isDoomed()) {
                    entity.setDestroyed(true);
                    getGame().moveToGraveyard(entity.getId());
                    gameManager.send(gameManager.createRemoveEntityPacket(entity.getId()));

                    // The entity's movement is completed.
                    return;
                }

                // TODO : what if a building collapses into rubble?
            }

            if (stepMoveType != EntityMovementType.MOVE_JUMP
                  && (step.getClearance() == 0
                  || (entity.getMovementMode().isWiGE() && (step.getClearance() == 1))
                  || curElevation == curHex.terrainLevel(Terrains.BLDG_ELEV)
                  || curElevation == curHex.terrainLevel(Terrains.BRIDGE_ELEV))) {
                IBuilding bldg = getGame().getBoard(curBoardId).getBuildingAt(curPos);
                if ((bldg != null) && (entity.getElevation() >= 0)) {
                    boolean wigeFlyingOver = entity.getMovementMode() == EntityMovementMode.WIGE
                          && ((curHex.containsTerrain(Terrains.BLDG_ELEV)
                          && curElevation > curHex.terrainLevel(Terrains.BLDG_ELEV)) ||
                          (curHex.containsTerrain(Terrains.BRIDGE_ELEV)
                                && curElevation > curHex.terrainLevel(Terrains.BRIDGE_ELEV)));
                    boolean collapse = gameManager.checkBuildingCollapseWhileMoving(bldg, entity, curPos);
                    gameManager.addAffectedBldg(bldg, collapse);
                    // If the building is collapsed by a WiGE flying over it, the WiGE drops one
                    // level of elevation.
                    // This could invalidate the remainder of the movement path, so we will send it
                    // back to the client.
                    if (collapse && wigeFlyingOver) {
                        curElevation--;
                        report = new Report(2378);
                        report.subject = entity.getId();
                        report.addDesc(entity);
                        addReport(report);
                        continueTurnFromLevelDrop = true;
                        entity.setPosition(curPos);
                        entity.setFacing(curFacing);
                        entity.setSecondaryFacing(curFacing);
                        entity.setElevation(curElevation);
                        break;
                    }
                }
            }

            // Sheer Cliffs, TO p.39
            boolean vehicleAffectedByCliff = entity instanceof Tank
                  && !entity.isAirborneVTOLorWIGE();
            boolean quadveeVehMode = entity instanceof QuadVee
                  && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE;
            boolean mekAffectedByCliff = (entity instanceof Mek || entity instanceof ProtoMek)
                  && moveType != EntityMovementType.MOVE_JUMP
                  && !entity.isAero();
            // Cliffs should only exist towards 1 or 2 level drops, check just to make sure
            // Everything that does not have a 1 or 2 level drop shouldn't be handled as a
            // cliff
            int stepHeight = curElevation + curHex.getLevel()
                  - (lastElevation + prevHex.getLevel());
            boolean isUpCliff = !lastPos.equals(curPos)
                  && curHex.hasCliffTopTowards(prevHex)
                  && (stepHeight == 1 || stepHeight == 2);
            boolean isDownCliff = !lastPos.equals(curPos)
                  && prevHex.hasCliffTopTowards(curHex)
                  && (stepHeight == -1 || stepHeight == -2);

            // Vehicles (exc. WIGE/VTOL) moving down a cliff
            if (vehicleAffectedByCliff && isDownCliff && !isPavementStep) {
                rollTarget = entity.getBasePilotingRoll(stepMoveType);
                rollTarget.append(new PilotingRollData(entity.getId(), 0, "moving down a sheer cliff"));
                if (gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                      lastPos, curPos, rollTarget, false) > 0) {
                    addReport(gameManager.vehicleMotiveDamage((Tank) entity, 0));
                    addNewLines();
                    turnOver = true;
                    break;
                }
            }

            // Meks and Protomeks moving down a cliff
            // QuadVees in vee mode ignore PSRs to avoid falls, IO p.133
            if (mekAffectedByCliff && !quadveeVehMode && isDownCliff && !isPavementStep) {
                rollTarget = entity.getBasePilotingRoll(moveType);
                rollTarget.append(new PilotingRollData(entity.getId(), -stepHeight - 1, "moving down a sheer cliff"));
                if (gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                      lastPos, curPos, rollTarget, true) > 0) {
                    addNewLines();
                    turnOver = true;
                    break;
                }
            }

            // Meks moving up a cliff
            if (mekAffectedByCliff && !quadveeVehMode && isUpCliff && !isPavementStep) {
                rollTarget = entity.getBasePilotingRoll(moveType);
                rollTarget.append(new PilotingRollData(entity.getId(), stepHeight, "moving up a sheer cliff"));
                if (gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                      lastPos, lastPos, rollTarget, false) > 0) {
                    report = new Report(2209);
                    report.addDesc(entity);
                    report.subject = entity.getId();
                    addReport(report);
                    addNewLines();
                    curPos = entity.getPosition();
                    mpUsed = step.getMpUsed();
                    continueTurnFromCliffAscent = true;
                    break;
                }
            }

            // TacOps Climbing PSR checks (TO:AR p.20)
            // Each level climbed requires a Piloting Skill Roll with +1 modifier.
            // Additional +2 modifier if only one functional arm.
            // On failure, the Mek falls from the last level successfully reached.
            // Multi-turn: if the climb costs more MP than available, only climb
            // affordable levels this turn and persist climbing state for next turn.
            if (step.isClimbing() && (entity instanceof Mek climbingMek)
                  && (stepHeight > 0)) {
                // Only process climbing PSRs for upward movement (stepHeight > 0)
                // Downward movement is handled by leaping rules
                int totalLevelsToClimb = Math.abs(stepHeight);
                int climbableArms = ClimbingHelper.countClimbableArms(climbingMek);
                int costPerLevel = ClimbingHelper.getClimbingMPCostPerLevel(climbingMek);
                int walkMP = climbingMek.getWalkMP();

                // Calculate how many levels we can afford this turn.
                // Take MP already spent before this climbing step from the previous step's
                // mpUsed (includes any walk + 1 MP for entering the climb hex). The previous
                // approach (step.getMpUsed() - totalLevelsToClimb * costPerLevel) was wrong
                // when the client compiled with chosenLevels < totalLevelsToClimb — it
                // back-calculated against the FULL climb cost and produced a negative
                // nonClimbMpUsed, granting more MP than the unit had.
                int nonClimbMpUsed = (previousStep != null) ? previousStep.getMpUsed() : 0;
                int availableMP = Math.max(0, walkMP - nonClimbMpUsed);
                int affordableLevels = availableMP / costPerLevel;
                int levelsThisTurn = Math.min(totalLevelsToClimb, Math.max(0, affordableLevels));
                // Honor the player's choice from the climbing dialog (TO:AR p.20).
                // Without this cap the server would climb the maximum affordable, ignoring
                // a smaller player-chosen count. The client pushes this via sendUpdateEntity
                // before committing the path. 0 means "no choice — use max affordable."
                int chosenLevels = entity.getClimbingLevelsChosen();
                if (chosenLevels > 0) {
                    levelsThisTurn = Math.min(levelsThisTurn, chosenLevels);
                }

                boolean fellWhileClimbing = false;
                // Track the climbing elevation - starts at the entity's elevation
                // in the lower hex and increments by 1 for each successful level
                int climbingElevation = lastElevation;
                // Total climb height includes levels already climbed in prior turns
                int levelsAlreadyClimbed = climbingElevation;
                int overallClimbHeight = levelsAlreadyClimbed + totalLevelsToClimb;

                logger.debug("Climbing: totalLevels={}, affordableLevels={}, levelsThisTurn={}, " +
                      "walkMP={}, availableMP={}, costPerLevel={}, nonClimbMpUsed={}, " +
                      "levelsAlreadyClimbed={}, overallClimbHeight={}",
                      totalLevelsToClimb, affordableLevels, levelsThisTurn,
                      walkMP, availableMP, costPerLevel, nonClimbMpUsed,
                      levelsAlreadyClimbed, overallClimbHeight);

                for (int levelClimbed = 1; levelClimbed <= levelsThisTurn; levelClimbed++) {
                    int overallLevel = levelsAlreadyClimbed + levelClimbed;
                    rollTarget = entity.getBasePilotingRoll(moveType);
                    rollTarget.append(new PilotingRollData(entity.getId(),
                          ClimbingHelper.CLIMBING_PSR_MODIFIER,
                          "climbing (level " + overallLevel + " of " + overallClimbHeight + ")"));
                    if (climbableArms == 1) {
                        rollTarget.append(new PilotingRollData(entity.getId(),
                              ClimbingHelper.ONE_ARM_PSR_MODIFIER,
                              "climbing with one arm"));
                    }

                    // When climbingElevation is 0 the Mek hasn't actually risen yet — failing
                    // the very first PSR means it slipped before getting off the ground, so
                    // skip the fall mechanic (no damage, no prone). Climbing aborts for the
                    // turn but the Mek stays on the ground. Per pragmatic reading of TO:AR p.20:
                    // "fall from the height it has currently reached" — height 0 = no fall.
                    boolean canFallFromHere = climbingElevation > 0;
                    if (gameManager.doSkillCheckWhileMoving(entity, climbingElevation,
                          lastPos, lastPos, rollTarget, canFallFromHere) > 0) {
                        if (!canFallFromHere) {
                            // Failed first PSR from elevation 0 — abort climb without fall.
                            logger.debug("[CLIMB-TRACE] Failed first climbing PSR from elevation 0; "
                                        + "no fall (entity stays on ground). entity={}, lastPos={}",
                                  entity.getDisplayName(), lastPos);
                            Report failReport = new Report(6464, Report.PUBLIC);
                            failReport.add(entity.getDisplayName());
                            addReport(failReport);
                            entity.setClimbing(false);
                            entity.setDangling(false);
                            entity.setClimbingLevelsChosen(0);
                            // Step processing earlier in the loop already moved the entity to the
                            // climb step's destination (the building roof / cliff top). Revert it
                            // to the source hex at ground level so the failed handhold actually
                            // leaves the unit on the ground — otherwise the entity stays at the
                            // top with climbing=false, looking like a successful zero-MP climb.
                            entity.setPosition(lastPos);
                            entity.setElevation(0);
                            curPos = lastPos;
                            curVTOLElevation = 0;
                            mpUsed = step.getMpUsed();
                            fellWhileClimbing = true;
                            turnOver = true;
                            break;
                        }
                        // Mek falls from the last level successfully reached
                        // doEntityFallsInto handles positioning and elevation for the terrain
                        entity.setClimbing(false);
                        curPos = entity.getPosition();
                        curVTOLElevation = entity.getElevation();
                        logger.debug("[FALL-TRACE] After doSkillCheckWhileMoving fall: " +
                              "entity.position={}, entity.elevation={}, entity.isProne={}, " +
                              "climbingElevation={}, lastPos={}, curPos={}, curVTOLElevation={}",
                              entity.getPosition(), entity.getElevation(), entity.isProne(),
                              climbingElevation, lastPos, curPos, curVTOLElevation);
                        Hex fallHex = getGame().getBoard(entity.getBoardId()).getHex(entity.getPosition());
                        if (fallHex != null) {
                            logger.debug("[FALL-TRACE] Fall hex: level={}, ceiling={}, floor={}, depth={}, " +
                                  "containsWater={}, isElevationValid={}",
                                  fallHex.getLevel(), fallHex.ceiling(), fallHex.floor(), fallHex.depth(),
                                  fallHex.containsTerrain(Terrains.WATER),
                                  entity.isElevationValid(entity.getElevation(), fallHex));
                        }
                        fellWhileClimbing = true;
                        fellDuringMovement = true;
                        turnOver = true;
                        break;
                    }
                    // Successfully climbed one more level
                    climbingElevation++;

                    // Building CF check during climbing (TO:AR p.20)
                    // "If the weight of the unit exceeds the Construction Factor of the
                    // hex — whether that occurs when the unit starts the climb, or during
                    // the climb — the hex collapses."
                    // The building being climbed is at the destination hex (curPos)
                    Hex climbHex = getGame().getBoard(entity.getBoardId()).getHex(curPos);
                    logger.debug("[FALL-TRACE] Building check: curPos={}, lastPos={}, " +
                          "containsBuilding={}", curPos, lastPos,
                          climbHex.containsTerrain(Terrains.BUILDING));
                    if (climbHex.containsTerrain(Terrains.BUILDING)) {
                        IBuilding climbBldg = getGame().getBoard(entity.getBoardId()).getBuildingAt(curPos);
                        if (climbBldg != null) {
                            // On the first level of a NEW climb, damage the building (entering the hex)
                            // Don't damage again on continued climbs (entity was already climbing)
                            if ((levelClimbed == 1) && (levelsAlreadyClimbed == 0)) {
                                int buildingDamage = (int) Math.ceil(entity.getWeight() / 10.0);
                                logger.debug("[FALL-TRACE] Building climbing damage: {} points to {} at {}",
                                      buildingDamage, climbBldg.getName(), curPos);
                                addReport(gameManager.damageBuilding(climbBldg, buildingDamage, curPos));
                            }

                            // Check if unit weight exceeds current CF (may have changed
                            // due to damage from other sources)
                            int currentCF = climbBldg.getCurrentCF(curPos);
                            logger.debug("[FALL-TRACE] Building CF check: weight={}, CF={}",
                                  entity.getWeight(),
                                  currentCF);
                            if (entity.getWeight() > currentCF) {
                                logger.debug("[FALL-TRACE] Building too weak for climbing entity: " +
                                      "weight={}, CF={}, climbingElevation={}, curPos={}, lastPos={}",
                                      entity.getWeight(), currentCF, climbingElevation, curPos, lastPos);
                                // Entity falls from climbing elevation before collapse
                                entity.setClimbing(false);
                                entity.setDangling(false);
                                entity.setElevation(climbingElevation);
                                entity.setPosition(lastPos);
                                // Process the fall from climbing elevation
                                PilotingRollData climbFallRoll = new PilotingRollData(entity.getId(),
                                      TargetRoll.AUTOMATIC_FAIL, "building too weak");
                                addReport(gameManager.doEntityFallsInto(entity, climbingElevation,
                                      lastPos, lastPos, climbFallRoll, true, 0));
                                curPos = entity.getPosition();
                                curVTOLElevation = entity.getElevation();
                                fellWhileClimbing = true;
                                fellDuringMovement = true;
                                turnOver = true;
                                break;
                            }
                        }
                    }
                }

                if (fellWhileClimbing) {
                    break;
                }

                // Did we complete the full climb or just a partial?
                if (levelsThisTurn < totalLevelsToClimb) {
                    // Partial climb - Mek clings to cliff face at intermediate elevation
                    // Stay in the lower hex, facing the higher hex, at climbing elevation
                    entity.setPosition(lastPos);
                    entity.setFacing(curFacing);
                    entity.setElevation(climbingElevation);
                    entity.setClimbing(true);
                    // Climbing back up exits any prior dangle state
                    entity.setDangling(false);
                    // Reset chosen-levels so next turn's dialog starts fresh
                    entity.setClimbingLevelsChosen(0);
                    curPos = lastPos;
                    curVTOLElevation = climbingElevation;
                    mpUsed = walkMP;
                    logger.debug("Climbing: partial climb, {} of {} levels. " +
                          "Clinging at elevation {} in hex {}",
                          levelsThisTurn, totalLevelsToClimb, climbingElevation, lastPos);
                    // End movement - spent all MP climbing
                    turnOver = true;
                    break;
                } else if (totalLevelsToClimb > 0) {
                    // Completed the climb - Mek enters the upper hex
                    entity.setClimbing(false);
                    // Climbing back up from a dangle terminates the dangle state
                    entity.setDangling(false);
                    entity.setClimbingLevelsChosen(0);
                    // Push a "reached the top!" toast so the player gets explicit feedback that
                    // the multi-turn climb finished. Without this they only know by spotting
                    // the elevation indicator and the cleared climbing flag.
                    Report topReport = new Report(6466, Report.PUBLIC);
                    topReport.add(entity.getDisplayName());
                    addReport(topReport);
                    Vector<Report> topSpecial = new Vector<>();
                    topSpecial.add(topReport);
                    gameManager.send(gameManager.createSpecialReportPacket(topSpecial));
                    logger.debug("Climbing: completed full climb of {} levels", totalLevelsToClimb);
                } else {
                    // No levels to climb (e.g. moved to same-level hex while climbing)
                    // Keep climbing state if still at intermediate elevation
                    if (climbingElevation > 0) {
                        entity.setClimbing(true);
                        entity.setElevation(climbingElevation);
                        logger.debug("Climbing: no levels to climb but still at elevation {}, keeping climbing state",
                              climbingElevation);
                    } else {
                        entity.setClimbing(false);
                        entity.setDangling(false);
                        entity.setClimbingLevelsChosen(0);
                        logger.debug("Climbing: at ground level, clearing climbing state");
                    }
                }
            }

            // did the entity just fall?
            if (!wasProne && entity.isProne()) {
                curFacing = entity.getFacing();
                curPos = entity.getPosition();
                mpUsed = step.getMpUsed();
                fellDuringMovement = true;
                break;
            }

            // dropping prone intentionally?
            if (step.getType() == MoveStepType.GO_PRONE) {
                mpUsed = step.getMpUsed();
                rollTarget = entity.checkDislodgeSwarmers(step, overallMoveType);
                if (rollTarget.getValue() == TargetRoll.CHECK_FALSE) {
                    // Not being swarmed
                    entity.setProne(true);
                    // check to see if we washed off infernos
                    gameManager.checkForWashedInfernos(entity, curPos, step.getBoardId());
                } else {
                    // Being swarmed
                    entity.setPosition(curPos);
                    if (gameManager.doDislodgeSwarmerSkillCheck(entity, rollTarget, curPos)) {
                        // Entity falls
                        curFacing = entity.getFacing();
                        curPos = entity.getPosition();
                        fellDuringMovement = true;
                        break;
                    }
                    // roll failed, go prone but don't dislodge swarmers
                    entity.setProne(true);
                    // check to see if we washed off infernos
                    gameManager.checkForWashedInfernos(entity, curPos, step.getBoardId());
                    break;
                }
            }

            // going hull down
            if (step.getType() == MoveStepType.HULL_DOWN) {
                mpUsed = step.getMpUsed();
                entity.setHullDown(true);
                logger.debug("[HullDown] {}: went hull-down via movement at {}", entity.getDisplayName(), curPos);
            }

            // Check for crushing buildings by Dropships/Mobile Structures
            for (Coords pos : step.getCrushedBuildingLocs()) {
                IBuilding bldg = getGame().getBoard(curBoardId).getBuildingAt(pos);
                Hex hex = getGame().getBoard(curBoardId).getHex(pos);

                report = new Report(3443);
                report.subject = entity.getId();
                report.addDesc(entity);
                report.add(bldg.getName());
                gameManager.getMainPhaseReport().add(report);

                final int cf = bldg.getCurrentCF(pos);
                final int numFloors = Math.max(0, hex.terrainLevel(Terrains.BLDG_ELEV));
                gameManager.getMainPhaseReport().addAll(gameManager.damageBuilding(bldg, 150, " is crushed for ", pos));
                int damage = (int) Math.round((cf / 10.0) * numFloors);
                HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                gameManager.getMainPhaseReport().addAll(gameManager.damageEntity(entity, hit, damage));
            }

            // Track this step's location.
            movePath.addElement(new UnitLocation(entity.getId(), curPos,
                  curFacing, step.getElevation(), entity.getBoardLocation().boardId()));

            // if the lastpos is not the same as the current position
            // then add the current position to the list of places passed
            // through
            if (!curPos.equals(lastPos)) {
                passedThrough.add(curPos);
                passedThroughFacing.add(curFacing);
            }

            // update lastPos, prevStep, prevFacing & prevHex
            if (!curPos.equals(lastPos)) {
                prevFacing = curFacing;
            }
            lastPos = curPos;
            lastElevation = curElevation;
            prevStep = step;
            prevHex = curHex;

            firstStep = false;

            // if we moved at all, we are no longer bracing "for free", except for when
            // the current step IS bracing
            if ((mpUsed > 0) && (step.getType() != MoveStepType.BRACE)) {
                entity.setBraceLocation(Entity.LOC_NONE);
            }
        }

    }

    /**
     * Wrapper for processPointblankShotCFR with packet error handling and consolidated reports
     *
     * @param step         MoveStep to prompt for a PBS
     * @param hiddenEntity Candidate to fire PBS
     *
     * @return Vector<Report> collection of reports; caller responsible for displaying these.
     */
    protected Vector<Report> processPossiblePBS(MoveStep step, Entity hiddenEntity) {
        Vector<Report> pbsReports = new Vector<>();
        Vector<Report> attackReports = new Vector<>();
        // Update hidden entity owner with current mover's position.
        gameManager.send(hiddenEntity.getOwnerId(),
              gameManager.createEntityPacket(this.entity.getId(), null));

        // Allow for packet data read failure
        try {
            attackReports = gameManager.processPointblankShotCFR(hiddenEntity, this.entity);
            if (attackReports != null) {
                pbsReports.addAll(attackReports);
            }
        } catch (InvalidPacketDataException e) {
            logger.error("Invalid packet data:", e);
        }

        // Report finding the hidden unit
        gameManager.entityUpdate(hiddenEntity.getId());
        report = new Report(9960);
        report.addDesc(this.entity);
        report.subject = this.entity.getId();
        report.add(hiddenEntity.getPosition().getBoardNum());
        pbsReports.add(report);

        // Report the block in Double Blind context
        if (gameManager.doBlind()) {
            report = new Report(9961);
            report.subject = hiddenEntity.getId();
            report.addDesc(hiddenEntity);
            report.addDesc(this.entity);
            report.add(step.getPosition().getBoardNum());
            pbsReports.add(report);
        }

        return pbsReports;
    }

    private String getReason(IBuilding bldgExited, IBuilding bldgEntered) {
        String reason;
        if (bldgExited == null) {
            // If we're not leaving a building, just handle the "entered".
            reason = "entering";
        } else if (bldgExited.equals(bldgEntered) && !(entity instanceof ProtoMek)
              && !(entity instanceof Infantry)) {
            // If we're moving within the same building, just handle the "within".
            reason = "moving in";
        } else {
            // If we have different buildings, roll for each.
            reason = "entering";
        }
        return reason;
    }

    /**
     * When something is improperly moved off board, like after a failed aero maneuver, we should move it back onto the
     * board so exceptions don't get thrown.
     *
     * @param position entity's current position
     * @param facing   entity's current facing
     *
     * @return new coords that are on the board
     */
    private Coords nudgeOntoBoard(Coords position, int facing) {
        Coords newPosition = position;

        Game game = getGame();
        Board board = game.getBoard();

        // When nudging horizontally, let's try to use the facing so we wind up in a more accurate position -
        // Unless the facing is north/south, then let's just pick a facing and nudge it back onto the board

        // If we're to the left of the board, nudge right until we're on the board
        while (newPosition.getX() < 0) {
            if (facing == 4 || facing == 1) {
                newPosition = newPosition.translated(1);
            } else {
                newPosition = newPosition.translated(2);
            }
        }

        // If we're to the right of the board, nudge left until we're on the board
        while (newPosition.getX() > (board.getWidth() - 1)) {
            if (facing == 2 || facing == 5) {
                newPosition = newPosition.translated(5);
            } else {
                newPosition = newPosition.translated(4);
            }
        }


        // If we're above the board, nudge down until we're on the board
        while (newPosition.getY() < 0) {
            newPosition = newPosition.translated(3);
        }

        // If we're below the board, nudge up until we're on the board
        // Note that Height is 1-indexed, so we need to make it 0-indexed.
        while (newPosition.getY() > (board.getHeight() - 1)) {
            newPosition = newPosition.translated(0);
        }

        return newPosition;
    }

    private boolean usingAeroOnGroundMovement() {
        return getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_GROUND_MOVE);
    }

    /**
     * Begins a bridge build for a Bridge-Building Engineer platoon: spends the bridge building budget, locks the
     * platoon into the build for its duration and reports the started work. The step was already validated when the
     * move path was compiled. TO:AUE p.152.
     *
     * @param infantry the platoon starting the build
     * @param step     the BUILD_BRIDGE step carrying the site, orientation and bridge type
     */
    private void processBuildBridgeStep(Infantry infantry, MoveStep step) {
        if (!(infantry instanceof ConvInfantry convInfantry)) {
            logger.warn("[BuildBridge] BUILD_BRIDGE step ignored for {}: not a conventional infantry platoon",
                  infantry.getShortName());
            return;
        }
        if (step.getBridgeTargetCoords() == null) {
            logger.warn("[BuildBridge] BUILD_BRIDGE step ignored for {}: no target hex in the step data",
                  convInfantry.getShortName());
            return;
        }
        // Another platoon may have started/paused a bridge in this hex since the path was plotted; an in-progress
        // bridge places no terrain, so reject a second build in the same hex here.
        if (ConvInfantry.isBridgeTargetClaimed(getGame(), convInfantry.getBoardId(), step.getBridgeTargetCoords(),
              convInfantry)) {
            logger.warn("[BuildBridge] BUILD_BRIDGE step ignored for {}: another platoon already has a bridge in "
                  + "progress at {}", convInfantry.getShortName(), step.getBridgeTargetCoords());
            return;
        }
        // A gap in an existing bridge (a destroyed section) is repaired rather than freshly built when the unofficial
        // bridge-repair option is on; the work is identical but the finished section matches the surviving span's deck.
        boolean repairAllowed = getGame().getOptions()
              .booleanOption(OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS);
        boolean isRepair = repairAllowed && BridgeConstruction.isBridgeRepairSite(
              getGame().getBoard(convInfantry.getBoardId()), step.getBridgeTargetCoords(), step.getBridgeExits());
        logger.info("[BuildBridge] {} begins a bridge {}: target {}, exits bitmask {}, type {} (1=light, 2=medium)",
              convInfantry.getShortName(), isRepair ? "repair" : "build", step.getBridgeTargetCoords(),
              step.getBridgeExits(), step.getBridgeType());
        // Building a bridge is the platoon's sole action, so any other ground posture (dug in / hitting the deck) ends
        convInfantry.clearGroundPostures();
        if (isRepair) {
            convInfantry.startBridgeRepair(step.getBridgeTargetCoords(), step.getBridgeExits(), step.getBridgeType());
        } else {
            convInfantry.startBridgeBuild(step.getBridgeTargetCoords(), step.getBridgeExits(), step.getBridgeType());
        }
        convInfantry.spendBridgeBuildPoints(step.getBridgeType());
        // Free facing change toward the construction site: the platoon works facing its bridge
        if (convInfantry.getPosition() != null) {
            int facingToBridge = convInfantry.getPosition().direction(step.getBridgeTargetCoords());
            convInfantry.setFacing(facingToBridge);
            convInfantry.setSecondaryFacing(facingToBridge);
        }
        Report report = new Report(isRepair ? 4288 : 4274);
        report.subject = convInfantry.getId();
        report.addDesc(convInfantry);
        report.add(step.getBridgeTargetCoords().getBoardNum());
        report.add(convInfantry.getBridgeBuildRequiredTurns());
        addReport(report);
    }

    /**
     * Cancels an in-progress bridge build and begins dismantling it. Dismantling takes as many turns as were spent
     * building; the spent budget is refunded only once dismantling finishes, in the END phase. TO:AUE p.152.
     *
     * @param infantry the platoon cancelling its build
     */
    private void processCancelBridgeStep(Infantry infantry) {
        if (!(infantry instanceof ConvInfantry convInfantry)) {
            logger.warn("[BuildBridge] CANCEL_BRIDGE step ignored for {}: not a conventional infantry platoon",
                  infantry.getShortName());
            return;
        }
        if (!convInfantry.isBuildingBridge()) {
            logger.warn("[BuildBridge] CANCEL_BRIDGE step ignored for {}: not currently building a bridge",
                  convInfantry.getShortName());
            return;
        }
        Coords target = convInfantry.getBridgeTargetCoords();
        convInfantry.startBridgeDismantle();
        logger.info("[BuildBridge] {} cancels its bridge build at {}: dismantling will take {} turn(s)",
              convInfantry.getShortName(), target, convInfantry.getBridgeDismantleRequiredTurns());
        Report report = new Report(4281);
        report.subject = convInfantry.getId();
        report.addDesc(convInfantry);
        if (target != null) {
            report.add(target.getBoardNum());
        } else {
            report.add("?");
        }
        report.add(convInfantry.getBridgeDismantleRequiredTurns());
        addReport(report);
    }

    /**
     * Resumes building, from either a paused build or a dismantling: the platoon resumes from the structure still
     * standing (dismantling) or the held progress (paused). TO:AUE p.152.
     *
     * @param infantry the platoon resuming its build
     */
    private void processResumeBridgeStep(Infantry infantry) {
        if (!(infantry instanceof ConvInfantry convInfantry)) {
            logger.warn("[BuildBridge] RESUME_BRIDGE step ignored for {}: not a conventional infantry platoon",
                  infantry.getShortName());
            return;
        }
        if (!convInfantry.isDismantlingBridge() && !convInfantry.isBridgePaused()) {
            logger.warn("[BuildBridge] RESUME_BRIDGE step ignored for {}: not currently dismantling or paused",
                  convInfantry.getShortName());
            return;
        }
        Coords target = convInfantry.getBridgeTargetCoords();
        convInfantry.resumeBridgeBuild();
        logger.info("[BuildBridge] {} resumes its bridge build at {}: {} of {} turns built",
              convInfantry.getShortName(), target, convInfantry.getBridgeBuildTurns(),
              convInfantry.getBridgeBuildRequiredTurns());
        Report report = new Report(4284);
        report.subject = convInfantry.getId();
        report.addDesc(convInfantry);
        report.add(convInfantry.getBridgeBuildTurns());
        report.add(convInfantry.getBridgeBuildRequiredTurns());
        addReport(report);
    }

    /**
     * Pauses an active bridge build: the platoon holds its progress and is freed to act normally until it returns to
     * resume. TO:AUE p.152.
     *
     * @param infantry the platoon pausing its build
     */
    private void processPauseBridgeStep(Infantry infantry) {
        if (!(infantry instanceof ConvInfantry convInfantry)) {
            logger.warn("[BuildBridge] PAUSE_BRIDGE step ignored for {}: not a conventional infantry platoon",
                  infantry.getShortName());
            return;
        }
        if (!convInfantry.isBuildingBridge()) {
            logger.warn("[BuildBridge] PAUSE_BRIDGE step ignored for {}: not currently building a bridge",
                  convInfantry.getShortName());
            return;
        }
        Coords target = convInfantry.getBridgeTargetCoords();
        convInfantry.pauseBridgeBuild();
        logger.info("[BuildBridge] {} pauses its bridge build at {}: {} of {} turns held",
              convInfantry.getShortName(), target, convInfantry.getBridgeBuildTurns(),
              convInfantry.getBridgeBuildRequiredTurns());
        Report report = new Report(4286);
        report.subject = convInfantry.getId();
        report.addDesc(convInfantry);
        report.add(convInfantry.getBridgeBuildTurns());
        report.add(convInfantry.getBridgeBuildRequiredTurns());
        addReport(report);
    }

    /**
     * Abandons any bridge work in progress: the partial structure is lost immediately and the spent budget is not
     * refunded. TO:AUE p.152.
     *
     * @param infantry the platoon abandoning its bridge
     */
    private void processAbandonBridgeStep(Infantry infantry) {
        if (!(infantry instanceof ConvInfantry convInfantry)) {
            logger.warn("[BuildBridge] ABANDON_BRIDGE step ignored for {}: not a conventional infantry platoon",
                  infantry.getShortName());
            return;
        }
        if (!convInfantry.hasBridgeInProgress()) {
            logger.warn("[BuildBridge] ABANDON_BRIDGE step ignored for {}: no bridge work in progress",
                  convInfantry.getShortName());
            return;
        }
        Coords target = convInfantry.getBridgeTargetCoords();
        convInfantry.abandonBridge();
        logger.info("[BuildBridge] {} abandons its bridge at {}; progress lost, points forfeit",
              convInfantry.getShortName(), target);
        Report report = new Report(4287);
        report.subject = convInfantry.getId();
        report.addDesc(convInfantry);
        if (target != null) {
            report.add(target.getBoardNum());
        } else {
            report.add("?");
        }
        addReport(report);
    }

    /**
     * Begins a unit's clearing of a rubble hex (a vehicle with a bulldozer/backhoe, or a backhoe Mek), TacOps. The
     * required number of turns is taken from the rubble's structure level (2/4/8/16, capped at 16) plus any backhoe
     * penalty. Re-declaring the same hex while already clearing it does not reset progress.
     *
     * @param entity   the clearing unit (a {@link RubbleClearer})
     * @param position the rubble hex being cleared (the unit's own hex)
     */
    private void beginRubbleClearing(Entity entity, Coords position) {
        if (!(entity instanceof RubbleClearer clearer)) {
            return;
        }
        // Defensive server-side checks mirror the MoveStep legality: a rubble-clearing tool (bulldozer, or backhoe
        // under the unofficial rule) and a rubble hex.
        if (!BulldozerRules.canClearRubble(entity, getGame())) {
            logger.debug("[Bulldozer] {}: clear rubble rejected - no working rubble-clearing equipment / rule off",
                  entity.getDisplayName());
            return;
        }
        Hex hex = getGame().getBoard(entity.getBoardId()).getHex(position);
        if ((hex == null) || (hex.terrainLevel(Terrains.RUBBLE) <= 0)) {
            logger.debug("[Bulldozer] {}: clear rubble rejected - hex at {} contains no rubble",
                  entity.getDisplayName(), position);
            return;
        }
        // Already clearing this same hex: leave the in-progress counter untouched so progress is not reset.
        if (clearer.isClearingRubble() && position.equals(clearer.getRubbleClearTarget())) {
            return;
        }
        int rubbleLevel = hex.terrainLevel(Terrains.RUBBLE);
        // The base time plus any backhoe penalty (a backhoe takes 4 turns longer, unofficial rule).
        int requiredTurns = BulldozerRules.totalClearingTurns(entity, hex, getGame());
        clearer.beginClearingRubble(position, requiredTurns);

        // Player-visible feedback that the work has started (the QA pain point): a movement-phase report and a toast,
        // naming the actual tool so a backhoe is not reported as a bulldozer.
        String toolName = BulldozerRules.clearingToolName(entity);
        Report report = new Report(5348);
        report.subject = entity.getId();
        report.addDesc(entity);
        report.add(toolName);
        report.add(requiredTurns);
        addReport(report);
        gameManager.sendToast(GameToastEvent.Level.INFO,
              Messages.getString("Bulldozer.beginClearToast", entity.getShortName(), requiredTurns), entity);
        logger.info("[Bulldozer] {} begins clearing rubble (level {}) at {}: {} turns required",
              entity.getShortName(), rubbleLevel, position, requiredTurns);
    }
}
