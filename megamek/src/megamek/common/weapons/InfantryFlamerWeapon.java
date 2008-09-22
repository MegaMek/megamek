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
 * Created on Sep 7, 2005
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
public class InfantryFlamerWeapon extends InfantryWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -5741978934100309295L;

    public InfantryFlamerWeapon() {
        super();
        this.techLevel = TechConstants.T_INTRO_BOXSET;
        this.name = "Infantry Flamer";
        this.setInternalName(this.name);
        this.addLookupName("InfantryFlamer");
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 3;
        // Flamer (man-portable), TM p. 319
        this.bv = 0.36;
        this.flags |= F_DIRECT_FIRE | F_FLAMER | F_ENERGY;
        String modes[] = { "Damage", "Heat" };
        this.setModes(modes);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        if ((game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId())
                .curMode().equals("Heat"))) {
            return new InfantryFlamerHeatHandler(toHit, waa, game, server);
        }
        return new InfantryFlamerHandler(toHit, waa, game, server);
    }
}
