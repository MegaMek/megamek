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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The Circinus Federation follows standard Star League organization (Field Manual: Periphery p.102), so it uses the
 * Inner Sphere baseline rules. It used to fall through to the Word of Blake Protectorate Militia rules, which offered
 * only Demi-Company, Lance and "Choir", and no conventional fighters (MegaMek/mekhq#10376 items 27 and 28).
 */
class CircinusRulesTest {

    private static final int ECHELON_BATTALION = 5;

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
    void circinusUsesTheStarLeagueBaselineMenus() throws Exception {
        Ruleset circinus = ShippedRulesetLoader.load("CIR.xml", "CIR", "IS",
              Files.createDirectories(temporaryDirectory.resolve("CIR")));
        Ruleset innerSphere = ShippedRulesetLoader.load("IS.xml", "IS",
              Files.createDirectories(temporaryDirectory.resolve("IS")));
        HashMap<String, Ruleset> isolated = new HashMap<>();
        isolated.put("CIR", circinus);
        isolated.put("IS", innerSphere);
        rulesetTable.set(null, isolated);

        TOCNode menus = Ruleset.findTOCNode(circinus);

        assertSame(innerSphere.getTOCNode(), menus);
        ForceDescriptor request = new ForceDescriptor();
        request.setFaction("CIR");
        request.setYear(3060);
        ValueNode unitTypes = menus.findUnitTypes(request);
        assertNotNull(unitTypes);
        assertTrue(List.of(unitTypes.getContent().split(",")).contains("Conventional Fighter"),
              "the McIntyre Wings fly conventional fighters: " + unitTypes.getContent());

        request.setUnitType(UnitType.MEK);
        assertTrue(List.of(menus.findEchelons(request).getContent().split(","))
              .contains(Integer.toString(ECHELON_BATTALION)), "Majors command battalions");
    }
}
