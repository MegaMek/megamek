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
package megamek.common.game;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the breakdown of initiative bonus components for display purposes. This allows the initiative report to show
 * what contributes to the total bonus (e.g., HQ bonus, TCP implant, quirks, etc.) rather than just a single number.
 *
 * @since 2025-12-15
 */
public record InitiativeBonusBreakdown(
      int hq,           // Mobile HQ bonus (TacOps option)
      int quirk,        // Quirk bonus (e.g., Improved Communications)
      String quirkName, // Name of the quirk providing the bonus (e.g., "Command Mek", "Battle Computer")
      int console,      // Command console or tech officer bonus (+2)
      int crewCommand,  // Crew command skill bonus (RPG option)
      int tcp,          // Triple Core Processor implant bonus
      int constant,     // Player's constant init bonus
      int compensation, // Initiative compensation bonus
      int crew          // Individual crew init bonus (for individual initiative mode)
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;

    /**
     * Creates a breakdown with all zeros.
     */
    public static InitiativeBonusBreakdown zero() {
        return new InitiativeBonusBreakdown(0, 0, null, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Creates a breakdown from a single total value (for backwards compatibility). The entire value is assigned to the
     * "constant" component.
     */
    public static InitiativeBonusBreakdown fromTotal(int total) {
        return new InitiativeBonusBreakdown(0, 0, null, 0, 0, 0, total, 0, 0);
    }

    /**
     * @return The total of all bonus components
     */
    // TODO: Per Xotl ruling (https://battletech.com/forums/index.php?topic=85848.0):
    //       - Negative modifiers stack cumulatively (no floor limit)
    //       - Positive modifiers do NOT stack (only use the highest one)
    //       Currently this sums all components. Fix before merge to main.
    public int total() {
        return hq + quirk + console + crewCommand + tcp + constant + compensation + crew;
    }

    /**
     * @return A formatted string showing the breakdown of bonuses, e.g., "+2 HQ, +1 TCP" Returns "0" if total is 0.
     */
    public String toBreakdownString() {
        if (total() == 0) {
            return "0";
        }

        List<String> parts = new ArrayList<>();

        if (hq != 0) {
            parts.add(formatComponent(hq, "HQ"));
        }
        if (quirk != 0) {
            String quirkLabel = (quirkName != null && !quirkName.isEmpty()) ? quirkName : "Quirk";
            parts.add(formatComponent(quirk, quirkLabel));
        }
        if (console != 0) {
            parts.add(formatComponent(console, "Console"));
        }
        if (crewCommand != 0) {
            parts.add(formatComponent(crewCommand, "Cmd"));
        }
        if (tcp != 0) {
            parts.add(formatComponent(tcp, "TCP"));
        }
        if (constant != 0) {
            parts.add(formatComponent(constant, "Base"));
        }
        if (compensation != 0) {
            parts.add(formatComponent(compensation, "Comp"));
        }
        if (crew != 0) {
            parts.add(formatComponent(crew, "Crew"));
        }

        return String.join(", ", parts);
    }

    private String formatComponent(int value, String label) {
        if (value > 0) {
            return "+" + value + " " + label;
        } else {
            return value + " " + label;
        }
    }

    /**
     * Creates a new breakdown by adding another breakdown's values to this one. For quirk names, keeps the name
     * associated with the higher bonus value.
     * <p>
     * Note: This method combines all values for display purposes. The actual initiative calculation
     * should use total() which applies the stacking rules (negatives stack, only highest positive applies).
     */
    public InitiativeBonusBreakdown add(InitiativeBonusBreakdown other) {
        // Use the quirk name from whichever has the higher quirk bonus
        String combinedQuirkName = this.quirk >= other.quirk ? this.quirkName : other.quirkName;

        return new InitiativeBonusBreakdown(
              this.hq + other.hq,
              this.quirk + other.quirk,
              combinedQuirkName,
              this.console + other.console,
              this.crewCommand + other.crewCommand,
              this.tcp + other.tcp,
              this.constant + other.constant,
              this.compensation + other.compensation,
              this.crew + other.crew
        );
    }
}
