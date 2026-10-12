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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * With an infantry class picked, the Inner Sphere infantry battalion rule must not turn the battalion into battle armor
 * and must not roll its own transport for it. Without a class, both rolls still happen.
 *
 * <p>Runs the shipped battalion rule's real {@link ForceNode#apply(ForceDescriptor)}. That looks rulesets up in
 * {@link Ruleset}'s global table, so the test puts the isolated Inner Sphere ruleset there for its own run and puts
 * the previous table back afterwards.</p>
 */
class InfantryClassRuleSkipTest {

    private static final int ECHELON_BATTALION = 5;
    private static final int ROLLS = 200;

    @TempDir
    Path temporaryDirectory;

    private Field rulesetTable;
    private Object previousRulesets;
    private Ruleset innerSphere;

    @BeforeEach
    void installInnerSphereRuleset() throws Exception {
        innerSphere = ShippedRulesetLoader.load("IS.xml", "IS", temporaryDirectory);
        rulesetTable = Ruleset.class.getDeclaredField("rulesets");
        rulesetTable.setAccessible(true);
        previousRulesets = rulesetTable.get(null);
        HashMap<String, Ruleset> isolated = new HashMap<>();
        isolated.put("IS", innerSphere);
        rulesetTable.set(null, isolated);
    }

    @AfterEach
    void restoreRulesets() throws Exception {
        rulesetTable.set(null, previousRulesets);
    }

    @Test
    void withAClassTheBattalionStaysInfantryAndKeepsNoRolledTransport() {
        for (int roll = 0; roll < ROLLS; roll++) {
            ForceDescriptor battalion = applyBattalionRule(InfantryClass.JUMP);

            assertEquals(UnitType.INFANTRY, battalion.getUnitType(), "converted to battle armor on roll " + roll);
            assertTrue(battalion.getMovementModes().isEmpty(), "rolled its own transport on roll " + roll);
        }
    }

    @Test
    void withoutAClassBothRollsStillHappen() {
        int battleArmor = 0;
        int rolledTransport = 0;
        for (int roll = 0; roll < ROLLS; roll++) {
            ForceDescriptor battalion = applyBattalionRule(null);
            if (battalion.getUnitType() == UnitType.BATTLE_ARMOR) {
                battleArmor++;
            }
            if (!battalion.getMovementModes().isEmpty()) {
                rolledTransport++;
            }
        }

        // About 1 in 12 battalions convert and about 1 in 5 roll a transport; across 200 rolls both must show up.
        assertTrue(battleArmor > 0, "the battle armor roll never happened, so the skip test proves nothing");
        assertTrue(rolledTransport > 0, "the transport roll never happened, so the skip test proves nothing");
    }

    private ForceDescriptor applyBattalionRule(InfantryClass infantryClass) {
        ForceDescriptor battalion = new ForceDescriptor();
        battalion.setFaction("IS");
        battalion.setYear(3067);
        battalion.setUnitType(UnitType.INFANTRY);
        battalion.setEchelon(ECHELON_BATTALION);
        battalion.setInfantryClass(infantryClass);
        ForceNode rule = innerSphere.findForceNode(battalion);
        assertNotNull(rule, "no Inner Sphere infantry battalion rule");
        rule.apply(battalion);
        return battalion;
    }
}
