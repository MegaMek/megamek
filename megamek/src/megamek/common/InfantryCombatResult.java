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
 * BattleMek, `Mek and AeroTek are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * BattleMekWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common;

/**
 * Represents the result of an infantry vs. infantry combat action based on
 * TOAR Infantry vs. Infantry Action Table.
 *
 * Results can be:
 * - ELIMINATED ('E'): Defender is eliminated
 * - REPULSED ('R'): Attacker is repulsed and takes half damage
 * - PARTIAL ('P'): Partial control, defenders eliminated or take half damage
 * - CASUALTIES: Numeric percentage of casualties (stored as value)
 */
public class InfantryCombatResult {

    public enum ResultType {
        /** Defender completely eliminated */
        ELIMINATED,
        /** Attacker repulsed (retreat/fall back) */
        REPULSED,
        /** Partial control achieved */
        PARTIAL,
        /** Both sides take casualties (percentage) */
        CASUALTIES
    }

    private final ResultType type;

    /** Percentage of attacker casualties (0-100) */
    private final int attackerCasualtiesPercent;

    /** Percentage of defender casualties (0-100) */
    private final int defenderCasualtiesPercent;

    /**
     * Creates an ELIMINATED result (defender eliminated).
     */
    public static InfantryCombatResult eliminated() {
        return new InfantryCombatResult(ResultType.ELIMINATED, 0, 100);
    }

    /**
     * Creates a REPULSED result (attacker repulsed, takes half damage).
     *
     * @param attackerCasualties percentage of attacker casualties
     */
    public static InfantryCombatResult repulsed(int attackerCasualties) {
        return new InfantryCombatResult(ResultType.REPULSED, attackerCasualties / 2, 0);
    }

    /**
     * Creates a PARTIAL result (partial control).
     * Interpretation depends on table entry (either eliminates defenders or half damage).
     *
     * @param attackerCasualties percentage of attacker casualties
     * @param defenderEliminated true if defenders eliminated, false if half damage
     */
    public static InfantryCombatResult partial(int attackerCasualties, boolean defenderEliminated) {
        int defenderCasualties = defenderEliminated ? 100 : attackerCasualties / 2;
        return new InfantryCombatResult(ResultType.PARTIAL, attackerCasualties, defenderCasualties);
    }

    /**
     * Creates a CASUALTIES result (both sides take percentage casualties).
     *
     * @param attackerCasualties percentage of attacker casualties
     * @param defenderCasualties percentage of defender casualties
     */
    public static InfantryCombatResult casualties(int attackerCasualties, int defenderCasualties) {
        return new InfantryCombatResult(ResultType.CASUALTIES, attackerCasualties, defenderCasualties);
    }

    private InfantryCombatResult(ResultType type, int attackerCasualtiesPercent, int defenderCasualtiesPercent) {
        this.type = type;
        this.attackerCasualtiesPercent = attackerCasualtiesPercent;
        this.defenderCasualtiesPercent = defenderCasualtiesPercent;
    }

    public ResultType getType() {
        return type;
    }

    public int getAttackerCasualtiesPercent() {
        return attackerCasualtiesPercent;
    }

    public int getDefenderCasualtiesPercent() {
        return defenderCasualtiesPercent;
    }

    public boolean isDefenderEliminated() {
        return type == ResultType.ELIMINATED ||
               (type == ResultType.PARTIAL && defenderCasualtiesPercent >= 100);
    }

    public boolean isAttackerRepulsed() {
        return type == ResultType.REPULSED;
    }

    public boolean isPartialControl() {
        return type == ResultType.PARTIAL;
    }

    // Convenience methods for shorter names
    public int getAttackerCasualties() {
        return attackerCasualtiesPercent;
    }

    public int getDefenderCasualties() {
        return defenderCasualtiesPercent;
    }

    @Override
    public String toString() {
        return switch (type) {
            case ELIMINATED -> "E (Defender Eliminated)";
            case REPULSED -> "R (Attacker Repulsed, " + attackerCasualtiesPercent + "% casualties)";
            case PARTIAL -> "P (Partial, A:" + attackerCasualtiesPercent + "% D:" +
                           (defenderCasualtiesPercent >= 100 ? "Eliminated" : defenderCasualtiesPercent + "%") + ")";
            case CASUALTIES -> attackerCasualtiesPercent + "%/" + defenderCasualtiesPercent + "%";
        };
    }
}
