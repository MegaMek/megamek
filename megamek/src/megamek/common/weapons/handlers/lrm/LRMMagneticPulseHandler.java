/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.MagneticPulseState;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Handler for LRM Magnetic Pulse (MP) missiles (TO:AUE p.182). MP missiles inflict no damage. Instead the salvo imposes
 * a +1 to-hit penalty on all of the target's own weapon attacks until the End Phase of the following turn, and adds +1
 * heat per 5 warheads that hit a fusion-powered, heat-tracking unit. MP missiles have no effect against conventional
 * infantry.
 */
public class LRMMagneticPulseHandler extends LRMHandler {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Warheads needed per +1 heat for LRM-class launchers (TO:AUE p.182). */
    private static final int LRM_HEAT_DIVISOR = MagneticPulseState.LRM_HEAT_DIVISOR;

    public LRMMagneticPulseHandler(ToHitData toHit, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager gameManager) throws EntityLoadingException {
        super(toHit, weaponAttackAction, game, gameManager);
    }

    @Override
    protected int calcDamagePerHit() {
        // MP missiles inflict no damage.
        return 0;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport, IBuilding bldg, int hits,
          int nCluster, int bldgAbsorbs) {
        handleMagneticPulseGroup(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs, LRM_HEAT_DIVISOR);
    }
}
