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
 * @author Sebastian Brocks
 */
public class ISHeavyPPC extends PPCWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -7742604546239137754L;

    /**
     * 
     */
    public ISHeavyPPC() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Heavy PPC";
        this.setInternalName(this.name);
        this.addLookupName("ISHeavyPPC");
        this.addLookupName("ISHPPC");
        this.heat = 15;
        this.damage = 15;
        this.minimumRange = 3;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.waterShortRange = 4;
        this.waterMediumRange = 7;
        this.waterLongRange = 10;
        this.waterExtremeRange = 14;
        this.setModes(new String[] { "Field Inhibitor ON",
                "Field Inhibitor OFF" });
        this.tonnage = 10.0f;
        this.criticals = 4;
        this.bv = 317;
        this.shortAV = 15;
        this.medAV = 15;
        this.maxRange = RANGE_MED;
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
        return new PPCHandler(toHit, waa, game, server);
    }

}
