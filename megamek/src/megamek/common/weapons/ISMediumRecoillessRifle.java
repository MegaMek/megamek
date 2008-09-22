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
 * Created on Sep 24, 2004
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
 * @author Andrew Hunter
 */
public class ISMediumRecoillessRifle extends Weapon {
    /**
     * 
     */
    private static final long serialVersionUID = -2795333414856085616L;

    /**
     * 
     */
    public ISMediumRecoillessRifle() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Medium Recoilless Rifle";
        this.setInternalName(this.name);
        this.addLookupName("ISMedium Recoilless Rifle");
        this.damage = 3;
        this.ammoType = AmmoType.T_NA;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.bv = 19;
        this.flags |= F_DIRECT_FIRE | F_NO_FIRES | F_BALLISTIC;
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
        return new MediumRecoillessHandler(toHit, waa, game, server);
    }
}
