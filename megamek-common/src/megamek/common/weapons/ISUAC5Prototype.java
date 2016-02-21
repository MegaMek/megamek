/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
/*
 * Created on Oct 1, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class ISUAC5Prototype extends UACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -2740269177146528640L;

    /**
     *
     */
    public ISUAC5Prototype() {
        super();
        techLevel.put(2635, TechConstants.T_IS_EXPERIMENTAL);
        name = "Ultra AC/5 Prototype";
        setInternalName("ISUltraAC5Prototype");
        addLookupName("IS Ultra AC/5 Prototype");
        flags = flags.or(F_PROTOTYPE);
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 2;
        shortRange = 6;
        mediumRange = 13;
        longRange = 20;
        extremeRange = 26;
        tonnage = 9.0f;
        criticals = 6;
        bv = 112;
        cost = 200000;
        explosionDamage = damage;
        shortAV = 7;
        medAV = 7;
        longAV = 7;
        maxRange = RANGE_LONG;
        introDate = 2635;
        extinctDate = 2640;
        reintroDate = 3035;
        availRating = new int[] { RATING_E, RATING_F, RATING_D };
        techRating = RATING_E;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        Mounted weapon = game.getEntity(waa.getEntityId()).getEquipment(
                waa.getWeaponId());
        if (weapon.curMode().equals("Ultra")) {
            return new PrototypeISUltraWeaponHandler(toHit, waa, game, server);
        }
        return super.getCorrectHandler(toHit, waa, game, server);
    }
}
