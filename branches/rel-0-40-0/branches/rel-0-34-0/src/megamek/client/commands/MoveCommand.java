/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
package megamek.client.commands;

import megamek.client.Client;
import megamek.client.ui.swing.MovementDisplay;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IEntityMovementMode;
import megamek.common.MovePath;

/**
 * @author dirk
 */
public class MoveCommand extends ClientCommand {

    public static final int GEAR_LAND = 0;
    public static final int GEAR_BACKUP = 1;
    public static final int GEAR_JUMP = 2;
    public static final int GEAR_CHARGE = 3;
    public static final int GEAR_DFA = 4;
    public static final int GEAR_TURN = 5;
    public static final int GEAR_SWIM = 6;

    // considering movement data
    private MovePath cmd;
    // considered ce()
    private int cen = Entity.NONE;
    int gear;

    public MoveCommand(Client client) {
        super(client, "move",
                "Move your units. Use #move HELP for more information.");
    }

    public String run(String[] args) {
        if (args.length > 1) {
            Coords target = null;
            if (args[1].equalsIgnoreCase("ABORT")) {
                clearAllMoves();
                cen = Entity.NONE;
                return "Move aborted, all movement data cleared.";
            } else if (args[1].equalsIgnoreCase("HELP")) {
                return "Available commands:\n"
                        + "#move ABORT = aborts planed move and deselect unit.\n"
                        + "#move SELECT unitID = Selects thhe unit named unit ID for movement. This is a prerequisite for all commands listed after this.\n"
                        + "#move COMMIT = comits the planed movement.\n"
                        + "#move JUMP = clears all movement and starts jump movement. Eiether the entire move is a jump or the entire move is a walk. switching gears will cancel all planned movement (but leave the unit selected).\n"
                        + "#move BACK [x y] = Start walking backwards, can be followed by a coordiate.\n"
                        + "#move WALK [x y] = Start walking/running forwards, this is the default. Can be followed by a coordiate.\n"
                        + "#move TURN [x y] = Starts turning towards target coordinate. Can be followed by a coordiate.\n"
                        + "#move CLIP = Clips to path to what is actually possible, and reports on what will happen if commited.\n"
                        + "#move GETUP = Attempt to stand up. Will require a piloting roll.\n"
                        + "#move CAREFUL = Attempt to stand up. Will require a piloting roll.\n"
                        + "#move x y = move towards coordinate in the current gear. It will do pathfinding for least cost path. Note that the entity will try to move to each coordinate supplied in order.\n";
            } else if (args[1].equalsIgnoreCase("SELECT")) {
                try {
                    clearAllMoves();
                    cen = Integer.parseInt(args[2]);
                    if (ce() == null) {
                        cen = Entity.NONE;
                        return "Not an entity ID or valid number.";
                    }
                    cmd = new MovePath(client.game, ce());

                    return "Entity " + ce().toString()
                            + " selected for movement.";
                } catch (Exception e) {
                    return "Not an entity ID or valid number." + e.toString();
                }
            } else if (ce() != null) {
                if (args[1].equalsIgnoreCase("JUMP")) {

                    clearAllMoves();
                    if (!cmd.isJumping()) {
                        cmd.addStep(MovePath.STEP_START_JUMP);
                    }
                    gear = GEAR_JUMP;

                    return "Entity " + ce().toString() + " is going to jump.";
                } else if (args[1].equalsIgnoreCase("COMMIT")) {
                    moveTo(cmd);
                    return "Move sent.";
                } else if (args[1].equalsIgnoreCase("BACK")) {
                    if (gear == MovementDisplay.GEAR_JUMP) {
                        clearAllMoves();
                    }
                    gear = GEAR_BACKUP;
                } else if (args[1].equalsIgnoreCase("WALK")) {
                    if (gear == MovementDisplay.GEAR_JUMP) {
                        clearAllMoves();
                    }
                    gear = GEAR_LAND;
                } else if (args[1].equalsIgnoreCase("TURN")) {
                    gear = GEAR_TURN;
                } else if (args[1].equalsIgnoreCase("CLIP")) {
                    cmd.clipToPossible();
                    return "Path cliped to whats actually possible. "
                            + ce().toString() + " is now in gear "
                            + gearName(gear) + " heading towards "
                            + cmd.getFinalCoords().toFriendlyString()
                            + " with a final facing of "
                            + getDirection(cmd.getFinalFacing())
                            + ". Total mp used: " + cmd.getMpUsed()
                            + " for a movement of: " + cmd.getHexesMoved();
                } else if (args[1].equalsIgnoreCase("GETUP")) {
                    if (cmd.getFinalProne() || cmd.getFinalHullDown()) {
                        cmd.addStep(MovePath.STEP_GET_UP);
                        return "Mech will try to stand up. this requieres a piloting roll.";
                    }

                    return "Trying to get up but the mech is not prone.";
                } else if (args[1].equalsIgnoreCase("CAREFULSTAND")) {
                    if (cmd.getFinalProne() || cmd.getFinalHullDown()
                            && client.game.getOptions().booleanOption("tacops_careful_stand")) {
                        cmd.addStep(MovePath.STEP_CAREFUL_STAND);
                        return "Mech will try to stand up. this requieres a piloting roll.";
                    }

                    return "Trying to get up but the mech is not prone.";
                } else {
                    target = new Coords(Integer.parseInt(args[1]) - 1, Integer
                            .parseInt(args[2]) - 1);
                }

                if (target == null && args.length > 3) {
                    target = new Coords(Integer.parseInt(args[2]) - 1, Integer
                            .parseInt(args[3]) - 1);
                }

                currentMove(target);

                return "Commands accepted " + ce().toString()
                        + " is now in gear " + gearName(gear)
                        + " heading towards "
                        + cmd.getFinalCoords().toFriendlyString()
                        + ". Total mp used: " + cmd.getMpUsed()
                        + " for a movement of: " + cmd.getHexesMoved();
            }
        }
        clearAllMoves();
        return "No arguments given, or there was an error parsing the arguments. All movement data cleared.";
    }

