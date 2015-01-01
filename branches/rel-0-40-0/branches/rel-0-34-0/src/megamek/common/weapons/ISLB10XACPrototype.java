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
 * Created on Oct 15, 2004
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
public class ISLB10XACPrototype extends LBXACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 4586376672142168553L;

    /**
     * 
     */
    public ISLB10XACPrototype() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "LB 10-X AC Prototype";
        this.setInternalName("ISLBXAC10Prototype");
        this.addLookupName("IS LB 10-X AC Prototype");
        this.flags |= F_PROTOTYPE;
        this.criticals = 7;
        this.heat = 2;
        this.damage = 10;
        this.rackSize = 10;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 11.0f;
        this.bv = 148;
        this.cost = 400000;
    }

    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId())
                .getEquipment(waa.getWeaponId()).getLinked().getType();
        if (atype.getMunitionType() == AmmoType.M_CLUSTER) {
            return new PrototypeLBXHandler(toHit, waa, game, server);
        } else {
            return super.getCorrectHandler(toHit, waa, game, server);
        }
    }
}
