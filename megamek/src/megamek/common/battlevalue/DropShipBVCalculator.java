/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
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
    protected Predicate<Mounted> frontWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_NOSE);
    }

    @Override
    protected Predicate<Mounted> rearWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_AFT);
    }

    @Override
    protected Predicate<Mounted> leftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_LWING) && !weapon.isRearMounted();
    }

    @Override
    protected Predicate<Mounted> leftAftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_LWING) && weapon.isRearMounted();
    }

    @Override
    protected Predicate<Mounted> rightWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_RWING) && !weapon.isRearMounted();
    }

    @Override
    protected Predicate<Mounted> rightAftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_RWING) && weapon.isRearMounted();
    }

    @Override
    protected int bvLocation(Mounted equipment) {
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
        switch (bvLocation) {
            case BVLOC_NOSE:
                return entity.getLocationName(Dropship.LOC_NOSE);
            case BVLOC_LEFT:
                return entity.getLocationName(Dropship.LOC_LWING);
            case BVLOC_LEFT_AFT:
                return entity.getLocationName(Dropship.LOC_LWING) + " (R)";
            case BVLOC_AFT:
                return entity.getLocationName(Dropship.LOC_AFT);
            case BVLOC_RIGHT_AFT:
                return entity.getLocationName(Dropship.LOC_RWING) + " (R)";
            case BVLOC_RIGHT:
                return entity.getLocationName(Dropship.LOC_RWING);
        }
        return "Error: Unexpected location value.";
    }
}