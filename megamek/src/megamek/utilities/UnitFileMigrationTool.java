/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.loaders.MtfFile;
import megamek.logging.MMLogger;

/**
 * This is not a functional tool, just some template code to use when changing unit files programmatically. Last used to
 * move the unit roles into the unit files. I leave this in, so I don't have to reinvent the wheel.
 */
public class UnitFileMigrationTool {
    private static final MMLogger logger = MMLogger.create(UnitFileMigrationTool.class);

    public static void main(String... args) throws IOException {
        MekSummaryCache cache = MekSummaryCache.getInstance(true);
        MekSummary[] units = cache.getAllMeks();

        List<String> lines = new ArrayList<>();

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
