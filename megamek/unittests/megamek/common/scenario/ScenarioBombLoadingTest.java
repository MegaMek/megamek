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
package megamek.common.scenario;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BombMounted;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.IBomber;
import org.junit.jupiter.api.Test;

/**
 * A scenario that declares {@code bombs: external: HE: N} must produce fighters that can actually drop
 * N bombs. Found live: every scenario-launched A2G benchmark game showed fighters with mounted bomb
 * racks and ZERO shots - dive bombing was impossible in ~50 headless games while lobby-configured
 * games bombed normally.
 */
class ScenarioBombLoadingTest {

    @Test
    void scenarioBombsAreLoadedWithAmmo() throws Exception {
        File scenarioFile = new File("testresources/data/scenarios/test_setups/AeroGroundAttackRun.mms");
        assertTrue(scenarioFile.exists(), "test scenario must exist: " + scenarioFile.getAbsolutePath());

        Scenario scenario = new ScenarioLoader(scenarioFile).load();
        // assertDoesNotThrow surfaces the cause message: createGame resolves boards and units, and
        // in CI those come only from testresources - a bare IllegalArgumentException here cost two
        // rounds of CI archaeology before the missing unit files were named.
        Game game = org.junit.jupiter.api.Assertions.assertDoesNotThrow(
              () -> (Game) scenario.createGame(),
              "scenario failed to load - a board or unit is missing from testresources/data");

        boolean sawBomber = false;
        for (Entity entity : game.getEntitiesVector()) {
            if (!(entity instanceof IBomber)) {
                continue;
            }
            List<BombMounted> bombs = entity.getBombs(AmmoType.F_GROUND_BOMB);
            if (bombs.isEmpty()) {
                continue;
            }
            sawBomber = true;
            int shots = bombs.stream().mapToInt(BombMounted::getBaseShotsLeft).sum();
            assertTrue(shots > 0, entity.getShortName() + " mounts " + bombs.size()
                  + " ground bomb(s) but has " + shots + " shots - the racks are empty");
        }
        assertTrue(sawBomber, "the scenario's fighters must mount their declared bombs at all");
    }
}
