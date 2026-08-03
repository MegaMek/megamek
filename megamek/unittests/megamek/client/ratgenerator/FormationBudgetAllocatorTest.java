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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the deterministic core of {@link FormationBudgetAllocator}: converting percentages to whole nodes,
 * ordering formations by rarity, and claiming nodes that can actually take them.
 *
 * <p>Exercised without a ruleset or a generated force, in the same way {@link WeightBudgetAllocatorTest} tests the
 * weight budget's arithmetic rather than a whole force. Whether the achieved distribution really lands near the
 * requested one is a question for a generated force, not for a unit test.</p>
 */
class FormationBudgetAllocatorTest {

    private static FormationMix mixOf(Object... namesAndPercents) {
        Map<String, Integer> percentages = new LinkedHashMap<>();
        for (int index = 0; index < namesAndPercents.length; index += 2) {
            percentages.put((String) namesAndPercents[index], (Integer) namesAndPercents[index + 1]);
        }
        return new FormationMix(percentages);
    }

    /** A node offering the named formations, each at weight 1. */
    private static ForceDescriptor node(String... formationNames) {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setUnitType(UnitType.MEK);
        Map<String, Integer> offered = new LinkedHashMap<>();
        for (String formationName : formationNames) {
            offered.put(formationName, 1);
        }
        descriptor.setEligibleFormations(offered);
        return descriptor;
    }

    private static long claimedFor(Map<ForceDescriptor, String> claimed, String formationName) {
        return claimed.values().stream().filter(formationName::equals).count();
    }

    // ===== integerQuotas =====

    @Test
    void evenSplitDividesCleanly() {
        Map<String, Integer> quotas = FormationBudgetAllocator.integerQuotas(mixOf("Battle", 50, "Fire", 50), 8);

        assertEquals(4, quotas.get("Battle"));
        assertEquals(4, quotas.get("Fire"));
    }

