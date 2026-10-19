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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The Clan size list offers Trinary, Binary and Star for VTOLs and conventional fighters, but the Clan ruleset had
 * no rules for either at those sizes. The dropdown showed the raw echelon numbers 5/4/3 and the generator built a
 * single unit (#9200). Both now follow Clan armor: two per Point, five Points per Star, so 10/20/30. A Point is a
 * matched pair of one model.
 */
class ClanVtolAndConventionalFighterRulesTest {

    private static final int ECHELON_ELEMENT = 1;
    private static final int ECHELON_POINT = 2;
    private static final int ECHELON_STAR = 3;

    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest
    @CsvSource({ "VTOL, 5, Trinary, 3", "VTOL, 4, Binary, 2", "VTOL, 3, Star, 5",
                 "Conventional Fighter, 5, Trinary, 3", "Conventional Fighter, 4, Binary, 2",
                 "Conventional Fighter, 3, Star, 5", "Conventional Fighter, 2, Point, 2", "VTOL, 2, Point, 2" })
    void clanSizeHasARuleWithTheArmorStructure(String unitTypeName, int echelon, String expectedName,
          int expectedChildren) throws Exception {
        Ruleset clanRuleset = ShippedRulesetLoader.load("CLAN.xml", "CLAN", temporaryDirectory);
        ForceDescriptor force = clanForce(UnitType.determineUnitTypeCode(unitTypeName), echelon);

        ForceNode rule = clanRuleset.findForceNode(force);

        assertNotNull(rule, "no Clan " + unitTypeName + " rule at echelon " + echelon);
        assertEquals(expectedName, rule.getEchelonName());
        List<ForceDescriptor> children = new ArrayList<>();
        for (SubForcesNode subForcesBlock : rule.subForces) {
            if (subForcesBlock.matches(force)) {
                children.addAll(subForcesBlock.generateSubForces(force, false));
            }
        }
        assertEquals(expectedChildren, children.size());
        int expectedChildEchelon = switch (echelon) {
            case ECHELON_POINT -> ECHELON_ELEMENT;
            case ECHELON_STAR -> ECHELON_POINT;
            default -> ECHELON_STAR;
        };
        for (ForceDescriptor child : children) {
            assertEquals(expectedChildEchelon, child.getEchelon());
            if (echelon == ECHELON_POINT) {
                // A Point is a matched pair: both slots resolve to one model
                assertEquals("model", child.getGenerationRule());
            }
        }
    }

    private static ForceDescriptor clanForce(int unitType, int echelon) {
        ForceDescriptor force = new ForceDescriptor();
        force.setFaction("CLAN");
        force.setYear(3060);
        force.setUnitType(unitType);
        force.setEchelon(echelon);
        force.setRating("SL");
        return force;
    }
}
