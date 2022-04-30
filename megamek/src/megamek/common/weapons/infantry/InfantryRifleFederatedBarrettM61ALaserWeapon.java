/*
 * Copyright (c) 2004-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

/**
 * @author Ben Grills
 * @since Sep 7, 2005
 */
public class InfantryRifleFederatedBarrettM61ALaserWeapon extends InfantryWeapon {
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryRifleFederatedBarrettM61ALaserWeapon() {
        super();

        name = "Laser Rifle (Federated-Barrett M61A)";
        setInternalName("InfantryFederatedBarrettM61A");
        addLookupName(name);
        addLookupName("Federated Barrett M61A");
        ammoType = AmmoType.T_INFANTRY;
        cost = 2150;
        bv = 3.83;
        tonnage = .006;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_INF_BURST);
        infantryDamage = 0.75;
        infantryRange = 2;
        ammoWeight = 0.0003;
        shots = 6;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3058, 3062, 3085, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_C);

    }
}
