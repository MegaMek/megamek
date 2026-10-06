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

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;

/**
 * Shared multi-turn rubble-clearing state for units that can clear a rubble hex with a bulldozer or backhoe (TacOps).
 * The unit must remain in the target hex for the required number of turns; the work is banked one turn at a time and
 * the hex opens up once it is done.
 *
 * <p>An implementor supplies the small amount of per-unit state - the target hex and the completed/required turn
 * counts - and inherits the state transitions as default methods. Both {@link Tank} and {@link Mek} implement this (a
 * Mek clears only with a backhoe, since the bulldozer is vehicle-only equipment), so the machine lives in one place
 * rather than being duplicated.</p>
 */
public interface RubbleClearer {

    /** @return the rubble hex this unit is clearing, or {@code null} if it is not clearing rubble */
    @Nullable
    Coords getRubbleClearTarget();

    /** @param target the rubble hex being cleared, or {@code null} to stop clearing */
    void setRubbleClearTarget(@Nullable Coords target);

    /** @return the number of turns of clearing banked so far */
    int getRubbleClearTurnsCompleted();

    /** @param turns the number of turns of clearing banked so far */
    void setRubbleClearTurnsCompleted(int turns);

    /** @return the total number of turns this rubble hex needs to be cleared */
    int getRubbleClearTurnsRequired();

    /** @param turns the total number of turns the clearing takes */
    void setRubbleClearTurnsRequired(int turns);

    /**
     * Begins clearing a rubble hex (TacOps). The unit must remain in {@code target} for {@code requiredTurns} turns;
     * this replaces any in-progress effort on a different hex.
     *
     * @param target        the rubble hex being cleared (the unit's own hex)
     * @param requiredTurns the number of full turns the clearing takes (2/4/8/16 by structure type, capped at 16, plus
     *                      any backhoe penalty)
     */
    default void beginClearingRubble(Coords target, int requiredTurns) {
        setRubbleClearTarget(target);
        setRubbleClearTurnsRequired(requiredTurns);
        setRubbleClearTurnsCompleted(0);
    }

    /** @return {@code true} if this unit is partway through clearing a rubble hex */
    default boolean isClearingRubble() {
        return getRubbleClearTarget() != null;
    }

    /**
     * Banks one full turn of clearing.
     *
     * @return the number of turns of clearing completed so far (including this one)
     */
    default int bankRubbleClearTurn() {
        setRubbleClearTurnsCompleted(getRubbleClearTurnsCompleted() + 1);
        return getRubbleClearTurnsCompleted();
    }

    /** Clears the in-progress rubble-clearing state; call when the unit stops or finishes clearing. */
    default void cancelClearingRubble() {
        setRubbleClearTarget(null);
        setRubbleClearTurnsRequired(0);
        setRubbleClearTurnsCompleted(0);
    }
}
