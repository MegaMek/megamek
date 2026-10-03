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
package megamek.common.weapons.bombs;

import java.io.Serial;

import megamek.common.weapons.missiles.thunderbolt.ThunderboltWeapon;

/**
 * Base class for the external ordnance missiles (AAA, LAA, AS and ASEW) that deal standard damage but use the capital
 * range brackets in aerospace combat (TO:AuE p. 169-171).
 *
 * <p>These weapons are not capital weapons: their damage and missile armor are in standard points, so
 * {@code capital} stays {@code false}. Only the range brackets are capital.</p>
 */
public abstract class CapitalRangeBombMissileWeapon extends ThunderboltWeapon {
    @Serial
    private static final long serialVersionUID = 2084L;

    @Override
    public boolean usesCapitalRangeBrackets() {
        return true;
    }
}
