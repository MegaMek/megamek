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

import java.nio.file.Path;
import java.util.List;

import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A heavy or assault infantry battalion may get an artillery or field-gun company attached. Before #9200 the
 * "nothing attached" option only applied at Heavy, so every Assault battalion got one, and the rule was shared with
 * battle armor, so a pure battle armor battalion got a company of foot gunners.
 *
 * <p>Rolls the shipped Inner Sphere battalion's attachments directly, through the real
 * {@link SubForcesNode#generateSubForces(ForceDescriptor, boolean)}, so the test exercises the authored odds
 * without generating any units.</p>
 */
class InfantryBattalionFireSupportTest {

    private static final int ECHELON_BATTALION = 5;

    /** Enough rolls that a 50% "nothing attached" chance cannot plausibly come up empty or full. */
    private static final int ROLLS = 200;

    @TempDir
    Path temporaryDirectory;

    @Test
    void assaultInfantryBattalionSometimesGetsNoFireSupport() throws Exception {
        int withSupport = countBattalionsWithFireSupport(UnitType.INFANTRY, EntityWeightClass.WEIGHT_ASSAULT);

        assertTrue(withSupport > 0, "an Assault infantry battalion never got fire support");
        assertTrue(withSupport < ROLLS, "every Assault infantry battalion got fire support");
    }

    @Test
    void heavyInfantryBattalionStillSometimesGetsFireSupport() throws Exception {
        int withSupport = countBattalionsWithFireSupport(UnitType.INFANTRY, EntityWeightClass.WEIGHT_HEAVY);

        assertTrue(withSupport > 0, "a Heavy infantry battalion never got fire support");
        assertTrue(withSupport < ROLLS, "every Heavy infantry battalion got fire support");
    }

    @Test
    void battleArmorBattalionNeverGetsFootFireSupport() throws Exception {
        int withSupport = countBattalionsWithFireSupport(UnitType.BATTLE_ARMOR, EntityWeightClass.WEIGHT_ASSAULT);

        assertEquals(0, withSupport, "a battle armor battalion got a foot artillery or field-gun company");
    }

    /**
     * Rolls the attachments of an Inner Sphere battalion {@link #ROLLS} times.
     *
     * @return how many of the rolls attached an artillery or field-gun company
     */
    private int countBattalionsWithFireSupport(int unitType, int weightClass) throws Exception {
        Ruleset ruleset = ShippedRulesetLoader.load("IS.xml", "IS", temporaryDirectory);
        ForceNode battalionRule = ruleset.findForceNode(battalion(unitType, weightClass));
        assertNotNull(battalionRule, "no IS battalion rule for unit type " + unitType);

        int withSupport = 0;
        for (int roll = 0; roll < ROLLS; roll++) {
            ForceDescriptor battalion = battalion(unitType, weightClass);
            for (SubForcesNode attachedBlock : battalionRule.attached) {
                if (!attachedBlock.matches(battalion)) {
                    continue;
                }
                List<ForceDescriptor> attachments = attachedBlock.generateSubForces(battalion, true);
                if (hasFireSupport(attachments)) {
                    withSupport++;
                }
            }
        }
        return withSupport;
    }

    private static boolean hasFireSupport(List<ForceDescriptor> attachments) {
        for (ForceDescriptor attachment : attachments) {
            if (attachment.getRoles().contains(MissionRole.ARTILLERY)
                  || attachment.getRoles().contains(MissionRole.FIELD_GUN)) {
                return true;
            }
        }
        return false;
    }

    private static ForceDescriptor battalion(int unitType, int weightClass) {
        ForceDescriptor battalion = new ForceDescriptor();
        battalion.setFaction("IS");
        battalion.setYear(3067);
        battalion.setUnitType(unitType);
        battalion.setEchelon(ECHELON_BATTALION);
        battalion.setWeightClass(weightClass);
        return battalion;
    }
}
