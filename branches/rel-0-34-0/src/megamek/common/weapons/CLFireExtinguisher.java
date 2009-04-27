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

import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class CLFireExtinguisher extends Weapon {
    /**
     * 
     */
    private static final long serialVersionUID = -5190894967392738394L;

    /**
     * 
     */
    public CLFireExtinguisher() {
        super();
        this.techLevel = TechConstants.T_CLAN_ADVANCED;
        this.name = "Fire Extinguisher";
        this.setInternalName(this.name);
        this.heat = 0;
        this.damage = 0;
        this.shortRange = 1;
        this.mediumRange = 1;
        this.longRange = 1;
        this.extremeRange = 1;
        this.tonnage = 0.0f;
        this.criticals = 0;
        this.flags |= F_SOLO_ATTACK | F_NO_FIRES;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new FireExtinguisherHandler(toHit, waa, game, server);
    }
}
