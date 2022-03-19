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
package megamek.common.weapons.lasers;

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 * @since Sep 8, 2005
 */
public class ISXPulseLaserMedium extends PulseLaserWeapon {
    private static final long serialVersionUID = -6576828912486084151L;

    public ISXPulseLaserMedium() {
        super();
        name = "Medium X-Pulse Laser";
        setInternalName("ISMediumXPulseLaser");
        addLookupName("IS X-Pulse Med Laser");
        addLookupName("IS Medium X-Pulse Laser");
        sortingName = "Laser XPULSE C";
        heat = 6;
        damage = 6;
        toHitModifier = -2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        waterShortRange = 2;
        waterMediumRange = 4;
        waterLongRange = 6;
        waterExtremeRange = 8;
        maxRange = RANGE_SHORT;
        shortAV = 6;
        tonnage = 2.0;
        criticals = 1;
        bv = 71;
        cost = 110000;
        rulesRefs = "321, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(DATE_NONE, 3047, 3078, DATE_NONE, DATE_NONE).setPrototypeFactions(F_LC,F_FS)
            .setProductionFactions(F_LC)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
