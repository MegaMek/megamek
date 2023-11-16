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

            /*
            File file = unit.getSourceFile();
            if (UnitRoleHandler.getRoleFor(unit) == UnitRole.UNDETERMINED) {
                continue;
            }
            if (file.toString().toLowerCase().endsWith(".blk")) {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                int line = 0;
                boolean found = false;
                for (; line < lines.size(); line++) {
                    if (lines.get(line).toLowerCase().startsWith("</type>")) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    lines.add(line + 1, "");
                    lines.add(line + 2, "<role>");
                    lines.add(line + 3, UnitRoleHandler.getRoleFor(unit).toString());
                    lines.add(line + 4, "</role>");
//                    Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
//                    System.out.println(lines);
                } else {
                    System.out.println("type line not found for: " + unit.getName());
                }
            }
*/


        }
    }
}