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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * What a {@link FormationMix} asked of a force against what the force could supply.
 *
 * <p>The two rarely match exactly. A formation offered by only a handful of a force's nodes cannot take a large share
 * however much is requested, and a mix that reaches for more than the force has to give will fall short. That is the
 * honest answer rather than a failure, but it has to be visible: a mix that silently delivers half of what was asked
 * reads to the player as a mix that did nothing, which is the complaint the whole feature exists to answer.</p>
 *
 * <p>This is a transient build-time value; it is never serialized into a saved game.</p>
 *
 * @param preview        what the force offered before anything was reassigned
 * @param requestedNodes formations asked for, in whole nodes, after converting percentages
 * @param assignedNodes  formations actually assigned, in whole nodes
 * @param warnings       one entry per request that could not be met in full, ready for display
 */
public record FormationMixReport(FormationMixPreview preview,
      Map<String, Integer> requestedNodes,
      Map<String, Integer> assignedNodes,
      List<String> warnings) {

    /** Canonical constructor, taking defensive immutable copies ordered by formation name. */
    public FormationMixReport {
        requestedNodes = Collections.unmodifiableMap(new TreeMap<>(requestedNodes));
        assignedNodes = Collections.unmodifiableMap(new TreeMap<>(assignedNodes));
        warnings = List.copyOf(warnings);
    }

    /**
     * @return the formations the mix asked for in total, in nodes
     */
    public int totalRequested() {
        return requestedNodes.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * @return the formations the mix actually claimed in total, in nodes
     */
    public int totalAssigned() {
        return assignedNodes.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * @return {@code true} when every requested formation was placed in full
     */
    public boolean wasFullyMet() {
        return warnings.isEmpty() && (totalAssigned() >= totalRequested());
    }

    /**
     * How many nodes of the given formation were assigned.
     *
     * @param formationName the formation type to look up
     *
     * @return the node count, or {@code 0} if none were assigned
     */
    public int assignedFor(String formationName) {
        return (formationName == null) ? 0 : assignedNodes.getOrDefault(formationName.trim(), 0);
    }

    /**
     * The share of the force's tweakable formations one type ended up with.
     *
     * @param formationName the formation type to look up
     *
     * @return the achieved percentage, or {@code 0} when the force had nothing to tweak
     */
    public double achievedSharePercent(String formationName) {
        int tweakable = preview.tweakableNodes();
        return (tweakable == 0) ? 0.0 : (100.0 * assignedFor(formationName) / tweakable);
    }
}
