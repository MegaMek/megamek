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

import megamek.common.AmmoType;
import megamek.common.EquipmentTypeLookup;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.Weapon;

/**
 * Added per IO Pg 53 - Tech Manual shows this is an IS weapon only
 * But IO seems to have made this a Clan weapon as well
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class CLBAMortarHeavy extends Weapon {
    private static final long serialVersionUID = -141763207003813118L;

    public CLBAMortarHeavy() {
        super();
        name = "Mortar (Heavy)";
        setInternalName(EquipmentTypeLookup.CL_BA_MORTAR_HEAVY);
        addLookupName("CL BA Heavy Mortar");
        addLookupName("ISBAHeavyMortar");
        addLookupName("IS BA Heavy Mortar");
        sortingName = "Mortar D";
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        ammoType = AmmoType.T_NA;
        minimumRange = 2;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        bv = 17;
        cost = 7500;
        tonnage = 0.4;
        criticals = 2;
        flags = flags.or(F_BALLISTIC).or(F_BURST_FIRE).or(F_BA_WEAPON).or(F_MORTARTYPE_INDIRECT)
                .andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "263, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setISAdvancement(3054, 3057, 3063, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3065, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_LC);
    }

    @Override
    public boolean hasIndirectFire() {
        return true;
    }

    @Override
    public void adaptToGameOptions(GameOptions gOp) {
        super.adaptToGameOptions(gOp);

        // Indirect Fire
        if (gOp.booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            addMode("");
            addMode("Indirect");
        } else {
            removeMode("");
            removeMode("Indirect");
        }
    }

    @Override
    public double getBattleForceDamage(int range, Mounted linked) {
        if (range <= AlphaStrikeElement.SHORT_RANGE) {
            return 0.249;
        } else if (range <= AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.3;
        } else {
            return 0;
        }
    }

    @Override
    public boolean isAlphaStrikeIndirectFire() {
        return true;
    }
}