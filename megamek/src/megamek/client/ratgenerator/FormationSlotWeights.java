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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

import jakarta.annotation.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityWeightClass;

/**
 * Works out which weight classes each slot of a formation may draw from when the formation builder fills it.
 *
 * <p>The force tree gives every slot of a lance its own weight class, for example Assault, Heavy, Heavy, Heavy for
 * a Heavy lance. The formation builder used to receive the union of those classes for every slot, so each slot could
 * draw any of them and a Heavy lance could come out as four Assault units. Each slot now asks for its own class first.
 * Only when that cannot fill the formation does the caller retry with the union, which is the old behaviour and never
 * builds fewer units than before.</p>
 */
final class FormationSlotWeights {
    /** Codes for the ground weight classes, indexed by {@link EntityWeightClass} value. */
    private static final String[] GROUND_CLASS_CODES = { "UL", "L", "M", "H", "A", "SH" };

    private FormationSlotWeights() {}

    /**
     * The weight class a slot was given by the force tree.
     *
     * @param slot one element of the formation
     *
     * @return the slot's weight class, or {@code null} when the slot has none or its unit type does not use one
     */
    static @Nullable Integer ownWeightClass(ForceDescriptor slot) {
        Integer weightClass = slot.getWeightClass();
        if (!slot.useWeightClass() || (weightClass == null)) {
            return null;
        }
        if (weightClass < EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
            return null;
        }
        return weightClass;
    }

    /**
     * Every weight class the formation's slots were given: the old, shared request.
     *
     * @param slots the formation's elements
     *
     * @return the weight classes in ascending order, empty when no slot has one
     */
    static Set<Integer> pooledWeightClasses(List<ForceDescriptor> slots) {
        Set<Integer> pooled = new TreeSet<>();
        for (ForceDescriptor slot : slots) {
            Integer weightClass = ownWeightClass(slot);
            if (weightClass != null) {
                pooled.add(weightClass);
            }
        }
        return pooled;
    }

    /**
     * The weight classes one slot asks the formation builder for.
     *
     * @param slot      one element of the formation
     * @param pooled    every weight class in the formation, from {@link #pooledWeightClasses(List)}
     * @param usePooled {@code true} for the fallback request, where every slot shares the pooled classes
     *
     * @return the classes to request, or {@code null} to let the formation's own weight range decide
     */
    static @Nullable Set<Integer> requestedWeightClasses(ForceDescriptor slot, Set<Integer> pooled,
          boolean usePooled) {
        Integer own = ownWeightClass(slot);
        if (usePooled || (own == null)) {
            // A slot with no weight class of its own keeps sharing the formation's classes, as it always did.
            return pooled.isEmpty() ? null : pooled;
        }
        return Set.of(own);
    }

    /**
     * Whether a per-slot request differs from the pooled one, so that retrying with the pooled classes could find
     * units the per-slot request did not.
     *
     * @param pooled every weight class in the formation
     *
     * @return {@code true} when the slots hold more than one weight class
     */
    static boolean canFallBackToPooled(Set<Integer> pooled) {
        return pooled.size() > 1;
    }

    /**
     * Puts the drawn units in slot order. The formation builder returns units grouped by request, not by slot, so an
     * Assault, Heavy, Heavy, Heavy lance can come back with the Assault unit last. Each slot takes the first remaining
     * unit of its own weight class; a slot with no match, or no weight class, takes the first unit left.
     *
     * @param slotWeightClasses each slot's own weight class, in slot order; {@code null} entries for slots without one
     * @param units             the drawn units
     *
     * @return the units in slot order, or {@code units} unchanged when the counts differ
     */
    static List<MekSummary> alignToSlots(List<Integer> slotWeightClasses, List<MekSummary> units) {
        if (slotWeightClasses.size() != units.size()) {
            return units;
        }
        List<MekSummary> remaining = new ArrayList<>(units);
        List<MekSummary> aligned = new ArrayList<>(units.size());
        for (Integer weightClass : slotWeightClasses) {
            aligned.add(remaining.remove(indexOfWeightClass(remaining, weightClass)));
        }
        return aligned;
    }

    /**
     * Writes weight classes as the letters the force generator uses, for log lines.
     *
     * @param weightClasses the weight classes
     *
     * @return the codes joined by commas, for example {@code H,A}
     */
    static String describe(Collection<Integer> weightClasses) {
        StringJoiner codes = new StringJoiner(",");
        for (Integer weightClass : weightClasses) {
            boolean isGroundClass = (weightClass >= EntityWeightClass.WEIGHT_ULTRA_LIGHT)
                  && (weightClass < GROUND_CLASS_CODES.length);
            codes.add(isGroundClass ? GROUND_CLASS_CODES[weightClass] : String.valueOf(weightClass));
        }
        return codes.toString();
    }

    private static int indexOfWeightClass(List<MekSummary> units, @Nullable Integer weightClass) {
        if (weightClass == null) {
            return 0;
        }
        for (int index = 0; index < units.size(); index++) {
            if (units.get(index).getWeightClass() == weightClass) {
                return index;
            }
        }
        return 0;
    }
}
