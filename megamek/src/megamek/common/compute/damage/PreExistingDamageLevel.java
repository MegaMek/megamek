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
package megamek.common.compute.damage;

/**
 * The pre-existing damage levels from the Pre-Existing Damage Table (First Succession War, p.145). Each level states
 * how many points of damage a unit receives per 5 tons of mass and how many guaranteed critical hits it suffers.
 *
 * <p>Damage is distributed randomly in 5-point (or fraction thereof) groups. {@link #LIGHT} never rolls critical
 * hits, even when internal structure is damaged; {@link #MODERATE} and {@link #HEAVY} also roll for critical hits
 * caused by internal structure damage.</p>
 */
public enum PreExistingDamageLevel {

    /** 1D6 roll of 1: no pre-existing damage. */
    NONE(0, 0, false),

    /** 1D6 roll of 2-3: 1 point per 5 tons, no critical hits even if internal structure is damaged. */
    LIGHT(1, 0, false),

    /** 1D6 roll of 4-5: 2 points per 5 tons, one guaranteed critical hit plus internal structure crits. */
    MODERATE(2, 1, true),

    /** 1D6 roll of 6: 4 points per 5 tons, two guaranteed critical hits plus internal structure crits. */
    HEAVY(4, 2, true);

    private final int damagePerFiveTons;
    private final int guaranteedCritCount;
    private final boolean rollsInternalStructureCrits;

    PreExistingDamageLevel(int damagePerFiveTons, int guaranteedCritCount, boolean rollsInternalStructureCrits) {
        this.damagePerFiveTons = damagePerFiveTons;
        this.guaranteedCritCount = guaranteedCritCount;
        this.rollsInternalStructureCrits = rollsInternalStructureCrits;
    }

    /** @return the number of guaranteed critical hits rolled to a random location */
    public int getGuaranteedCritCount() {
        return guaranteedCritCount;
    }

    /** @return {@code true} if this level determines critical hits from internal structure damage */
    public boolean rollsInternalStructureCrits() {
        return rollsInternalStructureCrits;
    }

    /**
     * @param weight the unit's weight in tons
     *
     * @return the total pre-existing damage for a unit of the given weight at this level
     */
    public int totalDamage(double weight) {
        return damagePerFiveTons * (int) Math.ceil(weight / 5.0);
    }
}
