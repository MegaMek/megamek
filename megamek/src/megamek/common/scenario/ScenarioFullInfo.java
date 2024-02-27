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

import megamek.common.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ScenarioFullInfo extends HashMap<String, Collection<String>> {


    public String getName() {
        return getString(ScenarioLoader.NAME);
    }

    public String getDescription() {
        return getString(ScenarioLoader.DESCRIPTION);
    }

    public String getFileName() {
        return getString(ScenarioLoader.FILENAME);
    }

    public String getSubDirectory() {
        String scenariosDir = Configuration.scenariosDir().toString();
        if (!getFileName().contains(scenariosDir)) {
            return "";
        } else {
            return subDirUnderScenarios(directoriesAsList(getFileName()));
        }
    }

    public static List<String> directoriesAsList(String fileName) {
        Path path = Paths.get(fileName);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < path.getNameCount(); i++) {
            result.add(path.getName(i).toString());
        }
        return result;
    }

    private String subDirUnderScenarios(List<String> directoryList) {
        if (!directoryList.contains("scenarios")) {
            return "";
        } else {
            int index = directoryList.indexOf("scenarios");
            // The next entry must not be the last, as the last entry is the scenario filename
            return (index + 2 < directoryList.size()) ? directoryList.get(index + 1) : "";
        }
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
