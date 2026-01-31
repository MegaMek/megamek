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

package megamek.common.compute;

import megamek.common.InfantryCombatResult;
import megamek.common.InfantryCombatResult.ResultType;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of TOAR Infantry vs. Infantry combat tables.
 *
 * <p>Includes:
 * <ul>
 *   <li>Infantry vs. Infantry Action Table (odds-based 2D6 resolution)</li>
 *   <li>Crew Casualties Table (percentage to hits conversion)</li>
 *   <li>Combat ratio calculation and lookup</li>
 * </ul>
 *
 * <p>These tables are context-agnostic and can be used for both building
 * combat and naval boarding actions.</p>
 */
public class InfantryCombatTables {

    /**
     * Infantry vs. Infantry Action Table from TOAR page 173.
     * Maps combat ratio + 2D6 roll to combat result.
     *
     * <p>Ratio format: "attacker:defender" where higher number is always first
     * in actual ratio (e.g., "3:1" means 3-to-1 advantage)</p>
     */
    private static final Map<String, Map<Integer, InfantryCombatResult>> ACTION_TABLE = new HashMap<>();

    static {
        initializeActionTable();
    }

    /**
     * Initialize the Infantry vs. Infantry Action Table based on TOAR rules.
     */
    private static void initializeActionTable() {
        // Ratio: 1 to 3 < (less than 1:3, severely outnumbered attacker)
        Map<Integer, InfantryCombatResult> ratio1to3less = new HashMap<>();
        ratio1to3less.put(2, InfantryCombatResult.repulsed(1));  // E/1% (R)
        ratio1to3less.put(3, InfantryCombatResult.repulsed(3));  // E/3% (R)
        ratio1to3less.put(4, InfantryCombatResult.repulsed(5));  // E/5% (R)
        ratio1to3less.put(5, InfantryCombatResult.repulsed(7));  // E/7% (R)
        ratio1to3less.put(6, InfantryCombatResult.repulsed(10)); // E/10% (R)
        ratio1to3less.put(7, InfantryCombatResult.repulsed(15)); // E/15% (R)
        ratio1to3less.put(8, InfantryCombatResult.repulsed(15)); // E/15% (R)
        ratio1to3less.put(9, InfantryCombatResult.repulsed(25)); // E/25% (R)
        ratio1to3less.put(10, InfantryCombatResult.repulsed(30)); // E/30% (R)
        ratio1to3less.put(11, InfantryCombatResult.repulsed(35)); // E/35% (R)
        ratio1to3less.put(12, InfantryCombatResult.partial(40, false)); // 30%/40% (P)
        ACTION_TABLE.put("1:3<", ratio1to3less);

        // Ratio: 1 to 3 (1:3, heavily outnumbered)
        Map<Integer, InfantryCombatResult> ratio1to3 = new HashMap<>();
        ratio1to3.put(2, InfantryCombatResult.repulsed(1));   // E/1% (R)
        ratio1to3.put(3, InfantryCombatResult.repulsed(3));   // E/3% (R)
        ratio1to3.put(4, InfantryCombatResult.repulsed(5));   // E/5% (R)
        ratio1to3.put(5, InfantryCombatResult.repulsed(7));   // E/7% (R)
        ratio1to3.put(6, InfantryCombatResult.repulsed(10));  // E/10% (R)
        ratio1to3.put(7, InfantryCombatResult.repulsed(15));  // E/15% (R)
        ratio1to3.put(8, InfantryCombatResult.repulsed(15));  // E/15% (R)
        ratio1to3.put(9, InfantryCombatResult.partial(25, false));  // E/25% (P)
        ratio1to3.put(10, InfantryCombatResult.partial(30, false)); // E/30% (P)
        ratio1to3.put(11, InfantryCombatResult.partial(30, false)); // E/30% (P)
        ratio1to3.put(12, InfantryCombatResult.partial(40, false)); // 25%/40% (P)
        ACTION_TABLE.put("1:3", ratio1to3);

        // Ratio: 1 to 2 (1:2, outnumbered)
        Map<Integer, InfantryCombatResult> ratio1to2 = new HashMap<>();
        ratio1to2.put(2, InfantryCombatResult.repulsed(5));   // E/5% (R)
        ratio1to2.put(3, InfantryCombatResult.repulsed(7));   // E/7% (R)
        ratio1to2.put(4, InfantryCombatResult.repulsed(10));  // E/10% (R)
        ratio1to2.put(5, InfantryCombatResult.repulsed(15));  // E/15% (R)
        ratio1to2.put(6, InfantryCombatResult.repulsed(20));  // E/20% (R)
        ratio1to2.put(7, InfantryCombatResult.partial(25, false));  // E/25% (P)
        ratio1to2.put(8, InfantryCombatResult.partial(30, false));  // E/30% (P)
        ratio1to2.put(9, InfantryCombatResult.partial(40, false));  // 40%/35% (P)
        ratio1to2.put(10, InfantryCombatResult.partial(35, false)); // 35%/40% (P)
        ratio1to2.put(11, InfantryCombatResult.partial(40, false)); // 30%/45% (P)
        ratio1to2.put(12, InfantryCombatResult.partial(50, true));  // 25%/50% (P)
        ACTION_TABLE.put("1:2", ratio1to2);

        // Ratio: 2 to 3 (between 1:1 and 1:1.5, slight disadvantage)
        Map<Integer, InfantryCombatResult> ratio2to3 = new HashMap<>();
        ratio2to3.put(2, InfantryCombatResult.casualties(75, 25));  // 75%/25% (R)
        ratio2to3.put(3, InfantryCombatResult.casualties(70, 30));  // 70%/30% (R)
        ratio2to3.put(4, InfantryCombatResult.casualties(65, 20));  // 65%/20%
        ratio2to3.put(5, InfantryCombatResult.casualties(60, 25));  // 60%/25%
        ratio2to3.put(6, InfantryCombatResult.casualties(55, 30));  // 55%/30%
        ratio2to3.put(7, InfantryCombatResult.casualties(50, 35));  // 50%/35%
        ratio2to3.put(8, InfantryCombatResult.casualties(45, 40));  // 45%/40%
        ratio2to3.put(9, InfantryCombatResult.casualties(40, 45));  // 40%/45%
        ratio2to3.put(10, InfantryCombatResult.casualties(35, 50)); // 35%/50%
        ratio2to3.put(11, InfantryCombatResult.casualties(30, 50)); // 30%/50%
        ratio2to3.put(12, InfantryCombatResult.casualties(25, 60)); // 25%/60% (P)
        ACTION_TABLE.put("2:3", ratio2to3);

        // Ratio: 1 to 1 (even match)
        Map<Integer, InfantryCombatResult> ratio1to1 = new HashMap<>();
        ratio1to1.put(2, InfantryCombatResult.casualties(75, 25));  // 75%/25% (R)
        ratio1to1.put(3, InfantryCombatResult.casualties(70, 30));  // 70%/30%
        ratio1to1.put(4, InfantryCombatResult.casualties(65, 35));  // 65%/35%
        ratio1to1.put(5, InfantryCombatResult.casualties(60, 40));  // 60%/40%
        ratio1to1.put(6, InfantryCombatResult.casualties(55, 45));  // 55%/45%
        ratio1to1.put(7, InfantryCombatResult.casualties(50, 45));  // 50%/45%
        ratio1to1.put(8, InfantryCombatResult.casualties(45, 50));  // 45%/50%
        ratio1to1.put(9, InfantryCombatResult.casualties(40, 60));  // 40%/60%
        ratio1to1.put(10, InfantryCombatResult.casualties(35, 60)); // 35%/60%
        ratio1to1.put(11, InfantryCombatResult.casualties(30, 65)); // 30%/65%
        ratio1to1.put(12, InfantryCombatResult.casualties(25, 75)); // 25%/75% (P)
        ACTION_TABLE.put("1:1", ratio1to1);

        // Ratio: 3 to 2 (between 1:1 and 2:1, slight advantage)
        Map<Integer, InfantryCombatResult> ratio3to2 = new HashMap<>();
        ratio3to2.put(2, InfantryCombatResult.casualties(70, 25));  // 70%/25% (R)
        ratio3to2.put(3, InfantryCombatResult.casualties(65, 30));  // 65%/30%
        ratio3to2.put(4, InfantryCombatResult.casualties(60, 35));  // 60%/35%
        ratio3to2.put(5, InfantryCombatResult.casualties(55, 40));  // 55%/40%
        ratio3to2.put(6, InfantryCombatResult.casualties(50, 45));  // 50%/45%
        ratio3to2.put(7, InfantryCombatResult.casualties(45, 50));  // 45%/50%
        ratio3to2.put(8, InfantryCombatResult.casualties(40, 50));  // 40%/50%
        ratio3to2.put(9, InfantryCombatResult.casualties(35, 60));  // 35%/60%
        ratio3to2.put(10, InfantryCombatResult.casualties(30, 65)); // 30%/65% (P)
        ratio3to2.put(11, InfantryCombatResult.casualties(25, 65)); // 25%/65% (P)
        ratio3to2.put(12, InfantryCombatResult.casualties(20, 70)); // 20%/70% (P)
        ACTION_TABLE.put("3:2", ratio3to2);

        // Ratio: 2 to 1 (2:1, significant advantage)
        Map<Integer, InfantryCombatResult> ratio2to1 = new HashMap<>();
        ratio2to1.put(2, InfantryCombatResult.casualties(65, 25));  // 65%/25% (R)
        ratio2to1.put(3, InfantryCombatResult.casualties(60, 30));  // 60%/30%
        ratio2to1.put(4, InfantryCombatResult.casualties(55, 35));  // 55%/35%
        ratio2to1.put(5, InfantryCombatResult.casualties(50, 40));  // 50%/40%
        ratio2to1.put(6, InfantryCombatResult.casualties(45, 45));  // 45%/45%
        ratio2to1.put(7, InfantryCombatResult.casualties(40, 50));  // 40%/50%
        ratio2to1.put(8, InfantryCombatResult.casualties(35, 55));  // 35%/55%
        ratio2to1.put(9, InfantryCombatResult.casualties(30, 60));  // 30%/60% (P)
        ratio2to1.put(10, InfantryCombatResult.casualties(25, 65)); // 25%/65% (P)
        ratio2to1.put(11, InfantryCombatResult.casualties(20, 70)); // 20%/70% (P)
        ratio2to1.put(12, InfantryCombatResult.casualties(15, 75)); // 15%/75% (P)
        ACTION_TABLE.put("2:1", ratio2to1);

        // Ratio: 3 to 1 (3:1, overwhelming advantage)
        Map<Integer, InfantryCombatResult> ratio3to1 = new HashMap<>();
        ratio3to1.put(2, InfantryCombatResult.casualties(60, 25));  // 60%/25% (R)
        ratio3to1.put(3, InfantryCombatResult.casualties(55, 30));  // 55%/30%
        ratio3to1.put(4, InfantryCombatResult.casualties(50, 35));  // 50%/35%
        ratio3to1.put(5, InfantryCombatResult.casualties(45, 40));  // 45%/40%
        ratio3to1.put(6, InfantryCombatResult.casualties(40, 45));  // 40%/45%
        ratio3to1.put(7, InfantryCombatResult.casualties(35, 50));  // 35%/50%
        ratio3to1.put(8, InfantryCombatResult.casualties(30, 55));  // 30%/55%
        ratio3to1.put(9, InfantryCombatResult.casualties(25, 60));  // 25%/60% (P)
        ratio3to1.put(10, InfantryCombatResult.casualties(20, 60)); // 20%/60% (P)
        ratio3to1.put(11, InfantryCombatResult.casualties(15, 60)); // 15%/60% (P)
        ratio3to1.put(12, InfantryCombatResult.casualties(10, 60)); // 10%/60% (P)
        ACTION_TABLE.put("3:1", ratio3to1);

        // Ratio: > 3 to 1 (more than 3:1, complete dominance)
        Map<Integer, InfantryCombatResult> ratioGT3to1 = new HashMap<>();
        ratioGT3to1.put(2, InfantryCombatResult.casualties(55, 25));  // 55%/25% (R)
        ratioGT3to1.put(3, InfantryCombatResult.casualties(50, 30));  // 50%/30%
        ratioGT3to1.put(4, InfantryCombatResult.casualties(45, 35));  // 45%/35%
        ratioGT3to1.put(5, InfantryCombatResult.casualties(40, 40));  // 40%/40%
        ratioGT3to1.put(6, InfantryCombatResult.casualties(35, 45));  // 35%/45%
        ratioGT3to1.put(7, InfantryCombatResult.casualties(30, 45));  // 30%/45%
        ratioGT3to1.put(8, InfantryCombatResult.casualties(25, 50));  // 25%/50%
        ratioGT3to1.put(9, InfantryCombatResult.casualties(20, 55));  // 20%/55% (P)
        ratioGT3to1.put(10, InfantryCombatResult.casualties(15, 55)); // 15%/55% (P)
        ratioGT3to1.put(11, InfantryCombatResult.casualties(10, 55)); // 10%/55% (P)
        ratioGT3to1.put(12, InfantryCombatResult.casualties(5, 55));  // 5%/55% (P)
        ACTION_TABLE.put(">3:1", ratioGT3to1);
    }

