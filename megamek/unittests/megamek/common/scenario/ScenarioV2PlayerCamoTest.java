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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import megamek.common.Player;
import megamek.common.game.IGame;
import org.junit.jupiter.api.Test;

/**
 * Scenario factions that declare no camo of their own must receive their player color's camouflage, the same
 * way lobby-created players do - otherwise every faction renders with the identical default camo and the sides
 * cannot be told apart on the board (found live in a Princess-versus-Princess objectives playtest, #8778).
 * Factions that do declare a camo must keep it.
 */
class ScenarioV2PlayerCamoTest {

    @Test
    void factionsWithoutACamoGetTheirPlayerColorCamouflage() throws Exception {
        IGame game = loadScenario("testresources/data/scenarios/test_setups/AeroGroundAttackRun.mms");

        Set<String> seenCamoNames = new HashSet<>();
        for (Player player : game.getPlayersList()) {
            assertTrue(player.getCamouflage().isColourCamouflage(),
                  player.getName() + " should have a color camouflage, not the default");
            assertEquals(player.getColour().name(), player.getCamouflage().getFilename(),
                  player.getName() + "'s camo should match their player color");
            seenCamoNames.add(player.getCamouflage().getFilename());
        }
        assertEquals(game.getPlayersList().size(), seenCamoNames.size(),
              "every faction should look different on the board");
    }

    @Test
    void factionsWithTheirOwnCamoKeepIt() throws Exception {
        IGame game = loadScenario("testresources/data/scenarios/test_setups/AeroScen.mms");

        for (Player player : game.getPlayersList()) {
            assertFalse(player.getCamouflage().isColourCamouflage(),
                  player.getName() + "'s scenario-declared camo must not be replaced");
        }
    }

    private IGame loadScenario(String path) throws Exception {
        File scenarioFile = new File(path);
        assertTrue(scenarioFile.exists(), "missing test fixture: " + scenarioFile.getAbsolutePath());
        return new ScenarioLoader(scenarioFile).load().createGame();
    }
}
