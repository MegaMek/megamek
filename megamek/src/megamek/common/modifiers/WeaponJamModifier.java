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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This is an EquipmentModifier that jams a weapon (processed by WeaponHandler) upon certain to hit roll results. Note that the modifier
 * works for all weapons, including energy weapons. Also, multiple such modifiers can be applied to a weapon; each of those is checked and
 * the weapon is jammed if any modifier's roll results happen. A jam uses ammo and generates heat but prevents damage according to
 * Mercenaries Supplemental Update, p.138
 */
public class WeaponJamModifier extends AbstractEquipmentModifier {

    private final Set<Integer> jamRollResults = new HashSet<>();

    /**
     * Creates a weapon modifier that makes the weapon jam if the die roll matches a given test.
     *
     * A modifier that makes the weapon jam on to hit rolls of 7 or more can be created like this:
     * <pre>{@code
     * new WeaponJamModifier(roll -> roll >= 7);
     * }</pre>
     *
     * @param misfireTest The test of the die roll result that returns true when a jam occurs
     */
    public WeaponJamModifier(Predicate<Integer> misfireTest, Reason reason) {
        super(reason);
        for (int i = 2; i <= 12; i++) {
            if (misfireTest.test(i)) {
                jamRollResults.add(i);
            }
        }
    }

    /**
     * Creates a weapon modifier that makes the weapon jam if the die roll matches any of the given numbers.
     *
     * @param jamRollResults A list of numbers (2...12) that make the weapon jam when rolled as a to-hit roll.
     */
    public WeaponJamModifier(Collection<Integer> jamRollResults, Reason reason) {
        super(reason);
        this.jamRollResults.addAll(jamRollResults);
    }

    public boolean isJammed(int toHitRollResult) {
        return jamRollResults.contains(toHitRollResult);
    }
}
