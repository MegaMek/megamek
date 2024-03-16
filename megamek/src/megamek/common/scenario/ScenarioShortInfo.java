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

public class ScenarioShortInfo {

    private final String name;
    private final String description;
    private final String filename;
    private final String planet;

    ScenarioShortInfo(String name, String description, String filename, String planet) {
        this.name = name;
        this.description = description;
        this.filename = filename;
        this.planet = planet;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getFileName() {
        return filename;
    }

    public String getPlanet() {
        return planet;
    }
}