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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The Scorpion Empire ({@code SE}) and the Escorpion Imperio ({@code CEI}) are era aliases of Clan Goliath Scorpion
 * ({@code CGS}), which has one ruleset for its whole history. Looking up an alias must reach that ruleset rather than
 * the generic Clan one (MegaMek/mekhq#10376).
 */
class RulesetAliasTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void anAliasUsesTheRulesetOfTheFactionItStandsFor() throws Exception {
        Ruleset goliathScorpion = ShippedRulesetLoader.load("CGS.xml", "CGS", temporaryDirectory);
        Map<String, Ruleset> rulesets = Map.of("CGS", goliathScorpion);
        FactionRecord goliathScorpionRecord = new FactionRecord("CGS");

        assertSame(goliathScorpion, Ruleset.findCanonicalRuleset("SE", goliathScorpionRecord, rulesets));
        assertSame(goliathScorpion, Ruleset.findCanonicalRuleset("CEI", goliathScorpionRecord, rulesets));
    }

    @Test
    void aKeyThatIsNotAnAliasIsLeftToTheUsualLookup() {
        FactionRecord goliathScorpionRecord = new FactionRecord("CGS");

        assertNull(Ruleset.findCanonicalRuleset("CGS", goliathScorpionRecord, Map.of()),
              "the faction's own key is already handled by the direct match");
        assertNull(Ruleset.findCanonicalRuleset("SE", null, Map.of()));
        assertNull(Ruleset.findCanonicalRuleset("SE", goliathScorpionRecord, Map.of()),
              "an alias of a faction with no ruleset falls through to the parent walk");
    }
}
