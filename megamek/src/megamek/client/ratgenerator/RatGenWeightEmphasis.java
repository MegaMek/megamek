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

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.EntityWeightClass;

/**
 * Optional, hidden Force Generator tuning: biases weight-composition rolls toward a formation's named weight class for
 * the extreme classes only. Light formations pull lighter; Assault formations pull heavier; Medium and Heavy are left
 * on the canon (Total Warfare p.265) distribution.
 *
 * <p>Controlled by two hidden client settings (see {@code ClientPreferences}):</p>
 * <ul>
 *   <li><b>scope</b>: 0 = off, 1 = leaf only (the lance/star -> element roll), 2 = full cascade (every echelon).</li>
 *   <li><b>magnitude</b>: the size of the +/- shift applied to the underlying 1D6 curve (1 or 2).</li>
 * </ul>
 *
 * <p>The shift is applied to the cumulative selection position over the matching options (sorted by their average
 * element weight), reproducing a 1D6 +/- modifier: a +1 shifts the pick by one sixth of the total weight toward the
 * heavier end, a -1 toward the lighter end, clamped to the available range. When off, or when the formation is not
 * Light/Assault, or when the options are not weight-composition options, callers fall back to the default uniform
 * weighted selection.</p>
 */
final class RatGenWeightEmphasis {
    /** ELEMENT echelon (see forcegenerator constants.txt). Leaf scope applies only at this level. */
    private static final int ELEMENT_ECHELON = 1;

    private RatGenWeightEmphasis() { }

    private static int scope() {
        try {
            return PreferenceManager.getClientPreferences().getForceGenWeightEmphasisScope();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int magnitude() {
        try {
            return PreferenceManager.getClientPreferences().getForceGenWeightEmphasisMagnitude();
        } catch (Exception ignored) {
            return 1;
        }
    }

    /** Direction and size of the modifier for a formation, or 0 when it should not apply. */
    private static int modifier(ForceDescriptor fd, int magnitude) {
        Integer wc = fd.getWeightClass();
        if (wc == null) {
            return 0;
        }
        if (wc == EntityWeightClass.WEIGHT_LIGHT) {
            return -magnitude;
        }
        if (wc == EntityWeightClass.WEIGHT_ASSAULT) {
            return magnitude;
        }
        return 0;
    }

    /** Average element weight of an option's weightClass list (L=1..A=4), or -1 if it is not a weight-composition option. */
    private static double averageWeight(ValueNode option) {
        String wc = option.assertions.getProperty("weightClass");
        if (wc == null || wc.isEmpty()) {
            return -1;
        }
        double sum = 0;
        int count = 0;
        for (String part : wc.split(",")) {
            int value = switch (part.trim()) {
                case "L" -> EntityWeightClass.WEIGHT_LIGHT;
                case "M" -> EntityWeightClass.WEIGHT_MEDIUM;
                case "H" -> EntityWeightClass.WEIGHT_HEAVY;
                case "A" -> EntityWeightClass.WEIGHT_ASSAULT;
                default -> -1;
            };
            if (value < 0) {
                return -1;
            }
            sum += value;
            count++;
        }
        return (count == 0) ? -1 : (sum / count);
    }

    /**
     * Returns a weight-emphasis-biased selection from the matching options, or {@code null} when emphasis does not
     * apply (off, non-extreme formation, non-weight options, or out of scope) and the caller should use the default
     * selection.
     *
     * @param matching the options that already passed {@code matches(fd)}
     * @param fd       the parent formation whose sub-forces are being chosen
     */
    static @Nullable ValueNode select(List<ValueNode> matching, ForceDescriptor fd) {
        int scope = scope();
        if (scope == 0 || matching.isEmpty()) {
            return null;
        }
        int modifier = modifier(fd, Math.max(1, magnitude()));
        if (modifier == 0) {
            return null;
        }

        // All matching options must be weight-composition options (carry a weightClass list).
        double[] averages = new double[matching.size()];
        for (int i = 0; i < matching.size(); i++) {
            averages[i] = averageWeight(matching.get(i));
            if (averages[i] < 0) {
                return null;
            }
        }

        // Leaf scope: only bias the element-generating roll.
        if (scope == 1) {
            String content = matching.get(0).getContent();
            if (content == null) {
                return null;
            }
            try {
                if (Integer.parseInt(content.replaceAll("\\D.*$", "")) != ELEMENT_ECHELON) {
                    return null;
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        // Sort ascending by average element weight, then shift the cumulative selection position.
        List<ValueNode> sorted = new ArrayList<>(matching);
        sorted.sort((a, b) -> Double.compare(averageWeight(a), averageWeight(b)));
        int total = 0;
        for (ValueNode option : sorted) {
            total += option.getWeight();
        }
        if (total <= 0) {
            return null;
        }
        double position = Compute.randomInt(total) + modifier * (total / 6.0);
        if (position < 0) {
            position = 0;
        }
        double max = total - 1.0e-9;
        if (position > max) {
            position = max;
        }
        int cumulative = 0;
        for (ValueNode option : sorted) {
            cumulative += option.getWeight();
            if (position < cumulative) {
                return option;
            }
        }
        return sorted.get(sorted.size() - 1);
    }
}
