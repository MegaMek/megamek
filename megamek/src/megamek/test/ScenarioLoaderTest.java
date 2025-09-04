/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import megamek.common.game.Game;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.scenario.ScenarioLoader;
import megamek.common.scenario.ScenarioLoaderException;
import megamek.common.scenario.ScenarioV1;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;

public class ScenarioLoaderTest {
    private final static MMLogger LOGGER = MMLogger.create(ScenarioLoaderTest.class);
    private final List<String> errCache = new ArrayList<>();
    private PrintStream originalErr;

    public static void main(String[] args) throws ScenarioLoaderException, IOException {
        ScenarioLoaderTest tester = new ScenarioLoaderTest();
        tester.runTests();
        System.exit(0);
    }

    public void runTests() throws ScenarioLoaderException, IOException {
        List<String> errorAccumulator = new ArrayList<>();
        PrintStream nullPs = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                // Output nothing
            }
        });
        PrintStream originalOut = System.out;
        System.setOut(nullPs);
        PrintStream cachedPs = new PrintStream(new OutputStream() {
            private final StringBuilder line = new StringBuilder();

            @Override
            public void write(int b) {
                if (b == '\n') {
                    String s = line.toString();
                    if (!s.startsWith("MMRandom: generating RNG")) {
                        errCache.add(s);
                    }
                    line.setLength(0);
                } else if (b != '\r') {
                    line.append((char) b);
                }
            }
        });
        originalErr = System.err;
        System.setErr(cachedPs);

        // Wait for MSC (we have to wait anyway, better to do it once if we want to measure)
        MekSummaryCache msc = MekSummaryCache.getInstance();
        while (!msc.isInitialized()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
        }

        File baseDir = new File("data/scenarios");
        checkScenarioFile(baseDir, errorAccumulator);
        System.setOut(originalOut);
        System.setErr(originalErr);
        cachedPs.close();
        nullPs.close();
    }

    private void checkScenarioFile(File file, List<String> errorAccumulator)
          throws ScenarioLoaderException, IOException {
        int port = 7770;
        if (null == file) {
            return;
        }
        if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".mms")) {
            ScenarioLoader loader = new ScenarioLoader(file);
            ScenarioV1 scenario = (ScenarioV1) loader.load();
            try {
                Game game = (Game) scenario.createGame();
                TWGameManager gameManager = new TWGameManager();
                Server server = new Server("test", port + 1, gameManager);
                server.setGame(game);
                scenario.applyDamage(gameManager);
                server.die();
            } catch (Exception ex) {
                LOGGER.error(ex, "Check Scenario File : {} ", ex.getMessage());
            }

            if (!errCache.isEmpty()) {
                errorAccumulator.add("ERROR in " + file.getPath());
                originalErr.println("ERROR in " + file.getPath());
                for (String line : errCache) {
                    errorAccumulator.add(line);
                    originalErr.println(line);
                }
                errCache.clear();
            }
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();

            if (files != null) {
                for (File subFile : files) {
                    checkScenarioFile(subFile, errorAccumulator);
                }
            }
        }
    }
}
