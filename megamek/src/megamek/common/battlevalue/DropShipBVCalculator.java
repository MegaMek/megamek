/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.battlevalue;

import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Mounted;

import java.util.function.Predicate;

public class DropShipBVCalculator extends LargeAeroBVCalculator {

    DropShipBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    protected Predicate<Mounted<?>> frontWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_NOSE);
    }

    @Override
    protected Predicate<Mounted<?>> rearWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_AFT);
    }

    @Override
    protected Predicate<Mounted<?>> leftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_LWING) && !weapon.isRearMounted();
    }

    @Override
    protected Predicate<Mounted<?>> leftAftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_LWING) && weapon.isRearMounted();
    }

    @Override
    protected Predicate<Mounted<?>> rightWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_RWING) && !weapon.isRearMounted();
    }

    @Override
    protected Predicate<Mounted<?>> rightAftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_RWING) && weapon.isRearMounted();
    }

    @Override
    protected int bvLocation(Mounted<?> equipment) {
        if (equipment.getLocation() == Dropship.LOC_NOSE) {
            return BVLOC_NOSE;
        } else if ((equipment.getLocation() == Dropship.LOC_LWING) && !equipment.isRearMounted()) {
            return BVLOC_LEFT;
        } else if ((equipment.getLocation() == Dropship.LOC_LWING) && equipment.isRearMounted()) {
            return BVLOC_LEFT_AFT;
        } else if (equipment.getLocation() == Dropship.LOC_AFT) {
            return BVLOC_AFT;
        } else if ((equipment.getLocation() == Dropship.LOC_RWING) && equipment.isRearMounted()) {
            return BVLOC_RIGHT_AFT;
        } else {
            return BVLOC_RIGHT;
        }
    }

    @Override
    protected String arcName(int bvLocation) {
        return switch (bvLocation) {
            case BVLOC_NOSE -> entity.getLocationName(Dropship.LOC_NOSE);
            case BVLOC_LEFT -> entity.getLocationName(Dropship.LOC_LWING);
            case BVLOC_LEFT_AFT -> entity.getLocationName(Dropship.LOC_LWING) + " (R)";
            case BVLOC_AFT -> entity.getLocationName(Dropship.LOC_AFT);
            case BVLOC_RIGHT_AFT -> entity.getLocationName(Dropship.LOC_RWING) + " (R)";
            case BVLOC_RIGHT -> entity.getLocationName(Dropship.LOC_RWING);
            default -> "Error: Unexpected location value.";
        };
    }
}
