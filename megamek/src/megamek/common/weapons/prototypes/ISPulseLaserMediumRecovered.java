/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 8, 2005
 *
 */
package megamek.common.weapons.prototypes;

import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PrototypeLaserHandler;
import megamek.common.weapons.lasers.PulseLaserWeapon;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class ISPulseLaserMediumRecovered extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -8402915088560062495L;

    /**
     *
     */
    public ISPulseLaserMediumRecovered() {
        super();
        name = "Medium Pulse Laser (Recoverd Prototype)";
        setInternalName("ISMediumPulseLaserRecovered");
        addLookupName("IS Pulse Med Laser Recovered");
        flags = flags.or(F_PROTOTYPE);
        heat = 4;
        damage = 6;
        toHitModifier = -2;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        waterShortRange = 2;
        waterMediumRange = 3;
        waterLongRange = 4;
        waterExtremeRange = 6;
        tonnage = 2.0;
        criticals = 1;
        bv = 48;
        cost = 240000;
        rulesRefs = "103,IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
            .setIntroLevel(false)
            .setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_F, RATING_X, RATING_X)
            .setISAdvancement(3031, DATE_NONE, DATE_NONE, 3037, DATE_NONE)
            .setISApproximate(true, false, false, true, false)
            .setPrototypeFactions(F_FS,F_DC);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     * megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new PrototypeLaserHandler(toHit, waa, game, server);
    }
}
