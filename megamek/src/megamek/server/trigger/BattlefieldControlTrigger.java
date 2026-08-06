/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server.trigger;

import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.common.units.EjectedCrew;
import megamek.common.weapons.TeleMissile;
import megamek.logging.MMLogger;

/**
 * This trigger reacts when only units of a single team remain alive and on board (this trigger disregards non-deployed
 * units, offboard units, TeleMissiles, GunEmplacements and MekWarriors!).
 */
public class BattlefieldControlTrigger implements Trigger {
    private static final MMLogger logger = MMLogger.create(BattlefieldControlTrigger.class);

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        if (game instanceof Game twGame) {
            return twGame.getEntitiesVector().stream()
                  .filter(e -> !e.isOffBoard())
                  .filter(e -> e.getPosition() != null)
                  .filter(e -> !(e instanceof EjectedCrew))
                  .filter(e -> !(e instanceof TeleMissile))
                  .filter(e -> !(e.isBuildingEntityOrGunEmplacement()))
                  .map(unit -> game.getPlayer(unit.getOwnerId()).getTeam())
                  .distinct()
                  .count() == 1;
        } else {
            logger.warn("BattlefieldControlTrigger is currently only available for TW games.");
            return false;
        }
    }

    @Override
    public String toString() {
        return "BattlefieldControl";
    }
}
