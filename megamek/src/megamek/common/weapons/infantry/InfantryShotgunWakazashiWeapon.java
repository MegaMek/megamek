/*
  Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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

/*
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;
import megamek.common.TechAdvancement;

/**
 * @author Ben Grills
 */
public class InfantryShotgunWakazashiWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryShotgunWakazashiWeapon() {
        super();
        name = "Shotgun (Wakazashi O-12)";
        setInternalName(name);
        addLookupName("WakazashiO12");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        cost = 180;
        bv = 0.35;
        tonnage = .0052;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.35;
        infantryRange = 0;
        ammoWeight = 0.0002;
        ammoCost = 2;
        shots = 10;
        bursts = 3;
        rulesRefs = " 176,HBHK";
        techAdvancement.setTechBase(TechAdvancement.TechBase.ALL);
        techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, 2100);
        techAdvancement.setTechRating(TechRating.D);
        techAdvancement.setAvailability(AvailabilityValue.C,
              AvailabilityValue.C,
              AvailabilityValue.C,
              AvailabilityValue.C);
    }
}
