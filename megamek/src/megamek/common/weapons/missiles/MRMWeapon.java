/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.missiles;

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.MRMHandler;
import megamek.server.gameManager.*;

/**
 * @author Sebastian Brocks
 */
public abstract class MRMWeapon extends MissileWeapon {

    private static final long serialVersionUID = 274817921444431878L;

    public MRMWeapon() {
        super();
        ammoType = AmmoType.T_MRM;
        toHitModifier = 1;
        atClass = CLASS_MRM;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, GameManager manager) {
        return new MRMHandler(toHit, waa, game, manager);
    }
    
    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        double damage = 0;
        if (range > getLongRange()) {
            return damage;
        }
        if (fcs != null && fcs.getType() instanceof MiscType
                && fcs.getType().hasFlag(MiscType.F_APOLLO)) {
            damage = Compute.calculateClusterHitTableAmount(6, getRackSize());
        } else {
            damage = Compute.calculateClusterHitTableAmount(7, getRackSize());
            damage *= 0.95; // +1 to hit            
        }
        if (range == 0 && getMinimumRange() > 0) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        return damage / 10.0;
    }

    @Override
    public String getSortingName() {
        String oneShotTag = hasFlag(F_ONESHOT) ? "OS " : "";
        if (name.contains("I-OS")) {
            oneShotTag = "XIOS ";
        }
        return "MRM " + oneShotTag + rackSize;
    }
}
