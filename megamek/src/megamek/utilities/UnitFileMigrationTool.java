/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.utilities;

import megamek.common.*;
import megamek.common.loaders.MtfFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * This is not a functional tool, just some template code to use when changing unit files programmatically.
 * Last used to move the unit roles into the unit files.
 * I leave this in so I don't have to reinvent the wheel.
 */
public class UnitFileMigrationTool {

    public static void main(String... args) throws IOException {
        MechSummaryCache cache = MechSummaryCache.getInstance(true);
        MechSummary[] units = cache.getAllMechs();
        for (MechSummary unit : units) {
            File file = unit.getSourceFile();
            if (file.toString().toLowerCase().endsWith(".mtf")) {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                if (lines.get(0).startsWith("Version:")) {
                    lines.remove(0);
                } else {
                    System.out.println(unit + " doesnt have Version");
                    continue;
                }
                if (!lines.get(0).contains(":")) {
                    String chassis = lines.remove(0);
                    lines.add(0, MtfFile.CHASSIS + chassis);
                } else {
                    System.out.println(unit + " doesnt have chassis without :");
                    continue;
                }
                if (!lines.get(1).contains(":")) {
                    String model = lines.remove(1);
                    lines.add(1, MtfFile.MODEL + model);
                } else {
                    System.out.println(unit + " doesnt have model without :");
                    continue;
                }
                Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
//                System.out.println(lines);
            }
        }
    }
}