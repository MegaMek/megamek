/*
 * Copyright (c) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.enums;

import java.util.HashMap;
import java.util.Map;

public enum MoveStepType {
    NONE(false, "???"),
    FORWARDS(true, "F"),
    BACKWARDS(true, "B"),
    TURN_LEFT(false, "L"),
    TURN_RIGHT(false, "R"),
    GET_UP(false, "Up"),
    GO_PRONE(false, "Prone"),
    START_JUMP(false, "StrJump"),
    JUMP_MEK_MECHANICAL_BOOSTER(false, "MekMJB"),
    CHARGE(false, "Ch"),
    DFA(false, "DFA"),
    FLEE(false, "Flee"),
    LATERAL_LEFT(true, "ShL"),
    LATERAL_RIGHT(true, "ShR"),
    LATERAL_LEFT_BACKWARDS(true, "ShLB"),
    LATERAL_RIGHT_BACKWARDS(true, "ShRB"),
    UNJAM_RAC(false, "Unjam"),
    LOAD(false, "Load"),
    UNLOAD(false, "Unload"),
    EJECT(false, "Eject"),
    ABANDON(false, "Abandon"),
    LAUNCH_ESCAPE_POD(false, "LaunchEscapePod"),
    CLEAR_MINEFIELD(false, "ClearMinefield"),
    UP(false, "U"),
    DOWN(false, "D"),
    SEARCHLIGHT(false, "SLight"),
    LAY_MINE(false, "LayMine"),
    HULL_DOWN(false, "HullDown"),
    CLIMB_MODE_ON(false, "CM+"),
    CLIMB_MODE_OFF(false, "CM-"),
    SWIM(false, "Swim"),
    DIG_IN(false, "DigIn"),
    FORTIFY(false, "Fortify"),
    SHAKE_OFF_SWARMERS(false, "ShakeOffSwarmers"),
    TAKEOFF(false, "Takeoff"),
    VERTICAL_TAKE_OFF(false, "Vertical Takeoff"),
    LAND(false, "Landing"),
    ACC(false, "Acc"),
    DEC(false, "Dec"),
    EVADE(false, "Evade"),
    SHUTDOWN(false, "Shutdown"),
    STARTUP(false, "Startup"),
    SELF_DESTRUCT(false, "SelfDestruct"),
    ACCELERATION(false, "AccN"),
    DECELERATION(false, "DecN"),
    ROLL(false, "Roll"),
    OFF(false, "Fly Off"),
    RETURN(false, "Fly Off (Return)"),
    LAUNCH(false, "Launch"),
    THRUST(false, "Thrust"),
    YAW(false, "Yaw"),
    CRASH(false, "Crash"),
    RECOVER(false, "Recover"),
    RAM(false, "Ram"),
    HOVER(false, "Hover"),
    MANEUVER(false, "Maneuver"),
    LOOP(false, "Loop"),
    CAREFUL_STAND(false, "Up"),  // note: same human-readable label as GET_UP!!!
    JOIN(false, "Join"),
    DROP(false, "Drop"),
    VERTICAL_LAND(false, "Vertical Landing"),
    MOUNT(false, "Mount"),
    UNDOCK(false, "Undock"),
    TAKE_COVER(false, "TakeCover"),
    CONVERT_MODE(false, "ConvMode"),
    BOOTLEGGER(false, "Bootlegger"),
    TOW(false, "Tow"),
    DISCONNECT(false, "Disconnect"),
    BRACE(false, "Brace"),
    CHAFF(false, "Chaff"),
    PICKUP_CARGO(false, "Pickup Cargo"),
    DROP_CARGO(false, "Drop Cargo"),
    CHANGE_BOARD(true, "Change Board");

    private final boolean entersNewHex;
    private final String humanReadableLabel;

    // Constructor for the enum constants
    MoveStepType(boolean entersNewHex, String humanReadableLabel) {
        this.entersNewHex = entersNewHex;
        this.humanReadableLabel = humanReadableLabel;
    }

    /**
     * Returns whether this move step causes the unit to enter a new hex.
     */
    public boolean entersNewHex() {
        return entersNewHex;
    }

    /**
     * Returns the human‑readable label for this move step.
     */
    public String getHumanReadableLabel() {
        return humanReadableLabel;
    }

    // Reverse lookup map from human-readable label to enum constant.
    private static final Map<String, MoveStepType> LABEL_TO_ENUM = new HashMap<>();

    static {
        for (MoveStepType type : values()) {
            // If duplicate labels exist (e.g. GET_UP and CAREFUL_STAND both return "Up"),
            // only the first one encountered will be stored.
            LABEL_TO_ENUM.putIfAbsent(type.getHumanReadableLabel(), type);
        }
    }

    /**
     * Returns the MoveStepType corresponding to the given human-readable label.
     *
     * @param label the label to look up (e.g., "F", "Up", "L", etc.)
     *
     * @return the corresponding MoveStepType
     *
     * @throws IllegalArgumentException if no matching type is found
     */
    public static MoveStepType fromLabel(String label) {
        MoveStepType type = LABEL_TO_ENUM.get(label);
        if (type == null) {
            type = valueOf(label);
        }
        return type;
    }

    /**
     * Returns a step type to use for simple path finding, given the direction to the destination (0 to 5) and the
     * facing of the unit (0 to 5). The step type can be used to get closer to the destination. Used only for Mek
     * Mechanical Jump Booster path finding.
     *
     * @param direction The direction to the destination, 0 = N, 4 = SW etc
     * @param facing    The facing of the unit
     *
     * @return A step type to use to get closer to the destination
     */
    public static MoveStepType stepTypeForRelativeDirection(int direction, int facing) {
        return switch ((6 + direction - facing) % 6) {
            case 1 -> MoveStepType.LATERAL_RIGHT;
            // TODO: backwards lateral shifts are switched:
            // LATERAL_LEFT_BACKWARDS moves back+right and vice-versa
            case 2 -> MoveStepType.LATERAL_LEFT_BACKWARDS;
            case 3 -> MoveStepType.BACKWARDS;
            // TODO: backwards lateral shifts are switched:
            // LATERAL_RIGHT_BACKWARDS moves back+left and vice-versa
            case 4 -> MoveStepType.LATERAL_RIGHT_BACKWARDS;
            case 5 -> MoveStepType.LATERAL_LEFT;
            default -> MoveStepType.FORWARDS;
        };
    }
}
