/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class ISAR10Weapon extends CapitalMissileWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public ISAR10Weapon() {
        super();
        // assume a barracuda is loaded
        this.techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        this.name = "AR10";
        this.setInternalName(this.name);
        this.addLookupName("AR10");
        this.heat = 20; // This should reflect the maximum possible heat
        this.damage = 2;
        this.ammoType = AmmoType.T_AR10;
        this.shortRange = 20;
        this.mediumRange = 30;
        this.longRange = 40;
        this.extremeRange = 50;
        this.tonnage = 250.0f;
        this.bv = 961;
        this.cost = 250000;
        // assume Barracuda is loaded
        this.shortAV = 2;
        this.medAV = 2;
        this.longAV = 2;
        this.extAV = 2;
        this.maxRange = RANGE_EXT;
        this.atClass = CLASS_AR10;
        introDate = 2550;
        techLevel.put(2550, techLevel.get(3071));
        extinctDate = 2850;
        reintroDate = 3051;
        techLevel.put(3051, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_E, RATING_F, RATING_E };
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId())
                .getEquipment(waa.getWeaponId()).getLinked().getType();
        if (atype.hasFlag(AmmoType.F_NUCLEAR)) {
            return new SantaAnnaHandler(toHit, waa, game, server);
        }
        if (atype.hasFlag(AmmoType.F_TELE_MISSILE)) {
            return new TeleMissileHandler(toHit, waa, game, server);
        }
        return new AR10Handler(toHit, waa, game, server);
    }
}
