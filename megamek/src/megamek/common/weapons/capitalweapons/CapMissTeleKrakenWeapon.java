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
import megamek.common.weapons.KrakenTHandler;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class CapMissTeleKrakenWeapon extends CapitalMissileWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public CapMissTeleKrakenWeapon() {
        super();
        this.name = "Tele-operated Missile (Kraken T)";
        this.setInternalName(this.name);
        this.addLookupName("KrakenT");
        this.addLookupName("Kraken T");
        String[] modeStrings = { "Normal", "Tele-Operated" };
        setModes(modeStrings);
        setInstantModeSwitch(false);
        this.heat = 50;
        this.damage = 10;
        this.ammoType = AmmoType.T_KRAKEN_T;
        this.shortRange = 11;
        this.mediumRange = 22;
        this.longRange = 34;
        this.extremeRange = 46;
        this.tonnage = 220.0f;
        this.bv = 2306;
        this.cost = 500000;
        this.flags = flags.or(F_MISSILE);
        this.atClass = CLASS_TELE_MISSILE;
        this.shortAV = 10;
        this.medAV = 10;
        this.longAV = 10;
        this.extAV = 10;
        this.missileArmor = 100;
        this.maxRange = RANGE_EXT;
        rulesRefs = "210,TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
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
     * megamek.common.actions.WeaponAttackAction, megamek.common.IGame,
     * megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        if (game.getBoard().inSpace()) {
            return new KrakenTHandler(toHit, waa, game, server);
        }
        return new CapitalMissileHandler(toHit, waa, game, server);
    }
}
