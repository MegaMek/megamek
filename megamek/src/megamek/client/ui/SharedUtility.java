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

import java.util.ArrayList;
import java.util.Enumeration;

import megamek.client.Client;
import megamek.common.Aero;
import megamek.common.Building;
import megamek.common.Compute;
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
        int curElevation = entity.getElevation();
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

            if (entity.isAirborne()) {
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
            curElevation = step.getElevation();

            final IHex curHex = client.game.getBoard().getHex(curPos);

            //check for leap
            if(!lastPos.equals(curPos) && (step.getMovementType() != IEntityMovementType.MOVE_JUMP)
                    && (entity instanceof Mech) && client.game.getOptions().booleanOption("tacops_leaping")) {
                int leapDistance = (lastElevation + client.game.getBoard().getHex(lastPos).getElevation()) - (curElevation + curHex.getElevation());
                if(leapDistance > 2) {
                    rollTarget = entity.getBasePilotingRoll(step.getMovementType());
                    entity.addPilotingModifierForTerrain(rollTarget, curPos);
                    rollTarget.append(new PilotingRollData(entity.getId(), 2 * leapDistance, "leaping (leg damage)"));
                    nagReport.append(SharedUtility.addNag(rollTarget));
                    rollTarget = entity.getBasePilotingRoll(step.getMovementType());
                    entity.addPilotingModifierForTerrain(rollTarget, curPos);
                    rollTarget.append(new PilotingRollData(entity.getId(), leapDistance, "leaping (fall)"));
                    nagReport.append(SharedUtility.addNag(rollTarget));
                }
            }

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
            rollTarget = entity.checkRecklessMove(step, curHex, lastPos, curPos, prevHex);
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

            // check for sideslip
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
                        //TODO: need to adjust for sprinting, but game options are not passed
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
                        } else if (client.game.getPlanetaryConditions().getGravity() > 1) {
                            rollTarget = entity.getBasePilotingRoll(step.getParent().getLastStepMovementType());
                            entity.addPilotingModifierForTerrain(rollTarget, step);
                            rollTarget.append(new PilotingRollData(entity.getId(), 0, "jumped in high gravity"));
                            nagReport.append(SharedUtility.addNag(rollTarget));
                        }
                    } else if (step.getMovementType() == IEntityMovementType.MOVE_SPRINT) {
                        if (step.getMpUsed() > entity.getSprintMP(false, false)) {
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

        //if we sprinted with MASC or a supercharger, then we need a PSR
        rollTarget = entity.checkSprintingWithMASC(overallMoveType, md.getMpUsed());
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
            nagReport.append(SharedUtility.addNag(rollTarget));
        }

        rollTarget = entity.checkSprintingWithSupercharger(overallMoveType, md.getMpUsed());
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
            // check for jumping into heavy woods
            if (client.game.getOptions().booleanOption("psr_jump_heavy_woods")) {
                rollTarget = entity.checkLandingInHeavyWoods(overallMoveType,
                        hex);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(SharedUtility.addNag(rollTarget));
                }
            }
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

        if (entity.isAirborne()) {
            // check to see if thrust exceeded SI
            Aero a = (Aero) entity;
            int thrust = md.getMpUsed();
            rollTarget = a.checkThrustSITotal(thrust, overallMoveType);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(SharedUtility.addNag(rollTarget));
            }

            // Atmospheric checks
            if (!client.game.getBoard().inSpace()) {
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
                if ((md.getFinalVelocity() == 0) && !(a.isSpheroid() || client.game.getPlanetaryConditions().isVacuum()) && !client.game.getBoard().inSpace() && !a.isVSTOL()) {
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
    public static String doThrustCheck(MovePath md, Client client) {

        StringBuffer nagReport = new StringBuffer();

        if(client.game.useVectorMove()) {
            return nagReport.toString();
        }
        
        final Entity entity = md.getEntity();      
        if(!(entity instanceof Aero)) {
            return nagReport.toString();
        }     
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
    
    public static MovePath moveAero(MovePath md, Client client) {
        final Entity entity = md.getEntity();      
        if(!(entity instanceof Aero)) {
            return md;
        }
        Aero a = (Aero) entity;
        
        // should check to see if md is null. If so I need to check and see
        // if the units
        // current velocity is zero
        if (md != null) {    
            boolean isRamming = false;
            if ((md.getLastStep() != null)
                    && (md.getLastStep().getType() == MovePath.STEP_RAM)) {
                isRamming = true;
            }
    
            // if using advanced movement then I need to add on movement
            // steps to get the vessel from point a to point b        
            if (client.game.useVectorMove()) {
                // if the unit is ramming then this is already done
                if(!isRamming) {
                    md = addSteps(md, entity, client);
                }
            }
            else if (a.isOutControlTotal()) { 
                // OOC units need a new movement path
                MovePath oldmd = md;  
                md = new MovePath(client.game, entity);
                int vel = a.getCurrentVelocity();
    
                while (vel > 0) {
                    md.addStep(MovePath.STEP_FORWARDS);
                    if(!client.game.getBoard().contains(md.getLastStep().getPosition())) {
                        md.removeLastStep();
                        md.addStep(MovePath.STEP_OFF);
                        break;
                    }
                    if (a.isRandomMove()) {
                        int roll = Compute.d6(1);
                        switch (roll) {
                        case 1:
                            md.addStep(MovePath.STEP_TURN_LEFT);
                            md.addStep(MovePath.STEP_TURN_LEFT);
                            break;
                        case 2:
                            md.addStep(MovePath.STEP_TURN_LEFT);
                            break;
                        case 5:
                            md.addStep(MovePath.STEP_TURN_RIGHT);
                            break;
                        case 6:
                            md.addStep(MovePath.STEP_TURN_RIGHT);
                            md.addStep(MovePath.STEP_TURN_RIGHT);
                            break;
                        }
                    }
                    vel--;
                }
                //check to see if old movement path contained a launch
                if (oldmd.contains(MovePath.STEP_LAUNCH)) {
                    // since launches have to be the last step
                    MoveStep lastStep = oldmd.getLastStep();
                    if (lastStep.getType() == MovePath.STEP_LAUNCH) {
                        md.addStep(lastStep.getType(), lastStep.getLaunched());
                    }
                }             
            }          
        }  
        return md;
    }
    
    /*
     * Add steps for advanced vector movement based on the given vectors when
     * splitting hexes, choose the hex with less tonnage in case OOC
     */
    private static MovePath addSteps(MovePath md, Entity en, Client client) {

        // if the last step is a launch or recovery, then I want to keep that at
        // the end
        MoveStep lastStep = md.getLastStep();
        if ((lastStep != null)
                && ((lastStep.getType() == MovePath.STEP_LAUNCH) || (lastStep
                        .getType() == MovePath.STEP_RECOVER))) {
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
        if (degree % 60 == 30) {
            split = true;
            in = Coords.intervening(start, end, true);
        }

        Coords current = start;
        int facing = md.getFinalFacing();
        for (int i = 1; i < in.size(); i++) {

            // check for split hexes
            // check for some number after a multiple of 3 (1,4,7,etc)
            if (((i % 3) == 1) && split) {

                Coords left = in.get(i);
                Coords right = in.get(i + 1);

                // get the total tonnage in each hex
                Enumeration<Entity> leftTargets = client.game.getEntities(left);
                double leftTonnage = 0;
                while (leftTargets.hasMoreElements()) {
                    leftTonnage += leftTargets.nextElement().getWeight();
                }
                Enumeration<Entity> rightTargets = client.game
                        .getEntities(right);
                double rightTonnage = 0;
                while (rightTargets.hasMoreElements()) {
                    rightTonnage += rightTargets.nextElement().getWeight();
                }

                // TODO: I will need to update this to account for asteroids

                // I need to consider both of these passed through
                // for purposes of bombing
                en.addPassedThrough(right);
                en.addPassedThrough(left);
                client.sendUpdateEntity(en);

                // if the left is preferred, increment i so next one is skipped
                if (leftTonnage < rightTonnage) {
                    i++;
                } else {
                    continue;
                }

            }

            Coords c = in.get(i);

            if(!client.game.getBoard().contains(c)) {
                md.addStep(MovePath.STEP_OFF);
                leftMap = true;
                break;
            }
            
            // which direction is this from the current hex?
            int dir = current.direction(c);
            // what kind of step do I need to get there?
            int diff = dir - facing;
            if (diff == 0) {
                md.addStep(MovePath.STEP_FORWARDS);
            } else if ((diff == 1) || (diff == -5)) {
                md.addStep(MovePath.STEP_LATERAL_RIGHT);
            } else if ((diff == -2) || (diff == 4)) {
                md.addStep(MovePath.STEP_LATERAL_RIGHT_BACKWARDS);
            } else if ((diff == -1) || (diff == 5)) {
                md.addStep(MovePath.STEP_LATERAL_LEFT);
            } else if ((diff == 2) || (diff == -4)) {
                md.addStep(MovePath.STEP_LATERAL_LEFT_BACKWARDS);
            } else if ((diff == 3) || (diff == -3)) {
                md.addStep(MovePath.STEP_BACKWARDS);
            }
            current = c;

        }

        // do I now need to add on the last step again?
        if (!leftMap && (lastStep != null) && (lastStep.getType() == MovePath.STEP_LAUNCH)) {
            md.addStep(MovePath.STEP_LAUNCH, lastStep.getLaunched());
        }

        if (!leftMap && (lastStep != null) && (lastStep.getType() == MovePath.STEP_RECOVER)) {
            md.addStep(MovePath.STEP_RECOVER, lastStep.getRecoveryUnit());
        }

        return md;
    }

    private static String addNag(PilotingRollData rollTarget) {
        return Messages.getString("MovementDisplay.addNag", new Object[] { rollTarget.getValueAsString(), rollTarget.getDesc() });//$NON-NLS-1$
    }
}
