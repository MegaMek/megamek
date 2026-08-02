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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import megamek.common.units.UnitRole;

/**
 * A requested distribution of {@link UnitRole} across the unit slots the Force Generator's role allocator governs.
 *
 * <p>Percentages are a floor rather than a partition: they need not sum to 100, and whatever share is left
 * unallocated is rolled by the normal weighted table with no role constraint. A mix in which every entry is zero
 * therefore reproduces the Force Generator's behaviour exactly as it was before role targeting existed, which is the
 * feature's regression guarantee.</p>
 *
 * <p>Ground and aerospace roles are disjoint and are presented to the user as two separate grids, but they are
 * carried together here because {@link UnitRole} already knows which domain each role belongs to. The allocator
 * narrows the mix to the relevant domain per force node with {@link #groundRoles()} and {@link #aerospaceRoles()}.</p>
 *
 * <p>This is a transient build-time value. It must never be referenced from an {@code Entity}, a {@code Mounted}, or
 * any other object graph that reaches a saved game: XStream 1.4 cannot deserialize records, so a record that reaches
 * saved state needs a hand-written converter in {@code SerializationHelper}. Keeping it out of that graph avoids the
 * problem entirely.</p>
 *
 * @param percentages requested share per role, keyed by role; only positive entries are retained
 */
public record RoleMix(Map<UnitRole, Integer> percentages) {

    /** A mix that requests nothing, leaving every governed slot to the normal weighted roll. */
    public static final RoleMix EMPTY = new RoleMix(Map.of());

    /**
     * Canonical constructor. Drops non-positive entries so an all-zero grid is indistinguishable from an untouched
     * one, and takes a defensive immutable copy.
     *
     * @throws IllegalArgumentException if any percentage exceeds 100
     */
    public RoleMix {
        Map<UnitRole, Integer> retained = new EnumMap<>(UnitRole.class);
        for (Map.Entry<UnitRole, Integer> entry : percentages.entrySet()) {
            int percent = (entry.getValue() == null) ? 0 : entry.getValue();
            if (percent > 100) {
                throw new IllegalArgumentException(
                      "Role percentage for " + entry.getKey() + " exceeds 100: " + percent);
            }
            if (percent > 0) {
                retained.put(entry.getKey(), percent);
            }
        }
        percentages = Collections.unmodifiableMap(retained);
    }

    /**
     * @return {@code true} when nothing is requested, so the allocator should leave the force alone entirely
     */
    public boolean isEmpty() {
        return percentages.isEmpty();
    }

    /**
     * The share of governed slots this mix accounts for. A total below 100 leaves the remainder unconstrained; a
     * total above 100 over-subscribes the force, and the allocator will run out of slots before satisfying every
     * role.
     *
     * @return the sum of all requested percentages
     */
    public int totalPercent() {
        return percentages.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * @param role the role to look up
     *
     * @return the requested percentage for the given role, or {@code 0} if it was not requested
     */
    public int percentFor(UnitRole role) {
        return percentages.getOrDefault(role, 0);
    }

    /**
     * @return the roles this mix actually asks for, in {@link UnitRole} declaration order
     */
    public Set<UnitRole> requestedRoles() {
        return percentages.keySet();
    }

    /**
     * @return the ground-role portion of this mix, empty if it requests none
     */
    public RoleMix groundRoles() {
        return restrictedTo(UnitRole::isGroundRole);
    }

    /**
     * @return the aerospace-role portion of this mix, empty if it requests none
     */
    public RoleMix aerospaceRoles() {
        return restrictedTo(UnitRole::isAerospaceRole);
    }

    /**
     * Narrows this mix to the roles matching the given test.
     *
     * @param roleFilter which roles to keep
     *
     * @return a mix holding only the matching entries; {@code this} when every entry matches
     */
    public RoleMix restrictedTo(Predicate<UnitRole> roleFilter) {
        Map<UnitRole, Integer> retained = new EnumMap<>(UnitRole.class);
        for (Map.Entry<UnitRole, Integer> entry : percentages.entrySet()) {
            if (roleFilter.test(entry.getKey())) {
                retained.put(entry.getKey(), entry.getValue());
            }
        }
        return (retained.size() == percentages.size()) ? this : new RoleMix(retained);
    }
}
