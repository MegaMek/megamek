/*
 * MegaMek -
 * Copyright (C) 2000-2007 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.primitive;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.SRMHandler;
import megamek.common.weapons.srms.SRMWeapon;
import megamek.server.GameManager;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISSRM6Primitive extends SRMWeapon {
    private static final long serialVersionUID = -2484546662230634777L;

    public ISSRM6Primitive() {
        super();

        name = "Primitive Prototype SRM 6";
        setInternalName(this.name);
        addLookupName("IS SRM-6 Primitive");
        addLookupName("ISSRM6p");
        addLookupName("IS SRM 6 Primitive");
        shortName = "SRM/6p";
        sortingName = "SRM Proto 6";
        flags = flags.or(F_PROTOTYPE).andNot(F_ARTEMIS_COMPATIBLE);
        heat = 4;
        rackSize = 6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 3.0;
        criticals = 2;
        bv = 59;
        cost = 80000;
        shortAV = 8;
        maxRange = RANGE_SHORT;
        ammoType = AmmoType.T_SRM_PRIMITIVE;
        // IO Doesn't strictly define when these weapons stop production. Checked with Herb, and
        // they would always be around. This to cover some of the back worlds in the Periphery.
        rulesRefs = "118, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2365, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        return new SRMHandler(toHit, waa, game, manager, -2);
    }
}
