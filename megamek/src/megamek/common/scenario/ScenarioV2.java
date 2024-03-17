/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

public class ScenarioV2 implements Scenario {

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String PLANET = "planet";
    private static final String GAMETYPE = "gametype";

    private JsonNode node;

    private final File file;

    private static final ObjectMapper yamlMapper =
            new ObjectMapper(new YAMLFactory());

    ScenarioV2(File file) throws IOException {
        this.file = file;
        load();
    }

    public void load() throws IOException {
        node = yamlMapper.readTree(file);
    }

    public String getName() {
        return node.get(NAME).textValue();
    }

    public String getDescription() {
        return node.get(DESCRIPTION).textValue();
    }

    public String getFileName() {
        return file.toString();
    }

    public String getPlanet() {
        return node.has(PLANET) ? node.get(PLANET).textValue() : "";
    }

    @Override
    public String getGameType() {
        return node.has(GAMETYPE) ? node.get(GAMETYPE).textValue() : "TW";
    }
}