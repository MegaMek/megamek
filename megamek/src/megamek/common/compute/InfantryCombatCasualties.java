/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

/**
 * The arithmetic of an infantry vs. infantry action roll (TO:AR pp. 172-174), kept free of game state so every step
 * can be tested against the book's worked example.
 *
 * <p>Each side loses Marine Points equal to a percentage of the <em>other</em> side's strength, rounded up. A repulsed
 * attacker loses double. A defender in full control of the building, or an attacker who announced a withdrawal, loses
 * half. The Marine Points lost are then turned back into people by the share of the side's own strength they
 * represent, rounding each unit's casualties down.</p>
 */
public final class InfantryCombatCasualties {

    private InfantryCombatCasualties() {
    }

    /**
     * Marine Points a side loses from one action roll (TO:AR p. 172).
     *
     * @param otherSideStrength the other side's Marine Points Score, which the percentage is taken from
     * @param percent           the percentage read from the Infantry vs. Infantry Action Table for this side
     * @param doubled           {@code true} for an attacker on an R result, who takes double casualties (p. 173)
     * @param halved            {@code true} for a defender still in full control, or an attacker who is withdrawing
     *
     * @return the Marine Points lost, rounded up, never negative
     */
    public static int marinePointsLost(int otherSideStrength, int percent, boolean doubled, boolean halved) {
        if ((otherSideStrength <= 0) || (percent <= 0)) {
            return 0;
        }
        double lost = otherSideStrength * (percent / 100.0);
        if (doubled) {
            lost *= 2;
        }
        if (halved) {
            lost /= 2;
        }
        return (int) Math.ceil(lost);
    }

    /**
     * The share of a side's own strength that a Marine Points loss represents (TO:AR p. 174, "the percent of
     * casualties suffered by the original force").
     *
     * @param marinePointsLost the Marine Points lost this roll
     * @param ownStrength      the side's Marine Points Score before the roll, without any building modifier
     *
     * @return a fraction from {@code 0.0} to {@code 1.0}
     */
    public static double casualtyFraction(int marinePointsLost, int ownStrength) {
        if ((ownStrength <= 0) || (marinePointsLost >= ownStrength)) {
            return 1.0;
        }
        if (marinePointsLost <= 0) {
            return 0.0;
        }
        return (double) marinePointsLost / ownStrength;
    }

    /**
     * Personnel a unit loses for a casualty fraction (TO:AR p. 174, simple conversion: round down). Troopers in armour
     * with a damage divisor of 2 or more lose half as many, rounded down again.
     *
     * @param personnel        the unit's people before the roll
     * @param casualtyFraction the side's casualty fraction from {@link #casualtyFraction(int, int)}
     * @param armoured         {@code true} for troopers in armour with a damage divisor of 2 or more
     *
     * @return the people lost, never more than {@code personnel}
     */
    public static int personnelLost(int personnel, double casualtyFraction, boolean armoured) {
        if ((personnel <= 0) || (casualtyFraction <= 0.0)) {
            return 0;
        }
        if (casualtyFraction >= 1.0) {
            return personnel;
        }
        int lost = (int) Math.floor(personnel * casualtyFraction);
        if (armoured) {
            lost = lost / 2;
        }
        return Math.min(personnel, lost);
    }
}
