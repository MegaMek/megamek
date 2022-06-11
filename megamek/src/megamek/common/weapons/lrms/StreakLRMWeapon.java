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
package megamek.common.weapons.lrms;

import megamek.common.AmmoType;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.StreakLRMHandler;
import megamek.server.GameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class StreakLRMWeapon extends LRMWeapon {

    private static final long serialVersionUID = -2552069184709782928L;

    public StreakLRMWeapon() {
        super();
        this.ammoType = AmmoType.T_LRM_STREAK;
        flags = flags.or(F_PROTO_WEAPON).andNot(F_ARTEMIS_COMPATIBLE);
        clearModes();
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
        .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
        .setClanApproximate(false, false, true, false,false)
        .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
        .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
    
    @Override
    public double getTonnage(Entity entity, int location, double size) {
        if ((entity != null) && entity.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
            return getRackSize() * 0.4;
        } else {
            return super.getTonnage(entity, location, size);
        }
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, GameManager manager) {
        return new StreakLRMHandler(toHit, waa, game, manager);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (range <= AlphaStrikeElement.LONG_RANGE) {
            return 0.1 * getRackSize();
        } else {
            return 0;
        }
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_STANDARD;
    }

    @Override
    public String getSortingName() {
        String oneShotTag = hasFlag(F_ONESHOT) ? "OS " : "";
        if (name.contains("I-OS")) {
            oneShotTag = "OSI ";
        }
        return "LRM STREAK " + oneShotTag + ((rackSize < 10) ? "0" + rackSize : rackSize);
    }


    @Override
    public boolean hasIndirectFire() {
        return false;
    }
}
