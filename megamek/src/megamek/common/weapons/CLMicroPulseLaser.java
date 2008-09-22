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
 * Created on Sep 12, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class CLMicroPulseLaser extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -3335298535182304490L;

    /**
     * 
     */
    public CLMicroPulseLaser() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Micro Pulse Laser";
        this.setInternalName("CLMicroPulseLaser");
        this.addLookupName("Clan Micro Pulse Laser");
        this.heat = 1;
        this.damage = 3;
        this.toHitModifier = -2;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.waterShortRange = 1;
        this.waterMediumRange = 2;
        this.waterLongRange = 2;
        this.waterExtremeRange = 4;
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.flags |= F_NO_FIRES;
        this.bv = 12;
        this.cost = 12500;
        this.shortAV = 3;
        this.maxRange = RANGE_SHORT;
        this.atClass = CLASS_POINT_DEFENSE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     *      megamek.server.Server)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new BurstPulseLaserWeaponHandler(toHit, waa, game, server);
    }
}
