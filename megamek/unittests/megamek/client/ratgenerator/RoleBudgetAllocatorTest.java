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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import megamek.client.ratgenerator.RoleBudgetAllocator.Bucket;
import megamek.common.units.UnitRole;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the deterministic core of {@link RoleBudgetAllocator}: converting percentages to whole slots,
 * ordering roles by scarcity, and routing a role's quota to the buckets that can supply it.
 *
 * <p>These exercise the allocator without a unit table, in the same way {@link WeightBudgetAllocatorTest} tests the
 * weight budget's integer maths rather than a whole generated force. Building real tables would require the RAT
 * generator and its data set, which belongs to integration testing rather than here.</p>
 */
class RoleBudgetAllocatorTest {

    private static RoleMix mix(Object... rolesAndPercents) {
        Map<UnitRole, Integer> percentages = new EnumMap<>(UnitRole.class);
        for (int index = 0; index < rolesAndPercents.length; index += 2) {
            percentages.put((UnitRole) rolesAndPercents[index], (Integer) rolesAndPercents[index + 1]);
        }
        return new RoleMix(percentages);
    }

    /**
     * Builds a bucket holding the given number of empty slots and able to supply the named roles.
     *
     * @param slotCount how many unit slots the bucket holds
     * @param available role to entry-count pairs describing what the bucket's table holds
     */
    private static Bucket bucket(int slotCount, Object... available) {
        Bucket bucket = new Bucket();
        for (int index = 0; index < slotCount; index++) {
            ForceDescriptor slot = new ForceDescriptor();
            slot.setUnitType(UnitType.MEK);
            bucket.slots.add(slot);
        }
        Map<UnitRole, Integer> counts = new EnumMap<>(UnitRole.class);
        for (int index = 0; index < available.length; index += 2) {
            counts.put((UnitRole) available[index], (Integer) available[index + 1]);
        }
        bucket.availableRoles = counts;
        return bucket;
    }

    private static long slotsAssigned(Bucket bucket, UnitRole role) {
        return bucket.slots.stream().filter(slot -> slot.getTargetUnitRole() == role).count();
    }

    // ===== integerQuotas =====

    @Test
    void evenSplitDividesCleanly() {
        Map<UnitRole, Integer> quotas = RoleBudgetAllocator.integerQuotas(
              mix(UnitRole.BRAWLER, 50, UnitRole.SNIPER, 50), 8);

        assertEquals(4, quotas.get(UnitRole.BRAWLER));
        assertEquals(4, quotas.get(UnitRole.SNIPER));
    }

    @Test
    void largestRemainderKeepsTheRequestedTotal() {
        // Three roles at 33% over ten slots is 9.9 slots; rounding claims all ten rather than losing one to a
        // rounding artefact, so the split is 4/3/3.
        Map<UnitRole, Integer> quotas = RoleBudgetAllocator.integerQuotas(
              mix(UnitRole.BRAWLER, 33, UnitRole.SNIPER, 33, UnitRole.SCOUT, 33), 10);

        int total = quotas.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(10, total, "a 99% mix over ten slots should claim all ten");
        for (UnitRole role : quotas.keySet()) {
            assertTrue((quotas.get(role) == 3) || (quotas.get(role) == 4),
                  role + " should get three or four slots, got " + quotas.get(role));
        }
    }

