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
package megamek.common.weapons.prototypes;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechAdvancement;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.LBXACWeapon;
import megamek.common.weapons.PrototypeACWeaponHandler;
import megamek.common.weapons.PrototypeLBXHandler;
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
        name = "LB 10-X AC Prototype";
        setInternalName("ISLBXAC10Prototype");
        addLookupName("IS LB 10-X AC Prototype");
        flags = flags.or(F_PROTOTYPE);
        criticals = 7;
        heat = 2;
        damage = 10;
        rackSize = 10;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 11.0f;
        bv = 148;
        cost = 400000;
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2590, DATE_NONE, DATE_NONE, 2595, 3035);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability( new int[] { RATING_E, RATING_F, RATING_D, RATING_X });
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId())
                .getEquipment(waa.getWeaponId()).getLinked().getType();
        if (atype.getMunitionType() == AmmoType.M_CLUSTER) {
            return new PrototypeLBXHandler(toHit, waa, game, server);
        }
        return new PrototypeACWeaponHandler(toHit, waa, game, server);
    }
}
