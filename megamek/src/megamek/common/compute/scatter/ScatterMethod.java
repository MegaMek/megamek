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
package megamek.common.compute.scatter;

import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;

/**
 * Selects how a missed shot scatters: the standard quick-scatter rules (TW) or the Advanced Scatter optional rules
 * (TO:AR), chosen by the {@link OptionsConstants#ADVANCED_COMBAT_ADVANCED_SCATTER} game option.
 */
public enum ScatterMethod {

    /** Standard scatter: one direction, distance equal to the margin of failure. */
    STANDARD {
        @Override
        public ScatterResult omnidirectional(Coords target, int standardDistance, int marginOfFailure, int reduction) {
            return Scatter.omnidirectional(target, Math.max(standardDistance - reduction, 0));
        }

        @Override
        public ScatterResult frontArc(Coords target, int facing, int marginOfFailure, int reduction) {
            return Scatter.frontArc(target, facing, Scatter.reducedDistance(marginOfFailure, reduction));
        }
    },

    /** Advanced Scatter (TO:AR): two-leg, dice-based scatter that can reach any hex. */
    ADVANCED {
        @Override
        public ScatterResult omnidirectional(Coords target, int standardDistance, int marginOfFailure, int reduction) {
            return Scatter.advanced(target, marginOfFailure, reduction);
        }

        @Override
        public ScatterResult frontArc(Coords target, int facing, int marginOfFailure, int reduction) {
            return Scatter.advancedAltitude(target, facing, marginOfFailure, reduction);
        }
    };

    /**
     * Scatters in any of the six directions using a caller-supplied standard scatter distance, for weapons whose
     * standard scatter distance is not simply the margin of failure (artillery cannons scatter half as far, orbital
     * bombardment twice as far). The standard distance is ignored under Advanced Scatter, which rolls its own dice from
     * the margin of failure.
     *
     * @param target           the intended target hex
     * @param standardDistance the scatter distance in hexes used under standard rules
     * @param marginOfFailure  the attack's margin of failure (sign ignored)
     * @param reduction        hexes to subtract from the scatter distance (e.g. Oblique Artilleryman, Golden Goose);
     *                         {@code 0} for none
     *
     * @return the scatter outcome
     */
    public abstract ScatterResult omnidirectional(Coords target, int standardDistance, int marginOfFailure,
          int reduction);

    /**
     * Scatters in any of the six directions, using the margin-of-failure magnitude as the standard scatter distance
     * (artillery, dive bombs).
     *
     * @param target          the intended target hex
     * @param marginOfFailure the attack's margin of failure (sign ignored)
     * @param reduction       hexes to subtract from the scatter distance; {@code 0} for none
     *
     * @return the scatter outcome
     */
    public ScatterResult omnidirectional(Coords target, int marginOfFailure, int reduction) {
        return omnidirectional(target, Scatter.standardDistance(marginOfFailure), marginOfFailure, reduction);
    }

    /**
     * Scatters into the front arc of a flight path (altitude bombing).
     *
     * @param target          the intended target hex
     * @param facing          the flight-path facing the bomb was dropped along
     * @param marginOfFailure the attack's margin of failure (sign ignored)
     * @param reduction       hexes to subtract from the scatter distance; {@code 0} for none
     *
     * @return the scatter outcome
     */
    public abstract ScatterResult frontArc(Coords target, int facing, int marginOfFailure, int reduction);

    /**
     * @param game the current game
     *
     * @return {@link #ADVANCED} if the Advanced Scatter game option is enabled, otherwise {@link #STANDARD}
     */
    public static ScatterMethod forGame(Game game) {
        return game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_ADVANCED_SCATTER)
              ? ADVANCED : STANDARD;
    }
}
