/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MegaMek.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import megamek.common.Game;
import megamek.common.MekSummaryCache;
import megamek.common.scenario.ScenarioLoader;
import megamek.common.scenario.ScenarioLoaderException;
import megamek.common.scenario.ScenarioV1;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;

public class ScenarioLoaderTest {
    private List<String> errCache = new ArrayList<>();
    private PrintStream cachedPs;
    private PrintStream originalOut;
    private PrintStream originalErr;

    public static void main(String[] args) throws ScenarioLoaderException, IOException {
        ScenarioLoaderTest tester = new ScenarioLoaderTest();
        tester.runTests();
        System.exit(0);
    }

    public List<String> runTests() throws ScenarioLoaderException, IOException {
        List<String> errorAccumulator = new ArrayList<>();
        PrintStream nullPs = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                // Output nothing
            }
        });
        originalOut = System.out;
        System.setOut(nullPs);
        cachedPs = new PrintStream(new OutputStream() {
            private StringBuilder line = new StringBuilder();

            @Override
            public void write(int b) throws IOException {
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
        return errorAccumulator;
    }

    private void checkScenarioFile(File file, List<String> errorAccumulator) throws ScenarioLoaderException, IOException {
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
                ex.printStackTrace();
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
            for (File subFile : file.listFiles()) {
                checkScenarioFile(subFile, errorAccumulator);
            }
        }
    }
}
