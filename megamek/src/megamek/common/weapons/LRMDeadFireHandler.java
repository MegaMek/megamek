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

import megamek.common.Game;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.totalwarfare.TWGameManager;

import java.io.Serial;
import java.util.Vector;

/**
 * @author Jason Tighe
 */
public class LRMDeadFireHandler extends LRMHandler {
    @Serial
    private static final long serialVersionUID = 9200751420492807777L;

    public LRMDeadFireHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        super(t, w, g, m);
        sSalvoType = " dead fire missile(s) ";
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // Per IntOps p. 132, dead-fire missiles do 2 damage per missile, but still in 5 point clusters
        // so we figure out how many missile hits get done first, then implement a hack similar to that in ATMHandler
        int hits = super.calcHits(vPhaseReport);
        // change to 5 damage clusters here, after AMS has been done
        hits = nDamPerHit * hits;
        nDamPerHit = 1;
        return hits;
    }

    @Override
    protected int getClusterModifiers(boolean clusterRangePenalty) {
        return super.getClusterModifiers(clusterRangePenalty) - 3;
    }

    @Override
    protected int calcDamagePerHit() {
        return 2;
    }
}
