/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.missiles;

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class ISThunderBolt10 extends ThunderBoltWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 6781882739979127656L;

    /**
     *
     */
    public ISThunderBolt10() {
        super();
        name = "Thunderbolt 10";
        setInternalName(name);
        addLookupName("IS Thunderbolt-10");
        addLookupName("ISThunderbolt10");
        addLookupName("IS Thunderbolt 10");
        addLookupName("ISTBolt10");
        ammoType = AmmoType.T_TBOLT_10;
        heat = 5;
        minimumRange = 5;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        shortAV = 10;
        medAV = 10;
        maxRange = RANGE_MED;
        tonnage = 7.0;
        criticals = 2;
        bv = 127;
        cost = 175000;
        flags = flags.or(F_LARGEMISSILE);
        this.missileArmor = 10;
        rulesRefs = "347, TO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3052, 3072, 3081, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
