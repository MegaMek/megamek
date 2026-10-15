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

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A Taurian vehicle Maniple is two vehicles (FM: Periphery p.52), generated as a matched pair of one model and numbered
 * like a Clan Point. It used to pick any two vehicles and had no name. The Calderon Protectorate uses the same rule.
 */
class TaurianManipleTest {

    private static final int ECHELON_POINT = 2;

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
    void aManipleIsAMatchedPair() throws Exception {
        Ruleset taurian = ShippedRulesetLoader.load("TC.xml", "TC", temporaryDirectory);
        // Applying a rule looks the ruleset up in the global table, so this test's copy is installed there.
        HashMap<String, Ruleset> isolated = new HashMap<>();
        isolated.put("TC", taurian);
        rulesetTable.set(null, isolated);

        ForceDescriptor maniple = new ForceDescriptor();
        maniple.setFaction("TC");
        maniple.setYear(3085);
        maniple.setUnitType(UnitType.TANK);
        maniple.setEchelon(ECHELON_POINT);
        ForceNode rule = taurian.findForceNode(maniple);
        assertNotNull(rule, "no Taurian vehicle Maniple rule");

        rule.apply(maniple);

        assertEquals("Maniple", rule.getEchelonName());
        assertEquals(2, maniple.getSubForces().size(), "a Maniple is two vehicles");
        assertEquals("model", maniple.getGenerationRule(), "both vehicles should be the same model");
    }
}
