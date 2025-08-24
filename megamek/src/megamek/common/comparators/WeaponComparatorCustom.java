/*
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.comparators;

import java.util.Comparator;

import megamek.common.equipment.WeaponMounted;
import megamek.common.units.Entity;

/**
 * Comparator for sorting Weapons (Mounted that have WeaponTypes) by a custom ordering.  The ordering is stored in the
 * Entity instance, using a map that maps the weapon's ID to its custom order index.
 *
 * @author arlith
 */
public record WeaponComparatorCustom(Entity entity) implements Comparator<WeaponMounted> {

    @Override
    public int compare(WeaponMounted lhs, WeaponMounted rhs) {
        int leftWeaponOrder = entity.getCustomWeaponOrder(lhs);
        int rightWeaponOrder = entity.getCustomWeaponOrder(rhs);
        return leftWeaponOrder - rightWeaponOrder;
    }
}
