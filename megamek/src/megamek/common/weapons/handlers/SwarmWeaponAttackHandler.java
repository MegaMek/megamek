/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers;

import java.io.Serial;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.game.Game;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Jay Lawson
 * @since Feb 21, 2013
 */
public class SwarmWeaponAttackHandler extends WeaponHandler {
    @Serial
    private static final long serialVersionUID = -2439937071168853215L;

    /**
     *
     */
    public SwarmWeaponAttackHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m) {
        super(toHit, waa, g, m);
        generalDamageType = HitData.DAMAGE_NONE;
    }

    @Override
    protected int calcDamagePerHit() {
        int damage = 0;
        if (attackingEntity instanceof BattleArmor ba) {
            damage = ba.calculateSwarmDamage();
        }
        // should this be affected by direct blows?
        // assume so for now
        if (bDirect) {
            damage = Math.min(damage + (toHit.getMoS() / 3), damage * 2);
        }
        return damage;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        return 1;
    }
}
