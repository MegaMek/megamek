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

package megamek.common.weapons.battlearmor.innerSphere;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.battlearmor.BAMGWeapon;

/**
 * Commented out in WeaponType. Clan version is same stats as IS one. And IS versions captures Tech Progression for
 * both.
 *
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class ISBAMGHeavy extends BAMGWeapon {
    private static final long serialVersionUID = -8064879485060186631L;

    public ISBAMGHeavy() {
        super();
        name = "Machine Gun (Heavy)";
        setInternalName("ISBAHeavyMachineGun");
        addLookupName("IS BA Heavy Machine Gun");
        addLookupName("ISBAHeavyMG");
        sortingName = "MG D";
        heat = 0;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        rackSize = 3;
        shortRange = 1;
        mediumRange = 2;
        longRange = 2;
        extremeRange = 2;
        tonnage = 0.15;
        criticalSlots = 1;
        bv = 6;
        cost = 7500;
        rulesRefs = "258, TM";
        techAdvancement.setTechBase(TechBase.IS);
        techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, 3068);
        techAdvancement.setTechRating(TechRating.C);
        techAdvancement.setAvailability(AvailabilityValue.X,
              AvailabilityValue.X,
              AvailabilityValue.C,
              AvailabilityValue.B);
    }

}