    @Test
    void largestRemainderKeepsTheRequestedTotal() {
        // Three at 33% over ten nodes is 9.9; rounding claims all ten rather than losing one to a rounding artefact.
        Map<String, Integer> quotas = FormationBudgetAllocator.integerQuotas(
              mixOf("Battle", 33, "Fire", 33, "Recon", 33), 10);

        assertEquals(10, quotas.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void partialMixLeavesTheRestToTheRuleset() {
        // Percentages are a floor, not a partition: 50% of twelve nodes is six, and the other six stay as rolled.
        Map<String, Integer> quotas = FormationBudgetAllocator.integerQuotas(mixOf("Battle", 30, "Fire", 20), 12);

        assertEquals(6, quotas.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void formationsRoundingToNothingAreDropped() {
        Map<String, Integer> quotas = FormationBudgetAllocator.integerQuotas(mixOf("Battle", 10), 4);

        assertNull(quotas.get("Battle"), "10% of four nodes rounds to nothing and should not be requested");
    }

    @Test
    void overSubscribedMixIsScaledBackProportionally() {
        // 80/80 cannot both be had; the ratio is preserved and the total capped at what exists.
        Map<String, Integer> quotas = FormationBudgetAllocator.integerQuotas(mixOf("Battle", 80, "Fire", 80), 10);

        assertTrue(quotas.values().stream().mapToInt(Integer::intValue).sum() <= 10,
              "quotas must not exceed the ten nodes on offer");
        assertEquals(quotas.get("Battle"), quotas.get("Fire"), "an even over-subscription stays even");
    }

    // ===== rarestFirst =====

    @Test
    void rarestFormationIsOrderedFirst() {
        List<ForceDescriptor> tweakable = List.of(
              node("Battle", "Fire"), node("Battle", "Recon"), node("Battle", "Fire", "Hunter"));

        List<String> ordered = FormationBudgetAllocator.rarestFirst(tweakable, List.of("Battle", "Hunter"));

        assertEquals("Hunter", ordered.getFirst(),
              "Hunter fits one node and Battle three, so Hunter must choose first");
    }

    // ===== assignFormation =====

    @Test
    void onlyNodesOfferingTheFormationAreClaimed() {
        ForceDescriptor offersIt = node("Battle", "Fire");
        ForceDescriptor doesNot = node("Recon", "Pursuit");
        Map<ForceDescriptor, String> claimed = new HashMap<>();

        int placed = FormationBudgetAllocator.assignFormation(List.of(offersIt, doesNot), claimed, "Battle", 2);

        assertEquals(1, placed, "only one node can take a Battle formation");
        assertEquals("Battle", claimed.get(offersIt));
        assertNull(claimed.get(doesNot), "a node whose rule never offered Battle must not be given one");
    }

    @Test
    void mostConstrainedNodeIsClaimedFirst() {
        // The two-option node is hard to use for anything else; the five-option node can still serve later
        // requests, so spending it first would strand the narrow one.
        ForceDescriptor narrow = node("Battle", "Fire");
        ForceDescriptor wide = node("Battle", "Fire", "Recon", "Pursuit", "Hunter");
        Map<ForceDescriptor, String> claimed = new HashMap<>();

        FormationBudgetAllocator.assignFormation(List.of(wide, narrow), claimed, "Battle", 1);

        assertEquals("Battle", claimed.get(narrow));
        assertNull(claimed.get(wide), "the flexible node should be left for a later request");
    }

    @Test
    void anAlreadyClaimedNodeIsNotClaimedTwice() {
        ForceDescriptor shared = node("Battle", "Fire");
        Map<ForceDescriptor, String> claimed = new HashMap<>();

        FormationBudgetAllocator.assignFormation(List.of(shared), claimed, "Battle", 1);
        int firePlaced = FormationBudgetAllocator.assignFormation(List.of(shared), claimed, "Fire", 1);

        assertEquals(0, firePlaced, "the only node was already spent on Battle");
        assertEquals("Battle", claimed.get(shared));
    }

    @Test
    void quotaLargerThanTheEligibleNodesStopsAtWhatExists() {
        Map<ForceDescriptor, String> claimed = new HashMap<>();

        int placed = FormationBudgetAllocator.assignFormation(
              List.of(node("Battle"), node("Battle", "Fire")), claimed, "Battle", 5);

        assertEquals(2, placed);
        assertEquals(2, claimedFor(claimed, "Battle"));
    }

    // ===== allocate =====

    @Test
    void emptyMixLeavesTheForceUntouched() {
        // The regression guarantee: no mix means the allocator does nothing and reports nothing.
        ForceDescriptor lance = node("Battle", "Fire");
        ForceDescriptor root = new ForceDescriptor();
        root.setUnitType(UnitType.MEK);
        ArrayList<ForceDescriptor> subForces = new ArrayList<>();
        subForces.add(lance);
        root.setSubForces(subForces);

        assertNull(FormationBudgetAllocator.allocate(root));
        assertNull(lance.getFormation());
    }

    @Test
    void nullRootIsHandled() {
        assertNull(FormationBudgetAllocator.allocate(null));
    }

    @Test
    void unsupportableRequestIsReportedRatherThanSilentlyDropped() {
        ForceDescriptor lance = node("Battle", "Fire");
        ForceDescriptor root = new ForceDescriptor();
        root.setUnitType(UnitType.MEK);
        ArrayList<ForceDescriptor> subForces = new ArrayList<>();
        subForces.add(lance);
        root.setSubForces(subForces);
        root.setFormationMix(mixOf("Hunter", 50));

        FormationMixReport report = FormationBudgetAllocator.allocate(root);

        assertEquals(1, report.warnings().size(),
              "a formation this force never offers must be reported, not silently ignored");
        assertTrue(report.warnings().getFirst().contains("Hunter"));
        assertEquals(0, report.totalAssigned());
    }
}
