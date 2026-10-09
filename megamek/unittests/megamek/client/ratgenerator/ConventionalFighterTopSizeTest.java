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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Every size a Formation menu offers needs a rule behind it, or it shows as a bare number and builds one craft. The
 * Capellan Fleet Regiment and the FWLM aerospace Regiment have rules for aerospace fighters only, so conventional
 * fighters were offered a size "6" (MegaMek/mekhq#10376 items 9 and 18).
 */
class ConventionalFighterTopSizeTest {

    private static final int ECHELON_SIX = 6;

    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest
    @CsvSource({ "CC.xml, CC", "FWL.xml, FWL" })
    void everyFighterSizeOfferedHasARule(String fileName, String faction) throws Exception {
        Ruleset ruleset = ShippedRulesetLoader.load(fileName, faction, temporaryDirectory);

        List<String> conventional = offered(ruleset, faction, UnitType.CONV_FIGHTER);
        assertFalse(conventional.contains(Integer.toString(ECHELON_SIX)),
              faction + " conventional fighters still offer size 6: " + conventional);

        List<String> aerospace = offered(ruleset, faction, UnitType.AEROSPACE_FIGHTER);
        assertTrue(aerospace.contains(Integer.toString(ECHELON_SIX)),
              faction + " aerospace fighters lost their top size: " + aerospace);
        ForceDescriptor top = request(faction, UnitType.AEROSPACE_FIGHTER);
        top.setEchelon(ECHELON_SIX);
        assertNotNull(ruleset.findForceNode(top), faction + " offers aerospace size 6 with no rule");
    }

    private static List<String> offered(Ruleset ruleset, String faction, int unitType) {
        ValueNode echelons = ruleset.getTOCNode().findEchelons(request(faction, unitType));
        assertNotNull(echelons, faction + " has no sizes for " + UnitType.getTypeName(unitType));
        return List.of(echelons.getContent().replaceAll("[^0-9,]", "").split(","));
    }

    private static ForceDescriptor request(String faction, int unitType) {
        ForceDescriptor request = new ForceDescriptor();
        request.setFaction(faction);
        request.setYear(3067);
        request.setUnitType(unitType);
        return request;
    }
}
