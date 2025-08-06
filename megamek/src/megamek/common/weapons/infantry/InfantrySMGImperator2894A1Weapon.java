/*
 * Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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

/**
 * @author Ben Grills
 */
public class InfantrySMGImperator2894A1Weapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySMGImperator2894A1Weapon() {
        super();

        name = "SMG (Imperator 2894A1)";
        setInternalName(name);
        addLookupName("InfantryImperator2894A1");
        addLookupName("Imperator 2894A1 SMG");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        cost = 100;
        bv = 0.20;
        tonnage = .004;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.22;
        infantryRange = 0;
        ammoWeight = 0.00038;
        ammoCost = 5;
        shots = 50;
        bursts = 5;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TechBase.IS).setISAdvancement(2838, 2842, 2900, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FW).setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.C);

    }
}
