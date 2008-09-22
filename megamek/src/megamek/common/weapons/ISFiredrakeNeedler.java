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
 * @author Sebastian Brocks
 */
public class ISFiredrakeNeedler extends Weapon {
    /**
     * 
     */
    private static final long serialVersionUID = -8852176757815947141L;

    /**
     * 
     */
    public ISFiredrakeNeedler() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Firedrake Needler";
        this.setInternalName(this.name);
        this.addLookupName("ISFiredrakeIncendiaryNeedler");
        this.damage = 1;
        this.ammoType = AmmoType.T_NA;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.bv = 2;
        this.flags |= F_DIRECT_FIRE | F_BALLISTIC | F_INCENDIARY_NEEDLES;
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
        return new FiredrakeHandler(toHit, waa, game, server);
    }
}
