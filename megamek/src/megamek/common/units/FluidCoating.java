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

/**
 * A chemical coating sprayed onto a unit by a Fluid Gun / Sprayer that changes how readily that unit can be
 * set alight (TO:AUE pp.173-174). Unlike the hex coatings (stored as terrain), this coating travels with the
 * unit, so ignition rolls against whatever hex the unit occupies are modified while the coating lasts.
 *
 * @author The MegaMek Team
 */
public enum FluidCoating {
    /** No fluid coating. */
    NONE(0),

    /** Oil Slick Ammo: oil is flammable, so the coated unit is easier to set alight (-2 to ignition rolls). */
    OIL_SLICK(-2),

    /** Flame-Retardant Foam Ammo: the coated unit strongly resists catching fire (+4 to ignition rolls). */
    FLAME_RETARDANT_FOAM(4);

    private final int ignitionModifier;

    FluidCoating(int ignitionModifier) {
        this.ignitionModifier = ignitionModifier;
    }

    /**
     * @return the target-number modifier this coating applies to attempts to set the coated unit's hex on
     *       fire (TO:AUE pp.173-174): -2 for an Oil Slick, +4 for Flame-Retardant Foam, 0 for none
     */
    public int ignitionModifier() {
        return ignitionModifier;
    }
}
