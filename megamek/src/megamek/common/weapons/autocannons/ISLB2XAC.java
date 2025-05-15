/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.autocannons;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Mounted;

/**
 * @author Andrew Hunter
 * @since Oct 15, 2004
 */
public class ISLB2XAC extends LBXACWeapon {
    private static final long serialVersionUID = -8906248294693269096L;

    public ISLB2XAC() {
        super();
        name = "LB 2-X AC";
        setInternalName("ISLBXAC2");
        addLookupName("IS LB 2-X AC");
        sortingName = "LB 02-X AC";
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 4;
        shortRange = 9;
        mediumRange = 18;
        longRange = 27;
        extremeRange = 36;
        tonnage = 6.0;
        criticals = 4;
        bv = 42;
        cost = 150000;
        shortAV = getBaseAeroDamage();
        medAV = shortAV;
        longAV = shortAV;
        extAV = shortAV;
        maxRange = RANGE_EXT;
        rulesRefs = "207, TM";
        techAdvancement.setTechBase(TechBase.IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(TechRating.X, TechRating.X, TechRating.E, TechRating.D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.FS)
                .setProductionFactions(Faction.FS);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        return (range <= AlphaStrikeElement.SHORT_RANGE) ? 0.069 : 0.105;
    }
}
