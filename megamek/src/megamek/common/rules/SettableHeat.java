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
package megamek.common.rules;

import java.util.Collection;

import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;

/**
 * The most heat a player or gamemaster can give a unit by hand, before the game in the lobby or during play in the
 * unit editor.
 *
 * <p>The limit is the top of the heat scale in play plus what the unit dissipates in a turn. The Heat Phase takes the
 * dissipation off before it checks heat effects, so a Mek sinking 30 that is set to 50 is checked at 20; without the
 * dissipation on top, nobody could test the top of the scale.</p>
 */
public final class SettableHeat {

    /** The standard scale's limit before dissipation: past its automatic shutdown at 30, as the lobby always offered. */
    public static final int STANDARD_MAXIMUM = 40;

    /** The Expanded Heat Scale's limit before dissipation: the top of that scale (TO:AR p.102). */
    public static final int EXPANDED_SCALE_MAXIMUM = 50;

    private SettableHeat() {}

    /**
     * Returns the most heat that can be set on one unit. It never goes below the heat the unit already carries, since
     * a unit can build past the limit in play and must still be editable.
     *
     * @param game        the unit's game, or {@code null} outside a game, as in MekHQ and MegaMekLab
     * @param currentHeat the heat the unit carries now
     * @param dissipation the heat the unit sinks each turn, including heat sinks under water
     *
     * @return the most heat that can be set
     */
    public static int maximum(@Nullable Game game, int currentHeat, int dissipation) {
        return Math.max(scaleMaximum(game) + Math.max(dissipation, 0), currentHeat);
    }

    /**
     * Returns the most heat that can be set on a group of units at once, as the lobby does: enough for the unit that
     * dissipates the most to reach the top of the scale.
     *
     * @param game  the game, or {@code null} outside a game
     * @param units the units to be given the same heat
     *
     * @return the most heat that can be set
     */
    public static int maximum(@Nullable Game game, Collection<Entity> units) {
        int maximum = scaleMaximum(game);
        for (Entity unit : units) {
            maximum = Math.max(maximum, maximum(game, unit.heat, unit.getHeatCapacityWithWater()));
        }
        return maximum;
    }

    private static int scaleMaximum(@Nullable Game game) {
        boolean isExpandedHeatScale = (game != null)
              && game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT);
        return isExpandedHeatScale ? EXPANDED_SCALE_MAXIMUM : STANDARD_MAXIMUM;
    }
}
