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
import megamek.common.WeaponType;

/**
 * @author Sebastian Brocks
 * @since Sep 8, 2005
 */
public class ISXPulseLaserSmall extends PulseLaserWeapon {
    private static final long serialVersionUID = 5322977585378755226L;

    public ISXPulseLaserSmall() {
        super();
        name = "Small X-Pulse Laser";
        setInternalName("ISSmallXPulseLaser");
        addLookupName("IS X-Pulse Small Laser");
        addLookupName("IS Small X-Pulse Laser");
        sortingName = "Laser XPULSE B";
        heat = 3;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        toHitModifier = -2;
        shortRange = 2;
        mediumRange = 4;
        longRange = 5;
        extremeRange = 7;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 3;
        waterExtremeRange = 4;
        tonnage = 1.0;
        criticals = 1;
        bv = 21;
        maxRange = RANGE_SHORT;
        shortAV = 3;
        cost = 31000;
        flags = flags.or(F_BURST_FIRE).andNot(F_PROTO_WEAPON);
        rulesRefs = "321, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
            .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
            .setISAdvancement(DATE_NONE, 3047, 3078, DATE_NONE, DATE_NONE).setPrototypeFactions(Faction.LC,Faction.FS)
            .setProductionFactions(Faction.LC)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public boolean isAlphaStrikePointDefense() {
        return true;
    }
}
