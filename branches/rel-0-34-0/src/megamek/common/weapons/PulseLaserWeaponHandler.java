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
import megamek.common.Compute;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

public class PulseLaserWeaponHandler extends EnergyWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = -5701939682138221449L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public PulseLaserWeaponHandler(ToHitData toHit, WeaponAttackAction waa,
            IGame g, Server s) {
        super(toHit, waa, g, s);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage();

        if ( game.getOptions().booleanOption("tacops_energy_weapons") && wtype.hasModes()){
            toReturn = Compute.dialDownDamage(weapon, wtype,nRange);
        }

        // during a swarm, all damage gets applied as one block to one location
        if ((ae instanceof BattleArmor)
                && (weapon.getLocation() == BattleArmor.LOC_SQUAD)
                && (ae.getSwarmTargetId() == target.getTargetId())) {
            toReturn *= ((BattleArmor) ae).getShootingStrength();
        }
        // Check for Altered Damage from Energy Weapons (TacOp, pg.83)
        int nRange = ae.getPosition().distance(target.getPosition());
        if (game.getOptions().booleanOption("tacops_altdmg")) {
            if (nRange <= 1) {
                toReturn++;
            } else if (nRange <= wtype.getMediumRange()) {
                // Do Nothing for Short and Medium Range
            } else if (nRange <= wtype.getLongRange()) {
                toReturn--;
            }
        }

        if ( game.getOptions().booleanOption("tacops_range") && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG]) ) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }

        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            toReturn = Compute.directBlowInfantryDamage(toReturn, bDirect ? toHit.getMoS()/3 : 0, Compute.WEAPON_PULSE, ((Infantry)target).isMechanized());
        } else if (bDirect){
            toReturn = Math.min(toReturn+(toHit.getMoS()/3), toReturn*2);
        } if (bGlancing) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }
        return (int) Math.ceil(toReturn);
    }
}
