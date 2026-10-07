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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parses one shipped faction ruleset through the real {@link Ruleset} parser without loading the whole data set.
 *
 * <p>The file is copied under a dotted faction key first. The parser then takes the parent from the key instead of
 * asking the {@link RATGenerator} singleton, which would start loading every unit in the background and leak into
 * other test classes that share the JVM.</p>
 */
final class ShippedRulesetLoader {

    private static File factionRulesDir;

    private ShippedRulesetLoader() {
    }

    /**
     * Finds the shipped faction_rules directory and loads the echelon constants it needs.
     *
     * @return the faction_rules directory
     */
    static File factionRulesDir() {
        if (factionRulesDir == null) {
            // Same search as FormationNamingConventionTest: the staged copy first, then mm-data itself.
            for (String candidatePath : new String[] { "data/forcegenerator/faction_rules",
                                                       "megamek/data/forcegenerator/faction_rules",
                                                       "../megamek/data/forcegenerator/faction_rules",
                                                       "../../mm-data/data/forcegenerator/faction_rules",
                                                       "../mm-data/data/forcegenerator/faction_rules" }) {
                File candidate = new File(candidatePath);
                if (candidate.isDirectory()) {
                    factionRulesDir = candidate;
                    break;
                }
            }
            assertNotNull(factionRulesDir, "could not locate the shipped faction_rules directory");
            Ruleset.loadConstants(new File(factionRulesDir, "constants.txt"));
        }
        return factionRulesDir;
    }

    /**
     * Parses a copy of a shipped ruleset.
     *
     * @param fileName           the shipped ruleset file, e.g. {@code IS.xml}
     * @param factionKey         the faction key declared in that file
     * @param temporaryDirectory where to write the isolated copy
     *
     * @return the parsed ruleset
     */
    static Ruleset load(String fileName, String factionKey, Path temporaryDirectory) throws Exception {
        String rulesetText = Files.readString(new File(factionRulesDir(), fileName).toPath(), StandardCharsets.UTF_8);
        String isolatedText = rulesetText.replaceFirst("faction=\"" + factionKey + "\"",
              "faction=\"" + factionKey + ".isolatedTest\"");
        Path isolatedFile = temporaryDirectory.resolve(fileName);
        Files.writeString(isolatedFile, isolatedText, StandardCharsets.UTF_8);

        Method createFromFile = Ruleset.class.getDeclaredMethod("createFromFile", File.class);
        createFromFile.setAccessible(true);
        Ruleset ruleset = (Ruleset) createFromFile.invoke(null, isolatedFile.toFile());
        assertNotNull(ruleset, "could not parse " + fileName);
        return ruleset;
    }
}
