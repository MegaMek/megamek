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
package megamek.common.weapons.prototypes;

import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PrototypeISUltraWeaponHandler;
import megamek.common.weapons.autocannons.UACWeapon;
import megamek.server.GameManager;

/**
 * @author Andrew Hunter
 * @since Oct 1, 2004
 */
public class ISUAC5Prototype extends UACWeapon {
    private static final long serialVersionUID = -2740269177146528640L;

    public ISUAC5Prototype() {
        super();
        name = "Prototype Ultra Autocannon/5";
        setInternalName("ISUltraAC5Prototype");
        addLookupName("IS Ultra AC/5 Prototype");
        shortName = "Ultra AC/5 (P)";
        flags = flags.or(F_PROTOTYPE);
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 2;
        shortRange = 6;
        mediumRange = 13;
        longRange = 20;
        extremeRange = 26;
        tonnage = 9.0;
        criticals = 6;
        bv = 112;
        cost = 1000000;
        explosionDamage = damage;
        shortAV = 7;
        medAV = 7;
        longAV = 7;
        maxRange = RANGE_LONG;
        flags = flags.or(F_PROTOTYPE);
        rulesRefs = "104, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(3029, DATE_NONE, DATE_NONE, 3035, DATE_NONE)
                .setISApproximate(false, false, false, true, false)
                .setPrototypeFactions(F_FS)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        Mounted weapon = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());
        if (weapon.curMode().equals("Ultra")) {
            return new PrototypeISUltraWeaponHandler(toHit, waa, game, manager);
        }
        return super.getCorrectHandler(toHit, waa, game, manager);
    }
}
