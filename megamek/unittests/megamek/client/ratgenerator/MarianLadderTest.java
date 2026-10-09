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
 * The Marian Hegemony uses one ladder for every arm: Century 5, Maniple 10 (2 Centuries), Cohort 30 (3 Maniples), as
 * given in the official rules answer on the BattleTech forum (10 Feb 2014,
 * https://www.battletech.com/forums/index.php/topic,36976.msg856722.html#msg856722). Fighters were labelled
 * "Wing", infantry and battle armor "Company", and a battle armor Maniple held 4 Centuries (MegaMek/mekhq#10376 item
 * 19).
 */
class MarianLadderTest {

    private static final int ECHELON_COMPANY = 4;
    private static final int ECHELON_WING = 5;

    @TempDir
    Path temporaryDirectory;

    private Field rulesetTable;
    private Object previousRulesets;
    private Ruleset marian;

    @BeforeEach
    void installMarianRules() throws Exception {
        rulesetTable = Ruleset.class.getDeclaredField("rulesets");
        rulesetTable.setAccessible(true);
        previousRulesets = rulesetTable.get(null);
        marian = ShippedRulesetLoader.load("MH.xml", "MH", temporaryDirectory);
        // Applying a rule looks the ruleset up in the global table, so this test's copy is installed there.
        HashMap<String, Ruleset> isolated = new HashMap<>();
        isolated.put("MH", marian);
        rulesetTable.set(null, isolated);
    }

    @AfterEach
    void restoreRulesets() throws Exception {
        rulesetTable.set(null, previousRulesets);
    }

    @Test
    void thirtyFightersAreACohort() {
        ForceNode rule = rule(UnitType.AEROSPACE_FIGHTER, ECHELON_WING);

        assertEquals("Cohort", rule.getEchelonName());
    }

    @Test
    void aBattleArmorManipleIsTwoCenturies() {
        ForceDescriptor maniple = request(UnitType.BATTLE_ARMOR, ECHELON_COMPANY);
        ForceNode rule = marian.findForceNode(maniple);
        assertNotNull(rule);

        rule.apply(maniple);

        assertEquals("Maniple", rule.getEchelonName());
        assertEquals(2, maniple.getSubForces().size(), "a Maniple is 2 Centuries of 5 squads");
    }

    @Test
    void anInfantryManipleIsNamedForWhatItIs() {
        assertEquals("Maniple", rule(UnitType.INFANTRY, ECHELON_COMPANY).getEchelonName());
    }

    private ForceNode rule(int unitType, int echelon) {
        ForceNode rule = marian.findForceNode(request(unitType, echelon));
        assertNotNull(rule, "no Marian rule for " + UnitType.getTypeName(unitType) + " at echelon " + echelon);
        return rule;
    }

    private static ForceDescriptor request(int unitType, int echelon) {
        ForceDescriptor request = new ForceDescriptor();
        request.setFaction("MH");
        request.setYear(3067);
        request.setUnitType(unitType);
        request.setEchelon(echelon);
        request.setRating("C");
        return request;
    }
}
