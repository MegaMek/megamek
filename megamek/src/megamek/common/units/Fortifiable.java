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

import megamek.logging.MMLogger;

/**
 * Shared vehicle-style fieldworks behaviour for units that build a fortified hex over several turns with a bulldozer,
 * backhoe or equivalent (Vehicles and Fieldworks, TO:AUE p.153, Corrected Sixth Printing). A unit progresses through
 * {@code DUG_IN_FORTIFYING_*} stages, one per turn, and the hex is fortified once it reaches the final stage; damage
 * taken during a turn holds the progress rather than advancing it.
 *
 * <p>An implementor supplies the small amount of per-unit state - the dug-in stage counter, the {@link FortifyState}
 * damage tracker and its current health signature - and inherits the stage logic as default methods. Both {@link Tank}
 * and {@link Mek} implement this so the machine lives in one place rather than being duplicated. {@link Infantry} is
 * intentionally not a {@code Fortifiable}: it interleaves foxhole dig-in stages with fortifying and so uses a different
 * stage encoding.</p>
 */
public interface Fortifiable {

    MMLogger FORTIFY_LOGGER = MMLogger.create(Fortifiable.class);

    /** Not building a fortification. */
    int DUG_IN_NONE = 0;
    /** First turn of fieldwork. */
    int DUG_IN_FORTIFYING_FIRST = 1;
    /** Final turn of fieldwork; the hex is fortified at the end of this turn. */
    int DUG_IN_FORTIFYING_LAST = 3;

    /** @return the current dug-in stage ({@link #DUG_IN_NONE} or one of the {@code DUG_IN_FORTIFYING_*} stages). */
    int getDugIn();

    /** @param stage the dug-in stage to set */
    void setDugIn(int stage);

    /** @return the per-unit fortification damage tracker. */
    FortifyState getFortifyState();

    /**
     * @return the health signature used to detect damage between fortifying turns: total armor plus internal structure.
     *       Any loss between turns lowers this and marks the turn as interrupted.
     */
    int currentFortifyHealthSignature();

    /** @return the unit's short name, for diagnostic logging. */
    String getShortName();

    /**
     * Begins the multi-turn fieldwork that raises a fortified hex (TO:AUE p.153, Corrected Sixth Printing). Seeds the
     * damage baseline used to detect an interrupting attack that extends the effort.
     */
    default void beginFortify() {
        setDugIn(DUG_IN_FORTIFYING_FIRST);
        getFortifyState().begin(currentFortifyHealthSignature());
    }

    /** @return {@code true} if this unit is partway through building a fortified hex. */
    default boolean isFortifying() {
        return (getDugIn() >= DUG_IN_FORTIFYING_FIRST) && (getDugIn() <= DUG_IN_FORTIFYING_LAST);
    }

    /** @return the current fortification stage (1..{@link #getFortifyTotalStages()}), or 0 when not fortifying. */
    default int getFortifyStage() {
        return isFortifying() ? (getDugIn() - DUG_IN_FORTIFYING_FIRST + 1) : 0;
    }

    /** @return the number of turns of work a fortified hex takes to complete. */
    default int getFortifyTotalStages() {
        return DUG_IN_FORTIFYING_LAST - DUG_IN_FORTIFYING_FIRST + 1;
    }

    /**
     * @return {@code true} if this unit's fortification effort was set back by damage this round. TO:AUE p.153,
     *       Corrected Sixth Printing.
     */
    default boolean isFortifyExtendedThisRound() {
        return getFortifyState().wasExtendedAtLastCheckpoint();
    }

    /** @return {@code true} if this unit is on the final turn of fieldwork (the hex is fortified at end of turn). */
    default boolean isFortifyOnFinalStage() {
        return getDugIn() == DUG_IN_FORTIFYING_LAST;
    }

    /** Stops any in-progress fieldwork and clears the damage baseline. */
    default void cancelFortify() {
        setDugIn(DUG_IN_NONE);
        getFortifyState().reset();
    }

    /**
     * Advances one round of fieldwork, called from {@code newRound}: damage taken during the turn holds the progress
     * counter for a turn (TO:AUE p.153, Corrected Sixth Printing); otherwise the stage advances and rolls back to
     * {@link #DUG_IN_NONE} once it passes the final stage (completion is recognized separately during END-phase
     * resolution).
     */
    default void advanceFortifyRound() {
        if (getDugIn() == DUG_IN_NONE) {
            return;
        }
        if (getFortifyState().checkpointWasDamaged(currentFortifyHealthSignature())) {
            FORTIFY_LOGGER.debug("[Fortify] {}: damaged while fortifying - effort extended by 1 turn (stage {})",
                  getShortName(), getDugIn());
        } else {
            int next = getDugIn() + 1;
            setDugIn(next > DUG_IN_FORTIFYING_LAST ? DUG_IN_NONE : next);
        }
    }
}
