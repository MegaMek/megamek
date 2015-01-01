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
public class ISERLargeLaserPrototype extends LaserWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -4745756742469577788L;

    public ISERLargeLaserPrototype() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "ER Large Laser Prototype";
        this.setInternalName("ISERLargeLaserPrototype");
        this.addLookupName("IS ER Large Laser Prototype");
        this.toHitModifier = 1;
        this.flags |= F_PROTOTYPE;
        this.heat = 12;
        this.damage = 8;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 19;
        this.extremeRange = 28;
        this.waterShortRange = 3;
        this.waterMediumRange = 9;
        this.waterLongRange = 12;
        this.waterExtremeRange = 18;
        this.tonnage = 5.0f;
        this.criticals = 2;
        this.bv = 163;
        this.cost = 200000;
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
        return new PrototypeLaserHandler(toHit, waa, game, server);
    }
}
