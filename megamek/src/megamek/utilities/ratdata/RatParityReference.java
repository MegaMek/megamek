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
package megamek.utilities.ratdata;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.SalvageChance;
import megamek.client.ratgenerator.UnitTable;
import megamek.codeUtilities.MathUtility;
import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * Writes MegaMek's own tables for a list of test cases, the reference the website's copy of the RAT Generator
 * ({@code web/ratgen/ratgen.js}) is checked against by {@code web/ratgen/parity.js}.
 * <p>
 * Each case line is {@code FACTION YEAR RATING UNITTYPE ROLES STRICTNESS WEIGHTS MOTIVES NETWORK}, with {@code -} for
 * none, {@code _} for a space and lists separated by commas, for example
 * {@code FS 3039 C Mek recon,cavalry 2 1,2 - 0}. Each table comes from {@link UnitTable#findTable}, the call MekHQ
 * and the RAT Generator dialog make.
 * </p>
 * <p>
 * Output is tab-separated, one row per entry: {@code case kind entry value}, where kind is {@code unit} (entry is the
 * unit name), {@code salvage} (entry is the faction key, value its share of all salvage out of
 * {@value RatTableContent#SALVAGE_WEIGHT_SCALE}), {@code salvagePct}, or {@code error} when MegaMek throws.
 * </p>
 * <p>
 * Arguments: {@code <cases file> <output file>}. Run from the megamek folder so {@code data/} resolves.
 * </p>
 */
public final class RatParityReference {

    private static final MMLogger LOGGER = MMLogger.create(RatParityReference.class);

    private RatParityReference() {}

    /**
     * One test case.
     *
     * @param faction        faction key
     * @param year           game year
     * @param rating         equipment rating, or {@code null} for none
     * @param unitType       {@code UnitType} code
     * @param roles          mission roles
     * @param roleStrictness role strictness
     * @param weightClasses  weight classes, empty for all
     * @param movementModes  movement modes, empty for all
     * @param networkMask    C3 network mask
     */
    record ParityCase(String faction, int year, @Nullable String rating, int unitType, List<MissionRole> roles,
          int roleStrictness, List<Integer> weightClasses, List<EntityMovementMode> movementModes, int networkMask) {

        /**
         * @param line a case line, as described on the class
         *
         * @return the parsed case
         *
         * @throws IllegalArgumentException if the line cannot be read
         */
        static ParityCase parse(String line) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length != 9) {
                throw new IllegalArgumentException("A case needs 9 fields: " + line);
            }
            int unitType = UnitType.determineUnitTypeCode(fields[3].replace('_', ' '));
            if (unitType < 0) {
                throw new IllegalArgumentException("Unknown unit type " + fields[3]);
            }
            List<MissionRole> roles = new ArrayList<>();
            for (String roleName : listOf(fields[4])) {
                MissionRole role = MissionRole.parseRole(roleName);
                if (role == null) {
                    throw new IllegalArgumentException("Unknown role " + roleName);
                }
                roles.add(role);
            }
            List<Integer> weightClasses = new ArrayList<>();
            for (String weightClass : listOf(fields[6])) {
                weightClasses.add(parseNumber(weightClass));
            }
            List<EntityMovementMode> movementModes = new ArrayList<>();
            for (String mode : listOf(fields[7])) {
                movementModes.add(EntityMovementMode.valueOf(mode));
            }
            String rating = "-".equals(fields[2]) ? null : fields[2].replace('_', ' ');
            return new ParityCase(fields[0], parseNumber(fields[1]), rating, unitType, roles, parseNumber(fields[5]),
                  weightClasses, movementModes, parseNumber(fields[8]));
        }

        private static List<String> listOf(String field) {
            return "-".equals(field) ? List.of() : List.of(field.split(","));
        }

        private static int parseNumber(String text) {
            int value = MathUtility.parseInt(text, Integer.MIN_VALUE);
            if (value == Integer.MIN_VALUE) {
                throw new IllegalArgumentException("Not a number: " + text);
            }
            return value;
        }
    }

    /**
     * Entry point.
     *
     * @param args the cases file and the output file
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: RatParityReference <cases file> <output file>");
            System.exit(2);
            return;
        }
        try {
            RATGenerator ratGenerator = RatDataExporter.waitForRatGenerator();
            int written = writeReference(ratGenerator, Path.of(args[0]), Path.of(args[1]));
            System.out.printf("[RatParity] Wrote MegaMek's tables for %d cases%n", written);
            System.exit(0);
        } catch (IOException | InterruptedException exception) {
            LOGGER.error(exception, "[RatParity] Could not write the reference tables");
            System.exit(1);
        }
    }

    private static int writeReference(RATGenerator ratGenerator, Path casesFile, Path outputFile) throws IOException {
        List<String> lines = Files.readAllLines(casesFile, StandardCharsets.UTF_8);
        int caseIndex = 0;
        try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8))) {
            output.println("case\tkind\tentry\tvalue");
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                writeCase(ratGenerator, output, caseIndex, line);
                caseIndex++;
            }
        }
        return caseIndex;
    }

    private static void writeCase(RATGenerator ratGenerator, PrintWriter output, int caseIndex, String line) {
        try {
            ParityCase parityCase = ParityCase.parse(line);
            FactionRecord faction = ratGenerator.getFaction(parityCase.faction());
            if (faction == null) {
                output.println(caseIndex + "\terror\tUnknownFaction\t0");
                return;
            }
            UnitTable table = UnitTable.findTable(faction, parityCase.unitType(), parityCase.year(),
                  parityCase.rating(), parityCase.weightClasses(), parityCase.networkMask(),
                  parityCase.movementModes(), parityCase.roles(), parityCase.roleStrictness(), faction);
            for (int entryIndex = 0; entryIndex < table.getNumEntries(); entryIndex++) {
                MekSummary unit = table.getMekSummary(entryIndex);
                if (unit != null) {
                    output.println(caseIndex + "\tunit\t" + unit.getName() + "\t" + table.getEntryWeight(entryIndex));
                }
            }
            SalvageChance salvageChance = table.getSalvageChance();
            for (FactionRecord salvageFaction : salvageChance.factionWeights().keySet()) {
                output.println(caseIndex + "\tsalvage\t" + salvageFaction.getKey() + "\t"
                      + RatTableContent.salvageWeight(salvageChance, salvageFaction));
            }
            output.println(caseIndex + "\tsalvagePct\t-\t" + RatTableContent.salvagePct(salvageChance));
        } catch (RuntimeException exception) {
            // MegaMek itself fails on some requests (see FactionRecord.getWeightDistribution); the website must too
            output.println(caseIndex + "\terror\t" + exception.getClass().getSimpleName() + "\t0");
        }
    }
}
