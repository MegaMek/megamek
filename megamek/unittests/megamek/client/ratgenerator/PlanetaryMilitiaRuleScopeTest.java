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

import java.nio.file.Path;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The Planetary Militia rule sits at the Brigade echelon, which shares the number 7 with the aerospace Wing
 * ({@code %AIR_REGIMENT%}). Rule lookup takes the first match, and the militia rule comes first in the file, so
 * without a unit-type condition it caught fighters: picking the top aerospace size built a garrison of infantry,
 * tanks and Meks with no fighters in it (#9200).
 *
 * <p>The shipped file is parsed through the real {@link Ruleset} parser (see {@link ShippedRulesetLoader}) and
 * searched with the real {@link Ruleset#findForceNode(ForceDescriptor)}.</p>
 */
class PlanetaryMilitiaRuleScopeTest {

    /** Brigade and the aerospace Wing both resolve to this number in constants.txt. */
    private static final int ECHELON_BRIGADE_AND_AIR_WING = 7;

    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest
    @ValueSource(ints = { UnitType.AEROSPACE_FIGHTER, UnitType.CONV_FIGHTER })
    void innerSphereTopAerospaceSizeResolvesToTheWingRule(int unitType) throws Exception {
        ForceNode forceNode = findBrigadeEchelonRule("IS.xml", "IS", unitType);

        assertNotNull(forceNode, "no rule matched the top aerospace size");
        assertEquals("Wing", forceNode.getEchelonName());
    }

    @Test
    void innerSphereCombinedArmsBrigadeStillResolvesToTheMilitiaRule() throws Exception {
        ForceNode forceNode = findBrigadeEchelonRule("IS.xml", "IS", null);

        assertNotNull(forceNode, "no rule matched a combined-arms brigade");
        assertEquals("Planetary Militia", forceNode.getEchelonName());
    }

    @Test
    void peripheryCombinedArmsBrigadeStillResolvesToTheMilitiaRule() throws Exception {
        ForceNode forceNode = findBrigadeEchelonRule("Periphery.xml", "Periphery", null);

        assertNotNull(forceNode, "no rule matched a combined-arms brigade");
        assertEquals("Planetary Militia", forceNode.getEchelonName());
    }

    @Test
    void peripheryMilitiaRuleDoesNotCatchAerospace() throws Exception {
        // Periphery does not offer echelon 7 for fighters today, so nothing should match at all - the militia
        // rule must not be the answer if a future size list adds it.
        ForceNode forceNode = findBrigadeEchelonRule("Periphery.xml", "Periphery", UnitType.AEROSPACE_FIGHTER);

        assertNull(forceNode, "the Periphery militia rule matched an aerospace force");
    }

    /**
     * Parses a copy of a shipped ruleset and returns the rule the generator would pick for a top-level force of the
     * given unit type at echelon 7.
     *
     * @param fileName   the shipped ruleset file
     * @param factionKey the faction key declared in that file
     * @param unitType   the force's unit type, or {@code null} for a combined-arms force
     *
     * @return the first matching rule, or {@code null} if none matches
     */
    private ForceNode findBrigadeEchelonRule(String fileName, String factionKey, Integer unitType) throws Exception {
        Ruleset ruleset = ShippedRulesetLoader.load(fileName, factionKey, temporaryDirectory);

        ForceDescriptor forceDescriptor = new ForceDescriptor();
        forceDescriptor.setFaction(factionKey);
        forceDescriptor.setYear(3067);
        forceDescriptor.setUnitType(unitType);
        forceDescriptor.setEchelon(ECHELON_BRIGADE_AND_AIR_WING);
        return ruleset.findForceNode(forceDescriptor);
    }
}
