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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;

import megamek.common.units.UnitRole;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RoleMix}.
 */
class RoleMixTest {

    private static RoleMix mixOf(UnitRole firstRole, int firstPercent, UnitRole secondRole, int secondPercent) {
        Map<UnitRole, Integer> percentages = new EnumMap<>(UnitRole.class);
        percentages.put(firstRole, firstPercent);
        percentages.put(secondRole, secondPercent);
        return new RoleMix(percentages);
    }

    @Test
    void emptyMixRequestsNothing() {
        assertTrue(RoleMix.EMPTY.isEmpty());
        assertEquals(0, RoleMix.EMPTY.totalPercent());
        assertTrue(RoleMix.EMPTY.requestedRoles().isEmpty());
    }

    @Test
    void allZeroMixIsIndistinguishableFromEmpty() {
        // The regression guarantee: an untouched spinner grid must leave generation exactly as it was.
        RoleMix allZero = mixOf(UnitRole.BRAWLER, 0, UnitRole.SNIPER, 0);

        assertTrue(allZero.isEmpty());
        assertEquals(0, allZero.totalPercent());
    }

    @Test
    void retainsPositiveEntriesAndSumsThem() {
        RoleMix mix = mixOf(UnitRole.BRAWLER, 50, UnitRole.SNIPER, 50);

        assertFalse(mix.isEmpty());
        assertEquals(100, mix.totalPercent());
        assertEquals(50, mix.percentFor(UnitRole.BRAWLER));
        assertEquals(50, mix.percentFor(UnitRole.SNIPER));
        assertEquals(2, mix.requestedRoles().size());
    }

    @Test
    void unrequestedRoleReportsZero() {
        RoleMix mix = mixOf(UnitRole.BRAWLER, 50, UnitRole.SNIPER, 50);

        assertEquals(0, mix.percentFor(UnitRole.JUGGERNAUT));
    }

    @Test
    void partialMixLeavesARemainder() {
        // Percentages are a floor, not a partition - 60% requested leaves 40% to the normal weighted roll.
        RoleMix mix = mixOf(UnitRole.BRAWLER, 30, UnitRole.SNIPER, 30);

        assertEquals(60, mix.totalPercent());
    }

    @Test
    void percentageAbove100IsRejected() {
        Map<UnitRole, Integer> percentages = new EnumMap<>(UnitRole.class);
        percentages.put(UnitRole.BRAWLER, 101);

        assertThrows(IllegalArgumentException.class, () -> new RoleMix(percentages));
    }

    @Test
    void percentagesAreImmutableAfterConstruction() {
        Map<UnitRole, Integer> source = new EnumMap<>(UnitRole.class);
        source.put(UnitRole.BRAWLER, 50);
        RoleMix mix = new RoleMix(source);

        source.put(UnitRole.SNIPER, 50);

        assertEquals(1, mix.requestedRoles().size(), "the mix must not see later edits to the source map");
        assertEquals(50, mix.totalPercent());
    }

    @Test
    void groundAndAerospacePortionsSplitCleanly() {
        Map<UnitRole, Integer> percentages = new EnumMap<>(UnitRole.class);
        percentages.put(UnitRole.BRAWLER, 40);
        percentages.put(UnitRole.SNIPER, 20);
        percentages.put(UnitRole.INTERCEPTOR, 25);
        RoleMix mix = new RoleMix(percentages);

        RoleMix ground = mix.groundRoles();
        RoleMix aerospace = mix.aerospaceRoles();

        assertEquals(2, ground.requestedRoles().size());
        assertEquals(60, ground.totalPercent());
        assertEquals(1, aerospace.requestedRoles().size());
        assertEquals(25, aerospace.totalPercent());
    }

    @Test
    void domainPortionIsEmptyWhenNothingMatches() {
        RoleMix groundOnly = mixOf(UnitRole.BRAWLER, 50, UnitRole.SNIPER, 50);

        assertTrue(groundOnly.aerospaceRoles().isEmpty());
        assertFalse(groundOnly.groundRoles().isEmpty());
    }

    @Test
    void restrictingToEverythingReturnsTheSameInstance() {
        RoleMix mix = mixOf(UnitRole.BRAWLER, 50, UnitRole.SNIPER, 50);

        assertEquals(mix, mix.restrictedTo(role -> true));
        assertSame(mix, mix.groundRoles(), "an all-ground mix need not be copied");
    }
}