    @Test
    void partialMixLeavesTheRestUnclaimed() {
        // Percentages are a floor, not a partition: 60% of twelve slots is seven, and the other five roll free.
        Map<UnitRole, Integer> quotas = RoleBudgetAllocator.integerQuotas(
              mix(UnitRole.BRAWLER, 30, UnitRole.SNIPER, 30), 12);

        assertEquals(7, quotas.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void rolesRoundingToNothingAreDropped() {
        Map<UnitRole, Integer> quotas = RoleBudgetAllocator.integerQuotas(mix(UnitRole.BRAWLER, 10), 4);

        assertNull(quotas.get(UnitRole.BRAWLER), "10% of four slots rounds to nothing and should not be requested");
    }

    @Test
    void quotasNeverExceedTheSlotsAvailable() {
        // An over-subscribed mix cannot claim more slots than the force actually has.
        Map<UnitRole, Integer> quotas = RoleBudgetAllocator.integerQuotas(
              mix(UnitRole.BRAWLER, 80, UnitRole.SNIPER, 80), 6);

        assertTrue(quotas.values().stream().mapToInt(Integer::intValue).sum() <= 6,
              "quotas must not exceed the six slots on offer");
    }

    // ===== scarcestFirst =====

    @Test
    void scarcestRoleIsOrderedFirst() {
        Bucket light = bucket(4, UnitRole.SCOUT, 20, UnitRole.STRIKER, 15);
        Bucket assault = bucket(4, UnitRole.JUGGERNAUT, 25, UnitRole.SCOUT, 2);

        List<UnitRole> ordered = RoleBudgetAllocator.scarcestFirst(List.of(light, assault),
              List.of(UnitRole.SCOUT, UnitRole.JUGGERNAUT));

        assertEquals(UnitRole.JUGGERNAUT, ordered.getFirst(),
              "Juggernaut fits one bucket and Scout two, so Juggernaut must choose first");
    }

    // ===== assignRole =====

    @Test
    void rolesGoOnlyToBucketsThatCanSupplyThem() {
        Bucket light = bucket(4, UnitRole.SCOUT, 20, UnitRole.STRIKER, 15);
        Bucket assault = bucket(4, UnitRole.JUGGERNAUT, 25);

        int placed = RoleBudgetAllocator.assignRole(List.of(light, assault), UnitRole.JUGGERNAUT, 4);

        assertEquals(4, placed);
        assertEquals(0, slotsAssigned(light, UnitRole.JUGGERNAUT),
              "a Light bucket holding no Juggernauts must not be handed one");
        assertEquals(4, slotsAssigned(assault, UnitRole.JUGGERNAUT));
    }

    @Test
    void unsupportableRolePlacesNothing() {
        Bucket light = bucket(4, UnitRole.SCOUT, 20);

        int placed = RoleBudgetAllocator.assignRole(List.of(light), UnitRole.JUGGERNAUT, 4);

        assertEquals(0, placed, "no bucket can supply a Juggernaut, so none should be assigned");
        assertTrue(light.slots.stream().allMatch(slot -> slot.getTargetUnitRole() == null));
    }

    @Test
    void richestBucketIsFilledFirst() {
        Bucket sparse = bucket(4, UnitRole.SNIPER, 2);
        Bucket rich = bucket(4, UnitRole.SNIPER, 30);

        RoleBudgetAllocator.assignRole(List.of(sparse, rich), UnitRole.SNIPER, 4);

        assertEquals(4, slotsAssigned(rich, UnitRole.SNIPER),
              "the bucket whose table holds thirty Snipers should be used before the one holding two");
        assertEquals(0, slotsAssigned(sparse, UnitRole.SNIPER));
    }

    @Test
    void aSlotIsNeverAssignedTwice() {
        Bucket both = bucket(4, UnitRole.SNIPER, 10, UnitRole.BRAWLER, 10);

        RoleBudgetAllocator.assignRole(List.of(both), UnitRole.SNIPER, 3);
        int brawlersPlaced = RoleBudgetAllocator.assignRole(List.of(both), UnitRole.BRAWLER, 3);

        assertEquals(1, brawlersPlaced, "only the one slot the Snipers left should remain");
        assertEquals(3, slotsAssigned(both, UnitRole.SNIPER));
        assertEquals(1, slotsAssigned(both, UnitRole.BRAWLER));
    }

    @Test
    void quotaLargerThanTheBucketStopsAtItsSlots() {
        Bucket small = bucket(2, UnitRole.SNIPER, 10);

        int placed = RoleBudgetAllocator.assignRole(List.of(small), UnitRole.SNIPER, 5);

        assertEquals(2, placed, "a two-slot bucket cannot absorb five");
    }

    // ===== allocate =====

    @Test
    void emptyMixLeavesTheForceUntouched() {
        // The regression guarantee: no mix means the allocator does nothing at all and reports nothing.
        ForceDescriptor leaf = new ForceDescriptor();
        leaf.setUnitType(UnitType.MEK);
        ForceDescriptor lance = new ForceDescriptor();
        lance.setUnitType(UnitType.MEK);
        lance.getSubForces().add(leaf);

        assertNull(RoleBudgetAllocator.allocate(lance));
        assertNull(leaf.getTargetUnitRole());
    }

    @Test
    void nullRootIsHandled() {
        assertNull(RoleBudgetAllocator.allocate(null));
    }
}
