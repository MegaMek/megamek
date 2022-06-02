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
package megamek.common.weapons.battlearmor;

import megamek.common.AmmoType;
import megamek.common.BattleForceElement;
import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AdvancedSRMHandler;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.srms.SRMWeapon;
import megamek.server.GameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class AdvancedSRMWeapon extends SRMWeapon {

    private static final long serialVersionUID = 8098857067349950771L;

    public AdvancedSRMWeapon() {
        super();
        this.ammoType = AmmoType.T_SRM_ADVANCED;
        flags = flags.andNot(F_ARTEMIS_COMPATIBLE);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, GameManager manager) {
        return new AdvancedSRMHandler(toHit, waa, game, manager);
    }

    /**
     * non-squad size version for AlphaStrike base damage
     */
    @Override 
    public double getBattleForceDamage(int range) {
        if (range > getLongRange()) {
            return 0;
        }
        double damage = Compute.calculateClusterHitTableAmount(8, getRackSize()) * 2;
        if (range == BattleForceElement.SHORT_RANGE && getMinimumRange() > 0) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        return damage / 10.0;
    }

    @Override
    public double getBattleForceDamage(int range, int baSquadSize) {
        if (range > getLongRange()) {
            return 0;
        }
        double damage = Compute.calculateClusterHitTableAmount(8, getRackSize() * baSquadSize);
        if (range == BattleForceElement.SHORT_RANGE && getMinimumRange() > 0) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        return damage / 10.0;
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_STANDARD;
    }

    @Override
    public String getSortingName() {
        if (sortingName != null) {
            return sortingName;
        } else {
            String oneShotTag = hasFlag(F_ONESHOT) ? "OS " : "";
            if (name.contains("I-OS")) {
                oneShotTag = "OSI ";
            }
            return "SRM Z Advanced " + oneShotTag + rackSize;
        }
    }
}
