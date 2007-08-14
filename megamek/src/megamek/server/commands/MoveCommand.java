package megamek.server.commands;

import java.util.Enumeration;

import megamek.client.ui.AWT.Messages;
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
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Terrains;
import megamek.common.VTOL;
import megamek.server.Server;

/**
 * This command exists to move units from the chat window using server commands. It's not implemented yet.
 * @author dirk
 *
 */
public class MoveCommand extends ServerCommand {
    
    public static final int GEAR_LAND = 0;
    public static final int GEAR_BACKUP = 1;
    public static final int GEAR_JUMP = 2;
    public static final int GEAR_CHARGE = 3;
    public static final int GEAR_DFA = 4;
    public static final int GEAR_TURN = 5;
    public static final int GEAR_SWIM = 6;
    
    // considering movement data
    private MovePath cmd;
    // considered entity
    private Entity entity;

    public MoveCommand(Server server) {
        super(server, "move", "This command allows you to move your units from the command line. Usage: It doesn't work yet.");
        // intended usage is something allong the lines of '/move unit# x y [gear]' which would make a path from the unit to the coordinates x y, using the gear specified or forwrads if none.
        //then use /move done to confirm and commit, and move cancel to abort.
        //This needs to be handeled client side, because if this is implemented as is other people can move your mechs.
    }

    @Override
    public void run(int connId, String[] args) {
        //ignore the code below it just exists to jog my memory. 
        /*
        cmd = new MovePath(server.getGame(), entity);
        currentMove(null, null);
        doPSRCheck(cmd);
        */
    }
    
