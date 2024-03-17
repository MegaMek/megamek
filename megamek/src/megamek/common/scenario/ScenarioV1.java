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

import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

/**
 * This class holds all scenario info loaded from a scenario (.mms) file. It is a map of constants given in
 * {@link ScenarioLoader} to a list of data for that constant.
 */
public class ScenarioV1 extends HashMap<String, Collection<String>> implements Scenario {

    private final File file;

    ScenarioV1(File file) throws IOException {
        this.file = file;
        load();
    }

    /**
     * @return The name of the scenario; keyword {@link ScenarioLoader#NAME}
     */
    public String getName() {
        return getString(ScenarioLoader.NAME);
    }

    /**
     * @return The description of the scenario; keyword {@link ScenarioLoader#DESCRIPTION}
     */
    public String getDescription() {
        return getString(ScenarioLoader.DESCRIPTION);
    }

    /**
     * @return The filename including directories of the scenario
     */
    public String getFileName() {
        return file.toString();
    }

    public void load() throws IOException {
        for (String line : readLines(file, s -> s.trim().startsWith("#"))) {
            if (!line.contains("=")) {
                LogManager.getLogger().error("Missing = in scenario line: " + line);
            } else {
                String[] elements = line.split("=", 2);
                String keyword = elements[0].trim();
                put(keyword, elements[1].trim());
            }
        }
    }

    public static List<String> readLines(File file, Predicate<String> isCommentLine) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        lines.removeIf(String::isBlank);
        lines.removeIf(isCommentLine);
        return lines;
    }

    public void put(String key, String value) {
        Collection<String> values = get(key);
        if (values == null) {
            values = new ArrayList<>();
            put(key, values);
        }
        values.add(value);
    }

    public String getString(String key) {
        return getString(key, ScenarioLoader.SEPARATOR_COMMA);
    }

    public String getString(String key, String separator) {
        Collection<String> values = get(key);
        if ((values == null) || values.isEmpty()) {
            return null;
        }

        boolean firstElement = true;
        StringBuilder sb = new StringBuilder();
        for (String val : values) {
            if (firstElement) {
                firstElement = false;
            } else {
                sb.append(separator);
            }
            sb.append(val);
        }
        return sb.toString();
    }

    /**
     * @return the number of values for this key in the file
     */
    public int getNumValues(String key) {
        Collection<String> values = get(key);
        return (values == null) ? 0 : values.size();
    }
}