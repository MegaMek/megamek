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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import megamek.common.Configuration;
import megamek.common.Player;
import megamek.common.game.IGame;
import megamek.common.units.Entity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The V1 scenario readme documents {@code CritHit}, {@code Camo_Faction} and the per-unit {@code Camo} key. The
 * loader used to ignore all three: it wanted {@code CriticalHit}, a lower-case {@code camo_}, and per-unit keys
 * starting with a lower-case letter that its own unit data pattern can never match. {@code PilotHits},
 * {@code DeploymentRound} and {@code Altitude} were read as 0, because their value was looked up as if it were a key.
 *
 * <p>The tests use only units found both in mm-data and in {@code testresources/data/mekfiles}. Some test
 * fixtures point the shared unit cache at that small test set and leave it there, so either cache may be loaded
 * when this class runs.</p>
 */
class ScenarioV1UnitKeysTest {

    private static final String CAMO_CATEGORY = "Clans/Wolf";
    private static final String CAMO_FILE = "Alpha Galaxy.jpg";

    /** A board that ships with the test resources, since V1 only reads boards from the boards directory. */
    private static final String TEST_BOARDS_DIRECTORY = "testresources/data/scenarios/test_setups";
    private static final String TEST_BOARD = "testiceonwater";

    private static File originalBoardsDirectory;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void useTestBoards() {
        originalBoardsDirectory = Configuration.boardsDir();
        Configuration.setBoardsDir(new File(TEST_BOARDS_DIRECTORY));
    }

    @AfterAll
    static void restoreBoardsDirectory() {
        Configuration.setBoardsDir(originalBoardsDirectory);
    }

    @Test
    void critHitSpellingFromTheReadmeRegistersACriticalHit() throws Exception {
        ScenarioV1 scenario = loadScenario("""
              Unit_A_1=Locust LCT-1V,Pilot One,3,4,N,5,5
              Unit_A_1_CritHit=1:3
              """);
        scenario.createGame();

        assertEquals(1, criticalHitPlanCount(scenario));
    }

    @Test
    void criticalHitSpellingStillRegistersACriticalHit() throws Exception {
        ScenarioV1 scenario = loadScenario("""
              Unit_A_1=Locust LCT-1V,Pilot One,3,4,N,5,5
              Unit_A_1_CriticalHit=1:3
              """);
        scenario.createGame();

        assertEquals(1, criticalHitPlanCount(scenario));
    }

    @Test
    void capitalizedFactionCamoKeyFromTheReadmeIsApplied() throws Exception {
        IGame game = loadScenario("""
              Unit_A_1=Locust LCT-1V,Pilot One,3,4,N,5,5
              Camo_A=%s,%s
              """.formatted(CAMO_CATEGORY, CAMO_FILE)).createGame();

        assertWolfCamo(findPlayer(game).getCamouflage().getCategory(),
              findPlayer(game).getCamouflage().getFilename());
    }

    @Test
    void lowerCaseFactionCamoKeyStillWorks() throws Exception {
        IGame game = loadScenario("""
              Unit_A_1=Locust LCT-1V,Pilot One,3,4,N,5,5
              camo_A=%s,%s
              """.formatted(CAMO_CATEGORY, CAMO_FILE)).createGame();

        assertWolfCamo(findPlayer(game).getCamouflage().getCategory(),
              findPlayer(game).getCamouflage().getFilename());
    }

    @Test
    void unitCamoKeyFromTheReadmeIsApplied() throws Exception {
        IGame game = loadScenario("""
              Unit_A_1=Locust LCT-1V,Pilot One,3,4,N,5,5
              Unit_A_1_Camo=%s,%s
              """.formatted(CAMO_CATEGORY, CAMO_FILE)).createGame();

        Entity locust = findUnit(game);
        assertWolfCamo(locust.getCamouflage().getCategory(), locust.getCamouflage().getFilename());
    }

    @Test
    void pilotHitsAreApplied() throws Exception {
        IGame game = loadScenario("""
              Unit_A_1=Locust LCT-1V,Pilot One,3,4,N,5,5
              Unit_A_1_PilotHits=2
              """).createGame();

        assertEquals(2, findUnit(game).getCrew().getHits());
    }

    @Test
    void deploymentRoundHoldsTheUnitBack() throws Exception {
        IGame game = loadScenario("""
              Unit_A_1=Locust LCT-1V,Pilot One,3,4,N,5,5
              Unit_A_1_DeploymentRound=3
              """).createGame();

        Entity locust = findUnit(game);
        assertEquals(3, locust.getDeployRound());
        assertFalse(locust.isDeployed());
    }

    @Test
    void altitudeIsAppliedToAnAerospaceUnit() throws Exception {
        IGame game = loadScenario("""
              Unit_A_1=Cheetah F-11,Pilot One,3,4,N,5,5
              Unit_A_1_Altitude=3
              """).createGame();

        assertEquals(3, findUnit(game).getAltitude());
    }

    private Entity findUnit(IGame game) {
        return (Entity) game.getInGameObjects().getFirst();
    }

    private void assertWolfCamo(String category, String filename) {
        assertEquals(CAMO_CATEGORY, category);
        assertEquals(CAMO_FILE, filename);
    }

    private Player findPlayer(IGame game) {
        Player player = game.getPlayersList().getFirst();
        assertNotNull(player);
        assertFalse(player.getCamouflage().isDefault(), "the scenario camo should have been applied");
        return player;
    }

    private ScenarioV1 loadScenario(String unitLines) throws Exception {
        String content = """
              MMSVersion=1
              Name=Unit key test
              Factions=A
              BoardWidth=1
              BoardHeight=1
              Maps=%s
              """.formatted(TEST_BOARD) + unitLines;
        File scenarioFile = tempDir.resolve("UnitKeyTest.mms").toFile();
        Files.writeString(scenarioFile.toPath(), content, StandardCharsets.UTF_8);
        return (ScenarioV1) new ScenarioLoader(scenarioFile).load();
    }

    private int criticalHitPlanCount(ScenarioV1 scenario) throws Exception {
        Field field = ScenarioV1.class.getDeclaredField("criticalHitPlans");
        field.setAccessible(true);
        return ((List<?>) field.get(scenario)).size();
    }
}
