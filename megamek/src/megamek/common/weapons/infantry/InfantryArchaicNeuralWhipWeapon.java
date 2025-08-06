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
public class InfantryArchaicNeuralWhipWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryArchaicNeuralWhipWeapon() {
        super();

        name = "Whip (Neural Whip)";
        setInternalName(name);
        addLookupName("InfantryNeuralWhip");
        addLookupName("Neural Whip");
        ammoType = AmmoType.AmmoTypeEnum.NA;
        cost = 500;
        bv = 0.08;
        tonnage = .0003;
        flags = flags.or(F_NO_FIRES).or(F_INF_NONPENETRATING).or(F_INF_POINT_BLANK).or(F_INF_ARCHAIC);
        infantryDamage = 0.09;
        infantryRange = 0;
        rulesRefs = "272, TM";
        techAdvancement.setTechBase(TechBase.ALL).setISAdvancement(2315, 2325, 2500, DATE_NONE, DATE_NONE)
              .setISApproximate(true, true, false, false, false).setClanAdvancement(2315, 2325, 2500, 2786, DATE_NONE)
              .setClanApproximate(true, true, false, false, false).setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC).setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.F);

    }
}