    /**
     * Calculates the combat ratio between attacker and defender MPS.
     *
     * <p>Per TOAR p. 172: "round in favor of the defender" when the ratio doesn't
     * exactly match a table column. Unless the ratio reaches the next tier exactly,
     * it rounds down.</p>
     *
     * <p>Example from TOAR: A ratio of 2.1:1 (attacker 2.1, defender 1.0) equals
     * 2.1, which meets the 2:1 tier threshold (2.0) and uses the 2:1 column.</p>
     *
     * @param attackerMPS attacker Marine Points Score
     * @param defenderMPS defender Marine Points Score
     * @return ratio string for table lookup (e.g., "2:1", "1:3", "1:1")
     */
    public static String calculateRatio(int attackerMPS, int defenderMPS) {
        // Edge cases: zero MPS
        if (attackerMPS <= 0 && defenderMPS <= 0) {
            return "1:1"; // Both zero, treat as even
        }
        if (defenderMPS <= 0) {
            return ">3:1"; // Defender has no strength
        }
        if (attackerMPS <= 0) {
            return "1:3<"; // Attacker has no strength
        }

        double ratio = (double) attackerMPS / (double) defenderMPS;

        // Per TOAR p. 172: "round in favor of the defender"
        // Rule: Unless the ratio reaches the next tier exactly, round down.
        // Standard ratios: 1:3 (1/3), 1:2 (1/2), 2:3 (2/3), 1:1 (1),
        //                  3:2 (3/2), 2:1 (2), 3:1 (3)

        if (ratio >= 3.0) {
            return "3:1";
        } else if (ratio >= 2.0) {
            return "2:1";
        } else if (ratio >= 3.0 / 2.0) {
            return "3:2";
        } else if (ratio >= 1.0) {
            return "1:1";
        } else if (ratio >= 2.0 / 3.0) {
            return "2:3";
        } else if (ratio >= 1.0 / 2.0) {
            return "1:2";
        } else if (ratio >= 1.0 / 3.0) {
            return "1:3";
        } else {
            return "1:3<";
        }
    }

