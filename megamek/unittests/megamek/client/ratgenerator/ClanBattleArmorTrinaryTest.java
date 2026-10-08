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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.io.TempDir;

/**
 * A Clan battle armor or infantry Trinary with Target Weight on Random has to come out as three Stars. Clan Wolf,
 * Smoke Jaguar and Jade Falcon chose their Stars by weight class but never rolled one, so nothing matched and the
 * Trinary was a single squad. Jade Falcon also chose its last two Stars without an Assault option, so an Assault roll
 * gave one Star (MegaMek/mekhq#10376).
 *
 * <p>Runs each shipped rule's real {@link ForceNode#apply(ForceDescriptor)}. Clan Wolf and Smoke Jaguar borrow the
 * base Clan Stars ({@code <asParent />}), which are looked up through {@link Ruleset}'s global table, so the test
 * installs the faction's ruleset and the base Clan one for its own run and restores the table afterwards.</p>
 */
class ClanBattleArmorTrinaryTest {

    private static final int ECHELON_TRINARY = 5;
    private static final int ROLLS = 50;

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
    @CsvSource({ "CW.xml, CW, 3135, BattleArmor", "CSJ.xml, CSJ, 3060, BattleArmor",
                 "CJF.xml, CJF, 3150, BattleArmor", "CW.xml, CW, 3135, Infantry", "CSJ.xml, CSJ, 3060, Infantry",
                 "CJF.xml, CJF, 3150, Infantry" })
    void aRandomWeightTrinaryHasThreeStars(String fileName, String faction, int year, String unitTypeName)
          throws Exception {
        Ruleset clan = ShippedRulesetLoader.load("CLAN.xml", "CLAN", temporaryDirectory);
        Ruleset factionRuleset = ShippedRulesetLoader.load(fileName, faction, "CLAN", temporaryDirectory);
        HashMap<String, Ruleset> isolated = new HashMap<>();
        isolated.put("CLAN", clan);
        isolated.put(faction, factionRuleset);
        rulesetTable.set(null, isolated);

        for (int roll = 0; roll < ROLLS; roll++) {
            for (String rating : new String[] { "FL", "SL" }) {
                ForceDescriptor trinary = new ForceDescriptor();
                trinary.setFaction(faction);
                trinary.setYear(year);
                trinary.setUnitType(ModelRecord.parseUnitType(unitTypeName));
                trinary.setEchelon(ECHELON_TRINARY);
                trinary.setRating(rating);
                ForceNode rule = factionRuleset.findForceNode(trinary);
                assertNotNull(rule, "no " + faction + " " + unitTypeName + " Trinary rule");

                rule.apply(trinary);

                assertEquals(3, trinary.getSubForces().size(),
                      faction + " " + rating + " " + unitTypeName + " Trinary on roll " + roll + " (weight class "
                            + trinary.getWeightClass() + ") did not get three Stars");
            }
        }
    }
}
