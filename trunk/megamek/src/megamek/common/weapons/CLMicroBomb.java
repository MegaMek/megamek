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
public class CLMicroBomb extends Weapon {
    /**
     * 
     */
    private static final long serialVersionUID = 1467436625346131281L;

    /**
     * 
     */
    public CLMicroBomb() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Micro Bomb";
        this.setInternalName("CLMicroBomb");
        this.addLookupName("CLMicro Bomb");
        this.heat = 0;
        this.damage = DAMAGE_VARIABLE;
        this.rackSize = 2;
        this.ammoType = AmmoType.T_BA_MICRO_BOMB;
        this.shortRange = 0;
        this.mediumRange = 0;
        this.longRange = 0;
        this.extremeRange = 0;
        this.bv = 0;
        this.flags |= F_NO_FIRES;
    }

    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new MicroBombHandler(toHit, waa, game, server);
    }
}
