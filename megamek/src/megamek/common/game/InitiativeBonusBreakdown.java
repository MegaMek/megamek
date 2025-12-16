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
     * Returns the total initiative bonus applying proper stacking rules.
     * <p>
     * Per Xotl ruling (https://battletech.com/forums/index.php?topic=85848.0):
     * <ul>
     *   <li>Negative modifiers stack cumulatively (no floor limit)</li>
     *   <li>Positive modifiers do NOT stack (only the highest one applies)</li>
     * </ul>
     *
     * @return The total initiative bonus with stacking rules applied
     */
    public int total() {
        int[] components = { hq, quirk, console, crewCommand, tcp, constant, compensation, crew };

        int negativeSum = 0;
        int highestPositive = 0;

        for (int component : components) {
            if (component < 0) {
                negativeSum += component;
            } else if (component > highestPositive) {
                highestPositive = component;
            }
        }

        return negativeSum + highestPositive;
    }

    /**
     * Returns the simple sum of all components without stacking rules. Use this for display purposes where you want to
     * show raw values.
     *
     * @return The raw sum of all bonus components
     */
    public int rawTotal() {
        return hq + quirk + console + crewCommand + tcp + constant + compensation + crew;
    }

    /**
     * Returns a formatted string showing all bonus components, e.g., "+3 TCP, +1 Command Mek".
     * Components are sorted from highest to lowest value, so the applied positive modifier
     * (the highest one) appears first. When multiple positive modifiers exist, appends
     * "(using highest modifier only)" to clarify that only the highest positive is applied.
     *
     * @return A formatted breakdown string, or "0" if no components have values
     */
    public String toBreakdownString() {
        // Collect all non-zero components with their values and labels
        List<int[]> components = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        if (hq != 0) {
            components.add(new int[] { hq, components.size() });
            labels.add("HQ");
        }
        if (quirk != 0) {
            components.add(new int[] { quirk, components.size() });
            String quirkLabel = (quirkName != null && !quirkName.isEmpty()) ? quirkName : "Quirk";
            labels.add(quirkLabel);
        }
        if (console != 0) {
            components.add(new int[] { console, components.size() });
            labels.add("Console");
        }
        if (crewCommand != 0) {
            components.add(new int[] { crewCommand, components.size() });
            labels.add("Cmd");
        }
        if (tcp != 0) {
            components.add(new int[] { tcp, components.size() });
            labels.add("TCP");
        }
        if (constant != 0) {
            components.add(new int[] { constant, components.size() });
            labels.add("Base");
        }
        if (compensation != 0) {
            components.add(new int[] { compensation, components.size() });
            labels.add("Comp");
        }
        if (crew != 0) {
            components.add(new int[] { crew, components.size() });
            labels.add("Crew");
        }

        if (components.isEmpty()) {
            return "0";
        }

        // Sort by value descending (highest first)
        components.sort((a, b) -> Integer.compare(b[0], a[0]));

        // Count positive modifiers first
        int positiveCount = 0;
        for (int[] comp : components) {
            if (comp[0] > 0) {
                positiveCount++;
            }
        }

        // Build the result string, bolding the highest positive when there are multiple
        List<String> parts = new ArrayList<>();
        boolean firstPositiveBolded = false;
        for (int[] comp : components) {
            int value = comp[0];
            int index = comp[1];
            String formatted = formatComponent(value, labels.get(index));
            // Bold the first (highest) positive only when there are multiple positives
            if (value > 0 && !firstPositiveBolded && positiveCount > 1) {
                formatted = "<b>" + formatted + "</b>";
                firstPositiveBolded = true;
            }
            parts.add(formatted);
        }

        String result = String.join(", ", parts);
        if (positiveCount > 1) {
            result += " (using highest modifier only)";
        }
        return result;
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
     * This method combines all raw values. The stacking rules (negatives stack cumulatively, only highest positive
     * applies) are applied by {@link #total()} when calculating the actual initiative bonus.
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
