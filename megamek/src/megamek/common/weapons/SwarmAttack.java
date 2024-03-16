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
package megamek.common.weapons;

import megamek.common.Game;
import megamek.common.Infantry;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.GameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 7, 2005
 */
public class SwarmAttack extends InfantryAttack {
    private static final long serialVersionUID = 8593642424068542897L;

    public SwarmAttack() {
        super();
        this.name = "Swarm Mek";
        this.setInternalName(Infantry.SWARM_MEK);
        techAdvancement.setTechBase(TECH_BASE_ALL).setAdvancement(2456, 2460, 2500)
            .setStaticTechLevel(SimpleTechLevel.STANDARD)
            .setApproximate(true, false, false).setTechBase(RATING_D)
            .setPrototypeFactions(F_LC).setProductionFactions(F_LC)
            .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        return new SwarmAttackHandler(toHit, waa, game, manager);
    }
}
