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
package megamek.common.weapons;

import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

public class EnergyWeaponHandler extends WeaponHandler {
    /**
     * 
     */
    private static final long serialVersionUID = 2452514543790235562L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public EnergyWeaponHandler(ToHitData toHit, WeaponAttackAction waa,
            IGame g, Server s) {
        super(toHit, waa, g, s);
        generalDamageType = HitData.DAMAGE_ENERGY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        float toReturn = wtype.getDamage();
        // during a swarm, all damage gets applied as one block to one location
        if (ae instanceof BattleArmor
                && weapon.getLocation() == BattleArmor.LOC_SQUAD
                && (ae.getSwarmTargetId() == target.getTargetId())) {
            toReturn *= ((BattleArmor) ae).getShootingStrength();
        }
        // Check for Altered Damage from Energy Weapons (MTR, pg.22)
        int nRange = ae.getPosition().distance(target.getPosition());
        if (game.getOptions().booleanOption("maxtech_altdmg")) {
            if (nRange <= 1) {
                toReturn++;
            } else if (nRange <= wtype.getMediumRange()) {
                // Do Nothing for Short and Medium Range
            } else if (nRange <= wtype.getLongRange()) {
                toReturn--;
            } else if (nRange <= wtype.getExtremeRange()) {
                toReturn = (int) Math.floor(toReturn / 2.0);
            }
        }
        if (bGlancing) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }

        if (target instanceof Infantry && !(target instanceof BattleArmor))
            toReturn /= 10;
        return (int) Math.ceil(toReturn);
    }

}
