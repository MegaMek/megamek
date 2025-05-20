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
package megamek.common.weapons.bayweapons;

import megamek.common.EquipmentTypeLookup;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.CapitalLaserBayOrbitalBombardmentHandler;
import megamek.server.totalwarfare.TWGameManager;

import java.io.Serial;

/**
 * @author Jay Lawson
 */
public class CapitalLaserBayWeapon extends BayWeapon {
    @Serial
    private static final long serialVersionUID = 8756042527483383101L;

    public CapitalLaserBayWeapon() {
        name = "Capital Laser Bay";
        setInternalName(EquipmentTypeLookup.CAPITAL_LASER_BAY);
        heat = 0;
        damage = DAMAGE_VARIABLE;
        shortRange = 12;
        mediumRange = 24;
        longRange = 40;
        extremeRange = 50;
        flags = flags.or(F_ENERGY);
        atClass = CLASS_CAPITAL_LASER;
        capital = true;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        if (waa.isOrbitToSurface(game)) {
            return new CapitalLaserBayOrbitalBombardmentHandler(toHit, waa, game, manager);
        } else {
            return super.getCorrectHandler(toHit, waa, game, manager);
        }
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_CAPITAL;
    }
}
