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

package megamek.common.weapons.battlearmor;

import megamek.common.TechAdvancement;
import megamek.common.WeaponType;

/**
 * Commented out in WeaponType. Clan version is same stats as IS one. And IS versions captures Tech Progression for
 * both.
 *
 * @author Sebastian Brocks
 */
public class ISBAMG extends BAMGWeapon {
    private static final long serialVersionUID = -4420620461776813639L;

    public ISBAMG() {
        super();
        name = "Machine Gun (Medium)";
        setInternalName("ISBAMG");
        addLookupName("IS BA Machine Gun");
        addLookupName("ISBAMachine Gun");
        addLookupName("ISBAMachineGun");
        sortingName = "MG C";
        heat = 0;
        damage = 2;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        rackSize = 2;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 0.1;
        criticals = 1;
        bv = 5;
        cost = 5000;
        rulesRefs = "258, TM";
        techAdvancement.setTechBase(TechAdvancement.TechBase.IS);
        techAdvancement.setISAdvancement(DATE_NONE, 1950, 3050);
        techAdvancement.setTechRating(TechRating.C);
        techAdvancement.setAvailability(AvailabilityValue.X,
              AvailabilityValue.D,
              AvailabilityValue.B,
              AvailabilityValue.B);
    }
}
