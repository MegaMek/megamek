/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons;

import megamek.common.ComputeECM;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.totalwarfare.TWGameManager;

import java.io.Serial;

/**
 * @author Jason Tighe
 */
public class LRMFollowTheLeaderHandler extends LRMHandler {
    @Serial
    private static final long serialVersionUID = 1740643533757582922L;

    public LRMFollowTheLeaderHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        super(t, w, g, m);
        sSalvoType = " FTL missile(s) ";
        nSalvoBonus = 1;
    }

    @Override
    public int getSalvoBonus() {
        if (ComputeECM.isAffectedByECM(ae, ae.getPosition(), target.getPosition())) {
            return 0;
        } else {
            return nSalvoBonus;
        }
    }

    @Override
    protected int calcnCluster() {
        if (ComputeECM.isAffectedByECM(ae, ae.getPosition(), target.getPosition())) {
            return super.calcnCluster();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    protected int calcDamagePerHit() {
        return 1;
    }
}
