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
public class InfantrySupportWireGuidedMissileWeapon extends InfantryWeapon {
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportWireGuidedMissileWeapon() {
        super();

        name = "Wire-Guided Missile Launcher";
        setInternalName(name);
        addLookupName("InfantryWireGuidedMissileLauncher");
        addLookupName("WireGuidedMissileLauncher");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        cost = 800000;
        tonnage = 0.095;
        bv = 0.00;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_MISSILE).or(F_INF_SUPPORT);
        infantryDamage = 1.08;
        infantryRange = 2;
        toHitModifier = -2;
        crew = 4;
        ammoWeight = 0.022;
        ammoCost = 2500;
        shots = 1;
        rulesRefs = "195, AToW-C";
        techAdvancement.setTechBase(TechBase.ALL).setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false).setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X);
    }
}
