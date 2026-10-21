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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Clan naval units use the touman's ladder: a Star of 5-6 vessels, a Binary of 10-12 and a Trinary of 15-18 (FM:
 * Crusader Clans p.9). The Clans offered only a naval Star or their named fleet, so the Binary and Trinary sizes were
 * missing (MegaMek/mekhq#10376 item 5), and the Rasalhague Dominion's DropShip Binary and Trinary showed as bare
 * numbers with no rule behind them (item 21).
 */
class ClanNavalSizesTest {

    private static final int ECHELON_BINARY = 4;
    private static final int ECHELON_TRINARY = 5;
    private static final List<Integer> NAVAL_TYPES = List.of(UnitType.WARSHIP, UnitType.DROPSHIP,
          UnitType.JUMPSHIP);

    @TempDir
    Path temporaryDirectory;

    private Field rulesetTable;
    private Object previousRulesets;

    @BeforeEach
    void rememberRulesets() throws Exception {
        rulesetTable = Ruleset.class.getDeclaredField("rulesets");
        rulesetTable.setAccessible(true);
        previousRulesets = rulesetTable.get(null);
    }

    @AfterEach
    void restoreRulesets() throws Exception {
        rulesetTable.set(null, previousRulesets);
    }

    @ParameterizedTest
    @ValueSource(ints = { ECHELON_TRINARY, ECHELON_BINARY })
    void theBaseClanRulesBuildNavalBinariesAndTrinariesFromStars(int echelon) throws Exception {
        Ruleset clan = ShippedRulesetLoader.load("CLAN.xml", "CLAN", temporaryDirectory);
        // Applying a rule looks the ruleset up in the global table, so this test's copy is installed there.
        HashMap<String, Ruleset> isolated = new HashMap<>();
        isolated.put("CLAN", clan);
        rulesetTable.set(null, isolated);
        int expectedStars = (echelon == ECHELON_TRINARY) ? 3 : 2;
        for (int unitType : NAVAL_TYPES) {
            ForceDescriptor naval = navalRequest("CLAN", unitType, echelon);
            ForceNode rule = clan.findForceNode(naval);
            assertNotNull(rule, "no naval rule for " + UnitType.getTypeName(unitType) + " at echelon " + echelon);

            rule.apply(naval);

            assertEquals(expectedStars, naval.getSubForces().size(), UnitType.getTypeName(unitType));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "CB", "CBS", "CCC", "CCO", "CDS", "CFM", "CGB", "CGS", "CHH", "CIH", "CJF", "CMG", "CNC",
                             "CSA", "CSJ", "CSL", "CSR", "CSV", "CW", "CWE", "CWI", "CWIE", "RD" })
    void everyClanOffersNavalBinariesAndTrinariesThatReachTheBaseRules(String faction) throws Exception {
        Ruleset ruleset = ShippedRulesetLoader.load(faction + ".xml", faction, temporaryDirectory);
        for (int unitType : NAVAL_TYPES) {
            ForceDescriptor menu = navalRequest(faction, unitType, ECHELON_TRINARY);
            ValueNode echelons = ruleset.getTOCNode().findEchelons(menu);
            assertNotNull(echelons, faction + " has no " + UnitType.getTypeName(unitType) + " sizes");
            List<String> offered = List.of(echelons.getContent().split(","));
            if (!offered.contains("3")) {
                // This faction offers no naval Star for the type at all (its menu lists something else), so
                // there is nothing to extend.
                continue;
            }
            assertTrue(offered.contains("5") && offered.contains("4"),
                  faction + " " + UnitType.getTypeName(unitType) + " offers " + offered + " without Binary/Trinary");

            for (int echelon : new int[] { ECHELON_TRINARY, ECHELON_BINARY }) {
                assertNull(ruleset.findForceNode(navalRequest(faction, unitType, echelon)),
                      faction + " has its own naval rule at echelon " + echelon
                            + " that would catch the request before the base Clan rules");
            }
        }
    }

    private static ForceDescriptor navalRequest(String faction, int unitType, int echelon) {
        ForceDescriptor naval = new ForceDescriptor();
        naval.setFaction(faction);
        naval.setYear(3067);
        naval.setUnitType(unitType);
        naval.setEchelon(echelon);
        naval.setRating("FL");
        return naval;
    }
}
