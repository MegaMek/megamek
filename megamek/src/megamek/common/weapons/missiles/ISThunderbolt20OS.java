/*
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

import megamek.common.SimpleTechLevel;
/**
 * The One-Shot Thunderbolt 20
 * @author Simon (Juliez)
 */
public class ISThunderbolt20OS extends Thunderbolt20Weapon {

    public ISThunderbolt20OS() {
        super();
        name = "Thunderbolt 20 (OS)";
        setInternalName(name);
        addLookupName("IS OS Thunderbolt-20");
        addLookupName("ISThunderbolt20 (OS)");
        addLookupName("ISTBolt20OS");
        addLookupName("IS Thunderbolt 20 (OS)");
        tonnage = 15.5;
        bv = 61;
        cost = 225000;
        flags = flags.or(F_ONESHOT);
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
