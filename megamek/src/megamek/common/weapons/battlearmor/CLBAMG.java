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
package megamek.common.weapons.battlearmor;

import megamek.common.WeaponType;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class CLBAMG extends BAMGWeapon {
    private static final long serialVersionUID = -5021714235121936669L;

    public CLBAMG() {
        super();
        name = "Machine Gun (Medium)";
        setInternalName("CLBAMG");
        addLookupName("Clan BA Machine Gun");
        addLookupName("ISBAMG");
        addLookupName("IS BA Machine Gun");
        addLookupName("ISBAMachine Gun");
        addLookupName("ISBAMachineGun");
        sortingName = "MG C";
        heat = 0;
        damage = 2;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        rackSize = 2;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 0.1;
        criticals = 1;
        bv = 5;
        cost = 5000;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_BA_WEAPON).or(F_BURST_FIRE)
                .andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "258, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_D, RATING_B, RATING_B)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3050, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, 2868, 2870, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setProductionFactions(F_CWF);
    }
}
