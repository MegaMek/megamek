/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.phaseDisplay.commands;

import java.util.ArrayList;

import megamek.client.ui.Messages;
import megamek.client.ui.panels.phaseDisplay.MovementDisplay;
import megamek.client.ui.panels.phaseDisplay.StatusBarPhaseDisplay;
import megamek.client.ui.util.KeyCommandBind;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

/**
 * This enumeration lists all the possible ActionCommands that can be carried out during the movement phase. Each
 * command has a string for the command plus a flag that determines what unit type it is appropriate for.
 *
 * @author arlith
 */
public enum MoveCommand implements StatusBarPhaseDisplay.PhaseCommand {
    MOVE_NEXT("moveNext", MovementDisplay.CMD_NONE),
    MOVE_TURN("moveTurn", MovementDisplay.CMD_GROUND | MovementDisplay.CMD_AERO),
    MOVE_WALK("moveWalk", MovementDisplay.CMD_GROUND),
    MOVE_JUMP("moveJump",
          MovementDisplay.CMD_MEK | MovementDisplay.CMD_TANK | MovementDisplay.CMD_INF | MovementDisplay.CMD_PROTOMEK),
    MOVE_BACK_UP("moveBackUp", MovementDisplay.CMD_MEK | MovementDisplay.CMD_TANK | MovementDisplay.CMD_VTOL | MovementDisplay.CMD_PROTOMEK),
    MOVE_GET_UP("moveGetUp", MovementDisplay.CMD_MEK),
    MOVE_FORWARD_INI("moveForwardIni", MovementDisplay.CMD_ALL),
    MOVE_CHARGE("moveCharge", MovementDisplay.CMD_MEK | MovementDisplay.CMD_TANK),
    MOVE_DFA("moveDFA", MovementDisplay.CMD_MEK),
    MOVE_GO_PRONE("moveGoProne", MovementDisplay.CMD_MEK),
    MOVE_FLEE("moveFlee", MovementDisplay.CMD_ALL),
    MOVE_EJECT("moveEject", MovementDisplay.CMD_ALL),
    MOVE_ABANDON("moveAbandon", MovementDisplay.CMD_MEK),
    MOVE_LOAD("moveLoad", MovementDisplay.CMD_MEK | MovementDisplay.CMD_TANK | MovementDisplay.CMD_VTOL),
    MOVE_UNLOAD("moveUnload", MovementDisplay.CMD_MEK | MovementDisplay.CMD_TANK | MovementDisplay.CMD_VTOL),
    MOVE_MOUNT("moveMount", MovementDisplay.CMD_GROUND),
    MOVE_TOW("moveTow", MovementDisplay.CMD_TANK),
    MOVE_DISCONNECT("moveDisconnect", MovementDisplay.CMD_TANK),
    MOVE_UNJAM("moveUnjam", MovementDisplay.CMD_NON_INF),
    MOVE_CLEAR("moveClear", MovementDisplay.CMD_INF),
    MOVE_CANCEL("moveCancel", MovementDisplay.CMD_NONE),
    MOVE_RAISE_ELEVATION("moveRaiseElevation", MovementDisplay.CMD_NON_VECTORED),
    MOVE_LOWER_ELEVATION("moveLowerElevation", MovementDisplay.CMD_NON_VECTORED),
    MOVE_SEARCHLIGHT("moveSearchlight", MovementDisplay.CMD_GROUND),
    MOVE_LAY_MINE("moveLayMine",
          MovementDisplay.CMD_MEK | MovementDisplay.CMD_TANK | MovementDisplay.CMD_INF | MovementDisplay.CMD_PROTOMEK),
    MOVE_HULL_DOWN("moveHullDown", MovementDisplay.CMD_MEK | MovementDisplay.CMD_TANK),
    MOVE_CLIMB_MODE("moveClimbMode",
          MovementDisplay.CMD_MEK | MovementDisplay.CMD_TANK | MovementDisplay.CMD_INF | MovementDisplay.CMD_PROTOMEK),
    MOVE_SWIM("moveSwim", MovementDisplay.CMD_MEK),
    MOVE_SHAKE_OFF("moveShakeOff", MovementDisplay.CMD_TANK | MovementDisplay.CMD_VTOL),
    MOVE_BRACE("moveBrace", MovementDisplay.CMD_MEK),
    MOVE_CHAFF("moveChaff", MovementDisplay.CMD_NON_INF),

    // Convert command to a single button, which can cycle through modes because MovePath state is available
    MOVE_MODE_CONVERT("moveModeConvert", MovementDisplay.CMD_CONVERTER),

