/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers.lrm;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jason Tighe
 */
public class LRMDeadFireHandler extends LRMHandler {
    @Serial
    private static final long serialVersionUID = 9200751420492807777L;

    public LRMDeadFireHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
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
