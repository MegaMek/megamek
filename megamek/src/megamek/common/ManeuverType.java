/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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


package megamek.common;

import java.text.MessageFormat;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Board;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;

/**
 * Maneuver types for Aerospace
 */
public class ManeuverType {

    public static final int MAN_NONE = 0;
    public static final int MAN_LOOP = 1;
    public static final int MAN_IMMELMAN = 2;
    public static final int MAN_SPLIT_S = 3;
    public static final int MAN_HAMMERHEAD = 4;
    public static final int MAN_HALF_ROLL = 5;
    public static final int MAN_BARREL_ROLL = 6;
    public static final int MAN_SIDE_SLIP_LEFT = 7;
    public static final int MAN_SIDE_SLIP_RIGHT = 8;
    public static final int MAN_VIFF = 9;

    private static final String[] names = { "None", "Loop", "Immelman", "Split S", "Hammerhead", "Half Roll",
                                            "Barrel Roll", "Side Slip (Left)", "Side Slip (Right)", "VIFF" };

    public static final int MAN_SIZE = names.length;

    public static String getTypeName(int type) {
        if ((type >= MAN_NONE) && (type < MAN_SIZE)) {
            return names[type];
        }
        throw new IllegalArgumentException("Unknown maneuver type");
    }

    /**
     * determines whether the maneuver can be performed
     */
    public static boolean canPerform(int type, int velocity, int altitude,
          int ceiling, boolean isVTOL, int distance,
          Board board, MovePath mp) {

        // We can only perform one maneuver in a turn (important for side-slip)
        for (final MoveStep step : mp.getStepVector()) {
            if (step.getType() == MoveStepType.MANEUVER) {
                return false;
            }
        }

        // Side-slip is the only maneuver that doesn't have to be at the start
        if ((distance > 0) && (type != MAN_SIDE_SLIP_LEFT)
              && (type != MAN_SIDE_SLIP_RIGHT)) {
            return false;
        }

        switch (type) {
            case MAN_NONE:
            case MAN_HAMMERHEAD:
            case MAN_HALF_ROLL:
                return true;
            case MAN_LOOP:
                return velocity >= 4;
            case MAN_IMMELMAN:
                return (velocity >= 3) && (altitude < 9);
            case MAN_SPLIT_S:
                return (altitude + 2) > ceiling;
            case MAN_BARREL_ROLL:
                return velocity >= 2;
            case MAN_SIDE_SLIP_LEFT:
            case MAN_SIDE_SLIP_RIGHT:
                if (velocity > 0) {
                    // If we're on a ground map, we need to make sure we can move
                    //  all 16 hexes
                    if (board.isGround()) {
                        MovePath tmpMp = mp.clone();
                        for (int i = 0; i < 8; i++) {
                            if (type == MAN_SIDE_SLIP_LEFT) {
                                tmpMp.addStep(MoveStepType.LATERAL_LEFT, true, true, type);
                            } else {
                                tmpMp.addStep(MoveStepType.LATERAL_RIGHT, true, true, type);
                            }
                        }
                        for (int i = 0; i < 8; i++) {
                            tmpMp.addStep(MoveStepType.FORWARDS, true, true, type);
                        }
                        return tmpMp.getLastStep().isLegal(tmpMp);
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            case MAN_VIFF:
                return isVTOL;
            default:
                return false;
        }
    }

    /**
     * Thrust cost of maneuver
     */
    public static int getCost(int type, int velocity) {
        return switch (type) {
            case MAN_LOOP, MAN_IMMELMAN -> 4;
            case MAN_SPLIT_S -> 2;
            case MAN_HAMMERHEAD -> velocity;
            case MAN_HALF_ROLL, MAN_BARREL_ROLL, MAN_SIDE_SLIP_LEFT, MAN_SIDE_SLIP_RIGHT -> 1;
            case MAN_VIFF -> velocity + 2;
            default -> 0;
        };
    }

    /**
     * Returns the Control Roll modifier for a particular maneuver.
     *
     * @param type       The type of maneuver performed
     * @param isVSTOL_CF Flag that determines whether the maneuvering unit is a conventional fighter with VSTOl, which
     *                   has effects for side-slips
     *
     * @return The control roll modifier
     */
    public static int getMod(int type, boolean isVSTOL_CF) {
        return switch (type) {
            case MAN_LOOP, MAN_IMMELMAN -> 1;
            case MAN_SPLIT_S, MAN_VIFF -> 2;
            case MAN_HAMMERHEAD -> 3;
            case MAN_HALF_ROLL -> -1;
            case MAN_SIDE_SLIP_LEFT, MAN_SIDE_SLIP_RIGHT -> isVSTOL_CF ? -1 : 0;
            default -> 0;
        };
    }

    /**
     * Generates an HTML-formatted tooltip for the specified maneuver type showing requirements,
     * thrust cost, control modifier, and effect. Uses localized strings from messages.properties.
     *
     * @param type       The maneuver type constant from ManeuverType
     * @param canPerform Whether the maneuver can currently be performed
     * @param velocity   Current unit velocity
     * @param altitude   Current unit altitude
     * @param isVSTOL_CF Whether the entity is a VSTOL with Improved Avionics
     * @return HTML-formatted tooltip string
     */
    public static String getManeuverTooltip(int type, boolean canPerform, int velocity, int altitude,
                                            boolean isVSTOL_CF) {
        StringBuilder tooltip = new StringBuilder("<HTML><BODY>");

        // Maneuver name header
        tooltip.append(UIUtil.fontHTML(UIUtil.uiLightViolet()))
               .append("<B>").append(getTypeName(type)).append("</B></FONT><BR>");

        // Get the maneuver name for resource key
        String maneuverKey = getManeuverResourceKey(type);

        // For "None" maneuver, just show simple message
        if (type == MAN_NONE) {
            tooltip.append(Messages.getString("ManeuverChoiceDialog.None.requirements"));
            tooltip.append("</BODY></HTML>");
            return tooltip.toString();
        }

        // Requirements
        tooltip.append(Messages.getString("ManeuverChoiceDialog.requirementsLabel")).append(": ");
        tooltip.append(Messages.getString("ManeuverChoiceDialog." + maneuverKey + ".requirements"));

        // Add current value indicators for unavailable maneuvers
        if (!canPerform) {
            tooltip.append(UIUtil.fontHTML(UIUtil.uiLightRed()));
            switch (type) {
                case MAN_LOOP:
                    if (velocity < 4) {
                        tooltip.append(" ").append(MessageFormat.format(
                            Messages.getString("ManeuverChoiceDialog.currentValue"), velocity));
                    }
                    break;
                case MAN_IMMELMAN:
                    if ((velocity < 3) || (altitude >= 9)) {
                        tooltip.append(" ").append(MessageFormat.format(
                            Messages.getString("ManeuverChoiceDialog.currentValues"), velocity, altitude));
                    }
                    break;
                case MAN_BARREL_ROLL:
                    if (velocity < 2) {
                        tooltip.append(" ").append(MessageFormat.format(
                            Messages.getString("ManeuverChoiceDialog.currentValue"), velocity));
                    }
                    break;
                case MAN_SIDE_SLIP_LEFT:
                case MAN_SIDE_SLIP_RIGHT:
                    if (velocity <= 0) {
                        tooltip.append(" ").append(MessageFormat.format(
                            Messages.getString("ManeuverChoiceDialog.currentValue"), velocity));
                    }
                    break;
                case MAN_VIFF:
                    tooltip.append(" ").append(Messages.getString("ManeuverChoiceDialog.notVSTOL"));
                    break;
            }
            tooltip.append("</FONT>");
        }
        tooltip.append("<BR>");

        // Thrust Cost
        tooltip.append(Messages.getString("ManeuverChoiceDialog.thrustCostLabel")).append(": ");
        String thrustCost = Messages.getString("ManeuverChoiceDialog." + maneuverKey + ".thrustCost");
        int cost = getCost(type, velocity);
        if ((type == MAN_HAMMERHEAD) || (type == MAN_VIFF)) {
            tooltip.append(MessageFormat.format(thrustCost, cost));
        } else {
            tooltip.append(thrustCost);
        }
        tooltip.append("<BR>");

        // Control Modifier
        tooltip.append(Messages.getString("ManeuverChoiceDialog.controlModLabel")).append(": ");
        String controlMod = Messages.getString("ManeuverChoiceDialog." + maneuverKey + ".controlMod");
        if ((type == MAN_SIDE_SLIP_LEFT) || (type == MAN_SIDE_SLIP_RIGHT)) {
            int mod = getMod(type, isVSTOL_CF);
            String modStr = (mod >= 0) ? ("+" + mod) : String.valueOf(mod);
            tooltip.append(MessageFormat.format(controlMod, modStr));
        } else {
            tooltip.append(controlMod);
        }
        tooltip.append("<BR>");

        // Effect
        tooltip.append(Messages.getString("ManeuverChoiceDialog.effectLabel")).append(": ");
        tooltip.append(Messages.getString("ManeuverChoiceDialog." + maneuverKey + ".effect"));

        tooltip.append("</BODY></HTML>");
        return tooltip.toString();
    }

    /**
     * Gets the resource key suffix for a maneuver type for use in loading localized strings.
     *
     * @param type The maneuver type constant
     * @return The resource key suffix (e.g., "Loop", "Immelman", "SplitS")
     */
    private static String getManeuverResourceKey(int type) {
        return switch (type) {
            case MAN_NONE -> "None";
            case MAN_LOOP -> "Loop";
            case MAN_IMMELMAN -> "Immelman";
            case MAN_SPLIT_S -> "SplitS";
            case MAN_HAMMERHEAD -> "Hammerhead";
            case MAN_HALF_ROLL -> "HalfRoll";
            case MAN_BARREL_ROLL -> "BarrelRoll";
            case MAN_SIDE_SLIP_LEFT -> "SideSlipLeft";
            case MAN_SIDE_SLIP_RIGHT -> "SideSlipRight";
            case MAN_VIFF -> "VIFF";
            default -> "None";
        };
    }
}
