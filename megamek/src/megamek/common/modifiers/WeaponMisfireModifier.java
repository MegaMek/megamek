/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.modifiers;

import java.util.*;
import java.util.function.Predicate;

public class WeaponMisfireModifier extends AbstractEquipmentModifier {

    private final Set<Integer> misfireRollResults = new HashSet<>();

    /**
     * Creates a weapon modifier that makes the weapon misfire if the die roll matches a given test.
     *
     * A modifier that makes the weapon misfire on to hit rolls of 7 or more can be created like this:
     * <pre>{@code
     * new WeaponMisfireModifier(roll -> roll >= 7);
     * }</pre>
     *
     * @param misfireTest The test of the die roll result that returns true when a misfire occurs
     */
    public WeaponMisfireModifier(Predicate<Integer> misfireTest, Reason reason) {
        super(reason);
        for (int i = 2; i <= 12; i++) {
            if (misfireTest.test(i)) {
                misfireRollResults.add(i);
            }
        }
    }

    /**
     * Creates a weapon modifier that makes the weapon misfire if the die roll matches any of the given numbers.
     *
     * @param misfireRollResults A list of numbers (2...12) that make the weapon misfire when rolled as a to-hit roll.
     */
    public WeaponMisfireModifier(Collection<Integer> misfireRollResults, Reason reason) {
        super(reason);
        this.misfireRollResults.addAll(misfireRollResults);
    }

    public boolean isMisfire(int toHitRollResult) {
        return misfireRollResults.contains(toHitRollResult);
    }
}
