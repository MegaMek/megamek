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
 * Created on May 3 2014
 *
 */
package megamek.common.weapons.prototypes;

import megamek.common.IGame;
import megamek.common.TechAdvancement;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PrototypeLaserHandler;
import megamek.common.weapons.lasers.PulseLaserWeapon;
import megamek.server.Server;

/**
 * @author David Nawton
 */
public class ISPulseLaserLargePrototype extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 94533476706680275L;

    /**
     *
     */
    public ISPulseLaserLargePrototype() {
        super();
        name = "Large Pulse Laser Prototype";
        setInternalName("ISLargePulseLaserPrototype");
        addLookupName("IS Pulse Large Laser Prototype");
        addLookupName("IS Prototype Large Pulse Laser");
        flags = flags.or(F_PROTOTYPE);
        heat = 10;
        damage = 9;
        toHitModifier = -1;
        shortRange = 3;
        mediumRange = 7;
        longRange = 10;
        extremeRange = 14;
        waterShortRange = 2;
        waterMediumRange = 5;
        waterLongRange = 7;
        waterExtremeRange = 10;
        tonnage = 7.0f;
        criticals = 2;
        bv = 119;
        cost = 175000;
        shortAV = 9;
        medAV = 9;
        maxRange = RANGE_MED;
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2595, DATE_NONE, DATE_NONE, 2609, 3037);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability( new int[] { RATING_E, RATING_F, RATING_E, RATING_X });
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

