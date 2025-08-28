/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers.prototype;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.handlers.GRHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sept 8, 2005
 */
public class PrototypeGaussHandler extends GRHandler {
    @Serial
    private static final long serialVersionUID = -156828547249911617L;

    /**
     *
     */
    public PrototypeGaussHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (doAmmoFeedProblemCheck(vPhaseReport)) {
            return true;
        }

        if ((roll.getIntValue() == 2) && !attackingEntity.isConventionalInfantry()) {
            Report r = new Report(3165);
            r.subject = subjectId;
            weapon.setJammed(true);
            weapon.setHit(true);
            vPhaseReport.addElement(r);
            return true;
        }
        return false;
    }
}
