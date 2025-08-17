/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.flamers.innerSphere;

import megamek.common.equipment.WeaponType;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.flamers.VehicleFlamerWeapon;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class ISVehicleFlamer extends VehicleFlamerWeapon {
    private static final long serialVersionUID = -5209851790302913451L;

    public ISVehicleFlamer() {
        super();

        this.name = "Flamer (Vehicle)";
        this.setInternalName(this.name);
        this.addLookupName("IS Vehicle Flamer");
        this.addLookupName("ISVehicleFlamer");
        this.addLookupName("CLVehicleFlamer");
        this.addLookupName("Clan Vehicle Flamer");
        this.addLookupName("Vehicle Flamer");
        sortingName = "Flamer V";
        this.heat = 3;
        this.damage = 2;
        this.infDamageClass = WeaponType.WEAPON_BURST_4D6;
        this.rackSize = 2;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.tonnage = 0.5;
        this.criticals = 1;
        this.bv = 5;
        this.cost = 7500;
        this.shortAV = 2;
        this.maxRange = RANGE_SHORT;
        rulesRefs = "218, TM";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A)
              .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false);
    }

    @Override
    public int getAlphaStrikeHeatDamage(int rangeband) {
        return (rangeband == AlphaStrikeElement.RANGE_BAND_SHORT) ? 2 : 0;
    }

    @Override
    public boolean isAlphaStrikePointDefense() {
        return true;
    }
}
