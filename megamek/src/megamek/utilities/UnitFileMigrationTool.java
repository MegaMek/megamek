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
package megamek.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.common.loaders.MtfFile;
import megamek.logging.MMLogger;

/**
 * This is not a functional tool, just some template code to use when changing
 * unit files programmatically. Last used to move the unit roles into the unit
 * files. I leave this in so I don't have to reinvent the wheel.
 */
public class UnitFileMigrationTool {
    private static final MMLogger logger = MMLogger.create(UnitFileMigrationTool.class);

    public static void main(String... args) throws IOException {
        MekSummaryCache cache = MekSummaryCache.getInstance(true);
        MekSummary[] units = cache.getAllMeks();

        List<String> lines = null;

        for (MekSummary unit : units) {
            File file = unit.getSourceFile();

            if (file.toString().toLowerCase().endsWith(".mtf")) {
                try {
                    lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                } catch (MalformedInputException exception) {
                    String message = String.format("MalformedInputException for file %s", file);
                    logger.error(exception, message);
                    String rawText = new String(Files.readAllBytes(file.toPath()));
                    lines.clear();
                    lines.add(rawText);
                    Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
                    continue;
                }

                if (lines.get(0).startsWith("Version:")) {
                    lines.remove(0);
                } else {
                    continue;
                }

                if (!lines.get(0).contains(":")) {
                    String chassis = lines.remove(0);
                    lines.add(0, MtfFile.CHASSIS + chassis);
                } else {
                    continue;
                }

                if (!lines.get(1).contains(":")) {
                    String model = lines.remove(1);
                    lines.add(1, MtfFile.MODEL + model);
                } else {
                    continue;
                }

                Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
            }
        }
    }
}
