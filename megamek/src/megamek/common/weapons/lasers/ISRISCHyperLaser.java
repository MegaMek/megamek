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
package megamek.common.weapons.lasers;

import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.HyperLaserHandler;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class ISRISCHyperLaser extends LaserWeapon {
    private static final long serialVersionUID = 4467522144065588079L;

    public ISRISCHyperLaser() {
        super();
        name = "RISC Hyper Laser";

        setInternalName("ISRISCHyperLaser");
        heat = 24;
        damage = 20;
        toHitModifier = 0;
        shortRange = 8;
        mediumRange = 15;
        longRange = 25;
        extremeRange = 30;
        waterShortRange = 5;
        waterMediumRange = 10;
        waterLongRange = 18;
        waterExtremeRange = 20;
        tonnage = 8.0;
        criticals = 6;
        bv = 596;
        cost = 750000;
        shortAV = 20;
        medAV = 20;
        maxRange = RANGE_EXT;
        explosionDamage = 10;
        explosive = true;
        rulesRefs = "93, IO";
        this.flags = flags.or(F_LASER).or(F_DIRECT_FIRE).or(F_HYPER);
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3134, DATE_NONE, DATE_NONE, 3141, DATE_NONE)
                .setPrototypeFactions(F_RS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              Server server) {
        return new HyperLaserHandler(toHit, waa, game, server);
    }
}
