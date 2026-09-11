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
package megamek.server.totalWarfare;

import java.util.List;

import megamek.common.Report;
import megamek.common.units.Entity;

/**
 * One side's losses from an infantry action roll, worked out before anything is applied (TO:AR p. 174): the Marine
 * Points lost, the side's own strength they are measured against, and what each unit gives up. Planning first lets
 * the report tell the story and show the working before the casualties land and the destroyed lines follow.
 *
 * @param marinePointsLost the side's loss in Marine Points
 * @param ownStrength      the side's own strength before the roll
 * @param units            the units that had someone to lose, in the side's order
 */
record InfantryActionSideLosses(int marinePointsLost, int ownStrength, List<UnitLoss> units) {

    /**
     * What one unit gives up.
     *
     * @param entity        the unit
     * @param headCount     the troopers or committed crew it had before the loss
     * @param personnelLost the people it loses: rounded down from its share, or for battle armor the troopers the
     *                      damage actually killed
     * @param isCrew        {@code true} for a building's crew, {@code false} for troopers
     * @param damageDealt   the standard-scale damage a battle armor squad took (TO:AR p. 174), {@code 0} otherwise
     * @param damageReports the engine's lines for that damage, written after the casualty lines
     */
    record UnitLoss(Entity entity, int headCount, int personnelLost, boolean isCrew, int damageDealt,
          List<Report> damageReports) {

        /** A loss with no damage of its own to report: infantry and crew. */
        UnitLoss(Entity entity, int headCount, int personnelLost, boolean isCrew) {
            this(entity, headCount, personnelLost, isCrew, 0, List.of());
        }

        /** @return the people the unit keeps */
        int remaining() {
            return headCount - personnelLost;
        }
    }

    /** @return the people lost across the side */
    int personnelLost() {
        int total = 0;
        for (UnitLoss loss : units) {
            total += loss.personnelLost();
        }
        return total;
    }

    /** @return {@code true} when nobody who counted for the side is left after the loss */
    boolean nobodyLeft() {
        for (UnitLoss loss : units) {
            if (loss.remaining() > 0) {
                return false;
            }
        }
        return true;
    }
}
