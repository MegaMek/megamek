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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;

import megamek.common.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the custom ruleset directory search path and the defaults-only marker.
 *
 * <p>Nothing here calls {@link Ruleset#loadData()} or {@link Ruleset#findRuleset(String)}: both reach
 * {@link RATGenerator#getInstance()}, which spawns a background thread that reads the whole force generator
 * data directory. The behaviour under test is the search path and the placeholder flag, both reachable
 * without that.</p>
 */
// Configuration.setForceGeneratorDir is deprecated for removal in 0.51.0, but it is the only supported way
// to relocate the directory, and this test must relocate it to prove Ruleset follows Configuration. When
// the setter goes, point builtInDirectoryFollowsConfiguredForceGeneratorDir at its replacement.
@SuppressWarnings("removal")
class RulesetTest {

    private File originalForceGeneratorDir;

    @BeforeEach
    void captureConfiguration() {
        originalForceGeneratorDir = Configuration.forceGeneratorDir();
        Ruleset.clearAdditionalRulesetDirectories();
    }

    @AfterEach
    void restoreConfiguration() {
        Configuration.setForceGeneratorDir(originalForceGeneratorDir);
        Ruleset.clearAdditionalRulesetDirectories();
    }

    @Test
    void searchPathContainsOnlyTheBuiltInDirectoryByDefault() {
        List<File> directories = Ruleset.rulesetDirectories();

        assertEquals(1, directories.size(), "no directories should be registered by default");
        assertEquals(new File(Configuration.forceGeneratorDir(), "faction_rules"), directories.get(0));
    }

    /**
     * {@code Ruleset} previously hardcoded {@code "data/forcegenerator/faction_rules"}, silently ignoring a
     * caller that had relocated the data with {@code setForceGeneratorDir}.
     */
    @Test
    void builtInDirectoryFollowsConfiguredForceGeneratorDir() {
        Configuration.setForceGeneratorDir(new File("relocated/forcegenerator"));

        List<File> directories = Ruleset.rulesetDirectories();

        assertEquals(new File("relocated/forcegenerator", "faction_rules"), directories.get(0));
    }

    /**
     * Added directories must come after the built-in one, because {@code loadData} keys rulesets by faction
     * and a later entry replaces an earlier one. Reversing the order would make user rulesets unreachable.
     */
    @Test
    void addedDirectoriesAreSearchedAfterTheBuiltInDirectory() {
        Ruleset.addRulesetDirectory("userdata/forcegenerator/faction_rules");

        List<File> directories = Ruleset.rulesetDirectories();

        assertEquals(2, directories.size());
        assertEquals(new File(Configuration.forceGeneratorDir(), "faction_rules"), directories.get(0));
        assertEquals(new File("userdata/forcegenerator/faction_rules"), directories.get(1));
    }

    @Test
    void addedDirectoriesKeepRegistrationOrder() {
        Ruleset.addRulesetDirectory("first");
        Ruleset.addRulesetDirectory("second");

        List<File> directories = Ruleset.rulesetDirectories();

        assertEquals(3, directories.size());
        assertEquals(new File("first"), directories.get(1));
        assertEquals(new File("second"), directories.get(2));
    }

    @Test
    void duplicateDirectoryIsIgnored() {
        Ruleset.addRulesetDirectory("userdata/forcegenerator/faction_rules");
        Ruleset.addRulesetDirectory("userdata/forcegenerator/faction_rules");

        assertEquals(2, Ruleset.rulesetDirectories().size(), "duplicate registration should be ignored");
    }

    @Test
    void nullDirectoryIsIgnored() {
        Ruleset.addRulesetDirectory(null);

        assertEquals(1, Ruleset.rulesetDirectories().size());
    }

    @Test
    void blankDirectoryIsIgnored() {
        Ruleset.addRulesetDirectory("   ");

        assertEquals(1, Ruleset.rulesetDirectories().size());
    }

    @Test
    void defaultsOnlyRulesetIsFlaggedAndCarriesTheGenericFaction() {
        Ruleset defaultsOnlyRuleset = Ruleset.createDefaultsOnlyRuleset();

        assertTrue(defaultsOnlyRuleset.isDefaultsOnly());
        assertEquals(FactionRecord.IS_GENERAL_KEY, defaultsOnlyRuleset.getFaction());
    }

    /**
     * A ruleset parsed from {@code IS_General.xml} is a real ruleset even though its faction key is
     * {@link FactionRecord#IS_GENERAL_KEY}. If it reported {@code true} here, consumers would warn about
     * falling back to generic rules for every faction that legitimately uses them.
     */
    @Test
    void ordinaryRulesetIsNotFlaggedAsDefaultsOnly() throws Exception {
        Constructor<Ruleset> constructor = Ruleset.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Ruleset ordinaryRuleset = constructor.newInstance();

        assertFalse(ordinaryRuleset.isDefaultsOnly());
        assertEquals(FactionRecord.IS_GENERAL_KEY, ordinaryRuleset.getFaction(),
              "guards the premise of this test: the generic faction key alone must not imply defaults-only");
    }

    @Test
    void clearAdditionalRulesetDirectoriesResetsTheSearchPath() {
        Ruleset.addRulesetDirectory("userdata/forcegenerator/faction_rules");
        assertEquals(2, Ruleset.rulesetDirectories().size());

        Ruleset.clearAdditionalRulesetDirectories();

        assertEquals(1, Ruleset.rulesetDirectories().size());
    }
}
