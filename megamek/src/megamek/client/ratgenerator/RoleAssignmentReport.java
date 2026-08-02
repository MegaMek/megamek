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
import java.util.List;
import java.util.Map;

import megamek.common.units.UnitRole;

/**
 * What a {@link RoleMix} asked of a force against what the force could actually supply.
 *
 * <p>The two maps rarely match exactly. A role that does not exist at a slot's weight class cannot be assigned there
 * however much of it was requested, and the shortfall is the honest answer rather than a failure - but it has to be
 * visible, because a mix that silently delivers half of what was asked reads as a mix that did nothing.</p>
 *
 * <p>This is a transient build-time value; it is never serialized into a saved game.</p>
 *
 * @param coverage       how many of the force's slots the mix was free to govern at all
 * @param requestedSlots slots asked for per role, after converting percentages to whole slots
 * @param assignedSlots  slots actually assigned per role, including those filled by a fallback substitute
 * @param warnings       one entry per role that could not be placed as requested, ready for display
 */
public record RoleAssignmentReport(RoleCoverageReport coverage,
      Map<UnitRole, Integer> requestedSlots,
      Map<UnitRole, Integer> assignedSlots,
      List<String> warnings) {

    /** Canonical constructor, taking defensive immutable copies. */
    public RoleAssignmentReport {
        requestedSlots = immutableRoleCounts(requestedSlots);
        assignedSlots = immutableRoleCounts(assignedSlots);
        warnings = List.copyOf(warnings);
    }

    /**
     * Copies role counts into an immutable {@link EnumMap}. Built by {@code putAll} rather than the copy constructor,
     * which rejects an empty source that is not already an {@code EnumMap} - and an empty map is the ordinary case
     * for a role that was never requested.
     */
    private static Map<UnitRole, Integer> immutableRoleCounts(Map<UnitRole, Integer> counts) {
        Map<UnitRole, Integer> copy = new EnumMap<>(UnitRole.class);
        copy.putAll(counts);
        return Collections.unmodifiableMap(copy);
    }

    /**
     * @return the slots the mix asked for in total
     */
    public int totalRequested() {
        return requestedSlots.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * @return the slots the mix actually claimed in total
     */
    public int totalAssigned() {
        return assignedSlots.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * @return {@code true} when every requested slot was assigned the role that was asked for
     */
    public boolean wasFullyMet() {
        return warnings.isEmpty() && (totalAssigned() >= totalRequested());
    }
}
