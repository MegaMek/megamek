/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.infantry.support.mg;

import java.io.Serial;

import megamek.common.SourceBookCode;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * Light machine gun from Shrapnel #22.
 *
 * <p>Damage and Battle Value come from the Infantry Weapons Calculator sheet, using the updated conversion
 * formulas and feedback supplied by a CGL freelancer. Cost, mass, shots, bursts and the reload figures are
 * from Shrapnel #22 itself.</p>
 */
public class InfantryMachineGunTIE124 extends InfantryWeapon {

    @Serial
    private static final long serialVersionUID = -4899999999999889134L;

    public InfantryMachineGunTIE124() {
        super();

        name = "Machine Gun (TIE-124)";
        setInternalName(name);
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        bv = 3.15;
        tonnage = 0.0151;
        ammoWeight = 0.0055;
        ammoCost = 150;
        infantryDamage = 1.575;
        infantryRange = 2;
        cost = 4500;
        shots = 200;
        bursts = 20;
        crew = 1;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_BURST).or(F_INF_SUPPORT).or(F_INF_ENCUMBER);
        rulesRefs = rulesRefs(SourceBookCode.SHRAPNEL_22);
        techAdvancement
              .setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(DATE_NONE, DATE_NONE, DATE_ES, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setProductionFactions(Faction.CLAN);
    }
}
