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
package megamek.common.enums;

import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;

/**
 * The unit types a cybernetic augmentation's construction rules are written against.
 *
 * <p><i>Interstellar Operations: Alternate Eras</i> lists these against every augmentation as the
 * units whose crew may carry it - a VDNI is available to {@code BM, IM, BA, CV, SV, AF, CF}, for
 * instance, which admits battle armour and excludes conventional infantry. The abbreviations here are
 * the book's.</p>
 */
public enum AugmentedUnitType {
    /** BM - BattleMek. */
    BATTLE_MEK,
    /** IM - IndustrialMek. */
    INDUSTRIAL_MEK,
    /** BA - battle armour. */
    BATTLE_ARMOR,
    /** CI - conventional infantry. */
    CONVENTIONAL_INFANTRY,
    /** CV - combat vehicle. */
    COMBAT_VEHICLE,
    /** SV - support vehicle. */
    SUPPORT_VEHICLE,
    /** AF - aerospace fighter. */
    AEROSPACE_FIGHTER,
    /** CF - conventional fighter. */
    CONVENTIONAL_FIGHTER,
    /** ProtoMek, which the Clan enhanced imaging rules name alongside battle armour and Meks. */
    PROTOMEK,
    /** Anything the categories above do not name, such as a large craft. */
    OTHER;

    /**
     * Classifies a unit for the purpose of reading an augmentation's construction rules.
     *
     * @param entity the unit whose crew is being augmented, or {@code null}
     *
     * @return the matching type, or {@link #OTHER} when the rules name no category for it
     */
    public static AugmentedUnitType forEntity(@Nullable Entity entity) {
        if (entity == null) {
            return OTHER;
        }
        if (entity instanceof Mek mek) {
            return mek.isIndustrial() ? INDUSTRIAL_MEK : BATTLE_MEK;
        }
        if (entity instanceof Infantry infantry) {
            // Battle armour is infantry to the engine but a separate category to the rules, and the
            // difference decides real cases - a battle armour trooper may carry a neural interface
            // where a foot trooper may not.
            return infantry.isBattleArmor() ? BATTLE_ARMOR : CONVENTIONAL_INFANTRY;
        }
        if (entity.isProtoMek()) {
            return PROTOMEK;
        }
        if (entity.isSupportVehicle()) {
            return SUPPORT_VEHICLE;
        }
        if (entity.isVehicle()) {
            return COMBAT_VEHICLE;
        }
        if (entity.isConventionalFighter()) {
            return CONVENTIONAL_FIGHTER;
        }
        if (entity.isAerospaceFighter()) {
            return AEROSPACE_FIGHTER;
        }
        return OTHER;
    }

    /**
     * @return {@code true} if a warrior of this type fights with their own body rather than through a
     *       machine, which is what decides whether an implant acting on the body does anything
     */
    public boolean fightsOnFoot() {
        return (this == CONVENTIONAL_INFANTRY) || (this == BATTLE_ARMOR);
    }
}
