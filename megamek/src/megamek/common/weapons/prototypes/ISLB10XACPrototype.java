/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.prototypes;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PrototypeACWeaponHandler;
import megamek.common.weapons.PrototypeLBXHandler;
import megamek.common.weapons.autocannons.LBXACWeapon;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 * @since Oct 15, 2004
 */
public class ISLB10XACPrototype extends LBXACWeapon {
    private static final long serialVersionUID = 4586376672142168553L;

    public ISLB10XACPrototype() {
        super();
        name = "Prototype LB 10-X Autocannon";
        setInternalName("ISLBXAC10Prototype");
        addLookupName("IS LB 10-X AC Prototype");
        shortName = "LB 10-X (P)";
        sortingName = "LB Proto 10-X AC";
        flags = flags.or(F_PROTOTYPE);
        criticals = 7;
        heat = 2;
        damage = 10;
        rackSize = 10;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 11.0;
        bv = 148;
        cost = 2000000; // Cost in the AoW is 160000 but not making another version for one field.
        rulesRefs = "71, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(2590, DATE_NONE, DATE_NONE, 2595, 3035)
                .setISApproximate(false, false, false, true, true)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              Server server) {
        AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId())
                .getEquipment(waa.getWeaponId()).getLinked().getType();
        if (atype.getMunitionType() == AmmoType.M_CLUSTER) {
            return new PrototypeLBXHandler(toHit, waa, game, server);
        }
        return new PrototypeACWeaponHandler(toHit, waa, game, server);
    }
}
