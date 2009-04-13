/*
 * MegaMek -
 * Copyright (C) 2008 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.ui;

import java.util.Enumeration;

import megamek.client.Client;
import megamek.common.Aero;
import megamek.common.Building;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IEntityMovementMode;
import megamek.common.IEntityMovementType;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.PilotingRollData;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Terrains;
import megamek.common.VTOL;

public class SharedUtility {
    /**
     * Checks to see if piloting skill rolls are needed for the currently
     * selected movement. This code is basically a simplified version of
     * Server.processMovement(), except that it just reads information (no
     * writing). Note that MovePath.clipToPossible() is called though, which
     * changes the md object.
     */
    public static String doPSRCheck(MovePath md, Client client) {

        StringBuffer nagReport = new StringBuffer();

        final Entity entity = md.getEntity();

        // okay, proceed with movement calculations
        Coords lastPos = entity.getPosition();
        Coords curPos = entity.getPosition();
        int lastElevation = entity.getElevation();
        int curFacing = entity.getFacing();
        int distance = 0;
        int moveType = IEntityMovementType.MOVE_NONE;
        int overallMoveType = IEntityMovementType.MOVE_NONE;
        boolean firstStep;
        int prevFacing = curFacing;
        IHex prevHex = null;
        final boolean isInfantry = (entity instanceof Infantry);

        PilotingRollData rollTarget;

        // Compile the move
        md.clipToPossible();

        overallMoveType = md.getLastStepMovementType();

        // iterate through steps
        firstStep = true;
        /* Bug 754610: Revert fix for bug 702735. */
        MoveStep prevStep = null;
        for (final Enumeration<MoveStep> i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = i.nextElement();
            boolean isPavementStep = step.isPavementStep();

            // stop for illegal movement
            if (step.getMovementType() == IEntityMovementType.MOVE_ILLEGAL) {
                break;
            }

            if (entity instanceof Aero) {
                // check for more than one roll
                Aero a = (Aero) entity;
                rollTarget = a.checkRolls(step, overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(SharedUtility.addNag(rollTarget));
                }

                rollTarget = a.checkManeuver(step, overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(SharedUtility.addNag(rollTarget));
                }
            }

            // check piloting skill for getting up
            rollTarget = entity.checkGetUp(step);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(SharedUtility.addNag(rollTarget));
            }

            // set most step parameters
            moveType = step.getMovementType();
            distance = step.getDistance();

            // set last step parameters
            curPos = step.getPosition();
            curFacing = step.getFacing();

            final IHex curHex = client.game.getBoard().getHex(curPos);

            // Check for skid.
            rollTarget = entity.checkSkid(moveType, prevHex, overallMoveType, prevStep, prevFacing, curFacing, lastPos, curPos, isInfantry, distance-1);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                // Have an entity-meaningful PSR message.
                nagReport.append(SharedUtility.addNag(rollTarget));
            }

            // check if we've moved into rubble
            rollTarget = entity.checkRubbleMove(step, curHex, lastPos, curPos);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(SharedUtility.addNag(rollTarget));
            }

            int lightPenalty = entity.getGame().getPlanetaryConditions().getLightPilotPenalty();
            if(lightPenalty > 0) {
                rollTarget.addModifier(lightPenalty, entity.getGame().getPlanetaryConditions().getLightCurrentName());
            }

            //check if we are moving recklessly
            rollTarget = entity.checkRecklessMove(step, curHex, lastPos, curPos, lastElevation);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(SharedUtility.addNag(rollTarget));
            }

            // check for crossing ice
            if (curHex.containsTerrain(Terrains.ICE) && curHex.containsTerrain(Terrains.WATER) && !(curPos.equals(lastPos)) && (step.getElevation() == 0) && (moveType != IEntityMovementType.MOVE_JUMP) && !(entity instanceof Infantry) && !(step.isPavementStep() && curHex.containsTerrain(Terrains.BRIDGE))) {
                nagReport.append(Messages.getString("MovementDisplay.IceMoving"));
            }

            // check if we've moved into water
            rollTarget = entity.checkWaterMove(step, curHex, lastPos, curPos, isPavementStep);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(SharedUtility.addNag(rollTarget));
            }

            // check for non-mech entering a fire
            if (curHex.containsTerrain(Terrains.FIRE) && !(entity instanceof Mech) && (step.getElevation() <= 1) && (moveType != IEntityMovementType.MOVE_JUMP) && !(curPos.equals(lastPos))) {
                nagReport.append(Messages.getString("MovementDisplay.FireMoving", new Object[] { new Integer(8) }));
            }

            // check for magma
            int level = curHex.terrainLevel(Terrains.MAGMA);
            if ((level == 1) && (step.getElevation() == 0) && (moveType != IEntityMovementType.MOVE_JUMP) && !(curPos.equals(lastPos))) {
                nagReport.append(Messages.getString("MovementDisplay.MagmaCrustMoving"));
            } else if ((level == 2) && (entity.getElevation() == 0) && (moveType != IEntityMovementType.MOVE_JUMP) && (entity.getMovementMode() != IEntityMovementMode.HOVER) && (entity.getMovementMode() != IEntityMovementMode.WIGE) && !(curPos.equals(lastPos))) {
                nagReport.append(Messages.getString("MovementDisplay.MagmaLiquidMoving"));
            }

            if ((entity instanceof VTOL) || (entity.getMovementMode() == IEntityMovementMode.HOVER) || (entity.getMovementMode() == IEntityMovementMode.WIGE)) {
                rollTarget = entity.checkSideSlip(moveType, prevHex, overallMoveType, prevStep, prevFacing, curFacing, lastPos, curPos, distance);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(SharedUtility.addNag(rollTarget));
                }
            }

            // check if we've moved into swamp
            rollTarget = entity.checkBogDown(step, curHex, lastPos, curPos, lastElevation, isPavementStep);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(SharedUtility.addNag(rollTarget));
            }

            // check if we used more MPs than the Mech/Vehicle would have in
            // normal gravity
            if (!i.hasMoreElements() && !firstStep) {
                if ((entity instanceof Mech) || (entity instanceof VTOL)) {
                    if ((step.getMovementType() == IEntityMovementType.MOVE_WALK) || (step.getMovementType() == IEntityMovementType.MOVE_VTOL_WALK) || (step.getMovementType() == IEntityMovementType.MOVE_RUN) || (step.getMovementType() == IEntityMovementType.MOVE_VTOL_RUN)) {
                        if (step.getMpUsed() > entity.getRunMP(false, false)) {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(SharedUtility.addNag(rollTarget));
                            }
                        }
                    } else if (step.getMovementType() == IEntityMovementType.MOVE_JUMP) {
                        if (step.getMpUsed() > entity.getJumpMP(false)) {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(SharedUtility.addNag(rollTarget));
                            }
                        }
                    }
                } else if (entity instanceof Tank) {
                    if ((step.getMovementType() == IEntityMovementType.MOVE_WALK) || (step.getMovementType() == IEntityMovementType.MOVE_VTOL_WALK) || (step.getMovementType() == IEntityMovementType.MOVE_RUN) || (step.getMovementType() == IEntityMovementType.MOVE_VTOL_RUN)) {

                        // For Tanks, we need to check if the tank had more MPs
                        // because it was moving along a road
                        if ((step.getMpUsed() > entity.getRunMP(false, false)) && !step.isOnlyPavement()) {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(SharedUtility.addNag(rollTarget));
                            }
                        }
                        // If the tank was moving on a road, he got a +1 bonus.
                        // N.B. The Ask Precentor Martial forum said that a 4/6
                        // tank on a road can move 5/7, **not** 5/8.
                        else if (step.getMpUsed() > entity.getRunMP(false, false) + 1) {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(SharedUtility.addNag(rollTarget));
                            }
                        }
                    }
                }
            }

            // Handle non-infantry moving into a building.
            int buildingMove = entity.checkMovementInBuilding(step, prevStep, curPos, lastPos);
            if ((buildingMove > 0) && !(entity instanceof Protomech)) {

                // Get the building being entered.
                Building bldgEntered = null;
                if ((buildingMove & 2) == 2) {
                    bldgEntered = client.game.getBoard().getBuildingAt(curPos);
                }

                if (bldgEntered != null) {
                    rollTarget = entity.rollMovementInBuilding(bldgEntered, distance, "entering", overallMoveType);
                    nagReport.append(SharedUtility.addNag(rollTarget));
                }
            }

            if (step.getType() == MovePath.STEP_GO_PRONE) {
                rollTarget = entity.checkDislodgeSwarmers(step);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(SharedUtility.addNag(rollTarget));
                }
            }

            if (((step.getType() == MovePath.STEP_BACKWARDS) || (step.getType() == MovePath.STEP_LATERAL_LEFT_BACKWARDS) || (step.getType() == MovePath.STEP_LATERAL_RIGHT_BACKWARDS)) && (client.game.getBoard().getHex(lastPos).getElevation() != curHex.getElevation()) && !(entity instanceof VTOL)) {
                nagReport.append(Messages.getString("MovementDisplay.BackWardsElevationChange"));
                nagReport.append(SharedUtility.addNag(entity.getBasePilotingRoll(overallMoveType)));
            }

            // update lastPos, prevStep, prevFacing & prevHex
            lastPos = new Coords(curPos);
            prevStep = step;
            /*
             * Bug 754610: Revert fix for bug 702735. if (prevHex != null &&
             * !curHex.equals(prevHex)) {
             */
            if (!curHex.equals(prevHex)) {
                prevFacing = curFacing;
            }
            prevHex = curHex;
            lastElevation = step.getElevation();

            firstStep = false;
        }

        // running with destroyed hip or gyro needs a check
        rollTarget = entity.checkRunningWithDamage(overallMoveType);
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
            nagReport.append(SharedUtility.addNag(rollTarget));
        }

        // but the danger isn't over yet! landing from a jump can be risky!
        if ((overallMoveType == IEntityMovementType.MOVE_JUMP) && !entity.isMakingDfa()) {
            // check for damaged criticals
            rollTarget = entity.checkLandingWithDamage(overallMoveType);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(SharedUtility.addNag(rollTarget));
            }
            // jumped into water?
            IHex hex = client.game.getBoard().getHex(curPos);
            int waterLevel = hex.terrainLevel(Terrains.WATER);
            if (hex.containsTerrain(Terrains.ICE) && (waterLevel > 0)) {
                if(!(entity instanceof Infantry)) {
                    nagReport.append(Messages.getString("MovementDisplay.IceLanding"));
                }
            } else if (!(prevStep.climbMode() && hex.containsTerrain(Terrains.BRIDGE))) {
                rollTarget = entity.checkWaterMove(waterLevel, overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(SharedUtility.addNag(rollTarget));
                }
            }

        }

        if (entity instanceof Aero) {
            // check to see if thrust exceeded SI
            Aero a = (Aero) entity;
            int thrust = md.getMpUsed();
            rollTarget = a.checkThrustSITotal(thrust, overallMoveType);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(SharedUtility.addNag(rollTarget));
            }

            // Atmospheric checks
            if (client.game.getBoard().inAtmosphere()) {
                // check to see if velocity is 2x thrust
                rollTarget = a.checkVelocityDouble(md.getFinalVelocity(), overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(SharedUtility.addNag(rollTarget));
                }

                // check to see if descended more than two hexes
                rollTarget = a.checkDown(md.getFinalNDown(), overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(SharedUtility.addNag(rollTarget));
                }

                // stalling out
                if ((md.getFinalVelocity() == 0) && !(a.isSpheroid() || client.game.getPlanetaryConditions().isVacuum()) && client.game.getBoard().inAtmosphere() && !a.isVSTOL()) {
                    rollTarget = a.checkStall(md.getFinalVelocity(), overallMoveType);
                    nagReport.append(SharedUtility.addNag(rollTarget));
                }

                // check for hovering
                rollTarget = a.checkHover(md);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(SharedUtility.addNag(rollTarget));
                }

            }
        }

        return nagReport.toString();
    }
    /**
     * Checks to see if piloting skill rolls are needed for excessive use of
     * thrust.
     */
    public static String doThrustCheck(MovePath md) {

        StringBuffer nagReport = new StringBuffer();

        final Entity entity = md.getEntity();
        int overallMoveType = IEntityMovementType.MOVE_NONE;

        Aero a = (Aero) entity;

        PilotingRollData rollTarget;

        overallMoveType = md.getLastStepMovementType();

        // cycle through movement. Collect thrust used until position changes.
        int thrustUsed = 0;
        int j = 0;
        for (final Enumeration<MoveStep> i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = i.nextElement();

            j++;
            // how do I figure out last step?
            if ((step.getDistance() == 0) && (md.length() != j)) {
                thrustUsed += step.getMp();
            } else {
                // if this was the last move and distance was zero, then add
                // thrust
                if ((step.getDistance() == 0) && (md.length() == j)) {
                    thrustUsed += step.getMp();
                }
                // then we moved to a new hex or the last step so check
                // conditions
                // structural damage
                rollTarget = a.checkThrustSI(thrustUsed, overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(SharedUtility.addNag(rollTarget));
                }

                // check for pilot damage
                int hits = entity.getCrew().getHits();
                int health = 6 - hits;

                if (thrustUsed > (2 * health)) {
                    int targetroll = 2 + (thrustUsed - 2 * health) + 2 * hits;
                    nagReport.append(Messages.getString("MovementDisplay.addNag", new Object[] { Integer.toString(targetroll), "Thrust exceeded twice pilot's health in single hex" }));
                }

                thrustUsed = 0;
            }
        }

        return nagReport.toString();

    }

    private static String addNag(PilotingRollData rollTarget) {
        return Messages.getString("MovementDisplay.addNag", new Object[] { rollTarget.getValueAsString(), rollTarget.getDesc() });//$NON-NLS-1$
    }
}
