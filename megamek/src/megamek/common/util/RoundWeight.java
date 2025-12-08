/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.function.BiFunction;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.ProtoMek;

/**
 * A series of rounding methods that account for floating point precision issues. These can used through static methods
 * but are implemented as an enum to make an easy way for the method itself to be stored in a field.
 */
public enum RoundWeight {
    NONE((w, e) -> w),
    /** Round up to next half ton */
    NEAREST_HALF_TON((w, e) -> Math.round(truncate(w) * 2.0) / 2.0),
    /** Round to the nearest kg */
    NEAREST_KG((w, e) -> Math.round(w * 1000.0) / 1000.0),
    /** Round to the nearest ton */
    NEAREST_TON((w, e) -> (double) Math.round(w)),
    /** Round up to next half ton */
    NEXT_HALF_TON((w, e) -> Math.ceil(truncate(w) * 2.0) / 2.0),
    /** Round up to the nearest kg (used for small SV engine and structure) */
    NEXT_KG((w, e) -> Math.ceil(truncate(w) * 1000.0) / 1000.0),
    /** Round up to the nearest ton */
    NEXT_TON((w, e) -> Math.ceil(truncate(w))),
    /** Round kg standard to next kg, ton-standard to next half ton */
    STANDARD((w, e) -> {
        if (null != e && usesKilogramStandard(e)) {
            return RoundWeight.NEXT_KG.round(w, e);
        } else {
            return RoundWeight.NEXT_HALF_TON.round(w, e);
        }
    }),
    /** Round kg standard to nearest kg, ton-standard to nearest half ton (used for SV engine weight per TM p.133) */
    SV_ENGINE((w, e) -> {
        if (null != e && usesKilogramStandard(e)) {
            return RoundWeight.NEAREST_KG.round(w, e);
        } else {
            return RoundWeight.NEAREST_HALF_TON.round(w, e);
        }
    });

    private final BiFunction<Double, Entity, Double> calc;

    RoundWeight(BiFunction<Double, Entity, Double> apply) {
        this.calc = apply;
    }

    /**
     * Convenience method for checking whether an {@link Entity} uses the kilogram standard in its construction rules
     * rather than tons.
     *
     * @param entity The unit to check
     *
     * @return Whether the construction weight is measured in kilograms
     */
    public static boolean usesKilogramStandard(Entity entity) {
        return entity instanceof ProtoMek
              || entity instanceof BattleArmor
              || (entity.isSupportVehicle() && entity.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT);
    }

    /**
     * Applies the rounding operation to a weight
     *
     * @param weight The weight to be rounded, in metric tons.
     * @param entity The unit the equipment is mounted on. This is needed for operations that depend on whether the unit
     *               uses the ton or kg standard. If {@code null}, the unit is assumed to use the ton standard.
     *
     * @return The result of the rounding operation.
     */
    public double round(double weight, Entity entity) {
        return calc.apply(weight, entity);
    }

    /**
     * Chops off trailing float irregularities by rounding to the gram. Used as the first step in rounding operations
     * that round up.
     *
     * @param weight The weight to round.
     *
     * @return The weight rounded to the gram.
     */
    public static double truncate(double weight) {
        return Math.round(weight * 1000000.0) / 1000000.0;
    }

    /**
     * Rounds normally to nearest half ton
     *
     * @param weight The weight in tons
     *
     * @return The weight in tons, rounded to the closest half ton.
     */
    public static double nearestHalfTon(double weight) {
        return NEAREST_HALF_TON.round(weight, null);
    }

    /**
     * Rounds normally to nearest ton
     *
     * @param weight The weight in tons
     *
     * @return The weight in tons, rounded to the closest ton.
     */
    public static double nearestTon(double weight) {
        return NEAREST_TON.round(weight, null);
    }

    /**
     * Rounds normally to nearest kg
     *
     * @param weight The weight in tons
     *
     * @return The weight in tons, rounded to the closest kilogram.
     */
    public static double nearestKg(double weight) {
        return NEAREST_KG.round(weight, null);
    }

    /**
     * Rounds up to the next half ton.
     *
     * @param weight The weight in tons
     *
     * @return The weight in tons rounded up to the half ton
     */
    public static double nextHalfTon(double weight) {
        return NEXT_HALF_TON.round(weight, null);
    }

    /**
     * Rounds up to the next kilogram
     *
     * @param weight The weight in tons
     *
     * @return The weight in tons rounded up to the kilogram
     */
    public static double nextKg(double weight) {
        return NEXT_KG.round(weight, null);
    }

    /**
     * Rounds up to the next full ton
     *
     * @param weight The weight to round in tons
     *
     * @return The weight in tons rounded up to the full ton
     */
    public static double nextTon(double weight) {
        return NEXT_TON.round(weight, null);
    }

    /**
     * Rounds using the standard method for the {@link Entity}. For kg-standard units this rounds to the closest kg. For
     * all others this rounds up to the half ton.
     *
     * @param weight The weight to round, in tons
     * @param entity The unit the equipment is mounted on. If {@code null}, ton-standard is assumed
     *
     * @return The weight in tons, rounded as appropriate for the construction rules of the unit.
     */
    public static double standard(double weight, Entity entity) {
        return STANDARD.round(weight, entity);
    }
}
