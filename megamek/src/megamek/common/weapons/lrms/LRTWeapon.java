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
package megamek.common.weapons.lrms;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.MissileWeaponHandler;
import megamek.common.weapons.missiles.MissileWeapon;
import megamek.server.GameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class LRTWeapon extends MissileWeapon {

    private static final long serialVersionUID = -7350712286691532142L;

    public LRTWeapon() {
        super();
        ammoType = AmmoType.T_LRM_TORPEDO;
        flags = flags.or(F_ARTEMIS_COMPATIBLE);

    }
    
    @Override
    public double getTonnage(Entity entity, int location, double size) {
        if ((entity != null) && entity.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
            return getRackSize() * 0.2;
        } else {
            return super.getTonnage(entity, location, size);
        }
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, GameManager manager) {
        return new MissileWeaponHandler(toHit, waa, game, manager);
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_TORP;
    }
    
    @Override
    public void adaptToGameOptions(GameOptions gOp) {
        super.adaptToGameOptions(gOp);

        // Indirect Fire
        if (gOp.booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            addMode("");
            addMode("Indirect");
        } else {
            removeMode("");
            removeMode("Indirect");
        }
    }

    @Override
    public String getSortingName() {
        String oneShotTag = hasFlag(F_ONESHOT) ? "OS " : "";
        if (name.contains("I-OS")) {
            oneShotTag = "XIOS ";
        }
        return "LRT " + oneShotTag + ((rackSize < 10) ? "0" + rackSize : rackSize);
    }
}
