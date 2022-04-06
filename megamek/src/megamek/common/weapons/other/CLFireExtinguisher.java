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
package megamek.common.weapons.other;

import megamek.common.Game;
import megamek.common.TechAdvancement;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.FireExtinguisherHandler;
import megamek.common.weapons.Weapon;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class CLFireExtinguisher extends Weapon {
    private static final long serialVersionUID = -5190894967392738394L;

    public CLFireExtinguisher() {
        super();
        name = "Fire Extinguisher";
        addLookupName("Clan Fire Extinguisher");
        setInternalName(name);
        heat = 0;
        damage = 0;
        shortRange = 1;
        mediumRange = 1;
        longRange = 1;
        extremeRange = 1;
        tonnage = 0.0;
        criticals = 0;
        flags = flags.or(F_SOLO_ATTACK).or(F_NO_FIRES);

        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(DATE_NONE, 2820, DATE_NONE);
        techAdvancement.setTechRating(RATING_B);
        techAdvancement.setAvailability(RATING_X, RATING_B, RATING_B, RATING_X);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, Server server) {
        return new FireExtinguisherHandler(toHit, waa, game, server);
    }
}
