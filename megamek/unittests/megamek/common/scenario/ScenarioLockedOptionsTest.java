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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Set;

import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.Test;

/**
 * A scenario can name the options that are its mission and lock just those, leaving every other option open.
 * Before this the choice was all or nothing: fix every option, so a player could not set even an unrelated one,
 * or fix none, so the mission itself could be changed at load - including switching objectives off, which
 * silently turns every control point in the file into decoration.
 */
class ScenarioLockedOptionsTest {

    @Test
    void aScenarioCanLockTheOptionsThatAreItsMission() throws Exception {
        Scenario scenario = new ScenarioLoader(
              new File("testresources/data/scenarios/test_setups/LockedOptions.mms")).load();
        scenario.createGame();

        assertEquals(Set.of(OptionsConstants.VICTORY_USE_OBJECTIVES, OptionsConstants.VICTORY_USE_GAME_TURN_LIMIT),
              scenario.lockedGameOptions());
        // locking is not fixing: the rest of the options dialog still opens for the player
        assertFalse(scenario.hasFixedGameOptions());
    }

    @Test
    void aMisspelledLockRefusesToLoadRatherThanSilentlyLockingNothing() throws Exception {
        // a typo that was ignored would leave the mission open without anyone knowing; the fixture is
        // LockedOptions.mms with its locked entry misspelled
        Scenario scenario = new ScenarioLoader(
              new File("testresources/data/scenarios/test_setups/LockedOptionsMisspelled.mms")).load();

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class, scenario::createGame);

        assertTrue(refusal.getMessage().contains("use_objectivez"), refusal.getMessage());
    }

    @Test
    void aScenarioThatLocksNothingLocksNothing() throws Exception {
        Scenario scenario = new ScenarioLoader(
              new File("testresources/data/scenarios/test_setups/AeroGroundAttackRun.mms")).load();
        scenario.createGame();

        assertTrue(scenario.lockedGameOptions().isEmpty());
    }
}
