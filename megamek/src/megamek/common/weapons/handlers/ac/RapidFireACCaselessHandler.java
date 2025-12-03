/*
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

package megamek.common.weapons.handlers.ac;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.rolls.Roll;
import megamek.common.weapons.handlers.RapidFireACWeaponHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Handles rapid-fire autocannon attacks with caseless ammunition. Combines the rapid-fire mechanics (jam checks,
 * cluster table) with the caseless-specific jam/explode checks (on attack roll <= 2).
 *
 * @see RapidFireACWeaponHandler
 * @see ACCaselessHandler
 */
public class RapidFireACCaselessHandler extends RapidFireACWeaponHandler {
    @Serial
    private static final long serialVersionUID = -6614562346449114L;

    public RapidFireACCaselessHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) throws EntityLoadingException {
        super(toHitData, weaponAttackAction, game, twGameManager);
    }

    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        // First apply rapid-fire jam checks
        if (super.doChecks(vPhaseReport)) {
            return true;
        }

        // Then apply caseless-specific jam/explode check (only if not already jammed by rapid-fire)
        if (!weapon.isJammed() && (roll.getIntValue() <= 2) && !attackingEntity.isConventionalInfantry()) {
            Roll diceRoll = Compute.rollD6(2);

            Report r = new Report(3164);
            r.subject = subjectId;
            r.add(diceRoll);

            if (diceRoll.getIntValue() >= 8) {
                // Round explodes destroying weapon
                weapon.setDestroyed(true);
                r.choose(false);
            } else {
                // Just a jam
                weapon.setJammed(true);
                r.choose(true);
            }
            vPhaseReport.addElement(r);
            return true;
        }
        return false;
    }
}
