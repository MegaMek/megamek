/*
 * Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons.infantry.support.heavy;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * @author Ben Grills
 */
public class InfantrySupportHeavyPulseLaserWeapon extends InfantryWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportHeavyPulseLaserWeapon() {
        super();

        name = "Support Pulse Laser (Heavy)";
        setInternalName(name);
        addLookupName("InfantryHeavyPulseLaser");
        addLookupName("Infantry Heavy Pulse Laser");
        addLookupName("InfantryMediumPulseLaser");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        cost = 60000;
        bv = 9.58;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_PULSE).or(F_INF_BURST).or(F_INF_SUPPORT);
        infantryDamage = 0.98;
        infantryRange = 4;
        crew = 4;
        ammoWeight = 0.003;
        shots = 18;
        bursts = 3;
        tonnage = .300;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TechBase.ALL).setISAdvancement(2610, 2615, 2675, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(2610, 2615, 2675, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false).setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D);

    }
}
