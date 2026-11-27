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

import static megamek.common.units.EntityWeightClass.WEIGHT_ASSAULT;
import static megamek.common.units.EntityWeightClass.WEIGHT_HEAVY;
import static megamek.common.units.EntityWeightClass.WEIGHT_MEDIUM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import megamek.common.loaders.MekSummary;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers how a formation's slots ask the formation builder for weight classes.
 *
 * <p>A Heavy lance's slots are Assault, Heavy, Heavy, Heavy. Every slot used to ask for both classes, so any of
 * them could draw an assault unit and the lance could come out all assault (MegaMek/mekhq#11219: a Heavy lance of
 * four 80-ton Chargers).</p>
 */
class FormationSlotWeightsTest {

    @Test
    @DisplayName("Each slot of a Heavy lance asks only for its own weight class")
    void heavyLanceSlotsAskForTheirOwnClass() {
        List<ForceDescriptor> lance = lance(WEIGHT_ASSAULT, WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_HEAVY);
        Set<Integer> pooled = FormationSlotWeights.pooledWeightClasses(lance);

        assertEquals(Set.of(WEIGHT_ASSAULT), FormationSlotWeights.requestedWeightClasses(lance.get(0), pooled, false));
        for (ForceDescriptor heavySlot : lance.subList(1, 4)) {
            assertEquals(Set.of(WEIGHT_HEAVY), FormationSlotWeights.requestedWeightClasses(heavySlot, pooled, false),
                  "a heavy slot must not be able to draw an assault unit");
        }
    }

    @Test
    @DisplayName("The fallback request shares every class in the lance, as before")
    void fallbackSharesThePooledClasses() {
        List<ForceDescriptor> lance = lance(WEIGHT_ASSAULT, WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_MEDIUM);
        Set<Integer> pooled = FormationSlotWeights.pooledWeightClasses(lance);

        assertEquals(Set.of(WEIGHT_MEDIUM, WEIGHT_HEAVY, WEIGHT_ASSAULT), pooled);
        assertTrue(FormationSlotWeights.canFallBackToPooled(pooled));
        for (ForceDescriptor slot : lance) {
            assertEquals(pooled, FormationSlotWeights.requestedWeightClasses(slot, pooled, true));
        }
    }

    @Test
    @DisplayName("A lance of one weight class has nothing different to fall back to")
    void singleClassLanceHasNoFallback() {
        Set<Integer> pooled = FormationSlotWeights.pooledWeightClasses(
              lance(WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_HEAVY));

        assertFalse(FormationSlotWeights.canFallBackToPooled(pooled),
              "retrying with the same single class would only repeat the failed draw");
    }

    @Test
    @DisplayName("A slot with no weight class of its own still shares the lance's classes")
    void slotWithoutAWeightClassSharesThePool() {
        List<ForceDescriptor> lance = lance(WEIGHT_HEAVY, WEIGHT_ASSAULT);
        ForceDescriptor unweighted = slot(null);
        lance.add(unweighted);
        Set<Integer> pooled = FormationSlotWeights.pooledWeightClasses(lance);

        assertNull(FormationSlotWeights.ownWeightClass(unweighted));
        assertEquals(pooled, FormationSlotWeights.requestedWeightClasses(unweighted, pooled, false));
    }

    @Test
    @DisplayName("With no weight classes at all, the formation's own range decides")
    void noWeightClassesLeavesTheFormationRange() {
        ForceDescriptor unweighted = slot(null);
        Set<Integer> pooled = FormationSlotWeights.pooledWeightClasses(List.of(unweighted));

        assertNull(FormationSlotWeights.requestedWeightClasses(unweighted, pooled, false));
    }

    @Test
    @DisplayName("Drawn units are put back in slot order by weight class")
    void unitsAreAlignedToTheirSlots() {
        MekSummary firstHeavy = unit(WEIGHT_HEAVY);
        MekSummary secondHeavy = unit(WEIGHT_HEAVY);
        MekSummary thirdHeavy = unit(WEIGHT_HEAVY);
        MekSummary assault = unit(WEIGHT_ASSAULT);
        // The builder returns units grouped by request, so the assault unit can come back last.
        List<MekSummary> drawn = List.of(firstHeavy, secondHeavy, thirdHeavy, assault);

        List<MekSummary> aligned = FormationSlotWeights.alignToSlots(
              Arrays.asList(WEIGHT_ASSAULT, WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_HEAVY), drawn);

        assertEquals(List.of(assault, firstHeavy, secondHeavy, thirdHeavy), aligned);
    }

    @Test
    @DisplayName("Fallback units that match no slot's class still fill every slot")
    void unmatchedUnitsStillFillEverySlot() {
        MekSummary firstAssault = unit(WEIGHT_ASSAULT);
        MekSummary secondAssault = unit(WEIGHT_ASSAULT);

        List<MekSummary> aligned = FormationSlotWeights.alignToSlots(
              Arrays.asList(WEIGHT_HEAVY, null), List.of(firstAssault, secondAssault));

        assertEquals(List.of(firstAssault, secondAssault), aligned);
    }

    @Test
    @DisplayName("A short draw is returned as it came")
    void shortDrawIsLeftAlone() {
        List<MekSummary> drawn = List.of(unit(WEIGHT_HEAVY));

        assertSame(drawn, FormationSlotWeights.alignToSlots(Arrays.asList(WEIGHT_HEAVY, WEIGHT_HEAVY), drawn));
    }

    @Test
    @DisplayName("Weight classes are described by their letters")
    void weightClassesAreDescribedByLetter() {
        assertEquals("M,H,A", FormationSlotWeights.describe(List.of(WEIGHT_MEDIUM, WEIGHT_HEAVY, WEIGHT_ASSAULT)));
    }

    private static List<ForceDescriptor> lance(Integer... weightClasses) {
        List<ForceDescriptor> slots = new ArrayList<>();
        for (Integer weightClass : weightClasses) {
            slots.add(slot(weightClass));
        }
        return slots;
    }

    private static ForceDescriptor slot(Integer weightClass) {
        ForceDescriptor slot = new ForceDescriptor();
        slot.setUnitType(UnitType.MEK);
        slot.setWeightClass(weightClass);
        return slot;
    }

    private static MekSummary unit(int weightClass) {
        MekSummary mekSummary = mock(MekSummary.class);
        when(mekSummary.getWeightClass()).thenReturn(weightClass);
        return mekSummary;
    }
}
