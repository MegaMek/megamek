/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter TODO: is this the right hierarchy location?
 */
public abstract class RACWeapon extends UACWeapon {

    
    private static final long serialVersionUID = 659000035767322660L;

    /**
     * 
     */
    public RACWeapon() {
        super();
        this.ammoType = AmmoType.T_AC_ROTARY;
        String[] modes = { "Single", "2-shot", "3-shot", "4-shot", "5-shot",
                "6-shot" };
        this.setModes(modes);
        // explosive when jammed
        this.explosive = true;
        this.explosionDamage = damage;
        
        this.atClass = CLASS_AC;
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
        Mounted weapon = game.getEntity(waa.getEntityId()).getEquipment(
                waa.getWeaponId());
        if (weapon.curMode().equals("6-shot")
                || weapon.curMode().equals("5-shot")
                || weapon.curMode().equals("4-shot")
                || weapon.curMode().equals("3-shot")) {
            return new RACHandler(toHit, waa, game, server);
        } else if (weapon.curMode().equals("2-shot")) {
            return new UltraWeaponHandler(toHit, waa, game, server);
        } else {
            return new ACWeaponHandler(toHit, waa, game, server);
        }
    }
}
