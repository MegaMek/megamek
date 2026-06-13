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

import java.util.HashMap;
import java.util.Map;

/**
 * A per-cluster-type weight-class budget, parsed from a {@code <weightTarget>} block on a CLUSTER force node.
 *
 * <p>The block names a target distribution of element weight classes for one unit type, for example
 * {@code <weightTarget unitType="Mek" spread="8">A:50,H:42,M:8,L:0</weightTarget>}. The {@link WeightBudgetAllocator}
 * reads it after the force tree is built and reshapes the cluster's element weights to match the target on average,
 * placing heavier units in heavier-role stars.</p>
 *
 * <p>This is a transient build-time value; it is never serialized into a saved game.</p>
 *
 * @param unitType the unit type this target governs (see {@link megamek.common.units.UnitType})
 * @param spread   the +/- percentage-point soft variation allowed per cluster around the target
 * @param pct      target percentage by weight-class int ({@link megamek.common.units.EntityWeightClass})
 */
record WeightTarget(int unitType, double spread, Map<Integer, Double> pct) {

    /** Default soft spread (percentage points) when a block omits the spread attribute. */
    static final double DEFAULT_SPREAD = 8.0;

    /** Default unit type for a block that omits the unitType attribute. */
    static final String DEFAULT_UNIT_TYPE = "Mek";

    /**
     * Builds a target from a {@code <weightTarget>} node, or {@code null} if it has no usable content. The node's
     * {@code if*} predicates are assumed to have already matched the descriptor.
     *
     * @param node a parsed {@code <weightTarget>} value node
     *
     * @return the parsed target, or {@code null} when the text holds no valid {@code CODE:pct} pairs
     */
    static WeightTarget fromNode(ValueNode node) {
        String content = node.getContent();
        if ((content == null) || content.isBlank()) {
            return null;
        }
        int unitType = ModelRecord.parseUnitType(node.assertions.getProperty("unitType", DEFAULT_UNIT_TYPE));
        double spread;
        try {
            spread = Double.parseDouble(node.assertions.getProperty("spread", String.valueOf(DEFAULT_SPREAD)));
        } catch (NumberFormatException ex) {
            spread = DEFAULT_SPREAD;
        }
        Map<Integer, Double> pct = new HashMap<>();
        for (String token : content.split(",")) {
            String trimmed = token.trim();
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            int weightClass = ForceDescriptor.decodeWeightClass(trimmed.substring(0, colon).trim());
            if (weightClass < 0) {
                continue;
            }
            try {
                pct.put(weightClass, Double.parseDouble(trimmed.substring(colon + 1).trim()));
            } catch (NumberFormatException ignored) {
                // skip a malformed pct value; other tokens still count
            }
        }
        return pct.isEmpty() ? null : new WeightTarget(unitType, spread, pct);
    }
}