    /**
     * Resolve an infantry vs. infantry combat action.
     *
     * @param ratio the combat ratio (from calculateRatio)
     * @param roll the 2D6 roll (2-12)
     * @return the combat result
     */
    public static InfantryCombatResult resolveAction(String ratio, int roll) {
        Map<Integer, InfantryCombatResult> ratioRow = ACTION_TABLE.get(ratio);
        if (ratioRow == null) {
            // Unknown ratio, default to even fight
            ratioRow = ACTION_TABLE.get("1:1");
        }

        InfantryCombatResult result = ratioRow.get(roll);
        if (result == null) {
            // Invalid roll, default to middle result
            result = ratioRow.get(7);
        }

        return result;
    }

    /**
     * Crew Casualties Table from TOAR page 174.
     * Converts percentage of casualties to number of crew hits.
     *
     * @param percentCasualties percentage of casualties (0-100)
     * @return number of crew hits (1-6)
     */
    public static int getCrewHits(int percentCasualties) {
        if (percentCasualties <= 0) {
            return 0;
        } else if (percentCasualties <= 20) {
            return 1;
        } else if (percentCasualties <= 35) {
            return 2;
        } else if (percentCasualties <= 50) {
            return 3;
        } else if (percentCasualties <= 65) {
            return 4;
        } else if (percentCasualties <= 80) {
            return 5;
        } else {
            return 6;
        }
    }
}
