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

package megamek.common.util;

import megamek.client.ui.dialogs.advancedsearch.ASAdvancedSearchPanel;
import megamek.client.ui.dialogs.advancedsearch.MekSearchFilter;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * A simple random lance creator similar to MM's random army "BV" tab, but generalized to allow creating forces of
 * various classes (Entity, MekSummary, AlphaStrikeElement) and with any "strength" function, i.e. forces can also be
 * created to match a total tonnage or cost or even a total Alpha Strike IF value instead of PV/BV. This creator doesn't
 * create any force structure, therefore it is called "lance" creator but it can be used for any amount of units.
 */
@SuppressWarnings("unused") // Utility class
public class SimpleRandomLanceCreator<T> {

    private static final int MAX_ITERATIONS = 100_000;

    private static final Random rnd = new Random();
    private final ToDoubleFunction<T> strengthFunction;

    /**
     * @return A random army creator for AlphaStrikeElement units. It uses the point value to balance the force. This
     *       army creator requires a list of AlphaStrikeElements as available units; to work with the cache, use
     *       {@link #mekSummaryWithPv()} instead.
     */
    public static SimpleRandomLanceCreator<AlphaStrikeElement> forAlphaStrike() {
        return new SimpleRandomLanceCreator<>(AlphaStrikeElement::getStrength);
    }

    /**
     * @return A random army creator for MekSummary units. It uses the Alpha Strike point value to balance the force and
     *       requires a roster of MekSummary entries that can be generated e.g. from an advanced search panel. This is
     *       the standard generator for Alpha Strike units.
     */
    public static SimpleRandomLanceCreator<MekSummary> mekSummaryWithPv() {
        return new SimpleRandomLanceCreator<>(MekSummary::getPointValue);
    }

    /**
     * @return A random army creator for MekSummary units. It uses the Total Warfare battle value to balance the force
     *       and requires a roster of MekSummary entries that can be generated e.g. from an advanced search panel. This
     *       is the standard generator for TW units.
     */
    public static SimpleRandomLanceCreator<MekSummary> mekSummaryWithBv() {
        return new SimpleRandomLanceCreator<>(MekSummary::getBV);
    }

    /**
     * @return A random army creator for MekSummary units. It uses the c-bill cost to balance the force (i.e., the
     *       strength of the units in battle value is not considered).
     */
    public static SimpleRandomLanceCreator<MekSummary> forCost() {
        return new SimpleRandomLanceCreator<>(ms -> (double) ms.getCost());
    }

    /**
     * @return A random army creator for MekSummary units. It uses the weight to balance the force (i.e., the strength
     *       of the units in battle value is not considered).
     */
    public static SimpleRandomLanceCreator<MekSummary> forTonnage() {
        return new SimpleRandomLanceCreator<>(MekSummary::getTons);
    }

    /**
     * Creates a random army creator for the given class parameter of unit that uses the given strength function to
     * balance created forces. The class parameter gives the type of unit that will be selected from a given roster,
     * e.g. MekSummary, Entity or AlphaStrikeElement. Other classes can be used but a roster of available units must be
     * created somehow and the MekSummaryCache is the most easily available roster. The strength function must act on
     * the given class of units and will typically be the PV or BV but other functions can be used as well, e.g. the
     * cost or weight or even more exotic values like the sum of the Alpha Strike IF and ARTx values (MekSummaries
     * contain both TW and AS values). Created forces will be chosen so that the sum of the function result for the
     * units falls into a given range.
     *
     * @param strengthFunction A method reference that maps a unit to an int value, such as MekSummary::getBV
     */
    public SimpleRandomLanceCreator(ToDoubleFunction<T> strengthFunction) {
        this.strengthFunction = strengthFunction;
    }

    /**
     * Returns a list of units taken only from the given list of available units; the returned list has the given unit
     * count and is close to the given target strength as determined by the strength function used in the constructor
     * (e.g. MekSummary::getBV). The strengthMargin is the tolerance for the total strength; a unit list will be
     * returned as soon as its total strength is not farther away from the target strength than the margin (both
     * directions). A margin of 0 can be used, but will lead to longer searches. The search is always terminated after a
     * number of tries and a force is then returned even if it falls outside the margin.
     *
     * @param available      A list of units to choose from (when empty, an empty list is returned)
     * @param unitCount      The number of units in the resulting list
     * @param targetStrength The total strength that the resulting units should have
     * @param strengthMargin The maximum deviation from the target to allow
     *
     * @return A list of units matching the parameters
     */
    public List<T> buildForce(List<? extends T> available, int unitCount, int targetStrength, int strengthMargin) {

        if (available.isEmpty() || unitCount < 1) {
            return Collections.emptyList();
        }

        List<T> currentResult = randomSample(available, unitCount);
        double currentStrength = currentResult.stream().mapToDouble(strengthFunction).sum();

        double temperature = currentStrength / 2.0;

        for (int iterations = 0; iterations < MAX_ITERATIONS; iterations++) {
            if (Math.abs(currentStrength - targetStrength) <= strengthMargin) {
                return currentResult; // success
            }

            int replacementIndex = rnd.nextInt(unitCount);
            T out = currentResult.get(replacementIndex);
            T in = available.get(rnd.nextInt(available.size()));

            double newStrength = currentStrength - strengthFunction.applyAsDouble(out) + strengthFunction.applyAsDouble(in);
            double delta = Math.abs(newStrength - targetStrength) - Math.abs(currentStrength - targetStrength);

            // temperature allows worsening of the result in the beginning while increasingly rejecting it later on
            if (delta <= 0 || rnd.nextDouble() < Math.exp(-delta / temperature)) {
                currentResult.set(replacementIndex, in);
                currentStrength = newStrength;
            }

            temperature *= 0.995; // lower the temperature over iterations
        }

        return currentResult; // best-so-far
    }

    private List<T> randomSample(List<? extends T> available, int unitCount) {
        List<T> result = new ArrayList<>(unitCount);
        for (int i = 0; i < unitCount; i++) {
            result.add(available.get(rnd.nextInt(available.size())));
        }
        return result;
    }

    /**
     * This helper method filters all units present in the cache by the given filters and returns the results as a list.
     * Any of the filters may be null. When all filters are null, the entire cache is returned. This can be used for any
     * army creator that works on MekSummary entries (<code>SimpleRandomLanceCreator{@literal <MekSummary>}</code>).
     *
     * @param asFilter     The Alpha Strike advanced filter
     * @param twFilter     The TW advanced filter
     * @param manualFilter A direct filter such as <code>ms -> ms.isBattleMek()</code>
     *
     * @return A list of units that match the given filters
     */
    public static List<MekSummary> advancedFilterResult(@Nullable ASAdvancedSearchPanel asFilter,
          @Nullable MekSearchFilter twFilter, @Nullable Predicate<MekSummary> manualFilter) {
        return Arrays.stream(MekSummaryCache.getInstance().getAllMeks())
              .filter(ms -> twFilter == null || MekSearchFilter.isMatch(ms, twFilter))
              .filter(ms -> asFilter == null || asFilter.matches(ms))
              .filter(ms -> manualFilter == null || manualFilter.test(ms))
              .toList();
    }
}
