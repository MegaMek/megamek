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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.loaders.MekSummary;
import org.junit.jupiter.api.Test;

/**
 * Pins how {@link UnitVariety} scales a unit table's weights. These run without the unit tables: the factors are
 * plain arithmetic, and the draw's odds follow from them, so no die is rolled.
 */
class UnitVarietyTest {

    private static final double TOLERANCE = 1e-9;

    private static MekSummary unit(String chassis, String model) {
        MekSummary summary = new MekSummary();
        summary.setChassis(chassis);
        summary.setName(chassis + " " + model);
        return summary;
    }

    /** A variety with nothing recorded yet, made the way generation makes one. */
    private static UnitVariety freshVariety() {
        ForceDescriptor lance = new ForceDescriptor();
        lance.setUnitVarietyBias(true);
        UnitVariety variety = UnitVariety.forGeneration(lance);
        assertNotNull(variety);
        return variety;
    }

    // ===== Factors =====

    @Test
    void weightFactor_leavesAFreshUnitAlone() {
        assertEquals(1.0, UnitVariety.weightFactor(0, 0, 0), TOLERANCE);
    }

    @Test
    void weightFactor_penalisesAnExactRepeatInTheFormationHardest() {
        assertEquals(UnitVariety.SAME_MODEL_IN_FORMATION, UnitVariety.weightFactor(1, 0, 0), TOLERANCE);
        assertEquals(Math.pow(UnitVariety.SAME_MODEL_IN_FORMATION, 2), UnitVariety.weightFactor(2, 0, 0), TOLERANCE);
    }

    @Test
    void weightFactor_penalisesAnotherVariantOfTheChassisLess() {
        assertEquals(UnitVariety.SAME_CHASSIS_IN_FORMATION, UnitVariety.weightFactor(0, 1, 0), TOLERANCE);
        assertTrue(UnitVariety.weightFactor(0, 1, 0) > UnitVariety.weightFactor(1, 0, 0),
              "a different variant of the chassis is more welcome than the same model again");
    }

    @Test
    void weightFactor_penalisesTheChassisElsewhereInTheCommandLightly_downToAFloor() {
        assertEquals(UnitVariety.SAME_CHASSIS_ELSEWHERE, UnitVariety.weightFactor(0, 0, 1), TOLERANCE);
        assertEquals(UnitVariety.ELSEWHERE_FLOOR, UnitVariety.weightFactor(0, 0, 20), TOLERANCE,
              "a faction's signature unit keeps turning up however often the command already has it");
    }

    @Test
    void weightFactor_combinesFormationAndCommandPenalties() {
        assertEquals(UnitVariety.SAME_MODEL_IN_FORMATION * UnitVariety.SAME_CHASSIS_ELSEWHERE,
              UnitVariety.weightFactor(1, 0, 1), TOLERANCE);
    }

    // ===== Draws =====

    @Test
    void drawWeights_countUnitsDrawnEarlierInTheSameCallAsFormationMates() {
        MekSummary atlas = unit("Atlas", "AS7-D");
        MekSummary atlasK = unit("Atlas", "AS7-K");
        MekSummary archer = unit("Archer", "ARC-2R");

        UnitVariety.DrawWeights weights = freshVariety().weightsForDraw(List.of(atlas));

        assertEquals(UnitVariety.SAME_MODEL_IN_FORMATION, weights.factor(atlas), TOLERANCE);
        assertEquals(UnitVariety.SAME_CHASSIS_IN_FORMATION, weights.factor(atlasK), TOLERANCE);
        assertEquals(1.0, weights.factor(archer), TOLERANCE);
    }

    @Test
    void aTopHeavyTableIsMuchLessLikelyToRepeatItsCommonestUnit() {
        // A table where one unit carries 80% of the weight, after that unit has been drawn once for the lance.
        MekSummary common = unit("Common", "A");
        List<MekSummary> table = List.of(common, unit("Rare", "B"), unit("Rarer", "C"), unit("Rarest", "D"));
        int[] baseWeights = { 800, 100, 50, 50 };

        UnitVariety.DrawWeights weights = freshVariety().weightsForDraw(List.of(common));
        double[] scaled = new double[table.size()];
        double total = 0;
        for (int index = 0; index < table.size(); index++) {
            scaled[index] = baseWeights[index] * weights.factor(table.get(index));
            total += scaled[index];
        }

        double repeatChance = scaled[0] / total;
        assertTrue(repeatChance < 0.3, "the commonest unit repeats far less than its 80% share: " + repeatChance);
        assertTrue(repeatChance > 0.0, "but it is never ruled out");
    }

    @Test
    void pickIndex_landsWhereTheRollFalls() {
        int[] weights = { 3, 0, 5, 2 };
        assertEquals(0, UnitTable.pickIndex(weights, 0));
        assertEquals(0, UnitTable.pickIndex(weights, 2));
        assertEquals(2, UnitTable.pickIndex(weights, 3), "a zero weight is never landed on");
        assertEquals(2, UnitTable.pickIndex(weights, 7));
        assertEquals(3, UnitTable.pickIndex(weights, 8));
        assertEquals(3, UnitTable.pickIndex(weights, 9));
    }

    // ===== Scope =====

    @Test
    void forGeneration_isOffUnlessTheForceAsksForIt() {
        assertNull(UnitVariety.forGeneration(new ForceDescriptor()));
    }

    @Test
    void forGeneration_followsAnAncestorsRequest() {
        ForceDescriptor command = new ForceDescriptor();
        command.setUnitVarietyBias(true);
        ForceDescriptor lance = command.createChild(0);
        command.addSubForce(lance);

        assertNotNull(UnitVariety.forGeneration(lance), "an added or re-rolled formation keeps its command's variety");
    }

    @Test
    void runFor_leavesNothingActiveBehind_evenWhenGenerationFails() {
        UnitVariety variety = freshVariety();
        ForceDescriptor force = new ForceDescriptor();

        assertThrows(IllegalStateException.class, () -> variety.runFor(force, () -> {
            assertNotNull(UnitVariety.active());
            throw new IllegalStateException("generation failed");
        }));
        assertNull(UnitVariety.active());
    }
}
