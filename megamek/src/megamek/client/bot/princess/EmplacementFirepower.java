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
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.common.compute.Compute;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;

/**
 * Works out what a gun emplacement's weapons are worth against a target at a given range, so that the ground an
 * emplacement overlooks can be judged by how hard it can actually hit there rather than by whether it can merely be
 * seen.
 *
 * <p>The difference is large. A gunner of skill 4 shooting at a moving target hits about 72 times in 100 at short
 * range, 42 at medium and 17 at long, so a hex inside short range is worth roughly four hexes at long range. An
 * emplacement of medium lasers therefore wants its ground close in, and gains nothing from a view down a boulevard
 * longer than its guns reach.</p>
 */
final class EmplacementFirepower {

    /** Gunnery skill assumed when the emplacement has no crew to ask. */
    static final int DEFAULT_GUNNERY = 4;

    /** The to-hit modifier assumed for the target: one that moved a few hexes. */
    static final int TARGET_MOVEMENT_MODIFIER = 2;

    /** What a hex in view is worth to an emplacement with no weapons, so that it still prefers a wide view. */
    static final int UNARMED_VALUE = 100;

    /** The to-hit modifier for firing indirectly at a target another unit is spotting. */
    static final int INDIRECT_FIRE_MODIFIER = 1;

    /**
     * What an indirect shot is worth beside a direct one at the same odds, in hundredths: it needs a friendly unit
     * that can see the target and has nothing better to do than spot.
     */
    static final int INDIRECT_FIRE_PERCENT = 75;

    /** How far back an emplacement with no weapons stands. */
    static final int DEFAULT_STANDOFF = 4;

    private static final int MEDIUM_RANGE_MODIFIER = 2;
    private static final int LONG_RANGE_MODIFIER = 4;

    private EmplacementFirepower() {
    }

    /**
     * The ranges and punch of one weapon.
     *
     * @param damage       the damage it does on a hit; for a missile rack, the size of the rack
     * @param minimumRange its minimum range, or 0 for none
     * @param shortRange   the last hex of short range
     * @param mediumRange  the last hex of medium range
     * @param longRange    the last hex of long range
     * @param indirect     {@code true} if it can fire at a target it cannot see, as an LRM rack or a mortar can
     */
    record WeaponReach(int damage, int minimumRange, int shortRange, int mediumRange, int longRange,
          boolean indirect) {

        /**
         * @param distance the range to the target in hexes
         *
         * @return the to-hit modifier for range, or {@code null} when the target is out of range
         */
        @Nullable Integer rangeModifier(int distance) {
            if (distance > longRange) {
                return null;
            }
            int modifier = 0;
            if (distance > mediumRange) {
                modifier = LONG_RANGE_MODIFIER;
            } else if (distance > shortRange) {
                modifier = MEDIUM_RANGE_MODIFIER;
            }
            if (distance <= minimumRange) {
                modifier += (minimumRange - distance) + 1;
            }
            return modifier;
        }
    }

    /**
     * @param emplacement the emplacement
     *
     * @return the reach of each of its weapons; empty when it has none
     */
    static List<WeaponReach> weaponsOf(Entity emplacement) {
        List<WeaponReach> weapons = new ArrayList<>();
        for (Mounted<?> mounted : emplacement.getWeaponList()) {
            if (mounted.getType() instanceof WeaponType weaponType) {
                int damage = (weaponType.getDamage() > 0) ? weaponType.getDamage()
                      : Math.max(1, weaponType.getRackSize());
                boolean isIndirect = weaponType.hasIndirectFire() && !weaponType.hasFlag(WeaponType.F_ARTILLERY);
                weapons.add(new WeaponReach(damage, Math.max(0, weaponType.getMinimumRange()),
                      weaponType.getShortRange(), weaponType.getMediumRange(), weaponType.getLongRange(),
                      isIndirect));
            }
        }
        return weapons;
    }

