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
import java.util.List;

import megamek.client.Client;
import megamek.common.Aero;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityMovementType;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.MoveStep;
import megamek.common.PilotingRollData;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.VTOL;
import megamek.server.Server;

public class SharedUtility {

    public static String doPSRCheck(MovePath md) {
        return (String) doPSRCheck(md, true);
    }

    @SuppressWarnings("unchecked")
    public static List<TargetRoll> getPSRList(MovePath md) {
        return (List<TargetRoll>) doPSRCheck(md, false);
    }

    /**
     * Checks to see if piloting skill rolls are needed for the currently
     * selected movement. This code is basically a simplified version of
     * Server.processMovement(), except that it just reads information (no
     * writing). Note that MovePath.clipToPossible() is called though, which
     * changes the md object.
     */
    private static Object doPSRCheck(MovePath md, boolean stringResult) {

        StringBuffer nagReport = new StringBuffer();
        List<TargetRoll> psrList = new ArrayList<TargetRoll>();

        final Entity entity = md.getEntity();
        final IGame game = entity.getGame();
        // okay, proceed with movement calculations
        Coords lastPos = entity.getPosition();
        Coords curPos = entity.getPosition();
        int lastElevation = entity.getElevation();
        int curElevation = entity.getElevation();
        int curFacing = entity.getFacing();
        int distance = 0;
        EntityMovementType moveType = EntityMovementType.MOVE_NONE;
        EntityMovementType overallMoveType = EntityMovementType.MOVE_NONE;
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
            if (step.getMovementType(md.isEndStep(step)) == EntityMovementType.MOVE_ILLEGAL) {
                break;
            }

            if (entity.isAirborne() && (entity instanceof Aero)) {
                // check for more than one roll
                Aero a = (Aero) entity;
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

            final IHex curHex = game.getBoard().getHex(curPos);

            //check for vertical takeoff
            if((step.getType() == MoveStepType.VTAKEOFF) && (entity instanceof Aero)) {
                rollTarget = ((Aero)entity).checkVerticalTakeOff();
                checkNag(rollTarget, nagReport, psrList);
            }

            //check for landing
            if ((step.getType() == MoveStepType.LAND)
                    && (entity instanceof Aero)) {
                rollTarget = ((Aero) entity).checkLanding(moveType,
                        step.getVelocity(), curPos, curFacing, false);
                checkNag(rollTarget, nagReport, psrList);
            }
            if ((step.getType() == MoveStepType.VLAND)
                    && (entity instanceof Aero)) {
                rollTarget = ((Aero) entity).checkLanding(moveType,
                        step.getVelocity(), curPos, curFacing, true);
                checkNag(rollTarget, nagReport, psrList);
            }

            //check for leap
            if(!lastPos.equals(curPos) && (moveType != EntityMovementType.MOVE_JUMP)
                    && (entity instanceof Mech) && game.getOptions().booleanOption("tacops_leaping")) {
                int leapDistance = (lastElevation + game.getBoard().getHex(lastPos).getLevel()) - (curElevation + curHex.getLevel());
                if(leapDistance > 2) {
                    rollTarget = entity.getBasePilotingRoll(moveType);
                    entity.addPilotingModifierForTerrain(rollTarget, curPos);
                    rollTarget.append(new PilotingRollData(entity.getId(), 2 * leapDistance, "leaping (leg damage)"));
                    SharedUtility.checkNag(rollTarget, nagReport, psrList);
                    rollTarget = entity.getBasePilotingRoll(moveType);
                    entity.addPilotingModifierForTerrain(rollTarget, curPos);
                    rollTarget.append(new PilotingRollData(entity.getId(), leapDistance, "leaping (fall)"));
                    SharedUtility.checkNag(rollTarget, nagReport, psrList);
                }
            }

            // Check for skid.
            rollTarget = entity.checkSkid(moveType, prevHex, overallMoveType, prevStep, prevFacing, curFacing, lastPos, curPos, isInfantry, distance-1);
            checkNag(rollTarget, nagReport, psrList);

            // check if we've moved into rubble
            boolean isLastStep = md.getLastStep().equals(step);
            rollTarget = entity.checkRubbleMove(step, overallMoveType, curHex,
                    lastPos, curPos, isLastStep);
            checkNag(rollTarget, nagReport, psrList);

            int lightPenalty = entity.getGame().getPlanetaryConditions().getLightPilotPenalty();
            if(lightPenalty > 0) {
                rollTarget.addModifier(lightPenalty, entity.getGame().getPlanetaryConditions().getLightDisplayableName());
            }

            //check if we are moving recklessly
            rollTarget = entity.checkRecklessMove(step, overallMoveType,
                    curHex, lastPos, curPos, prevHex);
            checkNag(rollTarget, nagReport, psrList);

            // check for crossing ice
            if (curHex.containsTerrain(Terrains.ICE) && curHex.containsTerrain(Terrains.WATER) && !(curPos.equals(lastPos)) && (step.getElevation() == 0) && (moveType != EntityMovementType.MOVE_JUMP) && !(entity instanceof Infantry) && !(step.isPavementStep() && curHex.containsTerrain(Terrains.BRIDGE))) {
                nagReport.append(Messages.getString("MovementDisplay.IceMoving"));
            }

            // check if we've moved into water
            rollTarget = entity.checkWaterMove(step, overallMoveType, curHex,
                    lastPos, curPos, isPavementStep);
            checkNag(rollTarget, nagReport, psrList);

            // check for non-mech entering a fire
            boolean underwater = curHex.containsTerrain(Terrains.WATER)
                    && (curHex.depth() > 0)
                    && (step.getElevation() < curHex.surface());
            if (curHex.containsTerrain(Terrains.FIRE) && !underwater && !(entity instanceof Mech) && (step.getElevation() <= 1) && (moveType != EntityMovementType.MOVE_JUMP) && !(curPos.equals(lastPos))) {
                nagReport.append(Messages.getString("MovementDisplay.FireMoving", new Object[] { new Integer(8) }));
            }

            // check for magma
            int level = curHex.terrainLevel(Terrains.MAGMA);
            if ((level == 1) && (step.getElevation() == 0) && (moveType != EntityMovementType.MOVE_JUMP) && !(curPos.equals(lastPos))) {
                nagReport.append(Messages.getString("MovementDisplay.MagmaCrustMoving"));
            } else if ((level == 2) && (entity.getElevation() == 0) && (moveType != EntityMovementType.MOVE_JUMP) && (entity.getMovementMode() != EntityMovementMode.HOVER) && (entity.getMovementMode() != EntityMovementMode.WIGE) && !(curPos.equals(lastPos))) {
                nagReport.append(Messages.getString("MovementDisplay.MagmaLiquidMoving"));
            }

            // check for sideslip
            if ((entity instanceof VTOL) || (entity.getMovementMode() == EntityMovementMode.HOVER) || (entity.getMovementMode() == EntityMovementMode.WIGE)) {
                rollTarget = entity.checkSideSlip(moveType, prevHex, overallMoveType, prevStep, prevFacing, curFacing, lastPos, curPos, distance);
                checkNag(rollTarget, nagReport, psrList);
            }

            // check if we've moved into swamp
            rollTarget = entity.checkBogDown(step, overallMoveType, curHex,
                    lastPos, curPos, lastElevation, isPavementStep);
            checkNag(rollTarget, nagReport, psrList);

            // check if used more MPs than Mech/Vehicle would have w/o gravity
            if (!i.hasMoreElements() && !firstStep) {
                if ((entity instanceof Mech) || (entity instanceof VTOL)) {
                    if ((moveType == EntityMovementType.MOVE_WALK)
                            || (moveType == EntityMovementType.MOVE_VTOL_WALK)
                            || (moveType == EntityMovementType.MOVE_RUN)
                            || (moveType == EntityMovementType.MOVE_VTOL_RUN)) {
                        //TODO: need to adjust for sprinting, but game options are not passed
                        if (step.getMpUsed() > entity.getRunMP(false, false, false)) {
                            rollTarget = entity.checkMovedTooFast(step, overallMoveType);
                            checkNag(rollTarget, nagReport, psrList);
                        }
                    } else if (moveType == EntityMovementType.MOVE_JUMP) {
                        int origWalkMP = entity.getWalkMP(false, false);
                        int gravWalkMP = entity.getWalkMP();
                        if (step.getMpUsed() > entity.getJumpMP(false)) {
                            rollTarget = entity.checkMovedTooFast(step, overallMoveType);
                            checkNag(rollTarget, nagReport, psrList);
                        } else if ((game.getPlanetaryConditions().getGravity() > 1)
                                && ((origWalkMP - gravWalkMP) > 0)) {
                            rollTarget = entity.getBasePilotingRoll(md.getLastStepMovementType());
                            entity.addPilotingModifierForTerrain(rollTarget, step);
                            int gravMod = game.getPlanetaryConditions()
                                    .getGravityPilotPenalty();
                            if ((gravMod != 0) && !game.getBoard().inSpace()) {
                                rollTarget.addModifier(gravMod, game
                                        .getPlanetaryConditions().getGravity()
                                        + "G gravity");
                            }
                            rollTarget.append(new PilotingRollData(entity.getId(), 0, "jumped in high gravity"));
                            SharedUtility.checkNag(rollTarget, nagReport, psrList);
                        }
                    } else if (moveType == EntityMovementType.MOVE_SPRINT) {
                        if (step.getMpUsed() > entity.getSprintMP(false, false, false)) {
                            rollTarget = entity.checkMovedTooFast(step, overallMoveType);
                            checkNag(rollTarget, nagReport, psrList);
                        }
                    }
                } else if (entity instanceof Tank) {
                    if ((moveType == EntityMovementType.MOVE_WALK)
                            || (moveType == EntityMovementType.MOVE_VTOL_WALK)
                            || (moveType == EntityMovementType.MOVE_RUN)
                            || (moveType == EntityMovementType.MOVE_VTOL_RUN)) {

                        // For Tanks, we need to check if the tank had more MPs
                        // because it was moving along a road
                        if ((step.getMpUsed() > entity.getRunMP(false, false, false)) && !step.isOnlyPavement()) {
                            rollTarget = entity.checkMovedTooFast(step, overallMoveType);
                            checkNag(rollTarget, nagReport, psrList);
                        }
                        // If the tank was moving on a road, he got a +1 bonus.
                        // N.B. The Ask Precentor Martial forum said that a 4/6
                        // tank on a road can move 5/7, **not** 5/8.
                        else if (step.getMpUsed() > (entity.getRunMP(false, false, false) + 1)) {
                            rollTarget = entity.checkMovedTooFast(step, overallMoveType);
                            checkNag(rollTarget, nagReport, psrList);
                        }
                    }
                }
            }

            // Handle non-infantry moving into a building.
            int buildingMove = entity.checkMovementInBuilding(step, prevStep, curPos, lastPos);
            if ((buildingMove > 1) && !(entity instanceof Protomech)) {

                // Get the building being entered.
                Building bldg = null;
                String reason ="entering";
                if ((buildingMove & 2) == 2) {
                    bldg = game.getBoard().getBuildingAt(curPos);
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

            IHex lastHex = game.getBoard().getHex(lastPos);
            if (((step.getType() == MoveStepType.BACKWARDS)
                    || (step.getType() == MoveStepType.LATERAL_LEFT_BACKWARDS)
                    || (step.getType() == MoveStepType.LATERAL_RIGHT_BACKWARDS))
                    && !(md.isJumping() && (entity.getJumpType() == Mech.JUMP_BOOSTER))
                    && ((lastHex.getLevel() + entity.calcElevation(curHex,
                            lastHex, step.getElevation(),
                            md.getFinalClimbMode(), false)) != (curHex
                            .getLevel() + entity.getElevation()))
                    && !(entity instanceof VTOL)
                    && !(md.getFinalClimbMode()
                            && curHex.containsTerrain(Terrains.BRIDGE) && ((curHex
                            .terrainLevel(Terrains.BRIDGE_ELEV) + curHex
                            .getLevel()) == (prevHex.getLevel() + (prevHex
                            .containsTerrain(Terrains.BRIDGE) ? prevHex
                            .terrainLevel(Terrains.BRIDGE_ELEV) : 0))))) {
                nagReport.append(Messages
                        .getString("MovementDisplay.BackWardsElevationChange"));
                SharedUtility.checkNag(
                        entity.getBasePilotingRoll(overallMoveType), nagReport,
                        psrList);
            }
            
            // Check for Ejecting
            if (step.getType() == MoveStepType.EJECT 
                    && (entity instanceof Mech)) {
                rollTarget = Server.getEjectModifiers(game, entity, false);
                checkNag(rollTarget, nagReport, psrList);
            }
            
            if (step.getType() == MoveStepType.UNLOAD) {
                Targetable targ = step.getTarget(game);
                if (game.getOptions().booleanOption("tacops_ziplines")
                        && (entity instanceof VTOL)
                        && (md.getFinalElevation() > 0)
                        && (targ instanceof Infantry)
                        && (((Entity)targ).getJumpMP() < 1)
                        && !((Infantry) targ).isMechanized()) {
                    rollTarget = Server.getEjectModifiers(game, (Entity) targ,
                            false, entity.getPosition(), "zip lining");
                    checkNag(rollTarget, nagReport, psrList);
                }
            }

            // update lastPos, prevStep, prevFacing & prevHex
            if (!curPos.equals(lastPos)) {
                prevFacing = curFacing;
            }
            lastPos = new Coords(curPos);
            prevStep = step;
            prevHex = curHex;
            lastElevation = step.getElevation();

            firstStep = false;
        }

        // running with destroyed hip or gyro needs a check
        rollTarget = entity.checkRunningWithDamage(overallMoveType);
        checkNag(rollTarget, nagReport, psrList);

        //if we sprinted with MASC or a supercharger, then we need a PSR
        rollTarget = entity.checkSprintingWithMASC(overallMoveType, md.getMpUsed());
        checkNag(rollTarget, nagReport, psrList);

        rollTarget = entity.checkSprintingWithSupercharger(overallMoveType, md.getMpUsed());
        checkNag(rollTarget, nagReport, psrList);

        // but the danger isn't over yet! landing from a jump can be risky!
        if ((overallMoveType == EntityMovementType.MOVE_JUMP) && !entity.isMakingDfa()) {
            // check for damaged criticals
            rollTarget = entity.checkLandingWithDamage(overallMoveType);
            checkNag(rollTarget, nagReport, psrList);
            // check for landing with prototype JJs
            rollTarget = entity.checkLandingWithPrototypeJJ(overallMoveType);
            checkNag(rollTarget, nagReport, psrList);
            // jumped into water?
            IHex hex = game.getBoard().getHex(curPos);
            // check for jumping into heavy woods
            if (game.getOptions().booleanOption("psr_jump_heavy_woods")) {
                rollTarget = entity.checkLandingInHeavyWoods(overallMoveType,
                        hex);
                checkNag(rollTarget, nagReport, psrList);
            }
            int waterLevel = hex.terrainLevel(Terrains.WATER);
            if (hex.containsTerrain(Terrains.ICE) && (waterLevel > 0)) {
                if(!(entity instanceof Infantry)) {
                    nagReport.append(Messages.getString("MovementDisplay.IceLanding"));
                }
            } else if (!(prevStep.climbMode() && hex.containsTerrain(Terrains.BRIDGE))) {
                if (!(entity.getMovementMode() == EntityMovementMode.HOVER)) {
                    rollTarget = entity.checkWaterMove(waterLevel, overallMoveType);
                    checkNag(rollTarget, nagReport, psrList);
                }

            }

        }

        if (entity.isAirborne() && (entity instanceof Aero)) {
            // check to see if thrust exceeded SI
            Aero a = (Aero) entity;
            int thrust = md.getMpUsed();
            rollTarget = a.checkThrustSITotal(thrust, overallMoveType);
            checkNag(rollTarget, nagReport, psrList);

            // Atmospheric checks
            if (!game.getBoard().inSpace() && !md.contains(MoveStepType.LAND) && !md.contains(MoveStepType.VLAND)) {
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

        if (stringResult) {
            return nagReport.toString();
        }
        return psrList;
    }
    private static void checkNag(PilotingRollData rollTarget,
            StringBuffer nagReport, List<TargetRoll> psrList) {
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
            psrList.add(rollTarget);
            nagReport.append(Messages.getString("MovementDisplay.addNag", new Object[] { rollTarget.getValueAsString(), rollTarget.getDesc() }));//$NON-NLS-1$
        }
    }
    /**
     * Checks to see if piloting skill rolls are needed for excessive use of
     * thrust.
     */
    public static String doThrustCheck(MovePath md, Client client) {

        StringBuffer nagReport = new StringBuffer();
        List<TargetRoll> psrList = new ArrayList<TargetRoll>();

        if(client.getGame().useVectorMove()) {
            return nagReport.toString();
        }

        final Entity entity = md.getEntity();
        if(!(entity instanceof Aero)) {
            return nagReport.toString();
        }
        EntityMovementType overallMoveType = EntityMovementType.MOVE_NONE;

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
                checkNag(rollTarget, nagReport, psrList);

                // check for pilot damage
                int hits = entity.getCrew().getHits();
                int health = 6 - hits;

                if (thrustUsed > (2 * health)) {
                    int targetroll = 2 + (thrustUsed - (2 * health)) + (2 * hits);
                    nagReport.append(Messages.getString("MovementDisplay.addNag", new Object[] { Integer.toString(targetroll), "Thrust exceeded twice pilot's health in single hex" }));
                }

                thrustUsed = 0;
            }
        }

        return nagReport.toString();

    }

    public static MovePath moveAero(MovePath md, Client client) {
        final Entity entity = md.getEntity();
        final IGame game = entity.getGame();
        if (!(entity instanceof Aero)) {
            return md;
        }
        Aero a = (Aero) entity;

        // need to check and see
        // if the units current velocity is zero

        boolean isRamming = false;
        if ((md.getLastStep() != null)
                && (md.getLastStep().getType() == MoveStepType.RAM)) {
            isRamming = true;
        }

        // if using advanced movement then I need to add on movement
        // steps to get the vessel from point a to point b
        if (game.useVectorMove()) {
            // if the unit is ramming then this is already done
            if (!isRamming) {
                md = addSteps(md, client);
            }
        } else if (a.isOutControlTotal()) {
            // OOC units need a new movement path
            MovePath oldmd = md;
            md = new MovePath(game, entity);
            int vel = a.getCurrentVelocity();

            while (vel > 0) {
                int steps = 1;
                //if moving on the ground map, then 16 hexes forward
                if(game.getBoard().onGround()) {
                    steps = 16;
                }
                while(steps > 0) {
                    md.addStep(MoveStepType.FORWARDS);
                    steps--;
                }
                if (!game.getBoard().contains(md.getLastStep().getPosition())) {
                    md.removeLastStep();
                    if(game.getOptions().booleanOption("return_flyover")) {
                        md.addStep(MoveStepType.RETURN);
                    } else {
                        md.addStep(MoveStepType.OFF);
                    }
                    break;
                }
                if (a.isRandomMove()) {
                    int roll = Compute.d6(1);
                    switch (roll) {
                    case 1:
                        md.addStep(MoveStepType.TURN_LEFT);
                        md.addStep(MoveStepType.TURN_LEFT);
                        break;
                    case 2:
                        md.addStep(MoveStepType.TURN_LEFT);
                        break;
                    case 5:
                        md.addStep(MoveStepType.TURN_RIGHT);
                        break;
                    case 6:
                        md.addStep(MoveStepType.TURN_RIGHT);
                        md.addStep(MoveStepType.TURN_RIGHT);
                        break;
                    }
                }
                vel--;
            }
            // check to see if old movement path contained a launch
            if (oldmd.contains(MoveStepType.LAUNCH)) {
                // since launches have to be the last step
                MoveStep lastStep = oldmd.getLastStep();
                if (lastStep.getType() == MoveStepType.LAUNCH) {
                    md.addStep(lastStep.getType(), lastStep.getLaunched());
                }
            }
            // check to see if old movement path contained an undocking
            if (oldmd.contains(MoveStepType.UNDOCK)) {
                // since launches have to be the last step
                MoveStep lastStep = oldmd.getLastStep();
                if (lastStep.getType() == MoveStepType.UNDOCK) {
                    md.addStep(lastStep.getType(), lastStep.getLaunched());
                }
            }
        }
        return md;
    }

    /**
     * Add steps for advanced vector movement based on the given vectors when
     * splitting hexes, choose the hex with less tonnage in case OOC
     */
    private static MovePath addSteps(MovePath md, Client client) {
        Entity en = md.getEntity();
        IGame game = en.getGame();

        // if the last step is a launch or recovery, then I want to keep that at
        // the end
        MoveStep lastStep = md.getLastStep();
        if ((lastStep != null)
                && ((lastStep.getType() == MoveStepType.LAUNCH) || (lastStep
                        .getType() == MoveStepType.RECOVER) || (lastStep
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
            // check for some number after a multiple of 3 (1,4,7,etc)
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

                // TODO: I will need to update this to account for asteroids

                // I need to consider both of these passed through
                // for purposes of bombing
                en.addPassedThrough(right);
                en.addPassedThrough(left);
                client.sendUpdateEntity(en);

                // if the left is preferred, increment i so next one is skipped
                if ((leftTonnage < rightTonnage) || !game.getBoard().contains(right)) {
                    i++;
                } else {
                    continue;
                }
            }

            if(!game.getBoard().contains(c)) {
                if(game.getOptions().booleanOption("return_flyover")) {
                    md.addStep(MoveStepType.RETURN);
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

    public static Targetable getTargetPicked(List<? extends Targetable> targets, String input) {
        if (input == null) {
            return null;
        }
        for (Targetable ent : targets) {
            if (input.equals(ent.getDisplayName())) {
                return ent;
            }
        }
        //Should never get here!
        return null;
    }

}
