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

package megamek.common.weapons.handlers;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.ConvInfantry;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves a firefighting-engineer platoon that, instead of rolling itself, is supporting the lead platoon fighting the
 * same blaze (TO:AuE p.153). A supporting platoon makes no roll of its own: its -1 modifier is already folded into the
 * lead platoon's to-hit. This handler only reports the assist and advances the platoon's consecutive-turn streak so a
 * platoon that later takes the lead keeps the bonus it earned while supporting.
 *
 * @see megamek.common.actions.compute.FirefightingSupport
 */
public class FirefightingSupportHandler extends WeaponHandler {
    @Serial
    private static final long serialVersionUID = 7330936526237826957L;

    public FirefightingSupportHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(toHit, waa, g, m);
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> returnedReports) {
        if (!cares(phase)) {
            return true;
        }
        Report report = new Report(3541);
        report.subject = subjectId;
        report.indent(3);
        report.addDesc(attackingEntity);
        report.add(target.getPosition().getBoardNum());
        returnedReports.addElement(report);

        // A supporting platoon is still actively fighting the blaze, so its streak continues (TO:AuE p.153).
        // isFirefighter() implies ConvInfantry, which records the consecutive-turn streak.
        if (attackingEntity.isFirefighter() && (attackingEntity instanceof ConvInfantry firefighter)) {
            firefighter.recordFirefight(target.getPosition(), game.getRoundCount());
        }
        return false;
    }
}
