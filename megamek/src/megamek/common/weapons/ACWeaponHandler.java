/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.*;

/**
 * @author Jason Tighe
 */
public class ACWeaponHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = 7326881584091651519L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public ACWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage();
        // during a swarm, all damage gets applied as one block to one
        // location
        if ((ae instanceof BattleArmor)
            && (weapon.getLocation() == BattleArmor.LOC_SQUAD)
            && !(weapon.isSquadSupportWeapon())
            && (ae.getSwarmTargetId() == target.getId())) {
            toReturn *= ((BattleArmor) ae).getShootingStrength();
        }
        // we default to direct fire weapons for anti-infantry damage
        if (target.isConventionalInfantry()) {
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                    bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null,
                    ae.getId(), calcDmgPerHitReport);
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3), toReturn * 2);
        }
        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)
            && nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG]) {
            toReturn = (int) Math.floor(toReturn * .75);
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn * .5);
        }        

        return (int) toReturn;
    }
}
