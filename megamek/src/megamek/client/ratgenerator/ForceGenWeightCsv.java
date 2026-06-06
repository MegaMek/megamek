/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ratgenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * Appends weight-mix results to a small CSV in the logs directory, so per-type tuning can read clean machine-readable
 * data instead of fishing lines out of the rotating game log.
 *
 * <p>The file is {@code logs/forcegen_weights.csv}. Each generated force appends one row PER weight-classed unit type
 * present (Mek, aerospace fighter, vehicle, battle armor): faction, the weight class the ruleset rolled, the unit type,
 * the total of that type, and the achieved count per weight class. The file is appended to across runs; delete it to
 * start a fresh batch. Purely diagnostic; no effect on generation.</p>
 */
final class ForceGenWeightCsv {
    private static final MMLogger LOGGER = MMLogger.create(ForceGenWeightCsv.class);
    private static final Path FILE = Path.of("logs", "forcegen_weights.csv");
    private static final String HEADER =
          "faction,year,rolledWeight,clusterFlags,unitType,total,ultraLight,light,medium,heavy,assault,superHeavy";

    private ForceGenWeightCsv() {}

    /**
     * Appends one row for each weight-classed unit type in this force. Writes the header first if the file does not yet
     * exist. Any I/O failure is logged and swallowed so diagnostics never break generation.
     *
     * @param faction      the generated faction key
     * @param year         the generation year (era affects unit availability), or -1 if unknown
     * @param rolledWeight the weight class the ruleset rolled for this force (e.g. "A", "RANDOM"), never null
     * @param clusterFlags semicolon-joined cluster identity flags (e.g. "battle"), for per-named-type tuning
     * @param byType       map of {@link UnitType} constant to per-weight-class counts (see
     *                     {@code ForceDescriptor.tallyWeightClassesByType})
     */
    static void append(String faction, int year, String rolledWeight, String clusterFlags,
          Map<Integer, int[]> byType) {
        if ((byType == null) || byType.isEmpty()) {
            return;
        }
        StringBuilder rows = new StringBuilder();
        for (Map.Entry<Integer, int[]> entry : byType.entrySet()) {
            int[] w = entry.getValue();
            int total = 0;
            for (int c : w) {
                total += c;
            }
            if (total == 0) {
                continue;
            }
            rows.append(String.join(",",
                  safe(faction),
                  Integer.toString(year),
                  safe(rolledWeight),
                  safe(clusterFlags),
                  safe(UnitType.getTypeName(entry.getKey())),
                  Integer.toString(total),
                  cell(w, EntityWeightClass.WEIGHT_ULTRA_LIGHT),
                  cell(w, EntityWeightClass.WEIGHT_LIGHT),
                  cell(w, EntityWeightClass.WEIGHT_MEDIUM),
                  cell(w, EntityWeightClass.WEIGHT_HEAVY),
                  cell(w, EntityWeightClass.WEIGHT_ASSAULT),
                  cell(w, EntityWeightClass.WEIGHT_SUPER_HEAVY))).append("\n");
        }
        if (rows.isEmpty()) {
            return;
        }
        try {
            boolean writeHeader = !Files.exists(FILE);
            if (writeHeader && (FILE.getParent() != null)) {
                Files.createDirectories(FILE.getParent());
            }
            String out = writeHeader ? (HEADER + "\n" + rows) : rows.toString();
            Files.writeString(FILE, out, StandardCharsets.UTF_8,
                  StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            LOGGER.warn("Could not append force-gen weight rows to {}: {}", FILE, ex.getMessage());
        }
    }

    private static String cell(int[] counts, int index) {
        return Integer.toString((index < counts.length) ? counts[index] : 0);
    }

    private static String safe(String value) {
        return (value == null) ? "" : value.replace(',', ' ');
    }
}
