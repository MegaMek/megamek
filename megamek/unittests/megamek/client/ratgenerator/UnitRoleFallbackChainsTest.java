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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import megamek.common.units.UnitRole;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UnitRoleFallbackChains}.
 *
 * <p>The chains encode published Alpha Strike Companion role relationships, so these tests guard the structural
 * invariants a substitution must never break rather than re-asserting the ranking itself.</p>
 */
class UnitRoleFallbackChainsTest {

    private static final List<UnitRole> GROUND_ROLES = List.of(UnitRole.AMBUSHER, UnitRole.BRAWLER,
          UnitRole.JUGGERNAUT, UnitRole.MISSILE_BOAT, UnitRole.SCOUT, UnitRole.SKIRMISHER,
          UnitRole.SNIPER, UnitRole.STRIKER);

    private static final List<UnitRole> AEROSPACE_FIGHTER_ROLES = List.of(UnitRole.ATTACK_FIGHTER,
          UnitRole.DOGFIGHTER, UnitRole.FAST_DOGFIGHTER, UnitRole.FIRE_SUPPORT, UnitRole.INTERCEPTOR);

    @Test
    void everyRoleReturnsANonNullChain() {
        for (UnitRole role : UnitRole.values()) {
            assertNotNull(UnitRoleFallbackChains.chainFor(role), "chain for " + role + " must not be null");
        }
    }

    @Test
    void groundChainsStayWithinGroundRoles() {
        // A ground unit can never be substituted with an aerospace role: UnitRole.isAvailableTo would reject it.
        for (UnitRole role : GROUND_ROLES) {
            for (UnitRole substitute : UnitRoleFallbackChains.chainFor(role)) {
                assertTrue(substitute.isGroundRole(),
                      role + " falls back to " + substitute + ", which is not a ground role");
            }
        }
    }

    @Test
    void aerospaceChainsStayWithinAerospaceRoles() {
        for (UnitRole role : AEROSPACE_FIGHTER_ROLES) {
            for (UnitRole substitute : UnitRoleFallbackChains.chainFor(role)) {
                assertTrue(substitute.isAerospaceRole(),
                      role + " falls back to " + substitute + ", which is not an aerospace role");
            }
        }
    }

    @Test
    void chainNeverContainsTheRoleItself() {
        for (UnitRole role : UnitRole.values()) {
            assertFalse(UnitRoleFallbackChains.chainFor(role).contains(role),
                  role + " must not list itself as its own substitute");
        }
    }

    @Test
    void chainEntriesAreDistinct() {
        for (UnitRole role : UnitRole.values()) {
            List<UnitRole> chain = UnitRoleFallbackChains.chainFor(role);
            assertEquals(chain.size(), new HashSet<>(chain).size(),
                  "chain for " + role + " repeats a substitute");
        }
    }

    @Test
    void everyCombatRoleHasThreeSubstitutes() {
        for (UnitRole role : GROUND_ROLES) {
            assertEquals(3, UnitRoleFallbackChains.chainFor(role).size(),
                  "ground role " + role + " should have three ranked substitutes");
        }
        for (UnitRole role : AEROSPACE_FIGHTER_ROLES) {
            assertEquals(3, UnitRoleFallbackChains.chainFor(role).size(),
                  "aerospace role " + role + " should have three ranked substitutes");
        }
    }

    @Test
    void transportHasNoSubstitute() {
        // Transport names a class of unit rather than a battlefield posture, and only five units in the
        // shipped data carry it, so no fighter role stands in for it.
        assertTrue(UnitRoleFallbackChains.chainFor(UnitRole.TRANSPORT).isEmpty());
    }

    @Test
    void placeholderRolesHaveNoSubstitute() {
        assertTrue(UnitRoleFallbackChains.chainFor(UnitRole.UNDETERMINED).isEmpty());
        assertTrue(UnitRoleFallbackChains.chainFor(UnitRole.NONE).isEmpty());
    }

    @Test
    void canonicalPairingsAreRankedFirst() {
        // The Alpha Strike Companion text names these relationships explicitly, and they are the substitutions
        // that rescue the weight bands where the requested role does not exist at all.
        assertEquals(UnitRole.STRIKER, UnitRoleFallbackChains.chainFor(UnitRole.SKIRMISHER).getFirst(),
              "Skirmishers are described as commanders of lighter Strikers");
        assertEquals(UnitRole.SNIPER, UnitRoleFallbackChains.chainFor(UnitRole.MISSILE_BOAT).getFirst(),
              "Missile Boats are described as similar to Snipers");
        assertEquals(UnitRole.FAST_DOGFIGHTER, UnitRoleFallbackChains.chainFor(UnitRole.INTERCEPTOR).getFirst(),
              "Fast Dogfighters are described as second shell interceptors");
    }
}
