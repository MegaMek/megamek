/*
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.commands;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.panels.phaseDisplay.MovementDisplay;
import megamek.common.ManeuverType;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;

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
    // considered currentEntity()
    private int cen = Entity.NONE;
    int gear;

    public MoveCommand(ClientGUI clientGUI) {
        super(clientGUI, "move",
              "Move your units. Use #move HELP for more information.");
    }

    @Override
    public String run(String[] args) {
        if (args.length > 1) {
            Coords target = null;
            if (args[1].equalsIgnoreCase("ABORT")) {
                clearAllMoves();
                cen = Entity.NONE;
                return "Move aborted, all movement data cleared.";
            } else if (args[1].equalsIgnoreCase("HELP")) {
                return """
                      Available commands:
                      #move ABORT = aborts planed move and deselect unit.
                      #move SELECT unitID = Selects the unit named unit ID for movement. This is a prerequisite for all commands listed after this. Also changed current hex.
                      #move COMMIT = commits the planed movement.
                      #move JUMP = clears all movement and starts jump movement. Either the entire move is a jump or the entire move is a walk. switching gears will cancel all planned movement (but leave the unit selected).
                      #move BACK [x y] = Start walking backwards, can be followed by a coordinate.
                      #move WALK [x y] = Start walking/running forwards, this is the default. Can be followed by a coordinate.
                      #move TURN [x y] = Starts turning towards target coordinate. Can be followed by a coordinate.
                      #move CLIP = Clips to path to what is actually possible, and reports on what will happen if committed.
                      #move GETUP = Attempt to stand up. Will require a piloting roll.
                      #move CAREFUL = Attempt to stand up. Will require a piloting roll.
                      #move x y = move towards coordinate in the current gear. It will do pathfinding for least cost path. Note that the entity will try to move to each coordinate supplied in order.
                      """;
            } else if (args[1].equalsIgnoreCase("SELECT")) {
                try {
                    clearAllMoves();
                    cen = Integer.parseInt(args[2]);
                    if (currentEntity() == null) {
                        cen = Entity.NONE;
                        return "Not an entity ID or valid number.";
                    }
                    cmd = new MovePath(getClient().getGame(), currentEntity());

                    getClientGUI().setCurrentHex(currentEntity().getPosition());
                    return "Entity " + currentEntity().toString()
                          + " selected for movement.";
                } catch (Exception e) {
                    return "Not an entity ID or valid number." + e;
                }
            } else if (currentEntity() != null) {
                if (args[1].equalsIgnoreCase("JUMP")) {

                    clearAllMoves();
                    if (!cmd.isJumping()) {
                        cmd.addStep(MoveStepType.START_JUMP);
                    }
                    gear = GEAR_JUMP;

                    return "Entity " + currentEntity().toString() + " is going to jump.";
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
                    return "Path clipped to whats actually possible. "
                          + currentEntity().toString() + " is now in gear "
                          + gearName(gear) + " heading towards "
                          + cmd.getFinalCoords().toFriendlyString()
                          + " with a final facing of "
                          + getDirection(cmd.getFinalFacing())
                          + ". Total mp used: " + cmd.getMpUsed()
                          + " for a movement of: " + cmd.getHexesMoved();
                } else if (args[1].equalsIgnoreCase("GETUP")) {
                    if (cmd.getFinalProne() || cmd.getFinalHullDown()) {
                        cmd.addStep(MoveStepType.GET_UP);
                        return "Mek will try to stand up. this requires a piloting roll.";
                    }

                    return "Trying to get up but the Mek is not prone.";
                } else if (args[1].equalsIgnoreCase("CAREFULSTAND")) {
                    if (cmd.getFinalProne() || cmd.getFinalHullDown() && getClient().getGame().getOptions()
                          .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_CAREFUL_STAND)) {
                        cmd.addStep(MoveStepType.CAREFUL_STAND);
                        return "Mek will try to stand up. this requires a piloting roll.";
                    }

                    return "Trying to get up but the Mek is not prone.";
                } else {
                    target = new Coords(Integer.parseInt(args[1]) - 1, Integer.parseInt(args[2]) - 1);
                }

                if (target == null && args.length > 3) {
                    target = new Coords(Integer.parseInt(args[2]) - 1, Integer.parseInt(args[3]) - 1);
                }

                currentMove(target);

                return "Commands accepted " + currentEntity().toString()
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
                cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest), false, ManeuverType.MAN_NONE);
            } else if (gear == GEAR_LAND || gear == GEAR_JUMP) {
                cmd.findPathTo(dest, MoveStepType.FORWARDS);
            } else if (gear == GEAR_BACKUP) {
                cmd.findPathTo(dest, MoveStepType.BACKWARDS);
            } else if (gear == GEAR_CHARGE) {
                cmd.findPathTo(dest, MoveStepType.CHARGE);
            } else if (gear == GEAR_DFA) {
                cmd.findPathTo(dest, MoveStepType.DFA);
            } else if (gear == GEAR_SWIM) {
                cmd.findPathTo(dest, MoveStepType.SWIM);
            }
        }
    }

    private String gearName(int intGear) {
        if (intGear == GEAR_TURN) {
            return "turning";
        } else if (intGear == GEAR_LAND) {
            return "walking";
        } else if (intGear == GEAR_BACKUP) {
            return "backup";
        } else if (intGear == GEAR_CHARGE) {
            return "charging";
        } else if (intGear == GEAR_DFA) {
            return "death from above-ing";
        } else if (intGear == GEAR_SWIM) {
            return "swimming";
        } else if (intGear == GEAR_JUMP) {
            return "jumping";
        }

        return "Unknown";
    }

    /**
     * Clears out the currently selected movement data and resets it.
     */
    private void clearAllMoves() {
        // switch back from swimming to normal mode.
        if (currentEntity() != null) {
            if (currentEntity().getMovementMode().isBipedSwim()) {
                currentEntity().setMovementMode(EntityMovementMode.BIPED);
            } else if (currentEntity().getMovementMode().isQuadSwim()) {
                currentEntity().setMovementMode(EntityMovementMode.QUAD);
            }

            cmd = new MovePath(getClient().getGame(), currentEntity());
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

        if (currentEntity().hasUMU()) {
            getClient().sendUpdateEntity(currentEntity());
        }
        getClient().moveEntity(cen, md);
    }

    /**
     * Returns the current Entity.
     */
    public Entity currentEntity() {
        return getClient().getGame().getEntity(cen);
    }
}