    /**
     * @param emplacement the emplacement
     *
     * @return {@code true} if it carries artillery, which needs no line of sight and is better off out of view
     */
    static boolean isIndirectFire(Entity emplacement) {
        for (Mounted<?> mounted : emplacement.getWeaponList()) {
            if ((mounted.getType() instanceof WeaponType weaponType) && weaponType.hasFlag(WeaponType.F_ARTILLERY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param weapons the weapons, from {@link #weaponsOf}
     *
     * @return {@code true} if any of them can fire indirectly
     */
    static boolean canFireIndirectly(List<WeaponReach> weapons) {
        for (WeaponReach weapon : weapons) {
            if (weapon.indirect()) {
                return true;
            }
        }
        return false;
    }

    /**
     * How far behind the front line an emplacement belongs: the range at which its weapons do their best work,
     * weighted by how hard each one hits. Medium lasers come out at three or four hexes, so they stand in the front
     * line; large lasers and autocannon stand a band behind. A weapon that may fire indirectly is taken at its medium
     * range, so missile racks and mortars stand further back still.
     *
     * @param weapons             the weapons, from {@link #weaponsOf}
     * @param isIndirectFireInUse {@code true} if the indirect fire rule is in play
     *
     * @return the distance from the front line, in hexes, at which the emplacement fights best
     */
    static int standoff(List<WeaponReach> weapons, boolean isIndirectFireInUse) {
        if (weapons.isEmpty()) {
            return DEFAULT_STANDOFF;
        }
        int weightedRange = 0;
        int totalDamage = 0;
        for (WeaponReach weapon : weapons) {
            boolean standsBack = isIndirectFireInUse && weapon.indirect();
            weightedRange += weapon.damage() * (standsBack ? weapon.mediumRange() : weapon.shortRange());
            totalDamage += weapon.damage();
        }
        return Math.round((float) weightedRange / totalDamage);
    }

    /**
     * How close a neighbouring emplacement must stand for this one to guard it: the medium range of its weapons,
     * weighted by how hard each one hits.
     *
     * @param weapons the weapons, from {@link #weaponsOf}
     *
     * @return the distance in hexes within which the emplacement can cover a neighbour
     */
    static int guardRange(List<WeaponReach> weapons) {
        if (weapons.isEmpty()) {
            return EmplacementDeployment.DEFAULT_REACH;
        }
        int weightedRange = 0;
        int totalDamage = 0;
        for (WeaponReach weapon : weapons) {
            weightedRange += weapon.damage() * weapon.mediumRange();
            totalDamage += weapon.damage();
        }
        return Math.round((float) weightedRange / totalDamage);
    }

    /**
     * @param emplacement the emplacement
     *
     * @return the gunnery skill of its crew, or {@link #DEFAULT_GUNNERY} when it has none
     */
    static int gunneryOf(Entity emplacement) {
        return (emplacement.getCrew() == null) ? DEFAULT_GUNNERY : emplacement.getCrew().getGunnery();
    }

    /**
     * The damage the weapons can expect to do to a moving target at a range, scaled by 100 to stay in whole numbers:
     * each weapon's damage times its chance in 100 to hit.
     *
     * @param weapons  the weapons, from {@link #weaponsOf}
     * @param distance the range to the target in hexes
     * @param gunnery  the gunnery skill of the crew
     *
     * @return the value of a hex at that range; 0 when no weapon reaches it
     */
    static int valueAtRange(List<WeaponReach> weapons, int distance, int gunnery) {
        if (weapons.isEmpty()) {
            return UNARMED_VALUE;
        }
        double value = 0;
        for (WeaponReach weapon : weapons) {
            Integer rangeModifier = weapon.rangeModifier(distance);
            if (rangeModifier != null) {
                int targetNumber = gunnery + TARGET_MOVEMENT_MODIFIER + rangeModifier;
                value += weapon.damage() * Compute.oddsAbove(targetNumber);
            }
        }
        return (int) Math.round(value);
    }

    /**
     * What the weapons that can fire indirectly are worth against a moving target the emplacement cannot see, scaled
     * as {@link #valueAtRange} is and then marked down by {@link #INDIRECT_FIRE_PERCENT}.
     *
     * @param weapons  the weapons, from {@link #weaponsOf}
     * @param distance the range to the target in hexes
     * @param gunnery  the gunnery skill of the crew
     *
     * @return the value of a hex at that range under indirect fire; 0 when no indirect weapon reaches it
     */
    static int indirectValueAtRange(List<WeaponReach> weapons, int distance, int gunnery) {
        double value = 0;
        for (WeaponReach weapon : weapons) {
            Integer rangeModifier = weapon.indirect() ? weapon.rangeModifier(distance) : null;
            if (rangeModifier != null) {
                int targetNumber = gunnery + TARGET_MOVEMENT_MODIFIER + rangeModifier + INDIRECT_FIRE_MODIFIER;
                value += weapon.damage() * Compute.oddsAbove(targetNumber);
            }
        }
        return (int) Math.round((value * INDIRECT_FIRE_PERCENT) / 100);
    }
}
