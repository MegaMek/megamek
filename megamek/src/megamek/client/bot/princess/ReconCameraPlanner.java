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
package megamek.client.bot.princess;

import java.util.List;

import megamek.common.actions.ReconCameraSpotAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.ReconCameraRules;
import megamek.logging.MMLogger;

/**
 * Chooses a Recon Camera spot for a bot unit in the Off-Board phase (TO:AUE p.150). Shared by Princess and CASPAR.
 *
 * <p>A ground unit spots whenever it can: the spot costs it nothing and spares its own shots the spotting penalty. An
 * aerospace unit gives up every other attack for the turn when it spots, so it spots only when it has no working
 * weapon to lose, as a dedicated spotter plane does. The target is the one the camera is likeliest to hit.</p>
 */
final class ReconCameraPlanner {

    private static final MMLogger LOGGER = MMLogger.create(ReconCameraPlanner.class);

    private ReconCameraPlanner() {}

    /**
     * Chooses the camera spot a bot unit should make this turn.
     *
     * @param game   the game
     * @param camera the bot's unit
     *
     * @return the spot to declare, or {@code null} when the unit should not spot
     */
    static @Nullable ReconCameraSpotAction planSpot(Game game, Entity camera) {
        List<Entity> targets = ReconCameraRules.spotTargets(game, camera);
        if (targets.isEmpty()) {
            return null;
        }
        if (ReconCameraRules.isAerospaceCamera(camera) && hasWorkingWeapon(camera)) {
            LOGGER.debug("[ReconCamera] {}: bot does not spot - an aerospace spot would cost its attacks",
                  camera.getShortName());
            return null;
        }
        Entity bestTarget = null;
        int bestRoll = Integer.MAX_VALUE;
        for (Entity target : targets) {
            int rollNeeded = ReconCameraRules.spotToHit(game, camera, target).getValue();
            if (rollNeeded < bestRoll) {
                bestRoll = rollNeeded;
                bestTarget = target;
            }
        }
        if (bestTarget == null) {
            return null;
        }
        LOGGER.debug("[ReconCamera] {}: bot spots {} (needs {}) from {} possible target(s)", camera.getShortName(),
              bestTarget.getShortName(), bestRoll, targets.size());
        return new ReconCameraSpotAction(camera.getId(), bestTarget.getId());
    }

    private static boolean hasWorkingWeapon(Entity unit) {
        for (WeaponMounted weapon : unit.getWeaponList()) {
            if (!weapon.isDestroyed() && !weapon.isBreached()) {
                return true;
            }
        }
        return false;
    }
}
