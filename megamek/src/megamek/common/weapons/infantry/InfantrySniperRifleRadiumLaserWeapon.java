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

/**
 * @author Ben Grills
 */
public class InfantrySniperRifleRadiumLaserWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySniperRifleRadiumLaserWeapon() {
        super();

        name = "Sniper Rifle (Radium Sniper)";
        setInternalName(name);
        addLookupName("InfantryRadiumSniper");
        addLookupName("Radium Sniper Rifle");
        addLookupName("Rifle (Radium Sniper)");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        cost = 9500;
        bv = 2.58;
        tonnage = .012;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
        infantryDamage = 0.36;
        infantryRange = 3;
        ammoWeight = 0.00033;
        ammoCost = 650;
        shots = 5;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TechBase.IS).setISAdvancement(2582, 2583, DATE_NONE, 2607, 3062)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TC)
              .setProductionFactions(Faction.TC).setReintroductionFactions(Faction.TC).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F);

    }
}
