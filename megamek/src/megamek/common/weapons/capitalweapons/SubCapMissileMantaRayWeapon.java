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
import megamek.common.weapons.MantaRayHandler;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class SubCapMissileMantaRayWeapon extends SubCapMissileWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 2277255235440703333L;

    /**
     * 
     */
    public SubCapMissileMantaRayWeapon() {
        super();
        this.name = "Sub-Capital Missile Launcher (Manta Ray)";
        this.setInternalName(this.name);
        this.addLookupName("MantaRay");
        this.addLookupName("Manta Ray");
        this.heat = 21;
        this.damage = 5;
        this.ammoType = AmmoType.T_MANTA_RAY;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 160.0f;
        this.bv = 396;
        this.cost = 150000;
        this.flags = flags.or(F_MISSILE);
        this.shortAV = 5;
        this.missileArmor = 50;
        this.maxRange = RANGE_SHORT;
        rulesRefs = "345,TO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
            .setIntroLevel(false)
            .setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
            .setISAdvancement(3066, 3072, 3145, DATE_NONE, DATE_NONE)
            .setISApproximate(true, false, false, false, false)
            .setClanAdvancement(DATE_NONE,DATE_NONE,3073,DATE_NONE,DATE_NONE)
            .setISApproximate(true, false, false, false, false)
            .setPrototypeFactions(F_WB)
            .setProductionFactions(F_WB);
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
        return new MantaRayHandler(toHit, waa, game, server);
    }
}
