/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.bayWeapons.capital;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import megamek.common.weapons.bayWeapons.BayWeapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.CapitalLaserBayOrbitalBombardmentHandler;
import megamek.server.totalwarfare.TWGameManager;

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
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        if (waa.isOrbitToSurface(game)) {
            return new CapitalLaserBayOrbitalBombardmentHandler(toHit, waa, game, manager);
        } else {
            return super.getCorrectHandler(toHit, waa, game, manager);
        }
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_CAPITAL;
    }
}
