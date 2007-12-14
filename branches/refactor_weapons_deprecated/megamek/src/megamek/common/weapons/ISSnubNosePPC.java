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
 * Created on Sep 13, 2004
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
 * 
 */
public class ISSnubNosePPC extends PPCWeapon {
    /**
     * 
     */
    public ISSnubNosePPC() {
        super();

        this.techLevel = TechConstants.T_IS_LEVEL_2;
        this.name = "Snub Nose PPC";
        this.setInternalName("ISSNPPC");
        this.addLookupName("ISSnubNosedPPC");
        this.heat = 10;
        this.damage = DAMAGE_VARIABLE;
        this.minimumRange = 0;
        this.shortRange = 9;
        this.mediumRange = 13;
        this.longRange = 15;
        this.extremeRange = 26;
        this.waterShortRange = 6;
        this.waterMediumRange = 8;
        this.waterLongRange = 9;
        this.waterExtremeRange = 16;
        this.tonnage = 6.0f;
        this.criticals = 2;
        this.bv = 165;
        this.setModes(new String[] {"Field Inhibitor ON", "Field Inhibitor OFF"});
        this.cost = 400000;
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
        return new SNPPCHandler(toHit, waa, game, server);
    }
    
}