    // Convert commands used for menus, where the MovePath state is unknown.
    MOVE_MODE_LEG("moveModeLeg", MovementDisplay.CMD_NO_BUTTON),
    MOVE_MODE_VEE("moveModeVee", MovementDisplay.CMD_NO_BUTTON),
    MOVE_MODE_AIR("moveModeAir", MovementDisplay.CMD_NO_BUTTON),
    MOVE_RECKLESS("moveReckless", MovementDisplay.CMD_MEK | MovementDisplay.CMD_TANK | MovementDisplay.CMD_VTOL),
    MOVE_CAREFUL_STAND("moveCarefulStand", MovementDisplay.CMD_NONE),
    MOVE_EVADE("MoveEvade", MovementDisplay.CMD_MEK | MovementDisplay.CMD_TANK | MovementDisplay.CMD_VTOL),
    MOVE_BOOTLEGGER("moveBootlegger", MovementDisplay.CMD_TANK | MovementDisplay.CMD_VTOL),
    MOVE_SHUTDOWN("moveShutDown", MovementDisplay.CMD_NON_INF),
    MOVE_STARTUP("moveStartup", MovementDisplay.CMD_NON_INF),
    MOVE_SELF_DESTRUCT("moveSelfDestruct", MovementDisplay.CMD_NON_INF),

    // Infantry only
    MOVE_DIG_IN("moveDigIn", MovementDisplay.CMD_INF),
    MOVE_FORTIFY("moveFortify", MovementDisplay.CMD_INF | MovementDisplay.CMD_TANK),
    MOVE_TAKE_COVER("moveTakeCover", MovementDisplay.CMD_INF),
    MOVE_CALL_SUPPORT("moveCallSupport", MovementDisplay.CMD_INF),

    // VTOL attacks, declared in the movement phase
    MOVE_STRAFE("moveStrafe", MovementDisplay.CMD_VTOL),
    MOVE_BOMB("moveBomb", MovementDisplay.CMD_VTOL | MovementDisplay.CMD_AIR_MEK),

    // Aero Movement
    MOVE_ACC("MoveAccelerate", MovementDisplay.CMD_AERO),
    MOVE_DEC("MoveDecelerate", MovementDisplay.CMD_AERO),
    MOVE_EVADE_AERO("MoveEvadeAero", MovementDisplay.CMD_AERO_BOTH),
    MOVE_ACCELERATION("MoveAccNext", MovementDisplay.CMD_AERO),
    MOVE_DECELERATION("MoveDecNext", MovementDisplay.CMD_AERO),
    MOVE_ROLL("MoveRoll", MovementDisplay.CMD_AERO_BOTH),
    MOVE_LAUNCH("MoveLaunch", MovementDisplay.CMD_AERO_BOTH),
    MOVE_DOCK("MoveDock", MovementDisplay.CMD_AERO_BOTH),
    MOVE_RECOVER("MoveRecover", MovementDisplay.CMD_AERO_BOTH),
    MOVE_DROP("MoveDrop", MovementDisplay.CMD_AERO_BOTH),
    MOVE_DUMP("MoveDump", MovementDisplay.CMD_AERO_BOTH),
    MOVE_RAM("MoveRam", MovementDisplay.CMD_AERO_BOTH | MovementDisplay.CMD_AIR_MEK),
    MOVE_HOVER("MoveHover", MovementDisplay.CMD_AERO | MovementDisplay.CMD_AIR_MEK),
    MOVE_MANEUVER("MoveManeuver", MovementDisplay.CMD_AERO_BOTH),
    MOVE_JOIN("MoveJoin", MovementDisplay.CMD_AERO_BOTH),
    MOVE_FLY_OFF("MoveOff", MovementDisplay.CMD_AERO_BOTH),
    MOVE_TAKE_OFF("MoveTakeOff", MovementDisplay.CMD_TANK),
    MOVE_VERT_TAKE_OFF("MoveVertTakeOff", MovementDisplay.CMD_TANK),
    MOVE_LAND("MoveLand", MovementDisplay.CMD_AERO_BOTH),
    MOVE_VERT_LAND("MoveVLand", MovementDisplay.CMD_AERO_BOTH),
    // Aero Vector Movement
    MOVE_TURN_LEFT("MoveTurnLeft", MovementDisplay.CMD_AERO_VECTORED),
    MOVE_TURN_RIGHT("MoveTurnRight", MovementDisplay.CMD_AERO_VECTORED),
    MOVE_THRUST("MoveThrust", MovementDisplay.CMD_AERO_VECTORED),
    MOVE_YAW("MoveYaw", MovementDisplay.CMD_AERO_VECTORED),
    MOVE_END_OVER("MoveEndOver", MovementDisplay.CMD_AERO_VECTORED),
    // Move envelope
    MOVE_ENVELOPE("MoveEnvelope", MovementDisplay.CMD_NONE),
    MOVE_LONGEST_RUN("MoveLongestRun", MovementDisplay.CMD_NONE),
    MOVE_LONGEST_WALK("MoveLongestWalk", MovementDisplay.CMD_NONE),
    // Traitor
    MOVE_TRAITOR("Traitor", MovementDisplay.CMD_NONE),
    MOVE_PICKUP_CARGO("movePickupCargo", MovementDisplay.CMD_MEK | MovementDisplay.CMD_PROTOMEK),
    MOVE_DROP_CARGO("moveDropCargo", MovementDisplay.CMD_MEK | MovementDisplay.CMD_PROTOMEK),
    MOVE_MORE("MoveMore", MovementDisplay.CMD_NONE);

