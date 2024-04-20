/*
 * Copyright (c) 2022, 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.scenario;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScenarioLoader {

    private final File scenarioFile;

    public ScenarioLoader(File f) {
        scenarioFile = f;
    }

    public ScenarioLoader(String filename) {
        this(new File(filename));
    }

    /**
     * Loads and returns the loaded scenario as a {@link ScenarioV1} or {@link ScenarioV2}.
     *
     * @return The loaded scenario
     * @throws ScenarioLoaderException When the file has malformed information and cannot be parsed
     * @throws IOException When the file cannot be accessed
     */
    public Scenario load() throws ScenarioLoaderException, IOException {
        int mmsVersion = findMmsVersion();
        if (mmsVersion == 1) {
            return new ScenarioV1(scenarioFile);
        } else if (mmsVersion == 2) {
            return new ScenarioV2(scenarioFile);
        } else {
            throw new ScenarioLoaderException("ScenarioLoaderException.missingMMSVersion", scenarioFile.toString());
        }
    }

    /**
     * @return The MMS version (1 or 2) or -1 if no version can be found
     * @throws FileNotFoundException When the current file doesn't exist
     */
    private int findMmsVersion() throws IOException {
        Scanner scanner = new Scanner(scenarioFile);
        Pattern versionPattern = Pattern.compile("^\\s*"+ Scenario.MMSVERSION +"\\s*[:=]\\s*(\\d)");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Matcher versionMatcher = versionPattern.matcher(line);
            if (!isCommentLine(line) && versionMatcher.find()) {
                return Integer.parseInt(versionMatcher.group(1));
            }
        }
        return -1;
    }

    private boolean isCommentLine(String line) {
        return line.trim().startsWith("#");
    }
}