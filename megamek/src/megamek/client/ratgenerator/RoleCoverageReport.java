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
package megamek.client.ratgenerator;

/**
 * How many of a generated force's unit slots a {@link megamek.common.units.UnitRole} percentage mix would be able to
 * govern, and why the remainder was excluded.
 *
 * <p>The Campaign Operations formation builder owns the slots of any node that carries a formation, so a role mix can
 * only shape what is left over. Surfacing that split is what keeps the feature honest: a mix that governs four slots
 * out of twelve is doing its job, but without this report it reads to the player as though it did nothing.</p>
 *
 * <p>The four exclusion counts are mutually exclusive and sum with {@code governedSlots} to {@code totalUnitSlots}.
 * They are evaluated in the order declared by {@link RoleSlotSurvey}, so a slot that is both inside an attached force
 * and under a formation is counted once, against the formation.</p>
 *
 * <p>This is a transient build-time value; it is never serialized into a saved game.</p>
 *
 * @param totalUnitSlots           every unit-producing leaf in the force, governed or not
 * @param governedSlots            leaves a role mix would be free to assign a role to
 * @param slotsSetByFormation      leaves whose unit choice belongs to a Campaign Operations formation
 * @param slotsInAttachedForces    leaves inside an {@code <attachedForces>} support detachment
 * @param slotsExcludedByUnitType  leaves whose unit type has no weight class, so role targeting cannot apply
 * @param slotsExcludedByArtillery leaves carrying an artillery mission role, which disables weight-class handling
 */
public record RoleCoverageReport(int totalUnitSlots,
      int governedSlots,
      int slotsSetByFormation,
      int slotsInAttachedForces,
      int slotsExcludedByUnitType,
      int slotsExcludedByArtillery) {

    /** An empty report, used when no force has been generated yet. */
    public static final RoleCoverageReport EMPTY = new RoleCoverageReport(0, 0, 0, 0, 0, 0);

    /**
     * @return {@code true} when at least one slot could be shaped by a role mix
     */
    public boolean hasGovernedSlots() {
        return governedSlots > 0;
    }

    /**
     * @return the governed share of the force as a percentage, or {@code 0} when the force holds no unit slots
     */
    public int governedPercent() {
        return (totalUnitSlots == 0) ? 0 : (int) Math.round(100.0 * governedSlots / totalUnitSlots);
    }
}
