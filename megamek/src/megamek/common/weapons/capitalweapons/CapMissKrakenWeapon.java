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
package megamek.common.weapons.capitalweapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.CapitalMissileHandler;
import megamek.common.weapons.TeleMissileHandler;
import megamek.server.Server;

/**
 * @author Ben Grills
 */
public class CapMissKrakenWeapon extends CapitalMissileWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public CapMissKrakenWeapon() {
        super();
        this.name = "Capital Missile Launcher (Kraken)";
        this.setInternalName(this.name);
        this.addLookupName("Kraken");
        this.heat = 50;
        this.damage = 10;
        this.ammoType = AmmoType.T_KRAKENM;
        this.shortRange = 11;
        this.mediumRange = 22;
        this.longRange = 34;
        this.extremeRange = 46;
        this.tonnage = 190.0f;
        this.bv = 1914;
        this.cost = 455000;
        this.flags = flags.or(F_MISSILE);
        this.shortAV = 10;
        this.medAV = 10;
        this.longAV = 10;
        this.extAV = 10;
        this.missileArmor = 100;
        this.maxRange = RANGE_EXT;
        rulesRefs = "Unoffical";
        techAdvancement.setTechBase(TECH_BASE_IS)
    	.setIntroLevel(false)
    	.setUnofficial(true)
        .setTechRating(RATING_F)
        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
        .setISAdvancement(3053, 3057, 3060, DATE_NONE, DATE_NONE)
        .setISApproximate(false, false, false,false, false)
        .setPrototypeFactions(F_CS,F_DC)
        .setProductionFactions(F_DC);
        
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
        if (atype.hasFlag(AmmoType.F_TELE_MISSILE) && game.getBoard().inSpace()) {
            return new TeleMissileHandler(toHit, waa, game, server);
        }
        return new CapitalMissileHandler (toHit, waa, game, server);
    }
}
