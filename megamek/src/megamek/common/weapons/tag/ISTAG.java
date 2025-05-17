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
package megamek.common.weapons.tag;

/**
 * @author Sebastian Brocks
 * @since Sep 7, 2005
 */
public class ISTAG extends TAGWeapon {
    private static final long serialVersionUID = -2474477168563228542L;

    public ISTAG() {
        super();
        name = "TAG";
        setInternalName("ISTAG");
        addLookupName("IS TAG");
        tonnage = 1;
        criticals = 1;
        hittable = true;
        spreadable = false;
        heat = 0;
        damage = 0;
        shortRange = 5;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        bv = 0;
        cost = 50000;
        rulesRefs = "238, TM";
        flags = flags.andNot(F_PROTO_WEAPON).andNot(F_BA_WEAPON);
        techAdvancement.setTechBase(TechBase.IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
                .setISAdvancement(2593, 2600, 3045, 2835, 3044)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.FS, Faction.LC);
    }
}
