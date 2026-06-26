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
package megamek.common.units;

import java.io.Serializable;

/**
 * Tracks whether a unit performing multi-turn fieldwork - digging in (TO:AR p.106) or raising a fortified hex as
 * Trench/Fieldworks Engineers (TO:AUE p.153) - has been damaged between turns. Per those rules, taking damage during a
 * turn spent fortifying extends the effort by one turn (regardless of how many attacks were made), so the unit's dug-in
 * progress counter must not advance on any turn in which it was attacked.
 *
 * <p>This class is intentionally agnostic about the unit type. Callers supply a "health signature": any
 * integer that only decreases when the unit takes damage (for example, total armor plus internal structure). A decrease
 * in that signature between two consecutive turns means the unit was damaged and the effort is set back by one
 * turn.</p>
 */
public class FortifyState implements Serializable {

    /** Sentinel meaning "no fieldwork is in progress, so there is no baseline to compare against yet". */
    private static final int UNSET = -1;

    private int healthSignatureSnapshot = UNSET;
    private boolean extendedAtLastCheckpoint = false;

    /**
     * Records the health signature at the moment fieldwork begins, establishing the baseline that the next turn's
     * checkpoint compares against.
     *
     * @param healthSignature the unit's current health signature (for example, total armor + internal)
     */
    public void begin(int healthSignature) {
        healthSignatureSnapshot = healthSignature;
        extendedAtLastCheckpoint = false;
    }

    /**
     * Compares the current health signature against the previous checkpoint and stores the new value for next turn. A
     * decrease since the last checkpoint means the unit was damaged.
     *
     * @param healthSignature the unit's current health signature (for example, total armor + internal)
     *
     * @return {@code true} if the unit has taken damage since the last checkpoint (so the effort should be extended by
     *       one turn); {@code false} on the first checkpoint or when no damage was taken
     */
    public boolean checkpointWasDamaged(int healthSignature) {
        boolean wasDamaged = (healthSignatureSnapshot != UNSET) && (healthSignature < healthSignatureSnapshot);
        healthSignatureSnapshot = healthSignature;
        extendedAtLastCheckpoint = wasDamaged;
        return wasDamaged;
    }

    /**
     * @return {@code true} if the most recent {@link #checkpointWasDamaged(int)} detected damage (i.e. the effort was
     *       extended). Used to report/notify the delay after the checkpoint has run.
     */
    public boolean wasExtendedAtLastCheckpoint() {
        return extendedAtLastCheckpoint;
    }

    /** Clears the in-progress baseline; call when the unit stops fortifying. */
    public void reset() {
        healthSignatureSnapshot = UNSET;
        extendedAtLastCheckpoint = false;
    }
}