    /**
     * Returns new MovePath for the currently selected movement type
     */
    private void currentMove(Coords dest) {
        if (dest != null) {
            if (gear == GEAR_TURN) {
                cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest), false);
            } else if (gear == GEAR_LAND || gear == GEAR_JUMP) {
                cmd.findPathTo(dest, MovePath.STEP_FORWARDS);
            } else if (gear == GEAR_BACKUP) {
                cmd.findPathTo(dest, MovePath.STEP_BACKWARDS);
            } else if (gear == GEAR_CHARGE) {
                cmd.findPathTo(dest, MovePath.STEP_CHARGE);
            } else if (gear == GEAR_DFA) {
                cmd.findPathTo(dest, MovePath.STEP_DFA);
            } else if (gear == GEAR_SWIM) {
                cmd.findPathTo(dest, MovePath.STEP_SWIM);
            }
        }
    }

    private String gearName(int gear) {
        if (gear == GEAR_TURN) {
            return "turning";
        } else if (gear == GEAR_LAND) {
            return "walking";
        } else if (gear == GEAR_BACKUP) {
            return "backup";
        } else if (gear == GEAR_CHARGE) {
            return "charging";
        } else if (gear == GEAR_DFA) {
            return "death from aboveing";
        } else if (gear == GEAR_SWIM) {
            return "swiming";
        } else if (gear == GEAR_JUMP) {
            return "jumping";
        }

        return "Unknown";
    }

    /**
     * Clears out the currently selected movement data and resets it.
     */
    private void clearAllMoves() {
        // switch back from swimming to normal mode.
        if (ce() != null) {
            if (ce().getMovementMode() == IEntityMovementMode.BIPED_SWIM)
                ce().setMovementMode(IEntityMovementMode.BIPED);
            else if (ce().getMovementMode() == IEntityMovementMode.QUAD_SWIM)
                ce().setMovementMode(IEntityMovementMode.QUAD);

            cmd = new MovePath(client.game, ce());
        } else {
            cmd = null;
        }

        gear = GEAR_LAND;
    }

    /**
     * Sends a data packet indicating the chosen movement.
     */
    private synchronized void moveTo(MovePath md) {
        md.clipToPossible();

        if (ce().hasUMU()) {
            client.sendUpdateEntity(ce());
        }
        client.moveEntity(cen, md);
    }

    /**
     * Returns the current Entity.
     */
    public Entity ce() {
        return client.game.getEntity(cen);
    }
}
