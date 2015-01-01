/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 8, 2005
 *
 */
package megamek.common.weapons;

import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class ISMediumPulseLaserPrototype extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -8402915088560062495L;

    /**
     * 
     */
    public ISMediumPulseLaserPrototype() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "Medium Pulse Laser Prototype";
        this.setInternalName("ISMediumPulseLaserPrototype");
        this.addLookupName("IS Pulse Med Laser Prototype");
        this.addLookupName("IS Medium Pulse Laser Prototype");
        this.flags |= F_PROTOTYPE;
        this.heat = 4;
        this.damage = 6;
        this.toHitModifier = -2;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.waterShortRange = 2;
        this.waterMediumRange = 3;
        this.waterLongRange = 4;
        this.waterExtremeRange = 6;
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 48;
        this.cost = 60000;
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
