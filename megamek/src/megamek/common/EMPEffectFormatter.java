/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import megamek.MMConstants;

/**
 * Shared utility class for formatting EMP-type effect names for display.
 * <p>
 * Used by both TSEMP weapons and EMP mines to provide consistent visual feedback for Interference, Shutdown, and No
 * Effect results.
 * </p>
 */
public final class EMPEffectFormatter {

    /** HTML color code for shutdown effect (red) */
    private static final String SHUTDOWN_COLOR = "C00000";

    private EMPEffectFormatter() {
        // Utility class - prevent instantiation
    }

    /**
     * Formats an effect constant into an HTML-styled string for report display.
     * <p>
     * Formatting:
     * <ul>
     *   <li>Shutdown: Red bold text</li>
     *   <li>Interference: Bold text</li>
     *   <li>No Effect: Plain text</li>
     * </ul>
     * <p>
     * Note: EMP_EFFECT_* and TSEMP_EFFECT_* constants have identical values,
     * so this method works for both EMP mines and TSEMP weapons.
     * </p>
     *
     * @param effect The effect constant (MMConstants.EMP_EFFECT_* or TSEMP_EFFECT_*)
     *
     * @return HTML-formatted effect string for use in reports
     */
    public static String formatEffect(int effect) {
        // EMP_EFFECT_* and TSEMP_EFFECT_* have identical values (0, 1, 2)
        return switch (effect) {
            case MMConstants.EMP_EFFECT_SHUTDOWN -> "<font color='" + SHUTDOWN_COLOR + "'><b>Shutdown!</b></font>";
            case MMConstants.EMP_EFFECT_INTERFERENCE -> "<b>Interference!</b>";
            default -> "No Effect!";
        };
    }

    /**
     * Gets the plain text effect name without HTML formatting.
     *
     * @param effect The effect constant (MMConstants.EMP_EFFECT_* or TSEMP_EFFECT_*)
     *
     * @return Plain text effect name
     */
    public static String getEffectName(int effect) {
        // EMP_EFFECT_* and TSEMP_EFFECT_* have identical values (0, 1, 2)
        return switch (effect) {
            case MMConstants.EMP_EFFECT_SHUTDOWN -> "Shutdown";
            case MMConstants.EMP_EFFECT_INTERFERENCE -> "Interference";
            default -> "No Effect";
        };
    }

    /**
     * Formats a roll result with optional modifier for report display.
     * <p>
     * Examples:
     * <ul>
     *   <li>No modifier: "8"</li>
     *   <li>With modifier: "8 (+2)"</li>
     * </ul>
     *
     * @param rollValue           The dice roll result
     * @param modifier            The modifier applied (0 for no modifier)
     * @param modifierDescription Optional description for the modifier (e.g., "drone", "weight")
     *
     * @return Formatted roll string
     */
    public static String formatRoll(int rollValue, int modifier, String modifierDescription) {
        if (modifier == 0) {
            return String.valueOf(rollValue);
        }
        String modSign = modifier > 0 ? "+" : "";
        if (modifierDescription != null && !modifierDescription.isEmpty()) {
            return rollValue + " (" + modSign + modifier + " " + modifierDescription + ")";
        }
        return rollValue + " (" + modSign + modifier + ")";
    }

    /**
     * Formats a roll result without modifier.
     *
     * @param rollValue The dice roll result
     *
     * @return Formatted roll string
     */
    public static String formatRoll(int rollValue) {
        return formatRoll(rollValue, 0, null);
    }
}
