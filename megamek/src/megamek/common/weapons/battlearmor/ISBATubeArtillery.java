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
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.artillery.ArtilleryWeapon;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class ISBATubeArtillery extends ArtilleryWeapon {
    private static final long serialVersionUID = -2803991494958411097L;

    public ISBATubeArtillery() {
        super();
        name = "Tube Artillery (BA)";
        setInternalName(EquipmentTypeLookup.IS_BA_TUBE_ARTY);
        rackSize = 3;
        ammoType = AmmoType.T_BA_TUBE;
        shortRange = 2;
        mediumRange = 2;
        longRange = 2;
        extremeRange = 2; // No extreme range.
        tonnage = 0.5;
        criticals = 4;
        bv = 27;
        cost = 200000;
        rulesRefs = "284, TO";
        flags = flags.or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).or(F_MEK_MORTAR).or(F_MISSILE);
        damage = DAMAGE_BY_CLUSTERTABLE;
        atClass = CLASS_NONE;
        flags = flags.or(F_MEK_MORTAR).or(F_MECH_WEAPON).or(F_MISSILE)
                .or(F_TANK_WEAPON);
        infDamageClass = WEAPON_CLUSTER_MISSILE;
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3070, 3075, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_CS)
                .setProductionFactions(F_CS);
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
}
