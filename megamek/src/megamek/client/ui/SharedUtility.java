/*
 * Copyright (c) 2008 - Ben Mazur (bmazur@sev.org).
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import megamek.client.Client;
import megamek.common.Hex;
import megamek.common.MPCalculationSetting;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EscapePods;
import megamek.common.game.Game;
import megamek.common.internationalization.I18n;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;
import megamek.common.weapons.TeleMissile;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;


public class SharedUtility {
    private final static MMLogger LOGGER = MMLogger.create(SharedUtility.class);

    public static String doPSRCheck(MovePath md) {
        return (String) doPSRCheck(md, true);
    }

    @SuppressWarnings(value = "unchecked")
    public static List<TargetRoll> getPSRList(MovePath md) {
        // certain types of entities, such as airborne aero units, do not require many
        // of the checks
        // carried out in the full PSR Check. So, we call a method that skips most of
        // those.
        if (md.getEntity().isAirborne() && md.getEntity().isAero()) {
            return (List<TargetRoll>) getAeroSpecificPSRList(md, false);
        } else {
            return (List<TargetRoll>) doPSRCheck(md, false);
        }
    }

    /**
     * Function that carries out PSR checks specific only to airborne aero units
     *
     * @param md           The path to check
     * @param stringResult Whether to return the report as a string
     *
     * @return Collection of PSRs that will be required for this activity
     */
    private static Object getAeroSpecificPSRList(MovePath md, boolean stringResult) {
        StringBuffer nagReport = new StringBuffer();
        List<TargetRoll> psrList = new ArrayList<>();

        final Entity entity = md.getEntity();
        final Game game = entity.getGame();
        // okay, proceed with movement calculations
        Coords curPos;
        int curFacing;

        PilotingRollData rollTarget;

        // Compile the move
        md.clipToPossible();

        EntityMovementType overallMoveType = md.getLastStepMovementType();

        // iterate through steps
        for (final ListIterator<MoveStep> i = md.getSteps(); i.hasNext(); ) {
            final MoveStep step = i.next();

            // stop for illegal movement
            if (step.getMovementType(md.isEndStep(step)) == EntityMovementType.MOVE_ILLEGAL) {
                break;
            }

            // check for more than one roll
            IAero a = (IAero) entity;
            rollTarget = a.checkRolls(step, overallMoveType);
            checkNag(rollTarget, nagReport, psrList);

            rollTarget = a.checkManeuver(step, overallMoveType);
            checkNag(rollTarget, nagReport, psrList);

            // set most step parameters

            // set last step parameters
            curPos = step.getPosition();
            curFacing = step.getFacing();

            // check for vertical takeoff
            if (step.getType() == MoveStepType.VERTICAL_TAKE_OFF) {
                rollTarget = ((IAero) entity).checkVerticalTakeOff();
                checkNag(rollTarget, nagReport, psrList);
            }

            // check for landing
            if (step.getType() == MoveStepType.LAND) {
                rollTarget = ((IAero) entity).getLandingControlRoll(step.getVelocity(), curPos, curFacing, false);
                checkNag(rollTarget, nagReport, psrList);
            }

            if (step.getType() == MoveStepType.VERTICAL_LAND) {
                rollTarget = ((IAero) entity).getLandingControlRoll(step.getVelocity(), curPos, curFacing, true);
                checkNag(rollTarget, nagReport, psrList);
            }

            // Check for Ejecting
            if (step.getType() == MoveStepType.EJECT
                  && (entity.isFighter())) {
                rollTarget = TWGameManager.getEjectModifiers(game, entity, 0, false);
                checkNag(rollTarget, nagReport, psrList);
            }
        }

        // check to see if thrust exceeded SI
        IAero a = (IAero) entity;
        int thrust = md.getMpUsed();
        rollTarget = a.checkThrustSITotal(thrust, overallMoveType);
        checkNag(rollTarget, nagReport, psrList);

        // Atmospheric checks
        if (!game.getBoard(md.getFinalBoardId()).isSpace() && !md.contains(MoveStepType.LAND)
              && !md.contains(MoveStepType.VERTICAL_LAND)) {
            // check to see if velocity is 2x thrust
            rollTarget = a.checkVelocityDouble(md.getFinalVelocity(),
                  overallMoveType);
            checkNag(rollTarget, nagReport, psrList);

            // check to see if descended more than two hexes
            rollTarget = a.checkDown(md.getFinalNDown(), overallMoveType);
            checkNag(rollTarget, nagReport, psrList);

            // stalling out
            rollTarget = a.checkStall(md);
            checkNag(rollTarget, nagReport, psrList);

            // check for hovering
            rollTarget = a.checkHover(md);
            checkNag(rollTarget, nagReport, psrList);
        }

        if (stringResult) {
            return nagReport.toString();
        }
        return psrList;
    }

    /**
     * Checks to see if piloting skill rolls are needed for the currently selected movement. This code is basically a
     * simplified version of Server.processMovement(), except that it just reads information (no writing). Note that
     * MovePath.clipToPossible() is called though, which changes the md object.
     */
    private static Object doPSRCheck(MovePath md, boolean stringResult) {

        StringBuffer nagReport = new StringBuffer();
        List<TargetRoll> psrList = new ArrayList<>();

        final Entity entity = md.getEntity();
        final Game game = entity.getGame();
        // okay, proceed with movement calculations
        Coords lastPos = entity.getPosition();
        Coords curPos = entity.getPosition();
        int curBoardId = entity.getBoardId();
        int lastElevation = entity.getElevation();
        int curElevation;
        int curFacing = entity.getFacing();
        int distance;
        EntityMovementType moveType;
        EntityMovementType overallMoveType;
        boolean firstStep;
        int prevFacing = curFacing;
        Hex prevHex = game.getBoard(curBoardId).getHex(curPos);
        final boolean isInfantry = (entity instanceof Infantry);

        PilotingRollData rollTarget;

        // Compile the move
        md.clipToPossible();

        overallMoveType = md.getLastStepMovementType();

        // iterate through steps
        firstStep = true;
        /* Bug 754610: Revert fix for bug 702735. */
        MoveStep prevStep = null;
        for (final ListIterator<MoveStep> i = md.getSteps(); i.hasNext(); ) {
            final MoveStep step = i.next();
            boolean isPavementStep = step.isPavementStep();

            // stop for illegal movement
            if (step.getMovementType(md.isEndStep(step)) == EntityMovementType.MOVE_ILLEGAL) {
                break;
            }

            if (entity.isAirborne() && entity.isAero()) {
                // check for more than one roll
                IAero a = (IAero) entity;
                rollTarget = a.checkRolls(step, overallMoveType);
                checkNag(rollTarget, nagReport, psrList);

                rollTarget = a.checkManeuver(step, overallMoveType);
                checkNag(rollTarget, nagReport, psrList);
            }

            // check piloting skill for getting up
            rollTarget = entity.checkGetUp(step, overallMoveType);
            checkNag(rollTarget, nagReport, psrList);

            // set most step parameters
            moveType = step.getMovementType(md.isEndStep(step));
            distance = step.getDistance();

            // set last step parameters
            curPos = step.getPosition();
            curFacing = step.getFacing();
            curElevation = step.getElevation();
            curBoardId = step.getBoardId();
            Board board = game.getBoard(curBoardId);

            final Hex curHex = board.getHex(curPos);

            // check for vertical takeoff
            if ((step.getType() == MoveStepType.VERTICAL_TAKE_OFF)
                  && entity.isAero()) {
                rollTarget = ((IAero) entity).checkVerticalTakeOff();
                checkNag(rollTarget, nagReport, psrList);
            }

            // check for landing
            if ((step.getType() == MoveStepType.LAND)
                  && entity.isAero()) {
                rollTarget = ((IAero) entity).getLandingControlRoll(step.getVelocity(), curPos, curFacing, false);
                checkNag(rollTarget, nagReport, psrList);
            }
            if ((step.getType() == MoveStepType.VERTICAL_LAND)
                  && entity.isAero()) {
                rollTarget = ((IAero) entity).getLandingControlRoll(step.getVelocity(), curPos, curFacing, true);
                checkNag(rollTarget, nagReport, psrList);
            }

            // check for leap
            if (!lastPos.equals(curPos) && (moveType != EntityMovementType.MOVE_JUMP) && (entity instanceof Mek)
                  && !entity.isAirborne() && (step.getClearance() <= 0) // Don't check airborne LAMs
                  && game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEAPING)) {
                int leapDistance = (lastElevation + board.getHex(lastPos).getLevel())
                      - (curElevation + curHex.getLevel());
                if (leapDistance > 2) {
                    rollTarget = entity.getBasePilotingRoll(moveType);
                    entity.addPilotingModifierForTerrain(rollTarget, curPos, step.getBoardId());
                    rollTarget.append(
                          new PilotingRollData(
                                entity.getId(),
                                2 * leapDistance,
                                Messages.getString("TacOps.leaping.leg_damage")
                          )
                    );
                    SharedUtility.checkNag(rollTarget, nagReport, psrList);
                    rollTarget = entity.getBasePilotingRoll(moveType);
                    entity.addPilotingModifierForTerrain(rollTarget, curPos, step.getBoardId());
                    rollTarget.append(
                          new PilotingRollData(
                                entity.getId(),
                                leapDistance,
                                Messages.getString("TacOps.leaping.fall_damage"))
                    );
                    SharedUtility.checkNag(rollTarget, nagReport, psrList);
                }
            }

            // Check for skid.
            rollTarget = entity.checkSkid(moveType, prevHex, overallMoveType,
                  prevStep, step, prevFacing, curFacing, lastPos, curPos,
                  isInfantry, distance - 1);
            checkNag(rollTarget, nagReport, psrList);

            // check if we've moved into rubble
            boolean isLastStep = md.getLastStep().equals(step);
            rollTarget = entity.checkRubbleMove(step, overallMoveType, curHex,
                  lastPos, curPos, isLastStep, isPavementStep);
            checkNag(rollTarget, nagReport, psrList);

            // check if we are moving recklessly
            rollTarget = entity.checkRecklessMove(step, overallMoveType,
                  curHex, lastPos, curPos, prevHex);
            checkNag(rollTarget, nagReport, psrList);

            // check for crossing ice
            if (curHex.containsTerrain(Terrains.ICE)
                  && curHex.containsTerrain(Terrains.WATER)
                  && !(curPos.equals(lastPos))
                  && (step.getElevation() == 0)
                  && (moveType != EntityMovementType.MOVE_JUMP)
                  && !(entity instanceof Infantry)
                  && !(isPavementStep && curHex
                  .containsTerrain(Terrains.BRIDGE))) {
                nagReport.append(Messages.getString("MovementDisplay.IceMoving"));
            }

            // check if we've moved into water
            rollTarget = entity.checkWaterMove(step, overallMoveType, curHex,
                  lastPos, curPos, isPavementStep);
            checkNag(rollTarget, nagReport, psrList);

            // check for non-heat tracking entering a fire
            boolean underwater = curHex.containsTerrain(Terrains.WATER)
                  && (curHex.depth() > 0)
                  && (step.getElevation() < curHex.getLevel());
            if (curHex.containsTerrain(Terrains.FIRE) && !underwater
                  && !entity.tracksHeat() && (step.getElevation() <= 1) && !entity.isAirborne()
                  && (moveType != EntityMovementType.MOVE_JUMP)
                  && !(curPos.equals(lastPos))) {
                nagReport.append(Messages.getString("MovementDisplay.FireMoving", 8));
            }

            // check for magma
            int level = curHex.terrainLevel(Terrains.MAGMA);
            boolean jumpedIntoMagma = (curPos.equals(lastPos)
                  && (curHex.terrainLevel(Terrains.MAGMA) == 2)
                  && (moveType == EntityMovementType.MOVE_JUMP));
            if ((level == 1) && (step.getElevation() == 0)
                  && (entity.getMovementMode() != EntityMovementMode.HOVER)
                  && (moveType != EntityMovementType.MOVE_JUMP)
                  && !(curPos.equals(lastPos))) {
                nagReport.append(Messages
                      .getString("MovementDisplay.MagmaCrustMoving"));
            } else if ((level == 2) && (step.getElevation() == 0)
                  && (moveType != EntityMovementType.MOVE_JUMP)
                  && (entity.getMovementMode() != EntityMovementMode.HOVER)
                  && (entity.getMovementMode() != EntityMovementMode.WIGE)
                  && !(curPos.equals(lastPos))) {
                nagReport.append(Messages
                      .getString("MovementDisplay.MagmaLiquidMoving"));
            }

            // Check for Hazardous Liquid
            if (curHex.containsTerrain(Terrains.HAZARDOUS_LIQUID) && (step.getElevation() <= 0)
                  && (moveType != EntityMovementType.MOVE_JUMP)
                  && (entity.getMovementMode() != EntityMovementMode.HOVER)
                  && (entity.getMovementMode() != EntityMovementMode.WIGE)
                  && !(curPos.equals(lastPos))) {
                nagReport.append(I18n
                      .getTextAt("megamek.client.messages", "MovementDisplay.HazardousLiquidMoving"));
            }

            // Check for Hazardous Liquid
            if (curHex.containsTerrain(Terrains.ULTRA_SUBLEVEL) && (step.getElevation() <= 0)
                  && (moveType != EntityMovementType.MOVE_JUMP)
                  && (entity.getMovementMode() != EntityMovementMode.HOVER)
                  && (entity.getMovementMode() != EntityMovementMode.WIGE)
                  && !(curPos.equals(lastPos))) {
                nagReport.append(I18n
                      .getTextAt("megamek.client.messages", "MovementDisplay.UltraSublevel"));
            }

            // check for sideslip
            if ((entity instanceof VTOL)
                  || (entity.getMovementMode() == EntityMovementMode.HOVER)
                  || (entity.getMovementMode() == EntityMovementMode.WIGE
                  && step.getClearance() > 0)) {
                rollTarget = entity.checkSideSlip(moveType, prevHex,
                      overallMoveType, prevStep, prevFacing, curFacing,
                      lastPos, curPos, distance, md.hasActiveMASC());
                checkNag(rollTarget, nagReport, psrList);
            }

            // check if we've moved into swamp; skip Liquid Magma bog-down check if not
            // jumping into the last hex
            if (level != 2 || jumpedIntoMagma) {
                rollTarget = entity.checkBogDown(step, overallMoveType, curHex,
                      lastPos, curPos, lastElevation, isPavementStep);
                checkNag(rollTarget, nagReport, psrList);
            }

            // Check if used more MPs than Mek/Vehicle would have w/o gravity
            if (!i.hasNext() && !firstStep) {
                if ((entity instanceof Mek) || (entity instanceof Tank)) {
                    if ((moveType == EntityMovementType.MOVE_WALK)
                          || (moveType == EntityMovementType.MOVE_VTOL_WALK)
                          || (moveType == EntityMovementType.MOVE_RUN)
                          || (moveType == EntityMovementType.MOVE_VTOL_RUN)
                          || (moveType == EntityMovementType.MOVE_SPRINT)
                          || (moveType == EntityMovementType.MOVE_VTOL_SPRINT)) {
                        int limit = entity.getRunningGravityLimit();
                        if (step.isOnlyPavementOrRoad() && entity.isEligibleForPavementOrRoadBonus()) {
                            limit++;
                        }
                        if (step.getMpUsed() > limit) {
                            rollTarget = entity.checkMovedTooFast(step, overallMoveType);
                            checkNag(rollTarget, nagReport, psrList);
                        }
                    } else if (moveType == EntityMovementType.MOVE_JUMP) {
                        int origWalkMP = entity.getWalkMP(MPCalculationSetting.NO_GRAVITY);
                        int gravWalkMP = entity.getWalkMP();
                        int availableJumpMP = step.isUsingMekJumpBooster() ? entity.getMechanicalJumpBoosterMP(
                              MPCalculationSetting.NO_GRAVITY) : entity.getJumpMP(MPCalculationSetting.NO_GRAVITY);
                        if (step.getMpUsed() > availableJumpMP) {
                            rollTarget = entity.checkMovedTooFast(step, overallMoveType);
                            checkNag(rollTarget, nagReport, psrList);
                        } else if ((game.getPlanetaryConditions().getGravity() > 1)
                              && ((origWalkMP - gravWalkMP) > 0)) {
                            rollTarget = entity.getBasePilotingRoll(md.getLastStepMovementType());
                            entity.addPilotingModifierForTerrain(rollTarget, step);
                            int gravMod = game.getPlanetaryConditions()
                                  .getGravityPilotPenalty();
                            if ((gravMod != 0) && !board.isSpace()) {
                                rollTarget.addModifier(gravMod, game
                                      .getPlanetaryConditions().getGravity()
                                      + "G gravity");
                            }
                            rollTarget.append(new PilotingRollData(entity
                                  .getId(), 0, "jumped in high gravity"));
                            SharedUtility.checkNag(rollTarget, nagReport,
                                  psrList);
                        }
                        if (step.getMpUsed() > entity.getSprintMP(MPCalculationSetting.NO_GRAVITY)) {
                            rollTarget = entity.checkMovedTooFast(step, overallMoveType);
                            checkNag(rollTarget, nagReport, psrList);
                        }
                    }
                }
            }

            // Sheer Cliffs, TO p.39
            // Roads over cliffs cancel the cliff effects for units that move on roads
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
                rollTarget = entity.getBasePilotingRoll(moveType);
                rollTarget.append(new PilotingRollData(entity.getId(), 0, "moving down a sheer cliff"));
                checkNag(rollTarget, nagReport, psrList);
            }

            // Meks moving down a cliff
            // QuadVees in vee mode ignore PSRs to avoid falls, IO p.133
            // ProtoMeks as Meks
            if (mekAffectedByCliff && !quadveeVehMode && isDownCliff && !isPavementStep) {
                rollTarget = entity.getBasePilotingRoll(moveType);
                rollTarget.append(new PilotingRollData(entity.getId(), -stepHeight - 1, "moving down a sheer cliff"));
                checkNag(rollTarget, nagReport, psrList);
            }

            // Meks moving up a cliff
            if (mekAffectedByCliff && !quadveeVehMode && isUpCliff && !isPavementStep) {
                rollTarget = entity.getBasePilotingRoll(moveType);
                rollTarget.append(new PilotingRollData(entity.getId(), stepHeight, "moving up a sheer cliff"));
                checkNag(rollTarget, nagReport, psrList);
            }

            // Handle non-protomek moving into a building.
            int buildingMove = entity.checkMovementInBuilding(step, prevStep, curPos, lastPos);
            if ((buildingMove > 1) && !(entity instanceof ProtoMek)) {
                // Get the building being entered.
                IBuilding bldg = null;
                String reason = "entering";
                if ((buildingMove & 2) == 2) {
                    bldg = board.getBuildingAt(curPos);
                }

                if (bldg != null) {
                    rollTarget = entity.rollMovementInBuilding(bldg, distance, reason, overallMoveType);
                    SharedUtility.checkNag(rollTarget, nagReport, psrList);
                }
            }

            if (step.getType() == MoveStepType.GO_PRONE) {
                rollTarget = entity.checkDislodgeSwarmers(step, overallMoveType);
                checkNag(rollTarget, nagReport, psrList);
            }

            Hex lastHex = board.getHex(lastPos);
            if (((step.getType() == MoveStepType.BACKWARDS)
                  || (step.getType() == MoveStepType.LATERAL_LEFT_BACKWARDS)
                  || (step.getType() == MoveStepType.LATERAL_RIGHT_BACKWARDS))
                  && !(md.isJumping() && md.contains(MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER))
                  && (lastHex.getLevel() + lastElevation != (curHex.getLevel() + step.getElevation()))
                  && !(entity instanceof VTOL)
                  && !(md.getFinalClimbMode()
                  && curHex.containsTerrain(Terrains.BRIDGE) && ((curHex
                  .terrainLevel(Terrains.BRIDGE_ELEV)
                  + curHex
                  .getLevel()) == (prevHex.getLevel()
                  + (prevHex
                  .containsTerrain(Terrains.BRIDGE)
                  ? prevHex
                  .terrainLevel(Terrains.BRIDGE_ELEV)
                  : 0))))) {
                nagReport.append(Messages.getString("MovementDisplay.BackWardsElevationChange"));
                SharedUtility.checkNag(entity.getBasePilotingRoll(overallMoveType), nagReport, psrList);
            }

            // Check for Ejecting
            if ((step.getType() == MoveStepType.EJECT) && (entity instanceof Mek)) {
                rollTarget = TWGameManager.getEjectModifiers(game, entity, 0, false);
                checkNag(rollTarget, nagReport, psrList);
            }

            if (step.getType() == MoveStepType.UNLOAD) {
                Targetable targ = step.getTarget(game);
                if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_ZIPLINES)
                      && (entity instanceof VTOL)
                      && (md.getFinalElevation() > 0)
                      && (targ instanceof Infantry)
                      && (((Entity) targ).getJumpMP() < 1)
                      && !((Infantry) targ).isMechanized()) {
                    rollTarget = TWGameManager.getEjectModifiers(game, (Entity) targ, 0,
                          false, md.getFinalCoords(), "zip lining");
                    // Factor in Elevation
                    if (md.getFinalElevation() > 0) {
                        rollTarget.addModifier(md.getFinalElevation(), "elevation");
                    }
                    checkNag(rollTarget, nagReport, psrList);
                }
            }

            if (step.isTurning()) {
                rollTarget = entity.checkTurnModeFailure(overallMoveType,
                      prevStep == null ? 0 : prevStep.getNStraight(), md.getMpUsed(), curPos);
                checkNag(rollTarget, nagReport, psrList);
            }

            if (step.getType() == MoveStepType.BOOTLEGGER) {
                rollTarget = entity.getBasePilotingRoll(overallMoveType);
                entity.addPilotingModifierForTerrain(rollTarget);
                rollTarget.addModifier(0, "bootlegger maneuver");
                checkNag(rollTarget, nagReport, psrList);
            }

            // update lastPos, prevStep, prevFacing & prevHex
            if (!curPos.equals(lastPos)) {
                prevFacing = curFacing;
            }
            lastPos = curPos;
            prevStep = step;
            prevHex = curHex;
            lastElevation = step.getElevation();

            firstStep = false;
        }

        // running with destroyed hip or gyro needs a check
        rollTarget = entity.checkRunningWithDamage(overallMoveType);
        checkNag(rollTarget, nagReport, psrList);

        // if we sprinted with MASC or a supercharger, then we need a PSR
        rollTarget = entity.checkSprintingWithMASCXorSupercharger(overallMoveType, md.getMpUsed());
        checkNag(rollTarget, nagReport, psrList);

        rollTarget = entity.checkSprintingWithMASCAndSupercharger(overallMoveType, md.getMpUsed());
        checkNag(rollTarget, nagReport, psrList);

        rollTarget = entity.checkUsingOverdrive(overallMoveType);
        checkNag(rollTarget, nagReport, psrList);

        rollTarget = entity.checkGunningIt(overallMoveType);
        checkNag(rollTarget, nagReport, psrList);

        // but the danger isn't over yet! landing from a jump can be risky!
        if ((overallMoveType == EntityMovementType.MOVE_JUMP) && !entity.isMakingDfa()) {
            // check for damaged critical slots
            rollTarget = entity.checkLandingWithDamage(overallMoveType);
            checkNag(rollTarget, nagReport, psrList);
            // check for landing with prototype JJs
            rollTarget = entity.checkLandingWithPrototypeJJ(overallMoveType);
            checkNag(rollTarget, nagReport, psrList);
            // jumped into water?
            Hex hex = game.getBoard(curBoardId).getHex(curPos);
            // check for jumping into heavy woods
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_PSR_JUMP_HEAVY_WOODS)) {
                rollTarget = entity.checkLandingInHeavyWoods(overallMoveType,
                      hex);
                checkNag(rollTarget, nagReport, psrList);
            }
            int waterLevel = hex.terrainLevel(Terrains.WATER);
            if (hex.containsTerrain(Terrains.ICE) && (waterLevel > 0)) {
                if (!(entity instanceof Infantry)) {
                    nagReport.append(Messages.getString("MovementDisplay.IceLanding"));
                }
            } else if (prevStep != null && !(prevStep.climbMode() && hex.containsTerrain(Terrains.BRIDGE))) {
                if (!entity.getMovementMode().isHoverOrWiGE()) {
                    rollTarget = entity.checkWaterMove(waterLevel, overallMoveType);
                    checkNag(rollTarget, nagReport, psrList);
                }

            }

            // check for magma
            int level = hex.terrainLevel(Terrains.MAGMA);
            if ((level == 1) && (lastElevation == 0)) {
                nagReport.append(Messages.getString("MovementDisplay.MagmaCrustJumpLanding"));
            } else if ((level == 2) && (lastElevation == 0)) {
                nagReport.append(Messages.getString("MovementDisplay.MagmaLiquidMoving"));
            }

            if ((hex.containsTerrain(Terrains.HAZARDOUS_LIQUID)) && (lastElevation == 0)) {
                nagReport.append(I18n
                      .getTextAt("megamek.client.messages", "MovementDisplay.HazardousLiquidMoving"));
            }

            if ((hex.containsTerrain(Terrains.ULTRA_SUBLEVEL) && lastElevation == 0)) {
                nagReport.append(I18n
                      .getTextAt("megamek.client.messages", "MovementDisplay.UltraSublevel"));
            }
        }

        if (entity.isAirborne() && entity.isAero()) {
            // check to see if thrust exceeded SI
            IAero a = (IAero) entity;
            int thrust = md.getMpUsed();
            rollTarget = a.checkThrustSITotal(thrust, overallMoveType);
            checkNag(rollTarget, nagReport, psrList);

            // Atmospheric checks
            if (!game.getBoard(curBoardId).isSpace() && !md.contains(MoveStepType.LAND)
                  && !md.contains(MoveStepType.VERTICAL_LAND)) {
                // check to see if velocity is 2x thrust
                rollTarget = a.checkVelocityDouble(md.getFinalVelocity(), overallMoveType);
                checkNag(rollTarget, nagReport, psrList);

                // check to see if descended more than two hexes
                rollTarget = a.checkDown(md.getFinalNDown(), overallMoveType);
                checkNag(rollTarget, nagReport, psrList);

                // stalling out
                rollTarget = a.checkStall(md);
                checkNag(rollTarget, nagReport, psrList);

                // check for hovering
                rollTarget = a.checkHover(md);
                checkNag(rollTarget, nagReport, psrList);
            }
        }

        return stringResult ? nagReport.toString() : psrList;
    }

    /**
     *
     */
    private static void checkNag(PilotingRollData rollTarget, StringBuffer nagReport,
          List<TargetRoll> psrList) {
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
            psrList.add(rollTarget);
            nagReport.append(Messages.getString("MovementDisplay.addNag",
                  rollTarget.getValueAsString(), rollTarget.getDesc()));
        }
    }

    /**
     * Checks to see if piloting skill rolls are needed for excessive use of thrust.
     */
    public static String doThrustCheck(MovePath md, Client client) {
        StringBuffer nagReport = new StringBuffer();
        List<TargetRoll> psrList = new ArrayList<>();

        if (client.getGame().useVectorMove()) {
            return nagReport.toString();
        }

        final Entity entity = md.getEntity();
        if (!entity.isAero()) {
            return nagReport.toString();
        }

        IAero a = (IAero) entity;
        PilotingRollData rollTarget;
        EntityMovementType overallMoveType = md.getLastStepMovementType();

        // cycle through movement. Collect thrust used until position changes.
        int thrustUsed = 0;
        int j = 0;
        for (final ListIterator<MoveStep> i = md.getSteps(); i.hasNext(); ) {
            final MoveStep step = i.next();
            j++;
            // how do I figure out last step?
            if ((step.getDistance() == 0) && (md.length() != j)) {
                thrustUsed += step.getMp();
            } else {
                // if this was the last move and distance was zero, then add thrust
                if ((step.getDistance() == 0) && (md.length() == j)) {
                    thrustUsed += step.getMp();
                }
                // then we moved to a new hex or the last step so check conditions
                // structural damage
                rollTarget = a.checkThrustSI(thrustUsed, overallMoveType);
                checkNag(rollTarget, nagReport, psrList);

                // check for pilot damage
                int hits = entity.getCrew().getHits();
                int health = 6 - hits;

                if (thrustUsed > (2 * health)) {
                    int targetRoll = 2 + (thrustUsed - (2 * health)) + (2 * hits);
                    nagReport.append(Messages.getString("MovementDisplay.addNag",
                          Integer.toString(targetRoll),
                          "Thrust exceeded twice pilot's health in single hex"));
                }

                thrustUsed = 0;
            }
        }

        return nagReport.toString();

    }

    public static MovePath moveAero(MovePath movePath, Client client) {
        final Entity entity = movePath.getEntity();
        final Game game = entity.getGame();
        // Don't process further unless the entity belongs in space
        if (!entity.isAero() && !(entity instanceof EjectedCrew)) {
            return movePath;
        }
        // Ejected crew/pilots and lifeboats can't move, so just add the inherited move
        // steps and be done with it
        if ((entity instanceof EjectedCrew)
              || ((entity instanceof EscapePods) && (entity.getOriginalWalkMP() <= 0))) {
            return addSteps(movePath, client);
        }
        IAero aero = (IAero) entity;

        // need to check and see if the units current velocity is zero
        boolean isRamming = (movePath.getLastStep() != null) && (movePath.getLastStep().getType() == MoveStepType.RAM);

        // if using advanced movement then I need to add on movement
        // steps to get the vessel from point aero to point b
        if (game.useVectorMove()) {
            // if the unit is ramming then this is already done
            if (!isRamming) {
                addSteps(movePath, client);
            }
        } else if (aero.isOutControlTotal()) {
            // OOC units need aero new movement path
            MovePath oldMovePath = movePath;
            movePath = new MovePath(game, entity);
            int vel = aero.getCurrentVelocity();

            while (vel > 0) {
                int steps = 1;
                // if moving on the ground map, then 16 hexes forward
                if (game.getBoard().isGround()) {
                    steps = 16;
                }
                while (steps > 0 &&
                      game.getBoard().contains(movePath.getFinalCoords())) {
                    movePath.addStep(MoveStepType.FORWARDS);
                    steps--;
                }
                if (!game.getBoard().contains(movePath.getFinalCoords())) {
                    movePath.removeLastStep();
                    if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_RETURN_FLYOVER)) {
                        // Telemissiles shouldn't get aero return option
                        if (entity instanceof TeleMissile) {
                            movePath.addStep(MoveStepType.OFF);
                        } else {
                            movePath.addStep(MoveStepType.RETURN);
                        }
                    } else {
                        movePath.addStep(MoveStepType.OFF);
                    }
                    break;
                }
                if (aero.isRandomMove()) {
                    int roll = Compute.d6(1);
                    switch (roll) {
                        case 1:
                            movePath.addStep(MoveStepType.TURN_LEFT);
                            movePath.addStep(MoveStepType.TURN_LEFT);
                            break;
                        case 2:
                            movePath.addStep(MoveStepType.TURN_LEFT);
                            break;
                        case 5:
                            movePath.addStep(MoveStepType.TURN_RIGHT);
                            break;
                        case 6:
                            movePath.addStep(MoveStepType.TURN_RIGHT);
                            movePath.addStep(MoveStepType.TURN_RIGHT);
                            break;
                    }
                }
                vel--;
            }
            // check to see if old movement path contained aero launch
            if (oldMovePath.contains(MoveStepType.LAUNCH)) {
                // since launches have to be the last step
                MoveStep lastStep = oldMovePath.getLastStep();
                if (lastStep.getType() == MoveStepType.LAUNCH) {
                    movePath.addStep(lastStep.getType(), lastStep.getLaunched());
                }
            }
            // check to see if old movement path contained an undocking
            if (oldMovePath.contains(MoveStepType.UNDOCK)) {
                // since launches have to be the last step
                MoveStep lastStep = oldMovePath.getLastStep();
                if (lastStep.getType() == MoveStepType.UNDOCK) {
                    movePath.addStep(lastStep.getType(), lastStep.getLaunched());
                }
            }
        }
        return movePath;
    }

    /**
     * Add steps for advanced vector movement based on the given vectors when splitting hexes, choose the hex with less
     * tonnage in case OOC
     */
    private static MovePath addSteps(MovePath md, Client client) {
        Entity en = md.getEntity();
        Game game = en.getGame();

        // if the last step is a launch or recovery, then I want to keep that at the end
        MoveStep lastStep = md.getLastStep();
        if ((lastStep != null)
              && ((lastStep.getType() == MoveStepType.LAUNCH) || (lastStep
              .getType() == MoveStepType.RECOVER)
              || (lastStep
              .getType() == MoveStepType.UNDOCK))) {
            md.removeLastStep();
        }

        // get the start and end
        Coords start = en.getPosition();
        Coords end = Compute.getFinalPosition(start, md.getFinalVectors());

        boolean leftMap = false;

        // (see LosEffects.java)
        ArrayList<Coords> in = Coords.intervening(start, end);
        // first check whether we are splitting hexes
        boolean split = false;
        double degree = start.degree(end);
        if ((degree % 60) == 30) {
            split = true;
            in = Coords.intervening(start, end, true);
        }

        Coords current = start;
        int facing = md.getFinalFacing();
        for (int i = 1; i < in.size(); i++) {

            Coords c = in.get(i);
            // check for split hexes
            // check for some number after a multiple of 3 (1, 4, 7, etc.)
            if (((i % 3) == 1) && split) {
                Coords left = in.get(i);
                Coords right = in.get(i + 1);

                // get the total tonnage in each hex
                double leftTonnage = 0;
                for (Entity ent : game.getEntitiesVector(left)) {
                    leftTonnage += ent.getWeight();
                }

                double rightTonnage = 0;
                for (Entity ent : game.getEntitiesVector(right)) {
                    rightTonnage += ent.getWeight();
                }

                // TODO : I will need to update this to account for asteroids

                // I need to consider both of these passed through for purposes of bombing
                en.addPassedThrough(right);
                en.addPassedThrough(left);
                if (client != null) {
                    client.sendUpdateEntity(en);
                }

                // if the left is preferred, increment i so next one is skipped
                if ((leftTonnage < rightTonnage) || !game.getBoard().contains(right)) {
                    i++;
                } else {
                    continue;
                }
            }

            if (!game.getBoard().contains(c)) {
                if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_RETURN_FLYOVER)) {
                    // Telemissiles shouldn't get a return option
                    if (en instanceof TeleMissile) {
                        md.addStep(MoveStepType.OFF);
                    } else {
                        md.addStep(MoveStepType.RETURN);
                    }
                } else {
                    md.addStep(MoveStepType.OFF);
                }
                leftMap = true;
                break;
            }

            // which direction is this from the current hex?
            int dir = current.direction(c);
            // what kind of step do I need to get there?
            int diff = dir - facing;
            if (diff == 0) {
                md.addStep(MoveStepType.FORWARDS);
            } else if ((diff == 1) || (diff == -5)) {
                md.addStep(MoveStepType.LATERAL_RIGHT);
            } else if ((diff == -2) || (diff == 4)) {
                md.addStep(MoveStepType.LATERAL_RIGHT_BACKWARDS);
            } else if ((diff == -1) || (diff == 5)) {
                md.addStep(MoveStepType.LATERAL_LEFT);
            } else if ((diff == 2) || (diff == -4)) {
                md.addStep(MoveStepType.LATERAL_LEFT_BACKWARDS);
            } else if ((diff == 3) || (diff == -3)) {
                md.addStep(MoveStepType.BACKWARDS);
            }
            current = c;
        }

        // do I now need to add on the last step again?
        if (!leftMap && (lastStep != null) && (lastStep.getType() == MoveStepType.LAUNCH)) {
            md.addStep(MoveStepType.LAUNCH, lastStep.getLaunched());
        }

        if (!leftMap && (lastStep != null) && (lastStep.getType() == MoveStepType.UNDOCK)) {
            md.addStep(MoveStepType.UNDOCK, lastStep.getLaunched());
        }

        if (!leftMap && (lastStep != null) && (lastStep.getType() == MoveStepType.RECOVER)) {
            md.addStep(MoveStepType.RECOVER, lastStep.getRecoveryUnit(), -1);
        }

        return md;
    }

    public static String[] getDisplayArray(List<? extends Targetable> entities) {
        String[] retVal = new String[entities.size()];
        int i = 0;
        for (Targetable ent : entities) {
            retVal[i++] = ent.getDisplayName();
        }
        return retVal;
    }

    public static @Nullable Targetable getTargetPicked(@Nullable List<? extends Targetable> targets,
          @Nullable String chosenDisplayName) {
        if ((chosenDisplayName == null) || (targets == null)) {
            return null;
        } else {
            return targets.stream().filter(t -> chosenDisplayName.equals(t.getDisplayName())).findAny().orElse(null);
        }
    }

    public static double predictLeapFallDamage(Entity movingEntity, TargetRoll data) {
        // Rough guess based on normal pilots
        double odds = Compute.oddsAbove(data.getValue(), false) / 100d;
        int fallHeight = data.getModifiers().get(data.getModifiers().size() - 1).value();
        double fallDamage = Math.round(movingEntity.getWeight() / 10.0)
              * (fallHeight + 1);
        LOGGER.trace("Predicting Leap fall damage for {} at {}% odds, {} fall height",
              movingEntity.getDisplayName(),
              odds,
              fallHeight);
        return fallDamage * (1 - odds);
    }

    /**
     * Per TacOps p 20, a leap carries the following risks: 1. risk of damaging each leg by distance leaped (3 or more
     * per leg); mod is 2 x distance leaped. 1.a 1 critical roll _per leg_. 1.b 1 _additional_ critical per leg that
     * takes internal structure damage due to leaping damage. 2. risk of falling; mod is distance leaped.
     */
    public static double predictLeapDamage(Entity movingEntity, TargetRoll data) {
        int legMultiplier = (movingEntity.isQuadMek()) ? 4 : 2;
        double odds = Compute.oddsAbove(data.getValue(), false) / 100d;
        int fallHeight = data.getModifiers().get(data.getModifiers().size() - 1).value() / 2;
        double legDamage = fallHeight * (legMultiplier);
        LOGGER.trace("Predicting Leap damage for {} at {}% odds, {} fall height",
              movingEntity.getDisplayName(),
              odds,
              fallHeight);
        int[] legLocations = { BipedMek.LOC_LEFT_LEG, BipedMek.LOC_RIGHT_LEG, QuadMek.LOC_LEFT_ARM,
                               QuadMek.LOC_RIGHT_ARM };

        // Add required crits; say the effective leg "damage" from a crit is 20 for now.
        int CRIT_VALUE = 100;
        legDamage += legMultiplier * CRIT_VALUE;
        LOGGER.trace("Adding {} leg critical chances as {} additional damage",
              legMultiplier,
              legMultiplier * CRIT_VALUE);

        // Add additional crits for each leg that would take internal damage
        for (int i = 0; i < legMultiplier; i++) {
            if (movingEntity.getArmor(legLocations[i]) < fallHeight) {
                LOGGER.trace("Adding additional critical for leg {} due to internal structure damage", i);
                legDamage += CRIT_VALUE;
            }
        }

        // Calculate odds of receiving this damage and return
        return legDamage * (1 - odds);
    }

}
