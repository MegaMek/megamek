package megamek.common.rules;
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


import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.units.Entity;

public abstract class RulesGame {

    /**
     * Allow ammo dumping.
     *
     * @return true if ammo dumping is allowed
     */
    public abstract boolean ammoDumping();

    /**
     * Is an entity eligible for a phase
     * @param entity the unit being considered
     * @param phase what phase it is in
     * @return is it eligible
     */
    public abstract boolean eligibleForPhase(Entity entity, @Nullable GamePhase phase);

    /**
     * Return the number of units to move.
     *
     * @param num_normal_turns array of normal turns
     * @param index the current index
     * @param min the minimum value
     * @param frontLoadOption true if front load option is enabled
     * @return the initiative order
     */
    public abstract int getInitiativeOrder(int[] num_normal_turns, int index, int min, boolean frontLoadOption);

    /**
     * Does tag increase BV?
     *
     * @return true if TAG increases battle value
     */
    public abstract boolean tagBVBump();
}
