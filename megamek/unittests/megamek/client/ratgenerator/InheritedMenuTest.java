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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A ruleset with no menus of its own uses its parent's. The Calderon Protectorate's file only names the Taurian
 * Concordat as its parent, but every ruleset gets an empty table of contents, so the Force Generator stopped there and
 * offered Calderon no unit types or sizes (MegaMek/mekhq#10376 item 8). The ComStar Explorer Corps had the same gap.
 */
class InheritedMenuTest {

    private static final int ECHELON_LANCE = 3;

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

    @Test
    void calderonUsesTheTaurianMenus() throws Exception {
        Ruleset calderon = ShippedRulesetLoader.load("CDP.xml", "CDP", "TC",
              Files.createDirectories(temporaryDirectory.resolve("CDP")));
        Ruleset taurian = ShippedRulesetLoader.load("TC.xml", "TC", Files.createDirectories(temporaryDirectory.resolve(
              "TC")));
        HashMap<String, Ruleset> isolated = new HashMap<>();
        isolated.put("CDP", calderon);
        isolated.put("TC", taurian);
        rulesetTable.set(null, isolated);

        assertTrue(calderon.getTOCNode().isEmpty(), "CDP.xml should have no menus of its own");

        TOCNode menus = Ruleset.findTOCNode(calderon);

        assertSame(taurian.getTOCNode(), menus);
        ForceDescriptor request = new ForceDescriptor();
        request.setFaction("CDP");
        request.setYear(3085);
        assertNotNull(menus.findUnitTypes(request), "Calderon should be offered the Taurian unit types");
    }

    @Test
    void calderonFormationsTakeTheirNameFromTheTaurianRules() throws Exception {
        Ruleset calderon = ShippedRulesetLoader.load("CDP.xml", "CDP", "TC",
              Files.createDirectories(temporaryDirectory.resolve("CDP")));
        Ruleset taurian = ShippedRulesetLoader.load("TC.xml", "TC", Files.createDirectories(temporaryDirectory.resolve(
              "TC")));
        HashMap<String, Ruleset> isolated = new HashMap<>();
        isolated.put("CDP", calderon);
        isolated.put("TC", taurian);
        rulesetTable.set(null, isolated);

        ForceDescriptor lance = new ForceDescriptor();
        lance.setFaction("CDP");
        lance.setYear(3085);
        lance.setUnitType(UnitType.TANK);
        lance.setEchelon(ECHELON_LANCE);

        assertEquals("Lance", lance.parseName(), "a Calderon Lance came out with a blank name");
    }

    @Test
    void aRulesetWithItsOwnMenusKeepsThem() throws Exception {
        Ruleset taurian = ShippedRulesetLoader.load("TC.xml", "TC", temporaryDirectory);

        assertSame(taurian.getTOCNode(), Ruleset.findTOCNode(taurian));
    }
}
