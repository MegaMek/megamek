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
 * Created on Oct 1, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class ISUAC5Prototype extends UACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -2740269177146528640L;

    /**
     * 
     */
    public ISUAC5Prototype() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "Ultra AC/5 Prototype";
        this.setInternalName("ISUltraAC5Prototype");
        this.addLookupName("IS Ultra AC/5 Prototype");
        this.flags |= F_PROTOTYPE;
        this.heat = 1;
        this.damage = 5;
        this.rackSize = 5;
        this.minimumRange = 2;
        this.shortRange = 6;
        this.mediumRange = 13;
        this.longRange = 20;
        this.extremeRange = 26;
        this.tonnage = 9.0f;
        this.criticals = 6;
        this.bv = 112;
        this.cost = 200000;
        this.explosionDamage = damage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.Game)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        Mounted weapon = game.getEntity(waa.getEntityId()).getEquipment(
                waa.getWeaponId());
        if (weapon.curMode().equals("Ultra")) {
            return new PrototypeUltraWeaponHandler(toHit, waa, game, server);
        } else {
            return super.getCorrectHandler(toHit, waa, game, server);
        }
    }
}