    /**
     * Returns new MovePath for the currently selected movement type
     */
    private void currentMove(Coords dest, String gear) {
        if (gear.equalsIgnoreCase("TURN")) {
            cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest));
        } else if (gear.equalsIgnoreCase("BACK")) {
            cmd.findPathTo(dest, MovePath.STEP_BACKWARDS);
        } else if (gear.equalsIgnoreCase("CHARGE")) {
            cmd.findPathTo(dest, MovePath.STEP_CHARGE);
        } else if (gear.equalsIgnoreCase("DFA")) {
            cmd.findPathTo(dest, MovePath.STEP_DFA);
        } else if (gear.equalsIgnoreCase("SWIM")) {
            cmd.findPathTo(dest, MovePath.STEP_SWIM);
        } else {
            cmd.findPathTo(dest, MovePath.STEP_FORWARDS);
        }
    }
    
    /**
     * There should probably be a central version of this function rather than one for each user interface.
     * @param entity
     * @param md
     * @return
     */
    private String doPSRCheck(MovePath md) {

        StringBuffer nagReport = new StringBuffer();

        // okay, proceed with movement calculations
        Coords lastPos = entity.getPosition();
        Coords curPos = entity.getPosition();
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
            
            // check piloting skill for getting up
            rollTarget = entity.checkGetUp(step);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }

            // set most step parameters
            moveType = step.getMovementType();
            distance = step.getDistance();
 
            // set last step parameters
            curPos = step.getPosition();
            curFacing = step.getFacing();

            final IHex curHex = server.getGame().getBoard().getHex(curPos);

            // Check for skid.
            rollTarget = entity.checkSkid(moveType, prevHex, overallMoveType,
                                          prevStep, prevFacing, curFacing,
                                          lastPos, curPos, isInfantry,
                                          distance);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                // Have an entity-meaningful PSR message.
                nagReport.append(addNag(rollTarget));
            }

            // check if we've moved into rubble
            rollTarget = entity.checkRubbleMove(step, curHex, lastPos, curPos);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }
            
            // check for crossing ice
            if(curHex.containsTerrain(Terrains.ICE) &&
                    curHex.containsTerrain(Terrains.WATER) &&
                    !(curPos.equals(lastPos)) &&
                    step.getElevation() == 0 &&
                    moveType != IEntityMovementType.MOVE_JUMP) {
                nagReport.append(Messages.getString("MovementDisplay.IceMoving"));
            }
            
            // check if we've moved into water
            rollTarget = entity.checkWaterMove(step, curHex, lastPos, curPos, isPavementStep);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }
            
            // check for non-mech entering a fire
            if(curHex.containsTerrain(Terrains.FIRE)
                    && !(entity instanceof Mech)
                    && step.getElevation() <= 1
                    && moveType != IEntityMovementType.MOVE_JUMP
                    && !(curPos.equals(lastPos))) {
                nagReport.append(Messages.getString("MovementDisplay.FireMoving", new Object[] {new Integer(8)}));
            }
            
            // check for magma
            int level = curHex.terrainLevel(Terrains.MAGMA);
            if(level == 1
                    && step.getElevation() == 0
                    && moveType != IEntityMovementType.MOVE_JUMP
                    && !(curPos.equals(lastPos))) {
                nagReport.append(Messages.getString("MovementDisplay.MagmaCrustMoving"));
            }
            else if(level == 2
                    && entity.getElevation() == 0
                    && moveType != IEntityMovementType.MOVE_JUMP
                    && entity.getMovementMode() != IEntityMovementMode.HOVER
                    && !(curPos.equals(lastPos))) {
                nagReport.append(Messages.getString("MovementDisplay.MagmaLiquidMoving"));
            }
            
            if(entity instanceof VTOL
                    || entity.getMovementMode() == IEntityMovementMode.HOVER
                    || entity.getMovementMode() == IEntityMovementMode.WIGE) {
                rollTarget = entity.checkSideSlip(moveType,prevHex,overallMoveType,
                                             prevStep,prevFacing,curFacing,
                                             lastPos,curPos,distance);
                if(rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(addNag(rollTarget));
                }
            }
            
            // check if we've moved into swamp
            rollTarget = entity.checkSwampMove(step, curHex, lastPos, curPos, isPavementStep);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }
            
            // check if we used more MPs than the Mech/Vehicle would have in normal gravity
            if (!i.hasMoreElements() && !firstStep) {
                if ((entity instanceof Mech) || (entity instanceof VTOL)) {
                    if ((step.getMovementType() == IEntityMovementType.MOVE_WALK)
                            || (step.getMovementType() == IEntityMovementType.MOVE_VTOL_WALK)
                            || (step.getMovementType() == IEntityMovementType.MOVE_RUN)
                            || (step.getMovementType() == IEntityMovementType.MOVE_VTOL_RUN)) {
                        if (step.getMpUsed() > entity.getRunMP(false)) {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(addNag(rollTarget));
                            }
                        }
                    } else if (step.getMovementType() == IEntityMovementType.MOVE_JUMP) {
                        if (step.getMpUsed() > entity.getOriginalJumpMP()) {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(addNag(rollTarget));
                            }
                        }    
                      }
                } else if (entity instanceof Tank) {
                    if ((step.getMovementType() == IEntityMovementType.MOVE_WALK)
                            || (step.getMovementType() == IEntityMovementType.MOVE_VTOL_WALK)
                            || (step.getMovementType() == IEntityMovementType.MOVE_RUN)
                            || (step.getMovementType() == IEntityMovementType.MOVE_VTOL_RUN)) {

                        // For Tanks, we need to check if the tank had more MPs because it was moving along a road
                        if (step.getMpUsed() > entity.getRunMP(false) && !step.isOnlyPavement()) {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(addNag(rollTarget));
                            }    
                        }
                        // If the tank was moving on a road, he got a +1 bonus.
                        // N.B. The Ask Precentor Martial forum said that a 4/6
                        //      tank on a road can move 5/7, **not** 5/8.
                        else if (step.getMpUsed() > entity.getRunMP(false) + 1)
                        {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(addNag(rollTarget));
                            }
                        }
                    }   
                }
            }

            // Handle non-infantry moving into a building.
            int buildingMove = entity.checkMovementInBuilding(step, prevStep, curPos, lastPos);
            if (buildingMove > 0) {

                // Get the building being exited.
                Building bldgExited = null;
                if((buildingMove & 1) == 1)
                    bldgExited = server.getGame().getBoard().getBuildingAt( lastPos );

                // Get the building being entered.
                Building bldgEntered = null;
                if((buildingMove & 2) == 2)
                    bldgEntered = server.getGame().getBoard().getBuildingAt( curPos );

                if ( bldgExited != null && bldgEntered != null && 
                     !bldgExited.equals(bldgEntered) ) {
                    // Exiting one building and entering another.
                    //  Brave, aren't we?
                    rollTarget = entity.rollMovementInBuilding(bldgExited, distance, "exiting");
                    nagReport.append(addNag(rollTarget));
                    rollTarget = entity.rollMovementInBuilding(bldgEntered, distance, "entering");
                    nagReport.append(addNag(rollTarget));
                } else {
                    Building bldg;
                    if (bldgEntered == null) {
                        // Exiting a building.
                        bldg = bldgExited;
                    } else {
                        // Entering or moving within a building.
                        bldg = bldgEntered;
                    }
                    if(bldg != null) {
                        rollTarget = entity.rollMovementInBuilding(bldg, distance, "");
                        nagReport.append(addNag(rollTarget));
                    }
                }
            }

            if (step.getType() == MovePath.STEP_GO_PRONE) {
                rollTarget = entity.checkDislodgeSwarmers(step);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(addNag(rollTarget));
                }
            }

            if ((step.getType() == MovePath.STEP_BACKWARDS
                    || step.getType() == MovePath.STEP_LATERAL_LEFT_BACKWARDS
                    || step.getType() == MovePath.STEP_LATERAL_RIGHT_BACKWARDS)
                    && server.getGame().getBoard().getHex(lastPos).getElevation() != curHex.getElevation() 
                    && !(entity instanceof VTOL)) {
                nagReport.append(Messages.getString("MovementDisplay.BackWardsElevationChange"));
                nagReport.append(addNag(entity.getBasePilotingRoll()));
            }
            
            // update lastPos, prevStep, prevFacing & prevHex
            lastPos = new Coords(curPos);
            prevStep = step;
            /* Bug 754610: Revert fix for bug 702735.
            if (prevHex != null && !curHex.equals(prevHex)) {
            */
            if (!curHex.equals(prevHex)) {
                prevFacing = curFacing;
            }
            prevHex = curHex;

            firstStep = false;
        }
        
        // running with destroyed hip or gyro needs a check
        rollTarget = entity.checkRunningWithDamage(overallMoveType);
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
            nagReport.append(addNag(rollTarget));
        }

        // but the danger isn't over yet!  landing from a jump can be risky!
        if (overallMoveType == IEntityMovementType.MOVE_JUMP && !entity.isMakingDfa()) {
            // check for damaged criticals
            rollTarget = entity.checkLandingWithDamage();
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }
            // jumped into water?
            IHex hex = server.getGame().getBoard().getHex(curPos);
            int waterLevel = hex.terrainLevel(Terrains.WATER);
            if(hex.containsTerrain(Terrains.ICE) && waterLevel > 0) {
                nagReport.append(Messages.getString("MovementDisplay.IceLanding"));
            }
            rollTarget = entity.checkWaterMove(waterLevel);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }
        }
        
        return nagReport.toString();
    }
    
    private String addNag(PilotingRollData rollTarget) {
        return Messages.getString("MovementDisplay.addNag", new Object[]{rollTarget.getValueAsString(), rollTarget.getDesc()});//$NON-NLS-1$
    }

}
