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
import megamek.common.TechAdvancement;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PrototypeLaserHandler;
import megamek.common.weapons.lasers.PulseLaserWeapon;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class ISMediumPulseLaserPrototype extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -8402915088560062495L;

    /**
     *
     */
    public ISMediumPulseLaserPrototype() {
        super();
        name = "Medium Pulse Laser Prototype";
        setInternalName("ISMediumPulseLaserPrototype");
        addLookupName("IS Pulse Med Laser Prototype");
        addLookupName("IS Medium Pulse Laser Prototype");
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
        tonnage = 2.0f;
        criticals = 1;
        bv = 48;
        cost = 60000;
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2595, DATE_NONE, DATE_NONE, 2609, 3037);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability( new int[] { RATING_E, RATING_F, RATING_D, RATING_X });
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
