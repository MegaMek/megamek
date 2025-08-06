/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

/**
 * @author Ben Grills
 * @since Sep 7, 2005
 */
public class InfantryPistolISPulseLaserPistolWeapon extends InfantryWeapon {
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryPistolISPulseLaserPistolWeapon() {
        super();

        name = "Pulse Laser Pistol (IS)";
        setInternalName(name);
        addLookupName("InfantryPulseLaserPistol");
        addLookupName("Pulse Laser Pistol");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        cost = 1000;
        bv = 0.13;
        tonnage = .001;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY);
        infantryDamage = 0.14;
        infantryRange = 0;
        ammoWeight = 0.0003;
        shots = 15;
        bursts = 3;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TechBase.ALL).setISAdvancement(2615, 2620, 2700, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH).setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.F, AvailabilityValue.C, AvailabilityValue.C);
    }
}