    /**
     * The command text.
     */
    public final String cmd;

    /**
     * Flag that determines what unit types can use a command.
     */
    public final int flag;

    /**
     * Priority that determines this button order
     */
    public int priority;

    MoveCommand(String commandString, int commandFlag) {
        cmd = commandString;
        flag = commandFlag;
        priority = ordinal();
    }

    @Override
    public String getCmd() {
        return cmd;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int newPriority) {
        priority = newPriority;
    }

    @Override
    public String toString() {
        return Messages.getString("MovementDisplay." + getCmd());
    }

    public String getHotKeyDesc() {
        String result;

        result = "<BR>";

        switch (this) {
            case MOVE_NEXT:
                String msgNext = Messages.getString("Next");
                String msgPrevious = Messages.getString("Previous");

                result += "&nbsp;&nbsp;" + msgNext + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_UNIT);
                result += "&nbsp;&nbsp;" + msgPrevious + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_UNIT);
                break;
            case MOVE_WALK, MOVE_JUMP:
                String msgToggleMoveJump = Messages.getString("MovementDisplay.tooltip.ToggleMoveJump");

                result += "&nbsp;&nbsp;" +
                      msgToggleMoveJump +
                      ": " +
                      KeyCommandBind.getDesc(KeyCommandBind.TOGGLE_MOVE_MODE);
                break;
            case MOVE_BACK_UP:
                result += KeyCommandBind.getDesc(KeyCommandBind.MOVE_BACKUP);
                break;
            case MOVE_GO_PRONE:
                result += KeyCommandBind.getDesc(KeyCommandBind.MOVE_GO_PRONE);
                break;
            case MOVE_GET_UP:
                result += KeyCommandBind.getDesc(KeyCommandBind.MOVE_GETUP);
                break;
            case MOVE_TURN:
                String msgLeft = Messages.getString("Left");
                String msgRight = Messages.getString("Right");

                result += "&nbsp;&nbsp;" + msgLeft + ": " + KeyCommandBind.getDesc(KeyCommandBind.TURN_LEFT);
                result += "&nbsp;&nbsp;" + msgRight + ": " + KeyCommandBind.getDesc(KeyCommandBind.TURN_RIGHT);
                break;
            case MOVE_MODE_AIR:
            case MOVE_MODE_CONVERT:
            case MOVE_MODE_LEG:
            case MOVE_MODE_VEE:
                String msgToggleMode = Messages.getString("MovementDisplay.tooltip.ToggleMode");

                result += "&nbsp;&nbsp;" +
                      msgToggleMode +
                      ": " +
                      KeyCommandBind.getDesc(KeyCommandBind.TOGGLE_CONVERSION_MODE);
                break;
            default:
                break;
        }

        return result;
    }

    /**
     * Return a list of valid commands for the given parameters.
     *
     * @param unitFlag   The unit flag to specify what unit type the commands are for.
     * @param opts       A {@link GameOptions} reference for checking game options
     * @param forwardIni A flag to see if we can pass the turn to a teammate
     *
     * @return An array of valid commands for the given parameters
     */
    public static MoveCommand[] values(int unitFlag, GameOptions opts, boolean forwardIni) {
        boolean selfDestruct = false;
        boolean advVehicle = false;
        boolean vtolStrafe = false;

        if (opts != null) {
            selfDestruct = opts.booleanOption(OptionsConstants.ADVANCED_TAC_OPS_SELF_DESTRUCT);
            advVehicle = opts.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ADVANCED_MANEUVERS);
            vtolStrafe = opts.booleanOption(OptionsConstants.ADVANCED_COMBAT_VTOL_STRAFING);
        }

        ArrayList<MoveCommand> flaggedCommands = new ArrayList<>();
        for (MoveCommand command : MoveCommand.values()) {
            // Check for movements that with disabled game options
            if ((command == MOVE_SELF_DESTRUCT) && !selfDestruct) {
                continue;
            } else if ((command == MOVE_FORWARD_INI) && !forwardIni) {
                continue;
            } else if ((command == MOVE_BOOTLEGGER) && !advVehicle) {
                continue;
            } else if ((command == MOVE_STRAFE) && !vtolStrafe) {
                continue;
            }

            // Check a unit type flag
            if ((command.flag & unitFlag) != 0) {
                flaggedCommands.add(command);
            }
        }
        return flaggedCommands.toArray(new MoveCommand[0]);
    }
}
